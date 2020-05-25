package com.lb.async_task_ex

import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**used to handle tasks that are associated to views, similar to Glide */
class AutoTaskManager(activity: AppCompatActivity?, fragment: androidx.fragment.app.Fragment?,
                      private val threadPool: AsyncTaskThreadPool?, private val cancelUsingInterruption: Boolean = false) {
    private val tasks = HashMap<View, AsyncTaskEx<*>>()

    init {
        fragment?.lifecycle ?: activity?.lifecycle?.addObserver(object : LifecycleObserver {
            @Suppress("unused")
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() = cancelAllTasks()
        })
    }

    @UiThread
    fun cancelAllTasks() {
        threadPool?.cancelAllTasks(cancelUsingInterruption)
        for (task in tasks.values)
            task.cancel(cancelUsingInterruption)
        tasks.clear()
    }

    @UiThread
    fun cancelTaskOfView(view: View) {
        val previousTask = tasks[view]
        previousTask?.cancel(cancelUsingInterruption)
    }

    @UiThread
    fun <A> addTaskAndCancelPreviousTask(view: View, task: AsyncTaskEx<A>): AsyncTaskEx<A> {
        //TODO add function for also executing right away
        val previousTask = tasks[view]
        previousTask?.cancel(cancelUsingInterruption)
        tasks[view] = task
        return task
    }
}
