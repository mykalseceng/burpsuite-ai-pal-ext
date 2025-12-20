package util;

import burp.api.montoya.logging.Logging;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class ThreadManager {
    private final ExecutorService executor;
    private final Logging logging;
    private volatile boolean shutdown = false;

    public ThreadManager(Logging logging) {
        this.logging = logging;
        this.executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "LLM-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        if (shutdown) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("ThreadManager is shut down"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                logging.logToError("Background task failed: " + e.getMessage());
                throw new CompletionException(e);
            }
        }, executor);
    }

    public void submitAsync(Runnable task, Consumer<Throwable> errorHandler) {
        if (shutdown) {
            errorHandler.accept(new IllegalStateException("ThreadManager is shut down"));
            return;
        }
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logging.logToError("Background task failed: " + e.getMessage());
                errorHandler.accept(e);
            }
        });
    }

    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    logging.logToError("ThreadManager did not terminate cleanly");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logging.logToOutput("ThreadManager shut down");
    }

    public boolean isShutdown() {
        return shutdown;
    }
}