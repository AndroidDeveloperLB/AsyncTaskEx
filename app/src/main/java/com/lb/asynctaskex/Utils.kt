package com.lb.asynctaskex

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.min
import kotlin.math.roundToInt

object Utils {
    private var appIconSize = 0

    @JvmStatic
    @JvmOverloads
    fun getAppIcon(context: Context, applicationInfo: ApplicationInfo, requestedImageSize: Int = 0): Bitmap? {
        val packageName = applicationInfo.packageName
        val iconResId = applicationInfo.icon
        val packageManager = context.packageManager
        while (true) {
            if (iconResId == 0)
                break
            val resources: Resources?
            try {
                resources = packageManager.getResourcesForApplication(applicationInfo)
                if (resources == null)
                    break
            } catch (e: Exception) {
                break
            }
            val bitmapOptions: BitmapFactory.Options?
            try {
                bitmapOptions = getBitmapOptions(resources, iconResId)
            } catch (e: Exception) {
                break
            }
            if (bitmapOptions.outHeight <= 0 || bitmapOptions.outWidth <= 0) {
                try {
                    val densityToUse = context.resources.displayMetrics.densityDpi.coerceAtLeast(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) DisplayMetrics.DENSITY_XXXHIGH else DisplayMetrics.DENSITY_XXHIGH)
                    val drawableForDensity = ResourcesCompat.getDrawableForDensity(resources, iconResId, densityToUse, null)
                    val drawable: Drawable = drawableForDensity ?: (run {
                        val otherContext = context.createPackageContext(packageName, 0)
                        AppCompatResources.getDrawable(otherContext, iconResId)
                    }) ?: break
                    val result: Bitmap?
                    result = convertAppIconDrawableToBitmap(context, drawable)
                    return result
                } catch (ignored: Exception) {
                }
                break
            }
            val appIconSize = if (requestedImageSize <= 0) getAppIconSize(context) else min(requestedImageSize, getAppIconSize(context))
            prepareBitmapOptionsForSampling(bitmapOptions, appIconSize, appIconSize)
            val result = BitmapFactory.decodeResource(resources, iconResId, bitmapOptions)
            if (result != null)
                return result
            break
        }
        try {
            val icon = packageManager.getApplicationIcon(packageName)
            return convertAppIconDrawableToBitmap(context, icon)
        } catch (ignored: Exception) {
        }
        return null
    }

    @JvmStatic
    fun getBitmapOptions(res: Resources, resId: Int): BitmapFactory.Options {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, bitmapOptions)
        return bitmapOptions
    }

    @JvmStatic
    fun convertAppIconDrawableToBitmap(context: Context, drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable)
            return drawable.bitmap
        val appIconSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable)
            convertDpToPixels(context, 108f).toInt()
        else getAppIconSize(context)
        return drawable.toBitmap(appIconSize, appIconSize, Bitmap.Config.ARGB_8888)
    }

    @JvmStatic
    fun convertDpToPixels(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    @JvmStatic
    fun getAppIconSize(context: Context): Int {
        if (appIconSize > 0)
            return appIconSize
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        //https://console.firebase.google.com/u/0/project/app-manager-cdf2c/crashlytics/app/android:com.lb.app_manager/issues/d5c98cf94a3d87148b915413f2583657?time=last-seven-days&sessionId=5D4B95C60017000214A7E2D1546BCD11_DNE_0_v2
        //TODO maybe after API 6 it's not needed to use try-catch
        appIconSize = try {
            activityManager.launcherLargeIconSize
        } catch (e: Exception) {
            convertDpToPixels(context, 48f).toInt()
        }
        return appIconSize
    }

    @JvmStatic
    fun prepareBitmapOptionsForSampling(bitmapOptions: BitmapFactory.Options, reqWidth: Int, reqHeight: Int) {
        bitmapOptions.inTargetDensity = 1
        bitmapOptions.inJustDecodeBounds = false
        if (reqHeight <= 0 && reqWidth <= 0)
            return
        bitmapOptions.inDensity = 1
        var sampleSize = 1
        bitmapOptions.inSampleSize = 1
        val height = bitmapOptions.outHeight
        val width = bitmapOptions.outWidth
        var preferHeight = false
        bitmapOptions.inDensity = 1
        bitmapOptions.inTargetDensity = 1
        if (height <= reqHeight && width <= reqWidth)
            return
        if (height > reqHeight || width > reqWidth)
            if (width > height && reqHeight >= 1) {
                preferHeight = true
                sampleSize = (height.toFloat() / reqHeight.toFloat()).roundToInt()
            } else if (reqWidth >= 1) {
                sampleSize = (width.toFloat() / reqWidth.toFloat()).roundToInt()
                preferHeight = false
            }
        // as much as possible, use google's way to downsample:
        while (bitmapOptions.inSampleSize * 2 <= sampleSize)
            bitmapOptions.inSampleSize *= 2
        // if google's way to downsample isn't enough, do some more :
        if (bitmapOptions.inSampleSize != sampleSize) {
            // downsample by bitmapOptions.inSampleSize/originalSampleSize .
            bitmapOptions.inTargetDensity = bitmapOptions.inSampleSize
            bitmapOptions.inDensity = sampleSize
        } else if (sampleSize == 1) {
            bitmapOptions.inTargetDensity = if (preferHeight) reqHeight else reqWidth
            bitmapOptions.inDensity = if (preferHeight) height else width
        }
    }
}
