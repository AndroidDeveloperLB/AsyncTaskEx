package com.lb.asynctaskex

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.lb.async_task_ex.AsyncTaskEx
import com.lb.async_task_ex.AsyncTaskThreadPool
import com.syncme.utils.images.AutoTaskManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_item.view.*

class MainActivity : AppCompatActivity() {
    private val pool = AsyncTaskThreadPool(1, 2)
    private val autoTaskManager = AutoTaskManager(this, null, pool, true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val installedApplications = packageManager.getInstalledApplications(0)
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(LayoutInflater.from(this@MainActivity).inflate(R.layout.list_item, parent, false)) {}
            }

            override fun getItemCount(): Int = installedApplications.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                holder.itemView.imageView.setImageBitmap(null)
                val applicationInfo = installedApplications[position]
                holder.itemView.textView.text = applicationInfo.loadLabel(packageManager) as String
                autoTaskManager.addTaskAndCancelPreviousTask(holder.itemView, object : AsyncTaskEx<Bitmap?>() {
                    override fun doInBackground(): Bitmap? {
//                        val startTime = System.currentTimeMillis()
                        //this function is usually very quick, but even on Pixel 4 it can take up to 54 ms, so we use background thread for it:
                        val result = Utils.getAppIcon(this@MainActivity, applicationInfo)
//                        val endTime = System.currentTimeMillis()
//                        Log.d("AppLog", "time:${endTime - startTime}")
                        return result
                    }

                    override fun onPostExecute(result: Bitmap?) {
                        super.onPostExecute(result)
                        holder.itemView.imageView.setImageBitmap(result)
                    }
                }).execute()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var url: String? = null
        when (item.itemId) {
            R.id.menuItem_all_my_apps -> url = "https://play.google.com/store/apps/developer?id=AndroidDeveloperLB"
            R.id.menuItem_all_my_repositories -> url = "https://github.com/AndroidDeveloperLB"
            R.id.menuItem_current_repository_website -> url = "https://github.com/AndroidDeveloperLB/AsyncTaskEx"
        }
        if (url == null)
            return true
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        @Suppress("DEPRECATION")
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        startActivity(intent)
        return true
    }
}
