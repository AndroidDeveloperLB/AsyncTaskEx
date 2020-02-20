
# AsyncTaskEx
A modified version of AsyncTask, in Kotlin, with some things removed and some added

Using some simple clases in a similar manner to Glide and AsyncTask, you can run tiny tasks in the background easily.

Advantages:
1. Can be created and executed everywhere. Not just on the main, UI thread
2. Optionally be auto-cancelled via the lifecycle
3. Has only one value to handle: the result. For parameters and progress, you can add your own implementation.
4. Kotlin, so safer in terms of nullability
5. Consistent behavior for all Android versions.

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
