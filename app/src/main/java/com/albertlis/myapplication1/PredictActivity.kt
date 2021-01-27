package com.albertlis.myapplication1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.theartofdev.edmodo.cropper.CropImage
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*


class PredictActivity : AppCompatActivity() {
    private var imageClassifier = ImageClassifier(this)

    private val cropActivityResultContract = object : ActivityResultContract<Any?, Uri>() {
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity()
                .setAspectRatio(1, 1)
                .getIntent(this@PredictActivity)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }
    }

    private val cropActivityResultLauncher = registerForActivityResult(cropActivityResultContract) {
        it?.let { imageUri ->
            val imageView = findViewById<ImageView>(R.id.imageView)
            imageView.setImageURI(imageUri)
            classifyImage(imageUri)
        } ?: run {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        cropActivityResultLauncher.launch(null)
        imageClassifier.initialize()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun classifyImage(imageUri: Uri) {
        if (imageClassifier.isInitialized) {
            val predictView = findViewById<TextView>(R.id.predictView)
            imageClassifier
                .classifyAsync(imageUri)
                .addOnSuccessListener { resultText ->
                    val parts = resultText.split("=")
                    val prediction = parts[0]
                    val value = parts[1].toFloat() * 100
                    val df = DecimalFormat("##.#")
                    df.roundingMode = RoundingMode.CEILING
                    predictView.text = "It's $prediction with probability ${df.format(value)}%" }
                .addOnFailureListener { _ ->
                    predictView.text = "Something went wrong"
                }
        }
    }
}