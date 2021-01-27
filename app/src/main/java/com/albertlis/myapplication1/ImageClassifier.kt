package com.albertlis.myapplication1

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.provider.MediaStore
import com.albertlis.myapplication1.ml.Flowers
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.checkerframework.checker.nullness.qual.NonNull
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ImageClassifier(private val context: Context) {
    var isInitialized = false
        private set

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0

    fun initialize(): Task<Void> {
        val task = TaskCompletionSource<Void>()
        executorService.execute {
            try {
                initializeInterpreter()
                task.setResult(null)
            } catch (e: IOException) {
                task.setException(e)
            }
        }
        return task.task
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {

        val assetManager = context.assets
        val model = loadModelFile(assetManager, "flowers.tflite")
        val options = Interpreter.Options()
//    options.setUseNNAPI(true)
        val interpreter = Interpreter(model, options)
        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        interpreter.close()

        isInitialized = true
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun classify(uri: Uri): String {
        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }

        val imageBuffer = preproces_image(uri)
        val predictions: Map<String, Float> = predict(imageBuffer)

//        println(predictions)
        val maxValue = predictions.maxByOrNull { it.value }
        return "$maxValue"
    }

    private fun predict(imageBuffer: @NonNull TensorBuffer): Map<String, Float> {
        val model = Flowers.newInstance(context)
        val outputs = model.process(imageBuffer)
        val labels = FileUtil.loadLabels(context, "labels.txt")
        return TensorLabel(labels, outputs.outputFeature0AsTensorBuffer).mapWithFloatValue
    }

    private fun preproces_image(uri: Uri): @NonNull TensorBuffer {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageWidth, inputImageHeight, ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(127.5F, 127.5F))
            .add(QuantizeOp(128.0F, (1 / 128.0).toFloat()))
            .build()
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val tfImage = TensorImage.fromBitmap(bitmap)
        return imageProcessor.process(tfImage).tensorBuffer
    }

    fun classifyAsync(uri: Uri): Task<String> {
        val task = TaskCompletionSource<String>()
        executorService.execute {
            val result = classify(uri)
            task.setResult(result)
        }
        return task.task
    }
}
