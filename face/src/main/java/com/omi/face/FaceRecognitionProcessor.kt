package com.omi.face

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.lws.device.camerapreview.Nv21ImageData
import com.lws.type.Logger
import com.omi.face.model.MaskDetectionModel
import com.omi.face.model.ModelInfo
import com.omi.face.model.database.AppDatabase
import com.omi.face.model.database.entity.SampleFaceRecognition
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import java.nio.ByteBuffer
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis


class FaceRecognitionProcessor constructor(
        private val activity: Context,
) : FaceRecognition {
    companion object {
        const val FACE_ROTATION_Y = 40
        val FACENET_512_QUANTIZED = ModelInfo(
            "FaceNet-512 Quantized" ,
            "facenet_512_int_quantized.tflite" ,
            0.2f ,
            19f ,
            512 ,
            160
        )
    }

    private val database: AppDatabase = AppDatabase.getInstance(activity)
    private val listSampleFaceGroupData = mutableListOf<SampleFaceRecognition>()
    private val modelConfig = FACENET_512_QUANTIZED
    private val maskDetectionModel = MaskDetectionModel( activity )
    private val useGpu = false
    private val useXNNPack = true
    private var interpreter: Interpreter
    private var detectionInProgress = false
    private var detected = false
    private var highScoreRecognition = 0
    private val mutex = Mutex()

    private lateinit var resultRecognitionCallback: (
            searchScore: Int,
            faceBitmap: Bitmap?,
            rect: Rect?,
            notFace: Boolean,
            notMask: Boolean
    ) -> Unit

    // Todo check other options config
    private val mlKitFaceDetection = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build()
    )

    // Todo check other options config
    // Todo handle in StandardizeOp
    private val imageTensorProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelConfig.inputDims, modelConfig.inputDims, ResizeOp.ResizeMethod.BILINEAR))
            .add(StandardizeOp())
            .build()

    init {
        // Initialize TFLiteInterpreter
        val interpreterOptions = Interpreter.Options().apply {
            // Add the GPU Delegate if supported.
            // See -> https://www.tensorflow.org/lite/performance/gpu#android
            if (useGpu) {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    addDelegate(GpuDelegate(CompatibilityList().bestOptionsForThisDevice))
                }
            } else {
                // Number of threads for computation
                numThreads = 4
            }
            this.useNNAPI = true
            this.useXNNPACK = useXNNPACK
        }
        interpreter = Interpreter(FileUtil.loadMappedFile(activity, modelConfig.assetsFilename), interpreterOptions)

    }

        fun faceDetect(image: Nv21ImageData, onFaceDetected: (Rect?) -> Unit) {
            val imageInput = InputImage.fromByteArray(
                image.nv21Data,
                image.width,
                image.height,
                image.rotation,
                InputImage.IMAGE_FORMAT_NV21
            )
            mlKitFaceDetection.process(imageInput)
                .addOnSuccessListener { faces ->
                    val faceRect = faces.lastOrNull()?.boundingBox
                    onFaceDetected(faceRect)
                }
                .addOnFailureListener {
                    onFaceDetected(null)
                }
        }

    suspend fun analyze(image: Bitmap, userId: String?) {
        if (detectionInProgress && !mutex.isLocked) {
            resetDetectResult()
            var notFace = true
            var notMask = false
            var rect: Rect? = null
            val timeSpent = measureTimeMillis {
                val frameBitmap = Utils.rotateBitmap(image, 0f)
                // Configure frameHeight and frameWidth for output2overlay transformation matrix.
                // ml-kit perform process detect face
                val faces = suspendCoroutine<List<Face>> { continuation ->
                    mlKitFaceDetection.process(image, 0)
                        .addOnSuccessListener { faces ->
                            continuation.resume(faces)
                        }
                        .addOnCompleteListener {
                            Log.i("vinhdt", "addOnCompleteListener")
                        }
                        .addOnFailureListener {
                            Log.i("vinhdt", "addOnFailureListener")
                        }
                        .addOnCanceledListener {
                            Log.i("vinhdt", "addOnCanceledListener")
                        }
                }
                withContext(Dispatchers.IO) {
                    mutex.withLock {
                        // have more than one face if have multiple people in the image
                        // but in this case, we just need only one face
                        // get last image because nearest face at the end
                        if (faces.isEmpty()) {
                            resultRecognitionCallback(
                                0,
                                null,
                                rect,
                                true,
                                false
                            )
                        } else {
                            faces.lastOrNull()?.also { it ->
                                rect = it.boundingBox

                                // Sample detect liveness
//                            var rgba = Mat(frameBitmap.height, frameBitmap.width, CvType.CV_8UC4)
//                            org.opencv.android.Utils.bitmapToMat(frameBitmap, rgba)
//                            val gray = Mat()
//                            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
//                            equalizeHist(gray, gray)
//                            val livenessScore = extractLBPFeatures(gray)
//                            Logger.i("face livenessScore: $livenessScore")
                                notFace = false
                                val imageCropped = Utils.cropRectFromBitmap(
                                    activity = activity,
                                    source = image,
                                    rect = it.boundingBox,
//                                nameSaving = "${groupName}-${Calendar.getInstance().timeInMillis}"
                                )
                                if (detected) {
                                    imageCropped?.also { _ ->
                                        val imageBuffer: ByteBuffer =
                                            convertBitmapToBuffer(imageCropped)
                                        val subject: FloatArray = runFaceNet(imageBuffer)
                                        var bestDistanceScore: Float? = null
                                        val sampleFaceRecognition =
                                            listSampleFaceGroupData.find {
                                                it.name == userId
                                            }
                                        notMask =
                                            maskDetectionModel.detectMask(imageCropped)
                                        if (it.headEulerAngleY < FACE_ROTATION_Y) {
                                            sampleFaceRecognition?.let { sampleFaceRecognition ->

                                                sampleFaceRecognition.listVectorEmbedding.forEach { sample ->
                                                    val distanceScore: Float =
                                                        cosineDistance(sample, subject)
                                                    var scoreByPercent: Int =
                                                        if (distanceScore <= modelConfig.cosineThreshold) 100
                                                        else (((2 * modelConfig.cosineThreshold - distanceScore) / modelConfig.cosineThreshold) * 100).roundToInt()
                                                    if (scoreByPercent < 0) scoreByPercent =
                                                        0
                                                    if (highScoreRecognition < scoreByPercent) {
                                                        highScoreRecognition =
                                                            scoreByPercent
                                                    }
                                                    if (bestDistanceScore == null || bestDistanceScore!! > distanceScore) {
                                                        bestDistanceScore = distanceScore
                                                    }
                                                }
                                            }
                                        }

                                    }
                                }
                                resultRecognitionCallback(
                                    highScoreRecognition,
                                    imageCropped,
                                    rect,
                                    notFace,
                                    notMask
                                )
                            }
                        }
                    }
                }

            }
        }
    }

    override fun addSampleFaces(
        id: String,
        groupName: String,
        bitmap: Bitmap,
        resultCallback: (state: AddFaceResult, message: String?, successCounter: Int) -> Unit
    ) {
        Logger.d("addSampleFaces: ${mutex.isLocked}")
        CoroutineScope(Dispatchers.IO).launch(
            CoroutineExceptionHandler { _, ex ->
                Log.e("addSampleFaces error:", "${ex.message}")
                resultCallback(AddFaceResult.FAIL, ex.message, 0)
            }
        ) {
            if (mutex.isLocked) {
                delay(1000)
                addSampleFaces(
                    id, groupName, bitmap, resultCallback
                )
            } else {
                var successCounter = 0
//            images.forEach { bitmap ->
                val timeSpent = measureTimeMillis {
                    // Todo instead OpenCV to ML-Kit late
                    // Use ML_Kit for face detection
                    /**
                     * input image: bitmap
                     * orientation: 0
                     */
                    mlKitFaceDetection.process(bitmap, 0).addOnSuccessListener {
                        CoroutineScope(Dispatchers.IO).launch(
                            CoroutineExceptionHandler { _, ex ->
                                Logger.e("Error in show current time!: $ex")
                            }
                        ) {
                            mutex.withLock {
                                if (it.size > 0) {
                                    ++successCounter
                                    // have more than one face if have multiple people in the image
                                    // but in this case, we just need only one face
                                    // get last image because nearest face at the end
                                    it.lastOrNull()?.let { face ->
                                        val boundingBox = face.boundingBox
                                        val imageCropped = Utils.cropRectFromBitmap(
                                            activity = activity,
                                            source = bitmap,
                                            rect = boundingBox,
                                            nameSaving = "${groupName}-${Calendar.getInstance().timeInMillis}"
                                        )
                                        imageCropped?.also {
                                            val imageBuffer: ByteBuffer =
                                                convertBitmapToBuffer(imageCropped)
                                            val faceEmbedding: FloatArray = runFaceNet(imageBuffer)
                                            updateFaceSample(groupName, faceEmbedding)

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateFaceSample(groupName: String, faceEmbedding: FloatArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val sampleFace = database.sampleFaceRecognitionDao().findSampleFaceByGroupName(groupName)
            if (sampleFace != null) {
                sampleFace.listVectorEmbedding.add(faceEmbedding)
                database.sampleFaceRecognitionDao().updateSampleFaceRecognition(sampleFace)
            } else {
                database.sampleFaceRecognitionDao().insert(SampleFaceRecognition(name = groupName, listVectorEmbedding = mutableListOf(faceEmbedding)))
            }
        }

    }

    override suspend fun getAllFaceSample(): List<SampleFaceRecognition> {
        return database.sampleFaceRecognitionDao().getAll()
    }

    override suspend fun deleteFaceSample(name: String) {
        database.sampleFaceRecognitionDao().deleteSampleFaceByGroupName(name)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun startRecognition(
        faceGroupName: String?,
        resultCallback: (
            searchScore: Int,
            faceBitmap: Bitmap?,
            rect: Rect?,
            notFace: Boolean,
            notMask: Boolean
        ) -> Unit
    ) {
        listSampleFaceGroupData.clear()
        detected = false
        faceGroupName?.let {
            detected = true
            CoroutineScope(Dispatchers.Default).launch {
                database.sampleFaceRecognitionDao().findSampleFaceByGroupName(faceGroupName)
                    ?.let { listSampleFaceGroupData.add(it) }
                if (listSampleFaceGroupData.size < 1) {
                    startRecognition(faceGroupName = faceGroupName, resultCallback = resultCallback)
                }
            }
        }
        detectionInProgress = true
        this.resultRecognitionCallback = resultCallback
    }

    override fun stopRecognition() {
        detectionInProgress = false
        listSampleFaceGroupData.clear()
        CoroutineScope(Dispatchers.Main).launch {
            // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
        }
    }

    override fun resetDetectResult() {
        highScoreRecognition = 0
//        listSampleFaceGroupData.forEach {
//            it.highScoreRecognition = 0
//        }
    }

    private fun runFaceNet(buffer: ByteBuffer): FloatArray {
        val faceNetModelOutputs = Array(1) { FloatArray(modelConfig.outputDims) }
        interpreter.run(buffer, faceNetModelOutputs)
        // Todo check why get only one element -> Check in native of Tensorflow
        return faceNetModelOutputs.first()
    }


    /**
     * By ChatGPT Cosine Similarity usually use for face recognition which present
     * by vector embedding make by FaceNet, OpenFace or other
     */
    private fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            normA += vector1[i] * vector1[i]
            normB += vector2[i] * vector2[i]
        }

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    private fun cosineDistance(vector1: FloatArray, vector2: FloatArray): Float {
        return 1 - cosineSimilarity(vector1, vector2)
    }

    // Resize the given bitmap and convert it to a ByteBuffer
    private fun convertBitmapToBuffer(image: Bitmap): ByteBuffer {
        // Tenserflow perform process
        return imageTensorProcessor.process(TensorImage.fromBitmap(image)).buffer
    }


}

// Op to perform standardization
// x' = ( x - mean ) / std_dev
class StandardizeOp : TensorOperator {

    override fun apply(p0: TensorBuffer?): TensorBuffer {
        val pixels = p0!!.floatArray
        val mean = pixels.average().toFloat()
        var std = sqrt(pixels.map { pi -> (pi - mean).pow(2) }.sum() / pixels.size.toFloat())
        std = max(std, 1f / sqrt(pixels.size.toFloat()))
        for (i in pixels.indices) {
            pixels[i] = (pixels[i] - mean) / std
        }
        val output = TensorBufferFloat.createFixedSize(p0.shape, DataType.FLOAT32)
        output.loadArray(pixels)
        return output
    }

}

enum class AddFaceResult {
    SUCCESS,
    FAIL
}