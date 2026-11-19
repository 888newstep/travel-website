package travel.file.storage;

import java.util.Map;
import java.util.Set;

/**
 * 扩展名 → 文件魔数（magic bytes）校验。
 * 白名单 + 魔数双因子：扩展名可伪造、Content-Type 可伪造，文件头最难伪造；
 * 三者一致才放行。文本类（txt/md/csv/json）无固定魔数，用"前 4KB 无 NUL 字节"嗅探。
 */
final class FileTypeSniffer {
    private FileTypeSniffer() {
    }

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF = {'G', 'I', 'F', '8'};
    private static final byte[] BMP = {'B', 'M'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_TAG = {'W', 'E', 'B', 'P'};
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final byte[] ZIP = {'P', 'K', 0x03, 0x04};
    private static final byte[] MP4_SIZE = {0x00, 0x00, 0x00};
    private static final byte[] MP4_FTYP = {'f', 't', 'y', 'p'};
    private static final byte[] MP3_ID3 = {'I', 'D', '3'};
    private static final byte[] MP3_SYNC = {(byte) 0xFF, (byte) 0xE0};

    /** 扩展名分组：组内共享同一魔数判定。 */
    private static final Map<String, String> EXT_TO_KIND = Map.ofEntries(
            Map.entry("jpg", "jpg"), Map.entry("jpeg", "jpg"),
            Map.entry("png", "png"),
            Map.entry("gif", "gif"),
            Map.entry("bmp", "bmp"),
            Map.entry("webp", "webp"),
            Map.entry("pdf", "pdf"),
            Map.entry("doc", "zip"), Map.entry("docx", "zip"),
            Map.entry("xls", "zip"), Map.entry("xlsx", "zip"),
            Map.entry("ppt", "zip"), Map.entry("pptx", "zip"),
            Map.entry("zip", "zip"),
            Map.entry("mp4", "mp4"), Map.entry("mov", "mp4"),
            Map.entry("mp3", "mp3"),
            Map.entry("txt", "text"), Map.entry("md", "text"),
            Map.entry("csv", "text"), Map.entry("json", "text"));

    /** FileStoragePolicy 白名单使用的扩展名集合（由本表派生，单一事实源）。 */
    static final Set<String> KNOWN_EXTENSIONS = Set.copyOf(EXT_TO_KIND.keySet());

    private static boolean startsWith(byte[] head, int headLen, byte[] magic) {
        if (headLen < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param head 文件头部字节（建议至少 16 字节）
     * @param ext  小写扩展名（不含点）
     * @return 内容与声明类型一致返回 true
     */
    static boolean matches(byte[] head, int headLen, String ext) {
        String kind = EXT_TO_KIND.get(ext);
        if (kind == null) {
            return false; // 未知扩展名一律拒绝（白名单外）
        }
        return switch (kind) {
            case "jpg" -> startsWith(head, headLen, JPG);
            case "png" -> startsWith(head, headLen, PNG);
            case "gif" -> startsWith(head, headLen, GIF);
            case "bmp" -> startsWith(head, headLen, BMP);
            case "pdf" -> startsWith(head, headLen, PDF);
            case "zip" -> startsWith(head, headLen, ZIP);
            case "webp" -> startsWith(head, headLen, RIFF) && headLen >= 12 && matchesAt(head, headLen, 8, WEBP_TAG);
            case "mp4" -> headLen >= 12 && matchesAt(head, headLen, 4, MP4_FTYP);
            case "mp3" -> startsWith(head, headLen, MP3_ID3) || (headLen >= 2 && startsWith(head, headLen, MP3_SYNC));
            case "text" -> isPlainText(head, headLen);
            default -> false;
        };
    }

    private static boolean matchesAt(byte[] head, int headLen, int offset, byte[] tag) {
        if (headLen < offset + tag.length) {
            return false;
        }
        for (int i = 0; i < tag.length; i++) {
            if (head[offset + i] != tag[i]) {
                return false;
            }
        }
        return true;
    }

    /** 文本嗅探：前 headLen 字节内不允许出现 NUL（二进制特征）。 */
    private static boolean isPlainText(byte[] head, int headLen) {
        for (int i = 0; i < headLen; i++) {
            if (head[i] == 0x00) {
                return false;
            }
        }
        return true;
    }
}
