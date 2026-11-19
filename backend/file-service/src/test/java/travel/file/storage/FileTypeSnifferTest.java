package travel.file.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypeSnifferTest {
    private final FileStoragePolicy policy = new FileStoragePolicy(
            "@tempDir@", "/resources/", 1024 * 1024, 10, 255);

    private static byte[] head(int... bytes) {
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = (byte) bytes[i];
        }
        return out;
    }

    @Test
    void shouldMatchRealMagicBytes() {
        assertTrue(FileTypeSniffer.matches(head((byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0), 4, "jpg"));
        assertTrue(FileTypeSniffer.matches(head((byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0), 12, "png"));
        assertTrue(FileTypeSniffer.matches("GIF89a".getBytes(StandardCharsets.US_ASCII), 6, "gif"));
        assertTrue(FileTypeSniffer.matches("BMxx".getBytes(StandardCharsets.US_ASCII), 4, "bmp"));
        assertTrue(FileTypeSniffer.matches("RIFF1234WEBP".getBytes(StandardCharsets.US_ASCII), 12, "webp"));
        assertTrue(FileTypeSniffer.matches("%PDF-1.7".getBytes(StandardCharsets.US_ASCII), 8, "pdf"));
        assertTrue(FileTypeSniffer.matches("PK\u0003\u0004zip-entry".getBytes(StandardCharsets.US_ASCII), 8, "docx"));
        assertTrue(FileTypeSniffer.matches("\u0000\u0000\u0000\u0018ftypmp42".getBytes(StandardCharsets.US_ASCII), 12, "mp4"));
        assertTrue(FileTypeSniffer.matches("ID3-tag".getBytes(StandardCharsets.US_ASCII), 7, "mp3"));
        assertTrue(FileTypeSniffer.matches("plain text no nul".getBytes(StandardCharsets.US_ASCII), 17, "txt"));
    }

    @Test
    void shouldRejectForgedContentAndUnknownTypes() {
        // exe 头伪装成 jpg（双扩展名 evil.php.jpg 的实际内容特征）
        assertFalse(FileTypeSniffer.matches(new byte[]{'M', 'Z', 0, 0}, 4, "jpg"));
        // PNG 声称是 GIF
        assertFalse(FileTypeSniffer.matches(head((byte) 0x89, 'P', 'N', 'G'), 4, "gif"));
        // 未登记的扩展名一律拒绝
        assertFalse(FileTypeSniffer.matches("anything".getBytes(StandardCharsets.US_ASCII), 8, "php"));
        assertFalse(FileTypeSniffer.matches("anything".getBytes(StandardCharsets.US_ASCII), 8, "svg"));
        // 二进制 NUL 出现在"文本"里
        assertFalse(FileTypeSniffer.matches(new byte[]{'a', 'b', 0x00, 'c'}, 4, "txt"));
        // 头太短无法判定
        assertFalse(FileTypeSniffer.matches(new byte[]{(byte) 0xFF}, 1, "jpg"));
    }

    @Test
    void shouldValidateWhitelistOnUpload() {
        // 白名单内：png 内容 + .png 声明 → 通过
        byte[] png = head((byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0);
        assertDoesNotThrow(() -> policy.validate(
                new MockMultipartFile("file", "avatar.png", "image/png", png)));
        // 白名单内但内容是 exe → 魔数校验拒绝（store 路径）
        MockMultipartFile evil = new MockMultipartFile("file", "notes.png", "image/png",
                new byte[]{'M', 'Z', 0, 0, 'x'});
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> policy.store(evil));
        assertTrue(ex.getMessage().contains("不一致"));

        // 白名单外：php、svg、双扩展 evil.php.jpg（ext=jpg 在白名单，但内容校验兜底）
        assertThrows(IllegalArgumentException.class, () -> policy.validate(
                new MockMultipartFile("file", "shell.php", "", "content".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(
                new MockMultipartFile("file", "page.svg", "", "<svg/>".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void shouldExposeSnifferDerivedWhitelist() throws IOException {
        // 白名单与嗅探表同源：白名单里的每个扩展名都存在魔数判定
        assertEquals(FileTypeSniffer.KNOWN_EXTENSIONS, FileStoragePolicy.ALLOWED_EXTENSIONS);
        assertFalse(FileStoragePolicy.ALLOWED_EXTENSIONS.contains("php"));
        assertFalse(FileStoragePolicy.ALLOWED_EXTENSIONS.contains("svg"));
    }
}
