package com.campushub.common.util;

import java.io.ByteArrayOutputStream;

/**
 * 图片元数据清洗（P3 §03 + P4 TRADE-01 完成标准）。
 *
 * 用途：上传图前清掉 EXIF / GPS / 拍摄设备 / XMP / Adobe Color profile 等隐私信息。
 *
 * 实现策略：**纯字节解析，不引外部 EXIF 库**。
 *   - JPEG：识别 SOI(0xFFD8) → 遍历 marker → 删除所有 APP0-APP15 (0xFFE0-0xFFEF) 段 → 到 SOS(0xFFDA) 后剩余数据原样输出
 *   - PNG：识别 8 字节签名 → 遍历 chunk → 仅保留"关键 chunk"（type 首字母大写），删除 tEXt / iTXt / zTXt / eXIf / iCCP 等
 *   - 其他/未知格式：原样返回（不阻塞业务）
 *
 * trade/wall 模块直接调：
 *   byte[] cleaned = ExifCleaner.clean(originalBytes, "image/jpeg");
 */
public final class ExifCleaner {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'
    };

    private ExifCleaner() {}

    public static byte[] clean(byte[] image, String contentType) {
        if (image == null || image.length < 4) return image;
        if (looksLikeJpeg(image, contentType)) return cleanJpeg(image);
        if (looksLikePng(image, contentType)) return cleanPng(image);
        return image;
    }

    // ───────────────────── JPEG ─────────────────────

    private static boolean looksLikeJpeg(byte[] data, String ct) {
        if (ct != null && ct.toLowerCase().contains("jpeg")) return true;
        return data.length >= 2 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8;
    }

    private static byte[] cleanJpeg(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        int i = 0;
        while (i + 1 < data.length) {
            if (data[i] != (byte) 0xFF) {
                // 数据流中间无 marker（不应发生在合法 JPEG header 段）；保险起见原样输出剩余
                out.write(data, i, data.length - i);
                return out.toByteArray();
            }
            int marker = data[i + 1] & 0xFF;

            // 无 length 的 standalone markers：SOI / EOI / TEM / RST0-7 / 0x00 padding / 0xFF padding
            if (marker == 0xD8 || marker == 0xD9 || marker == 0x01 || marker == 0x00 || marker == 0xFF
                    || (marker >= 0xD0 && marker <= 0xD7)) {
                out.write(data[i]);
                out.write(data[i + 1]);
                i += 2;
                continue;
            }

            // SOS：之后是 entropy-coded 图像数据，原样输出剩余字节
            if (marker == 0xDA) {
                out.write(data, i, data.length - i);
                return out.toByteArray();
            }

            // 普通段：length 在 marker 后 2 字节（big-endian，包含 length 自身的 2 字节）
            if (i + 4 > data.length) {
                out.write(data, i, data.length - i);
                return out.toByteArray();
            }
            int segLen = ((data[i + 2] & 0xFF) << 8) | (data[i + 3] & 0xFF);
            if (segLen < 2 || i + 2 + segLen > data.length) {
                // 段长度异常 → 原样输出剩余，避免吞数据
                out.write(data, i, data.length - i);
                return out.toByteArray();
            }
            int segEnd = i + 2 + segLen;

            // APP0-APP15 (0xE0-0xEF) 全删 —— 覆盖 EXIF (APP1) / XMP (APP1) / Adobe (APP14) / ICC (APP2) / JFIF (APP0)
            // COM (0xFE) 也删（图像注释字段，常含元信息）
            boolean shouldStrip = (marker >= 0xE0 && marker <= 0xEF) || marker == 0xFE;
            if (!shouldStrip) {
                out.write(data, i, segEnd - i);
            }
            i = segEnd;
        }
        return out.toByteArray();
    }

    // ───────────────────── PNG ─────────────────────

    private static boolean looksLikePng(byte[] data, String ct) {
        if (ct != null && ct.toLowerCase().contains("png")) {
            return data.length >= 8 && matchPngSignature(data);
        }
        return data.length >= 8 && matchPngSignature(data);
    }

    private static boolean matchPngSignature(byte[] data) {
        for (int j = 0; j < PNG_SIGNATURE.length; j++) {
            if (data[j] != PNG_SIGNATURE[j]) return false;
        }
        return true;
    }

    /**
     * PNG chunk 结构：length(4) + type(4) + data(length) + crc(4)
     * 关键 chunk（图像渲染必需）：type 首字母大写，例 IHDR / IDAT / IEND / PLTE
     * 非关键 chunk（删除）：tEXt / iTXt / zTXt / eXIf / iCCP / pHYs / tIME 等首字母小写
     */
    private static byte[] cleanPng(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        out.write(data, 0, 8);
        int i = 8;
        while (i + 12 <= data.length) {
            int chunkLen = ((data[i] & 0xFF) << 24)
                    | ((data[i + 1] & 0xFF) << 16)
                    | ((data[i + 2] & 0xFF) << 8)
                    | (data[i + 3] & 0xFF);
            if (chunkLen < 0 || i + 12 + chunkLen > data.length) break;

            int totalLen = 12 + chunkLen; // length + type + data + crc
            char typeFirstChar = (char) (data[i + 4] & 0xFF);
            if (Character.isUpperCase(typeFirstChar)) {
                out.write(data, i, totalLen);
            }
            i += totalLen;
        }
        return out.toByteArray();
    }
}
