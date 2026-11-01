package social;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class Producers {
    private final ExecutorService executor;

    public Producers(ExecutorService executor) {
        this.executor = executor;
    }

    public void run(List<Runnable> jobs) throws InterruptedException, ExecutionException {
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Runnable job : jobs) {
                futures.add(executor.submit(job));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
            boolean interrupted = false;
            while (!executor.isTerminated()) {
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
