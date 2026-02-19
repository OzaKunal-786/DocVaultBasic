package com.docvaultbasic

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class DocVaultApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            // OpenCV initialization failed
            android.util.Log.e("DocVaultApp", "OpenCV initialization failed!")
        } else {
            // OpenCV initialized successfully
            android.util.Log.d("DocVaultApp", "OpenCV initialized successfully!")
        }
    }
}
