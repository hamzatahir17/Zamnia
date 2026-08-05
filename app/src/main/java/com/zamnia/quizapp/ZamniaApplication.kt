package com.zamnia.quizapp

import android.app.Application

class ZamniaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize the backend engine synchronously to ensure 
        // repository is ready before ViewModels are created.
        ZamniaEngine.initialize(this)
    }
}
