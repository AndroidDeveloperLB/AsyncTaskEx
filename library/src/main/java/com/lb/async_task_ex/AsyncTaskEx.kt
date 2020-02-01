/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lb.async_task_ex

import android.os.*
import java.io.File
import java.io.FileFilter
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.math.max

/**
 * same as [AsyncTask] but doesn't have a limit on the number of tasks,plus it uses threads as many as the<br></br>
 * number of cores , minus 1 (or a single thread if it's a single core device)
 */
abstract class AsyncTaskEx<Result> {
    private val future: FutureTask<Result>
    private val cancelled = AtomicBoolean()
    private val taskInvoked = AtomicBoolean()
    private val onFinishedListeners = HashSet<OnFinishedListener>()
    /**
     * Returns the current status of this task.
     *
     * @return The current status.
     */
    @Volatile
    var status = Status.PENDING
        private set
    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed normally. If you are calling [.cancel] on the task,
     * the value returned by this method should be checked periodically from [.doInBackground] to end the task as soon as possible.
     *
     * @return <tt>true</tt> if task was cancelled before it completed
     * @see .cancel
     */
    val isCancelled: Boolean
        get() = cancelled.get()

    /**
     * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     */
    init {
        val worker = object : WorkerRunnable<Result>() {
            override fun call(): Result? {
                taskInvoked.set(true)
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                return postResult(doInBackground())
            }
        }
        future = object : FutureTask<Result>(worker) {
            override fun done() {
                try {
                    postResultIfNotInvoked(get())
                } catch (e: InterruptedException) {
                    android.util.Log.w(LOG_TAG, e)
                } catch (e: ExecutionException) {
                    throw RuntimeException("An error occured while executing doInBackground()", e.cause)
                } catch (e: CancellationException) {
                    postResultIfNotInvoked(null)
                }
            }
        }
    }

    private fun postResultIfNotInvoked(result: Result?) {
        val wasTaskInvoked = taskInvoked.get()
        if (!wasTaskInvoked)
            postResult(result)
    }

    private fun postResult(result: Result?): Result? {
        val message = sHandler.obtainMessage(MESSAGE_POST_RESULT, AsyncTaskExResult(this, result))
        message.sendToTarget()
        return result
    }

    /**
     * Override this method to perform a computation on a background thread. The specified parameters are the parameters
     * passed to [.execute] by the caller of this task. This method can call [.publishProgress] to publish
     * updates on the UI thread.
     *
     * @return A result, defined by the subclass of this task.
     * @see .onPreExecute
     * @see .onPostExecute
     *
     * @see .publishProgress
     */
    abstract fun doInBackground(): Result

    /**
     * Runs on the UI thread before [.doInBackground].
     *
     * @see .onPostExecute
     *
     * @see .doInBackground
     */
    protected open fun onPreExecute() {}

    /**
     *
     *
     * Runs on the UI thread after [.doInBackground]. The specified result is the value returned by [.doInBackground].
     *
     *
     *
     * This method won't be invoked if the task was cancelled.
     *
     *
     * @param result The result of the operation computed by [.doInBackground].
     * @see .onPreExecute
     *
     * @see .doInBackground
     *
     * @see .onCancelled
     */
    open fun onPostExecute(result: Result?) {}

    /**
     *
     *
     * Runs on the UI thread after [.cancel] is invoked and [.doInBackground] has finished.
     *
     *
     *
     * The default implementation simply invokes [.onCancelled] and ignores the result. If you write your own implementation, do not call `super.onCancelled(result)`.
     *
     *
     * @param result The result, if any, computed in [.doInBackground], can be null
     * @see .cancel
     * @see .isCancelled
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun onCancelled(@Suppress("UNUSED_PARAMETER") result: Result?) {
        onCancelled()
    }

    /**
     *
     *
     * Applications should preferably override [.onCancelled]. This method is invoked by the default implementation of [.onCancelled].
     *
     *
     *
     * Runs on the UI thread after [.cancel] is invoked and [.doInBackground] has finished.
     *
     *
     * @see .onCancelled
     * @see .cancel
     * @see .isCancelled
     */
    protected open fun onCancelled() {}

