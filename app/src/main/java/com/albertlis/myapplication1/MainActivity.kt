package com.albertlis.myapplication1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.theartofdev.edmodo.cropper.CropImage


class MainActivity : AppCompatActivity() {
    private val resultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK)
            Toast.makeText(this@MainActivity, "Ok", Toast.LENGTH_LONG).show()
        else if (result.resultCode == Activity.RESULT_CANCELED)
            Toast.makeText(this@MainActivity, "Canceled", Toast.LENGTH_LONG).show()
        else
            Toast.makeText(this@MainActivity, "Error", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = findViewById<Button>(R.id.chooseBtn)

        btn.setOnClickListener {
            val intent = Intent(this, PredictActivity::class.java)
            resultContract.launch(intent)
        }
    }
}