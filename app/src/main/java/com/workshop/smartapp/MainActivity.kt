package com.workshop.smartapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.workshop.smartapp.data.WorkshopDatabase
import com.workshop.smartapp.ui.WorkshopApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = WorkshopDatabase(applicationContext)
        setContent {
            WorkshopApp(database = database)
        }
    }
}
