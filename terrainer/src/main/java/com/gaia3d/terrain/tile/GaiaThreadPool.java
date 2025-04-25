package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import jdk.jfr.Experimental;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
@Experimental
public class GaiaThreadPool {
    private static GaiaThreadPool instance;

    private final ExecutorService executorService;
    private final byte multiThreadCount;

    public GaiaThreadPool() {
        this.multiThreadCount = setDefaultThreadCount();
        this.executorService = Executors.newFixedThreadPool(multiThreadCount);
    }

    public static GaiaThreadPool getInstance() {
        GaiaThreadPool.instance = new GaiaThreadPool();
        return GaiaThreadPool.instance;
    }

    public void execute(List<Runnable> tasks) throws InterruptedException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        try {
            for (Runnable task : tasks) {
                Future<?> future = executorService.submit(task);
                if (globalOptions.isDebugMode()) {
                    future.get();
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute thread.", e);
            throw new RuntimeException(e);
        }
        executorService.shutdown();
        do {
            if (executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        } while (!executorService.awaitTermination(2, TimeUnit.SECONDS));
    }

    private byte setDefaultThreadCount() {
        // Get the number of processors available to the Java virtual machine.
        int processorCount = Runtime.getRuntime().availableProcessors();
        int threadCount = processorCount > 1 ? processorCount / 2 : 1;
        return (byte) threadCount;
    }
}
