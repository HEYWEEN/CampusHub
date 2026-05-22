package com.campushub.common.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class ExifCleanerTest {

    // ────────── JPEG 测试 ──────────

    @Test
    void cleanJpeg_removesApp1ExifContainingGps() throws Exception {
        // 构造：SOI + APP1(EXIF GPS payload) + APP0(JFIF) + DQT + SOS + image data + EOI
        String gpsPayload = "GPSLatitude=39.9042;GPSLongitude=116.4074";
        byte[] jpeg = buildJpegWithApp1(gpsPayload);

        byte[] cleaned = ExifCleaner.clean(jpeg, "image/jpeg");

        String cleanedStr = new String(cleaned, StandardCharsets.ISO_8859_1);
        assertFalse(cleanedStr.contains(gpsPayload),
                "清洗后 JPEG 字节流不应再包含 GPS payload");
    }

    @Test
    void cleanJpeg_preservesDqtAndImageData() throws Exception {
        // DQT 段（0xFFDB）不应被删
        byte[] jpeg = buildJpegWithApp1AndDqt("GPSdata", new byte[]{1, 2, 3, 4});
        byte[] cleaned = ExifCleaner.clean(jpeg, "image/jpeg");
        // 验证 DQT marker 仍在
        boolean hasDqt = false;
        for (int i = 0; i + 1 < cleaned.length; i++) {
            if (cleaned[i] == (byte) 0xFF && cleaned[i + 1] == (byte) 0xDB) {
                hasDqt = true;
                break;
            }
        }
        assertTrue(hasDqt, "DQT 段必须保留");
    }

    @Test
    void cleanJpeg_returnsOriginalIfMagicWrong() {
        byte[] notJpeg = new byte[]{0x12, 0x34, 0x56, 0x78};
        byte[] cleaned = ExifCleaner.clean(notJpeg, "image/jpeg");
        assertArrayEquals(notJpeg, cleaned, "魔数不匹配时应原样返回");
    }

    @Test
    void cleanJpeg_alsoStripsComSegment() throws Exception {
        // COM (0xFFFE) 常含 "edited with Adobe Photoshop" 等隐式信息
        byte[] jpeg = buildJpegWithComSegment("Edited with PrivateApp 2024");
        byte[] cleaned = ExifCleaner.clean(jpeg, "image/jpeg");
        String str = new String(cleaned, StandardCharsets.ISO_8859_1);
        assertFalse(str.contains("Edited with PrivateApp"), "COM 段应一并清除");
    }

    // ────────── PNG 测试 ──────────

    @Test
    void cleanPng_removesTextAndExifChunks() throws Exception {
        // 构造：PNG 签名 + IHDR + tEXt("Software=Camera") + eXIf("GPSdata") + IDAT + IEND
        byte[] png = buildPngWithChunks(
                new Chunk("IHDR", new byte[13]),
                new Chunk("tEXt", "Software=PrivateCamera".getBytes(StandardCharsets.ISO_8859_1)),
                new Chunk("eXIf", "GPSdata-12345".getBytes(StandardCharsets.ISO_8859_1)),
                new Chunk("IDAT", new byte[]{1, 2, 3}),
                new Chunk("IEND", new byte[0])
        );

        byte[] cleaned = ExifCleaner.clean(png, "image/png");
        String str = new String(cleaned, StandardCharsets.ISO_8859_1);

        assertFalse(str.contains("PrivateCamera"), "tEXt 隐私字段应被清除");
        assertFalse(str.contains("GPSdata-12345"), "eXIf 应被清除");
        assertTrue(containsChunkType(cleaned, "IHDR"));
        assertTrue(containsChunkType(cleaned, "IDAT"));
        assertTrue(containsChunkType(cleaned, "IEND"));
    }

    @Test
    void cleanPng_returnsOriginalIfSignatureWrong() {
        byte[] notPng = new byte[]{0x12, 0x34, 0x56, 0x78, 0x12, 0x34, 0x56, 0x78};
        byte[] cleaned = ExifCleaner.clean(notPng, "image/png");
        assertArrayEquals(notPng, cleaned);
    }

    // ────────── 其他格式不动 ──────────

    @Test
    void clean_unknownFormat_returnsOriginal() {
        byte[] data = "some-other-binary".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(data, ExifCleaner.clean(data, "application/octet-stream"));
    }

    @Test
    void clean_nullOrTooShort_returnsAsIs() {
        assertNull(ExifCleaner.clean(null, "image/jpeg"));
        byte[] tiny = {1};
        assertArrayEquals(tiny, ExifCleaner.clean(tiny, "image/jpeg"));
    }

    // ───────────── 构造辅助 ─────────────

    private static byte[] buildJpegWithApp1(String exifPayload) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        // SOI
        b.write(0xFF); b.write(0xD8);
        // APP1 (EXIF)
        writeSegment(b, 0xE1, exifPayload.getBytes(StandardCharsets.ISO_8859_1));
        // APP0 (JFIF) — 也会被删
        writeSegment(b, 0xE0, "JFIF\0".getBytes(StandardCharsets.ISO_8859_1));
        // SOS 之后假装是图像数据
        b.write(0xFF); b.write(0xDA);
        b.write(0x00); b.write(0x02); // segLen=2 (空)
        b.write(new byte[]{1, 2, 3, 4});
        // EOI
        b.write(0xFF); b.write(0xD9);
        return b.toByteArray();
    }

    private static byte[] buildJpegWithApp1AndDqt(String exifPayload, byte[] dqtData) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(0xFF); b.write(0xD8);
        writeSegment(b, 0xE1, exifPayload.getBytes(StandardCharsets.ISO_8859_1));
        writeSegment(b, 0xDB, dqtData);
        b.write(0xFF); b.write(0xDA);
        b.write(0x00); b.write(0x02);
        b.write(new byte[]{1});
        b.write(0xFF); b.write(0xD9);
        return b.toByteArray();
    }

    private static byte[] buildJpegWithComSegment(String comment) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(0xFF); b.write(0xD8);
        writeSegment(b, 0xFE, comment.getBytes(StandardCharsets.ISO_8859_1));
        b.write(0xFF); b.write(0xDA);
        b.write(0x00); b.write(0x02);
        b.write(new byte[]{0x55});
        b.write(0xFF); b.write(0xD9);
        return b.toByteArray();
    }

    private static void writeSegment(ByteArrayOutputStream b, int marker, byte[] payload) throws IOException {
        b.write(0xFF);
        b.write(marker);
        int segLen = 2 + payload.length;
        b.write((segLen >> 8) & 0xFF);
        b.write(segLen & 0xFF);
        b.write(payload);
    }

    // PNG 辅助
    private record Chunk(String type, byte[] data) {}

    private static byte[] buildPngWithChunks(Chunk... chunks) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        // PNG signature
        b.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        for (Chunk c : chunks) {
            byte[] typeBytes = c.type.getBytes(StandardCharsets.US_ASCII);
            int len = c.data.length;
            b.write((len >> 24) & 0xFF);
            b.write((len >> 16) & 0xFF);
            b.write((len >> 8) & 0xFF);
            b.write(len & 0xFF);
            b.write(typeBytes);
            b.write(c.data);
            CRC32 crc = new CRC32();
            crc.update(typeBytes);
            crc.update(c.data);
            long v = crc.getValue();
            b.write((int) ((v >> 24) & 0xFF));
            b.write((int) ((v >> 16) & 0xFF));
            b.write((int) ((v >> 8) & 0xFF));
            b.write((int) (v & 0xFF));
        }
        return b.toByteArray();
    }

    private static boolean containsChunkType(byte[] png, String type) {
        byte[] needle = type.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 8; i + needle.length <= png.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (png[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
