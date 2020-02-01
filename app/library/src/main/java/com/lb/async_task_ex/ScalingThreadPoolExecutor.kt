package com.lb.async_task_ex

import java.util.concurrent.BlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class ScalingThreadPoolExecutor(corePoolSize: Int, maximumPoolSize: Int, keepAliveTime: Long, unit: TimeUnit, workQueue: BlockingQueue<Runnable>, threadFactory: ThreadFactory) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory) {
    /**
     * number of threads that are actively executing tasks
     */
    private val activeCount = AtomicInteger()

    override fun getActiveCount(): Int {
        return activeCount.get()
    }

    override fun beforeExecute(t: Thread, r: Runnable) {
        activeCount.incrementAndGet()
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        activeCount.decrementAndGet()
    }
}
