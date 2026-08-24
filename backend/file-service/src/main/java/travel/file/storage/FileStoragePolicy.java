package travel.file.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储策略：上传目录配置化、原始文件名净化与长度限制、危险扩展名拒绝、
 * 单文件/单批大小限制。所有可调参数通过 {@code file.storage.*} 注入，
 * 见 file-service 的 application.yml。
 */
@Slf4j
@Component
public class FileStoragePolicy {

    /** 禁止上传的脚本/可执行扩展名。 */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "ps1", "vbs", "js", "jar", "msi",
            "scr", "com", "dll", "php", "jsp", "asp", "aspx", "wsf");

    private final Path rootDir;
    private final String publicPath;
    private final long maxFileSizeBytes;
    private final int maxBatchFiles;
    private final int maxFilenameLength;

    public FileStoragePolicy(
            @Value("${file.storage.root-dir:${user.home}/travel-resources}") String rootDir,
            @Value("${file.storage.public-path:/resources/}") String publicPath,
            @Value("${file.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes,
            @Value("${file.storage.max-batch-files:10}") int maxBatchFiles,
            @Value("${file.storage.max-filename-length:255}") int maxFilenameLength) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxBatchFiles = maxBatchFiles;
        this.maxFilenameLength = Math.max(maxFilenameLength, 1);
    }

    private static String normalizePublicPath(String publicPath) {
        if (publicPath == null || publicPath.isBlank()) {
            return "/resources/";
        }
        String p = publicPath.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        return p;
    }

    /**
     * 校验并持久化文件到磁盘，返回存储元信息。
     *
     * @throws IllegalArgumentException 文件为空、超限或类型被拒绝
     */
    public StoredFile store(MultipartFile file) {
        validate(file);
        String originalFilename = sanitizeOriginal(file.getOriginalFilename());
        String storedFilename = uniqueStoredFilename(originalFilename);
        Path target = resolveStoredPath(storedFilename);
        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("文件写入磁盘失败: " + storedFilename, e);
        }
        return new StoredFile(target, originalFilename, storedFilename,
                file.getSize(), resolveExtension(storedFilename));
    }

    /** 校验单文件：非空、不超大小上限、扩展名未被拒绝。 */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("文件超出大小上限: " + maxFileSizeBytes + " 字节");
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String ext = resolveExtension(name);
            if (BLOCKED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException("不允许的文件类型: ." + ext);
            }
        }
    }

    /** 校验单批数量上限。 */
    public void validateBatchSize(int count) {
        if (count > maxBatchFiles) {
            throw new IllegalArgumentException("单批文件数超出上限: " + maxBatchFiles);
        }
    }

    /** 将存储文件名转换为对外可访问的公网路径（写入数据库 filePath 字段）。 */
    public String toPublicPath(String storedFilename) {
        return publicPath + storedFilename;
    }

    /**
     * 由数据库保存的 filePath/fileName 反查磁盘绝对路径，含路径穿越防护。
     *
     * @param filePath 数据库 filePath，形如 {@code /resources/<storedFilename>}
     * @param fileName 原始文件名（回退用）
     */
    public Path resolveStoredPath(String filePath, String fileName) {
        String storedFilename = stripPublicPrefix(filePath);
        if (storedFilename == null || storedFilename.isBlank()) {
            storedFilename = sanitizeStoredName(fileName);
        }
        return resolveStoredPath(storedFilename);
    }

    /** 静默删除磁盘文件，失败仅记录日志。 */
    public void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除文件失败 (忽略): path={}", path, e);
        }
    }

    private Path resolveStoredPath(String storedFilename) {
        Path resolved = rootDir.resolve(storedFilename).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new IllegalArgumentException("非法的文件路径: " + storedFilename);
        }
        return resolved;
    }

    private String stripPublicPrefix(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        if (filePath.startsWith(publicPath)) {
            return filePath.substring(publicPath.length());
        }
        int idx = filePath.lastIndexOf('/');
        return idx >= 0 ? filePath.substring(idx + 1) : filePath;
    }

    /** 剥离路径分隔符前缀并按上限截断，保留扩展名。 */
    private String sanitizeOriginal(String raw) {
        String name = raw == null ? "file" : raw;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.isBlank()) {
            return "file";
        }
        if (name.length() > maxFilenameLength) {
            String ext = resolveExtension(name);
            int keep = Math.max(maxFilenameLength - ext.length() - 1, 1);
            String base = ext.isEmpty() ? name : name.substring(0, name.length() - ext.length() - 1);
            name = (base.length() > keep ? base.substring(0, keep) : base) + (ext.isEmpty() ? "" : "." + ext);
        }
        return name;
    }

    private String uniqueStoredFilename(String originalFilename) {
        String ext = resolveExtension(originalFilename);
        String base = UUID.randomUUID().toString().replace("-", "");
        return ext.isEmpty() ? base : base + "." + ext;
    }

    private String sanitizeStoredName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return UUID.randomUUID().toString();
        }
        int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String name = slash >= 0 ? fileName.substring(slash + 1) : fileName;
        return name.isBlank() ? UUID.randomUUID().toString() : name;
    }

    private static String resolveExtension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }

    /** 存储后的文件元信息。 */
    public record StoredFile(Path path, String originalFilename, String storedFilename,
                              long size, String fileType) {
    }
}
