
# AsyncTaskEx
A modified version of AsyncTask, in Kotlin, with some things removed and some added

Using some simple clases in a similar manner to Glide and AsyncTask, you can run tiny tasks in the background easily.

Check the sample for an example of how to use it.

Here's a short version of it:

    private val pool = AsyncTaskThreadPool(1, 2)
    private val autoTaskManager = AutoTaskManager(this, null, pool, true)
    
    ...
    
    autoTaskManager.addTaskAndCancelPreviousTask(holder.itemView, object : AsyncTaskEx<Bitmap?>() {
    ...
    }).execute()
        

To import, use (link: https://jitpack.io/#AndroidDeveloperLB/AsyncTaskEx/ ) :

    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
    
    dependencies {
        implementation 'com.github.AndroidDeveloperLB:AsyncTaskEx:#'
    }
