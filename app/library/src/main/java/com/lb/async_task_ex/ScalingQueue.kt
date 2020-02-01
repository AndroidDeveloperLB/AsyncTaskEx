package com.lb.async_task_ex

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor

class ScalingQueue<T> : LinkedBlockingQueue<T> {
    /**
     * The executor this Queue belongs to
     */
    private var executor: ThreadPoolExecutor? = null

    /**
     * Creates a TaskQueue with a capacity of [Integer.MAX_VALUE].
     */
    constructor() : super()

    /**
     * Creates a TaskQueue with the given (fixed) capacity.
     *
     * @param capacity the capacity of this queue.
     */
    constructor(capacity: Int) : super(capacity)

    /**
     * Sets the executor this queue belongs to.
     */
    fun setThreadPoolExecutor(executor: ThreadPoolExecutor) {
        this.executor = executor
    }

    /**
     * Inserts the specified element at the tail of this queue if there is at least one available thread to run the
     * current task. If all pool threads are actively busy, it rejects the offer.
     *
     * @param o the element to add.
     * @return true if it was possible to add the element to this queue, else false
     * @see ThreadPoolExecutor.execute
     */
    override fun offer(o: T): Boolean {
        val allWorkingThreads = executor!!.activeCount + super.size
        return allWorkingThreads < executor!!.poolSize && super.offer(o)
    }

    companion object {
        private const val serialVersionUID = 2868771663367097439L
    }
}
