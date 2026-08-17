package travel.common.utils;

import okhttp3.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 第三方 HTTP 响应体读取工具，按字节数上限读取，防止异常响应占满堆内存。
 */
public final class BoundedHttpBodyReader {

    private BoundedHttpBodyReader() {
    }

    /**
     * 读取 OkHttp 响应体并按 UTF-8 解码；超过 maxBytes 抛 IOException。
     */
    public static String readUtf8(ResponseBody body, long maxBytes) throws IOException {
        if (body == null) {
            throw new IOException("Response body is null");
        }
        return readUtf8(body.byteStream(), maxBytes);
    }

    /**
     * 读取输入流并按 UTF-8 解码；超过 maxBytes 抛 IOException。
     * 供 RestTemplate（ClientHttpResponse#getBody）等非 OkHttp 调用方使用。
     */
    public static String readUtf8(InputStream in, long maxBytes) throws IOException {
        if (in == null) {
            throw new IOException("Input stream is null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > maxBytes) {
                throw new IOException("Response body exceeds limit: " + maxBytes + " bytes");
            }
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
