package com.albertlis.myapplication1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.theartofdev.edmodo.cropper.CropImage
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
        it?.let { uri ->
            val imageView = findViewById<ImageView>(R.id.imageView)
            imageView.setImageURI(uri)
            classifyDrawing(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        cropActivityResultLauncher.launch(null)
        imageClassifier
            .initialize()
            .addOnFailureListener { e -> Log.e(
                "PredictActivity",
                "Error to setting up digit classifier.",
                e
            ) }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        imageClassifier.close()
        super.onDestroy()
    }


    private fun classifyDrawing(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        if ((bitmap != null) && (imageClassifier.isInitialized)) {
            val predictView = findViewById<TextView>(R.id.predictView)
            imageClassifier
                .classifyAsync(bitmap)
                .addOnSuccessListener { resultText -> predictView.text = resultText }
                .addOnFailureListener { e ->
                    predictView.text = "Something went wrong"
                }
        }
    }
}