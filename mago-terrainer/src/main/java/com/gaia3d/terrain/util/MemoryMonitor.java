package com.gaia3d.terrain.util;

import com.gaia3d.util.DecimalUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Memory monitoring utility for detecting and preventing OutOfMemoryError during mesh refinement.
 * Provides proactive memory checks with configurable thresholds.
 */
@Slf4j
public class MemoryMonitor {

    // Memory thresholds
    private static final double CRITICAL_MEMORY_THRESHOLD = 0.10;  // 10% free = STOP
    private static final double WARNING_MEMORY_THRESHOLD = 0.20;   // 20% free = WARN

    /**
     * Represents the current memory state of the JVM
     */
    public static class MemoryState {
        public final long maxMemory;
        public final long totalMemory;
        public final long freeMemory;
        public final long usedMemory;
        public final double freeMemoryPercent;
        public final boolean isCritical;
        public final boolean isWarning;

        /**
         * Constructor calculates all derived values from current JVM state
         */
        public MemoryState() {
            Runtime runtime = Runtime.getRuntime();
            this.maxMemory = runtime.maxMemory();
            this.totalMemory = runtime.totalMemory();
            this.freeMemory = runtime.freeMemory();
            this.usedMemory = totalMemory - freeMemory;

            // Calculate available memory as: (maxMemory - usedMemory)
            // This accounts for heap that can still be allocated
            long availableMemory = maxMemory - usedMemory;
            this.freeMemoryPercent = (double) availableMemory / maxMemory;

            this.isCritical = freeMemoryPercent < CRITICAL_MEMORY_THRESHOLD;
            this.isWarning = freeMemoryPercent < WARNING_MEMORY_THRESHOLD && !isCritical;
        }

        public String getMaxMemoryDisplay() {
            return DecimalUtils.byteCountToDisplaySize(maxMemory);
        }

        public String getTotalMemoryDisplay() {
            return DecimalUtils.byteCountToDisplaySize(totalMemory);
        }

        public String getFreeMemoryDisplay() {
            return DecimalUtils.byteCountToDisplaySize(freeMemory);
        }

        public String getUsedMemoryDisplay() {
            return DecimalUtils.byteCountToDisplaySize(usedMemory);
        }

        public String getAvailableMemoryDisplay() {
            long availableMemory = maxMemory - usedMemory;
            return DecimalUtils.byteCountToDisplaySize(availableMemory);
        }

        public String getFormattedPercent() {
            return String.format("%.1f", freeMemoryPercent * 100.0);
        }
    }

    /**
     * Check memory and log appropriate messages based on state
     *
     * @param context Description of current operation (e.g., "RefineMesh", "Iteration 5")
     * @return MemoryState object containing all memory metrics
     */
    public static MemoryState checkMemory(String context) {
        MemoryState state = new MemoryState();

        if (state.isCritical) {
            log.error("[{}] CRITICAL memory pressure: {}% free ({} / {}). " +
                    "Recommend stopping to prevent OutOfMemoryError.",
                    context, state.getFormattedPercent(),
                    state.getAvailableMemoryDisplay(), state.getMaxMemoryDisplay());
        } else if (state.isWarning) {
            log.warn("[{}] LOW memory warning: {}% free ({} / {}). Approaching limits.",
                    context, state.getFormattedPercent(),
                    state.getAvailableMemoryDisplay(), state.getMaxMemoryDisplay());
        }

        return state;
    }

    /**
     * Memory metrics for trend analysis - calculates per-element memory usage
     */
    public static class MemoryMetrics {
        public final long bytesPerTriangle;
        public final long bytesPerVertex;
        public final String formattedBytesPerTriangle;
        public final String formattedBytesPerVertex;

        public MemoryMetrics(long usedMemory, int triangleCount, int vertexCount) {
            this.bytesPerTriangle = triangleCount > 0 ? usedMemory / triangleCount : 0;
            this.bytesPerVertex = vertexCount > 0 ? usedMemory / vertexCount : 0;
            this.formattedBytesPerTriangle = bytesPerTriangle + " bytes";
            this.formattedBytesPerVertex = bytesPerVertex + " bytes";
        }
    }

    /**
     * Memory growth tracking between iterations
     */
    public static class MemoryGrowth {
        public final long memoryDelta;
        public final int triangleDelta;
        public final double growthRateMB;
        public final String formattedDelta;

        public MemoryGrowth(long previousUsed, long currentUsed,
                           int previousTriangles, int currentTriangles) {
            this.memoryDelta = currentUsed - previousUsed;
            this.triangleDelta = currentTriangles - previousTriangles;
            this.growthRateMB = memoryDelta / (1024.0 * 1024.0);
            this.formattedDelta = DecimalUtils.byteCountToDisplaySize(memoryDelta);
        }
    }

    /**
     * Calculate memory growth trends
     *
     * @param previousUsed Previous used memory
     * @param currentUsed Current used memory
     * @param previousTriangles Previous triangle count
     * @param currentTriangles Current triangle count
     * @return Growth rate information
     */
    public static MemoryGrowth calculateGrowth(long previousUsed, long currentUsed,
                                                int previousTriangles, int currentTriangles) {
        return new MemoryGrowth(previousUsed, currentUsed, previousTriangles, currentTriangles);
    }
}
