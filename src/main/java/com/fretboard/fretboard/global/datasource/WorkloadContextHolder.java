package com.fretboard.fretboard.global.datasource;

public final class WorkloadContextHolder {

    private static final ThreadLocal<ReadWorkload> WORKLOAD = new ThreadLocal<>();

    private WorkloadContextHolder() {
    }

    public static void set(ReadWorkload workload) {
        WORKLOAD.set(workload);
    }

    public static ReadWorkload getWorkload() {
        ReadWorkload workload = WORKLOAD.get();
        return workload != null ? workload : ReadWorkload.SERVING;
    }

    public static void clear() {
        WORKLOAD.remove();
    }
}