    /**
     *
     *
     * Attempts to cancel execution of this task. This attempt will fail if the task has already completed, already been cancelled,
     * or could not be cancelled for some other reason. If successful, and this task has not started when <tt>cancel</tt> is called,
     * this task should never run. If the task has already started, then the <tt>mayInterruptIfRunning</tt> parameter determines whether the thread
     * executing this task should be interrupted in an attempt to stop the task.
     *
     *
     *
     * Calling this method will result in [.onCancelled] being invoked on the UI thread after [.doInBackground] returns.
     * Calling this method guarantees that [.onPostExecute] is never invoked. After invoking this method, you should check the value
     * returned by [.isCancelled] periodically from [.doInBackground] to finish the task as early as possible.
     *
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this task should be interrupted; otherwise, in-progress tasks
     * are allowed to complete.
     * @return <tt>false</tt> if the task could not be cancelled, typically because it has already completed normally; <tt>true</tt> otherwise
     * @see .isCancelled
     * @see .onCancelled
     */
    fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancelled.set(true)
        return future.cancel(mayInterruptIfRunning)
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     *
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException    If the computation threw an exception.
     * @throws InterruptedException  If the current thread was interrupted while waiting.
     */
    @Throws(InterruptedException::class, ExecutionException::class)
    fun get(): Result {
        return future.get()
    }

    /**
     * Waits if necessary for at most the given time for the computation to complete, and then retrieves its result.
     *
     * @param timeout Time to wait before cancelling the operation.
     * @param unit    The time unit for the timeout.
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException    If the computation threw an exception.
     * @throws InterruptedException  If the current thread was interrupted while waiting.
     * @throws TimeoutException      If the wait timed out.
     */
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    operator fun get(timeout: Long, unit: TimeUnit): Result {
        return future.get(timeout, unit)
    }

    /**
     * Executes the task with the specified parameters. The task returns itself (this) so that the caller can keep a
     * reference to it.
     *
     *
     * Note: this function schedules the task on a queue for a single background thread or pool of threads depending on the platform version. When first introduced, CustomAsyncTasks were executed serially on a single background thread. Starting with [android.os.Build.VERSION_CODES.DONUT], this was changed to a pool of threads allowing multiple tasks to operate in parallel. Starting [android.os.Build.VERSION_CODES.HONEYCOMB], tasks are back to being executed on a single thread to avoid common application errors caused by parallel execution. If you truly want parallel execution, you can use the [.executeOnExecutor] version of this method with [.THREAD_POOL_EXECUTOR]; however, see commentary there for warnings on its use.
     *
     *
     * This method must be invoked on the UI thread.
     *
     * @return This instance of CustomAsyncTask.
     * @throws IllegalStateException If [.getStatus] returns either [AsyncTaskEx.Status.RUNNING] or [AsyncTaskEx.Status.FINISHED].
     * @see .executeOnExecutor
     * @see .execute
     */
    fun execute(): AsyncTaskEx<Result> {
        return executeOnExecutor(sDefaultExecutor)
    }

