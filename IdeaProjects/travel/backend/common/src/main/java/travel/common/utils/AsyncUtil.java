package travel.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
public class AsyncUtil {

    private static final Logger log = LoggerFactory.getLogger(AsyncUtil.class);

    /**
     * 异步执行任务
     */
    @Async
    public void execute(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("异步执行任务失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步执行有返回值的任务
     */
    @Async
    public <T> void executeWithResult(Supplier<T> task, ResultCallback<T> callback) {
        try {
            T result = task.get();
            if (callback != null) {
                callback.onSuccess(result);
            }
        } catch (Exception e) {
            log.error("异步执行任务失败: {}", e.getMessage(), e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * 结果回调接口
     */
    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    /**
     * 批处理工具方法
     */
    public static <T> void batchProcess(List<T> items, int batchSize, BatchProcessor<T> processor) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int totalSize = items.size();
        int batchCount = (totalSize + batchSize - 1) / batchSize;

        for (int i = 0; i < batchCount; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, totalSize);
            List<T> batchItems = items.subList(start, end);
            
            try {
                processor.process(batchItems);
                log.debug("处理批次 {}，共 {} 条数据", i + 1, batchItems.size());
            } catch (Exception e) {
                log.error("处理批次 {} 失败: {}", i + 1, e.getMessage(), e);
            }
        }
    }

    /**
     * 批处理器接口
     */
    public interface BatchProcessor<T> {
        void process(List<T> items) throws Exception;
    }
}
