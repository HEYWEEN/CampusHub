package com.campushub.common.storage;

import com.campushub.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageStorageTest {

    @TempDir
    Path tempDir;

    private ImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new ImageStorage(tempDir.toString(), "/uploads");
    }

    @Test
    void put_cleansExifFromJpegBeforeWrite() throws Exception {
        String gpsPayload = "GPSLatitude=39.9042;GPSLongitude=116.4074";
        byte[] jpeg = buildJpegWithApp1(gpsPayload);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", jpeg);

        String url = storage.put(file);

        Path relative = Path.of(url.replace("/uploads/", ""));
        byte[] onDisk = Files.readAllBytes(storage.getBaseDir().resolve(relative));
        String onDiskStr = new String(onDisk, StandardCharsets.ISO_8859_1);
        assertFalse(onDiskStr.contains(gpsPayload), "落盘 JPEG 不应再含 GPS payload");
    }

    @Test
    void put_rejectsFileOver5Mb() {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", oversized);

        BizException ex = assertThrows(BizException.class, () -> storage.put(file));
        assertEquals(4002, ex.getCode());
    }

    private static byte[] buildJpegWithApp1(String exifPayload) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write(0xFF);
        b.write(0xD8);
        writeSegment(b, 0xE1, exifPayload.getBytes(StandardCharsets.ISO_8859_1));
        b.write(0xFF);
        b.write(0xDA);
        b.write(0x00);
        b.write(0x02);
        b.write(new byte[]{1, 2, 3});
        b.write(0xFF);
        b.write(0xD9);
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
}