    /**
     * Executes the task with the specified parameters. The task returns itself (this) so that the caller can keep a
     * reference to it.
     *
     *
     * This method is typically used with [.THREAD_POOL_EXECUTOR] to allow multiple tasks to run in parallel on a pool of threads managed by CustomAsyncTask, however you can also use your own [Executor] for custom behavior.
     *
     *
     * *Warning:* Allowing multiple tasks to run in parallel from a thread pool is generally *not* what one wants, because the order of their
     * operation is not defined. For example, if these tasks are used to modify any state in common (such as writing a file due to a button click), there
     * are no guarantees on the order of the modifications. Without careful work it is possible in rare cases for the newer version of the data to be over-written
     * by an older one, leading to obscure data loss and stability issues. Such changes are best executed in serial; to guarantee such work is serialized regardless
     * of platform version you can use this function with.
     *
     *
     * This method must be invoked on the UI thread.
     *
     * @param exec The executor to use. [.THREAD_POOL_EXECUTOR] is available as a convenient process-wide thread
     * pool for tasks that are loosely coupled.
     * @return This instance of CustomAsyncTask.
     * @throws IllegalStateException If [.getStatus] returns either [AsyncTaskEx.Status.RUNNING] or [AsyncTaskEx.Status.FINISHED].
     * @see .execute
     */
    fun executeOnExecutor(exec: Executor): AsyncTaskEx<Result> {
        if (status != Status.PENDING)
            when (status) {
                Status.RUNNING -> throw IllegalStateException("Cannot execute task:" + " the task is already running.")
                Status.FINISHED -> throw IllegalStateException("Cannot execute task:" + " the task has already been executed " + "(a task can be executed only once)")
                else -> {
                }
            }
        status = Status.RUNNING
        onPreExecute()
        exec.execute(future)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    private fun finish(result: Any?) {
        if (isCancelled)
            onCancelled(result as Result?)
        else
            onPostExecute(result as Result?)
        for (listener in onFinishedListeners)
            listener.onFinished()
        status = Status.FINISHED
    }

    fun addOnFinishedListener(onFinishedListener: OnFinishedListener) {
        this.onFinishedListeners.add(onFinishedListener)
    }

    fun removeOnFinishedListener(onFinishedListener: OnFinishedListener) {
        this.onFinishedListeners.remove(onFinishedListener)
    }

    /**
     * Indicates the current status of the task. Each status will be set only once during the lifetime of a task.
     */
    enum class Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that [AsyncTaskEx.onPostExecute] has finished.
         */
        FINISHED
    }

    interface OnFinishedListener {
        fun onFinished()
    }

    private class InternalHandler(mainLooper: Looper) : Handler(mainLooper) {
        override fun handleMessage(msg: Message) {
            val result = msg.obj as AsyncTaskExResult<*>
            when (msg.what) {
                MESSAGE_POST_RESULT ->
                    // There is only one result
                    result.task.finish(result.data)
            }
        }
    }

    private abstract class WorkerRunnable<Result> : Callable<Result>
    private class AsyncTaskExResult<Data>
    internal constructor(internal val task: AsyncTaskEx<*>, val data: Data?)

    companion object {
        private const val LOG_TAG = "CustomAsyncTask"
        private const val CORE_POOL_SIZE = 2
        private const val KEEP_ALIVE = 1
        private val sThreadFactory = object : ThreadFactory {
            private val count = AtomicInteger(1)
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "CustomAsyncTask #" + count.getAndIncrement())
            }
        }
        private val sPoolWorkQueue = LinkedBlockingQueue<Runnable>()

        /**
         * An [Executor] that executes tasks one at a time in serial order. This serialization is global to a
         * particular process.
         */
        // public static final Executor SERIAL_EXECUTOR =new SerialExecutor();
        private const val MESSAGE_POST_RESULT = 0x1
        private val sHandler = InternalHandler(Looper.getMainLooper())
        /**
         * return the number of cores of the device.
         * based on : http://stackoverflow.com/a/10377934/878126
         */
        private var coresCount: Int = 0
            get() {
                Runtime.getRuntime().availableProcessors()
                if (field > 0)
                    return field
                class CpuFilter : FileFilter {
                    override fun accept(pathname: File): Boolean {
                        return Pattern.matches("cpu[0-9]+", pathname.name)
                    }
                }
                try {
                    val dir = File("/sys/devices/system/cpu/")
                    val files = dir.listFiles(CpuFilter())
                    if (files != null) {
                        field = files.size
                        return field
                    }
                } catch (ignored: Exception) {
                }
                field = max(1, Runtime.getRuntime().availableProcessors())
                return field
            }
        private val MAXIMUM_POOL_SIZE = max(CORE_POOL_SIZE, coresCount - 1)
        /**
         * An [Executor] that can be used to execute tasks in parallel.
         */
        private val THREAD_POOL_EXECUTOR: Executor = ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE.toLong(), TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory)
        private val sDefaultExecutor = THREAD_POOL_EXECUTOR

        /**
         * Convenience version of [.execute] for use with a simple Runnable object. See [.execute] for more information on the order of execution.
         *
         * @see .execute
         * @see .executeOnExecutor
         */
        fun execute(runnable: Runnable) {
            sDefaultExecutor.execute(runnable)
        }
    }
}
