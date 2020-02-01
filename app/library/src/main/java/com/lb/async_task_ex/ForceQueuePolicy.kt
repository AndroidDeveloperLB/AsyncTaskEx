package com.lb.async_task_ex

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

internal class ForceQueuePolicy : RejectedExecutionHandler {
    override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
        try {
            executor.queue.put(r)
        } catch (e: InterruptedException) {
            // should never happen since we never wait
            throw RejectedExecutionException(e)
        }

    }
}
