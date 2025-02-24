package com.betting.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理
 *
 * @author dufy
 **/
public class ThreadPoolManagerUtils {

    /**
     * 核心线程数，等于当前系统的处理器数量。 该值表示线程池在没有新任务时，会保持的最小线程数。
     */
    public static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程数，是核心线程数的两倍。 该值表示线程池允许的最大并发线程数。
     */
    public static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    /**
     * 阻塞队列容量，表示等待执行任务的最大数量。 当队列已满且线程池中的线程数小于最大线程数时，将创建新线程处理任务。
     */
    public static final int QUEUE_CAPACITY = 1000;

    /**
     * 线程空闲的最大时间，单位为秒。 该值表示当线程池中的线程空闲超过该时间时，空闲线程将被回收。
     */
    public static final long KEEP_ALIVE_TIME = 60L;

    /**
     * 创建并返回一个自定义的线程池
     */
    public static ThreadPoolExecutor createExecutor() {
        // 创建自定义线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY), new ThreadPoolExecutor.CallerRunsPolicy());

        return executor;

    }
}
