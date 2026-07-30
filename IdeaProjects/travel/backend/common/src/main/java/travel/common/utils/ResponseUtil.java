package travel.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Slf4j
public class ResponseUtil {

    /**
     * 返回成功响应
     */
    public static <T> ResponseEntity<Result<T>> success(T data) {
        Result<T> result = Result.success(data);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 返回成功响应（无数据）
     */
    public static <T> ResponseEntity<Result<T>> success() {
        Result<T> result = Result.success();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 返回错误响应
     */
    public static <T> ResponseEntity<Result<T>> error(int code, String message) {
        Result<T> result = Result.error(code, message);
        HttpStatus status = getHttpStatus(code);
        return new ResponseEntity<>(result, status);
    }

    /**
     * 返回错误响应（默认状态码）
     */
    public static <T> ResponseEntity<Result<T>> error(String message) {
        Result<T> result = Result.error(message);
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 根据错误码获取HTTP状态码
     */
    private static HttpStatus getHttpStatus(int code) {
        if (code >= 400 && code < 500) {
            return HttpStatus.BAD_REQUEST;
        } else if (code >= 500) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (code == 401) {
            return HttpStatus.UNAUTHORIZED;
        } else if (code == 403) {
            return HttpStatus.FORBIDDEN;
        } else if (code == 404) {
            return HttpStatus.NOT_FOUND;
        } else if (code == 408) {
            return HttpStatus.REQUEST_TIMEOUT;
        } else if (code == 415) {
            return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        } else if (code == 429) {
            return HttpStatus.TOO_MANY_REQUESTS;
        } else if (code == 503) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            return HttpStatus.OK;
        }
    }

    /**
     * 返回分页响应
     */
    public static <T> ResponseEntity<Result<PageResponse<T>>> successWithPage(T data, long total, int page, int size) {
        PageResponse<T> pageResponse = new PageResponse<>(data, total, page, size);
        return success(pageResponse);
    }

    /**
     * 分页响应封装
     */
    public static class PageResponse<T> {
        private T data;
        private long total;
        private int page;
        private int size;
        private int pages;

        public PageResponse(T data, long total, int page, int size) {
            this.data = data;
            this.total = total;
            this.page = page;
            this.size = size;
            this.pages = (int) Math.ceil((double) total / size);
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getPages() {
            return pages;
        }

        public void setPages(int pages) {
            this.pages = pages;
        }
    }
}
