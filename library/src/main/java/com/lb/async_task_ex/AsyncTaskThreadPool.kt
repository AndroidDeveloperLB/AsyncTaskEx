package com.lb.async_task_ex

import com.lb.async_task_ex.AsyncTaskEx.OnFinishedListener
import java.util.*
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * a thread pool that contains tasks to run like on asyncTask. <br></br>
 * also has the ability to use a stack instead of a queue for the order of tasks
 */
class AsyncTaskThreadPool @JvmOverloads constructor(minNumberOfThread: Int = CORE_POOL_SIZE, maxNumberOfThread: Int = MAXIMUM_POOL_SIZE, keepAliveTimeInSeconds: Int = TIME_TO_KEEP_ALIVE_IN_SECONDS) {
    private val executor: ThreadPoolExecutor
    private val tasks = HashSet<AsyncTaskEx<*>>()
    val tasksCount: Int
        get() = tasks.size

    init {
        val poolWorkQueue = ScalingQueue<Runnable>()
        val threadFactory = object : ThreadFactory {
            private val count = AtomicInteger(1)
            override fun newThread(r: Runnable): Thread {
                val thread = Thread(r, "thread #" + count.getAndIncrement())
                thread.priority = Thread.MIN_PRIORITY
                return thread
            }
        }
        // needed because normal ThreadPoolExecutor always uses a single thread.
        executor = ScalingThreadPoolExecutor(minNumberOfThread, maxNumberOfThread, keepAliveTimeInSeconds.toLong(), TimeUnit.SECONDS, poolWorkQueue, threadFactory)
        executor.rejectedExecutionHandler = ForceQueuePolicy()
        poolWorkQueue.setThreadPoolExecutor(executor)
    }

    /**
     * runs the task and remembers it till it finishes.
     */
    fun <Result> executeAsyncTask(task: AsyncTaskEx<Result>) {
        task.addOnFinishedListener(object : OnFinishedListener {
            override fun onFinished() {
                tasks.remove(task)
                task.removeOnFinishedListener(this)
            }
        })
        tasks.add(task)
        task.executeOnExecutor(executor)
    }

    fun cancelAllTasks(alsoInterrupt: Boolean) {
        for (task in tasks)
            task.cancel(alsoInterrupt)
        tasks.clear()
    }

    companion object {
        // classes used for the threadPool were created because of an issue with customized parameters of
        // ThreadPoolExecutor:
        // https://github.com/kimchy/kimchy.github.com/blob/master/_posts/2008-11-23-juc-executorservice-gotcha.textile
        private const val TIME_TO_KEEP_ALIVE_IN_SECONDS = 10
        private const val CORE_POOL_SIZE = 1
        private val MAXIMUM_POOL_SIZE = max(CORE_POOL_SIZE, Runtime.getRuntime().availableProcessors() - 1)
    }
}
