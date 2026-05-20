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
import android.media.AudioManager
import android.media.ToneGenerator

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
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 1000)
    private var lastToastTime = 0L
    private var isDialogCaller: Boolean = false

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
        // LOG 1: Kiểm tra hàm có được gọi không
        Log.d("FACE_DEBUG", "--- Bắt đầu analyze ---")
        Log.d("FACE_DEBUG", "detectionInProgress: $detectionInProgress, detected: $detected, mutex locked: ${mutex.isLocked}")

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
//                                        if (it.headEulerAngleY < FACE_ROTATION_Y) {
//                                            sampleFaceRecognition?.let { sampleFaceRecognition ->
//
//                                                sampleFaceRecognition.listVectorEmbedding.forEach { sample ->
//                                                    val distanceScore: Float = cosineDistance(sample, subject)
//
//                                                    // --- CHÈN LOG TẠI ĐÂY ---
//                                                    Log.d("FACE_DEBUG", "--------------------------------------")
//                                                    Log.d("FACE_DEBUG", "Đang kiểm tra User: $userId")
//                                                    Log.d("FACE_DEBUG", "Khoảng cách đo được (Distance): $distanceScore")
//                                                    Log.d("FACE_DEBUG", "Ngưỡng thiết lập (Threshold): ${modelConfig.cosineThreshold}")
//                                                    // -----------------------
//
//                                                    var scoreByPercent: Int = if (distanceScore <= modelConfig.cosineThreshold) 100
//                                                        else (((2 * modelConfig.cosineThreshold - distanceScore) / modelConfig.cosineThreshold) * 100).roundToInt()
//                                                    if (scoreByPercent < 40) scoreByPercent = 0 // Chỉ chấp nhận trên 40% mới tính điểm, dưới đó coi như không khớp
//
//                                                    // --- LOG ĐIỂM SAU KHI TÍNH ---
//                                                    Log.d("FACE_DEBUG", "Điểm % tính được: $scoreByPercent")
//                                                    // ----------------------------
//
//                                                    if (highScoreRecognition < scoreByPercent) {
//                                                        highScoreRecognition =
//                                                            scoreByPercent
//                                                    }
//                                                    if (bestDistanceScore == null || bestDistanceScore!! > distanceScore) {
//                                                        bestDistanceScore = distanceScore
//                                                    }
//                                                }
//                                            }
//                                        }
                                        if (it.headEulerAngleY < FACE_ROTATION_Y) {
                                            // Xác định danh sách mẫu cần so sánh:
                                            // Nếu userId == null (Teacher) thì lấy tất cả mẫu trong listSampleFaceGroupData
                                            // Nếu userId != null (Student) thì chỉ lấy đúng mẫu của User đó
                                            val samplesToCheck = if (userId == null) {
                                                listSampleFaceGroupData
                                            } else {
                                                listSampleFaceGroupData.filter { it.name == userId }
                                            }

                                            // Duyệt qua danh sách mẫu đã xác định (Hỗ trợ cả 1:1 và 1:N)
                                            samplesToCheck.forEach { sampleFaceRecognition ->
                                                sampleFaceRecognition.listVectorEmbedding.forEach { sample ->
                                                    val distanceScore: Float = cosineDistance(sample, subject)

                                                    // --- CHÈN LOG TẠI ĐÂY ---
                                                    Log.d("FACE_DEBUG", "--------------------------------------")
                                                    Log.d("FACE_DEBUG", "Đang kiểm tra User: ${sampleFaceRecognition.name}")
                                                    Log.d("FACE_DEBUG", "Khoảng cách đo được (Distance): $distanceScore")
                                                    Log.d("FACE_DEBUG", "Ngưỡng thiết lập (Threshold): ${modelConfig.cosineThreshold}")
                                                    // -----------------------

                                                    var scoreByPercent: Int = if (distanceScore <= modelConfig.cosineThreshold) 100
                                                    else (((2 * modelConfig.cosineThreshold - distanceScore) / modelConfig.cosineThreshold) * 100).roundToInt()

                                                    if (scoreByPercent < 40) scoreByPercent = 0 // Chỉ chấp nhận trên 40% mới tính điểm, dưới đó coi như không khớp

                                                    // --- LOG ĐIỂM SAU KHI TÍNH ---
                                                    Log.d("FACE_DEBUG", "Điểm % tính được: $scoreByPercent")
                                                    // ----------------------------

                                                    if (highScoreRecognition < scoreByPercent) {
                                                        highScoreRecognition = scoreByPercent
                                                    }
                                                    if (bestDistanceScore == null || bestDistanceScore!! > distanceScore) {
                                                        bestDistanceScore = distanceScore
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Log kết quả cuối cùng trước khi trả về callback
                                Log.i("FACE_DEBUG", "==> KẾT QUẢ CUỐI CÙNG: $highScoreRecognition điểm $isDialogCaller")

                                // --- THÊM LOGIC PHÁT ÂM THANH TẠI ĐÂY ---
                                // Giả sử ngưỡng đạt là 40 điểm (bạn có thể thay đổi số này)
                                if (isDialogCaller && highScoreRecognition < 40) {
                                    val currentTime = System.currentTimeMillis()
                                    // Chỉ hiện Toast và phát âm thanh nếu đã qua ít nhất 2 giây kể từ lần trước
                                    if (currentTime - lastToastTime > 2000) {
                                        lastToastTime = currentTime

                                        playFailSound()

                                        withContext(Dispatchers.Main) {
                                            val message = "Khuôn mặt không khớp ($highScoreRecognition%)"

                                            // Tạo SpannableString để định dạng chữ
                                            val spannableString = android.text.SpannableString(message)

                                            // Phóng to chữ lên gấp 1.5 lần (bạn có thể chỉnh 2.0f nếu muốn to nữa)
                                            spannableString.setSpan(
                                                    android.text.style.RelativeSizeSpan(1.5f),
                                                    0,
                                                    message.length,
                                                    0
                                            )

                                            android.widget.Toast.makeText(
                                                    activity,
                                                    spannableString, // Truyền spannableString thay vì String thuần
                                                    android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    Log.d("FACE_DEBUG", "Phát âm thanh cảnh báo: Điểm thấp ($highScoreRecognition)")
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

    // Thêm vào FaceRecognitionProcessor.kt
    suspend fun compareTwoBitmaps(bitmap1: Bitmap, bitmap2: Bitmap): Int {
        return withContext(Dispatchers.Default) {
            // 1. Trích xuất vector (embedding) cho ảnh 1
            val emb1 = getEmbeddingFromSingleBitmap(bitmap1)
            // 2. Trích xuất vector (embedding) cho ảnh 2
            val emb2 = getEmbeddingFromSingleBitmap(bitmap2)

            if (emb1 == null || emb2 == null) return@withContext 0

            // 3. Tính khoảng cách Cosine
            val distance = cosineDistance(emb1, emb2)

            // 4. Chuyển đổi khoảng cách thành điểm số % (Sử dụng ngưỡng thắt chặt 0.11f)
            if (distance <= modelConfig.cosineThreshold) {
                // Khớp tốt (Điểm từ 90-100)
                (((modelConfig.cosineThreshold - distance) / modelConfig.cosineThreshold) * 10 + 40).toInt()
            } else {
                // Không khớp (Người lạ) -> Trả về 0 điểm
                0
            }
        }
    }

    // Hàm hỗ trợ trích xuất embedding từ 1 Bitmap duy nhất
    private suspend fun getEmbeddingFromSingleBitmap(bitmap: Bitmap): FloatArray? = suspendCoroutine { cont ->
        mlKitFaceDetection.process(bitmap, 0)
                .addOnSuccessListener { faces ->
                    val face = faces.lastOrNull()
                    if (face != null) {
                        val cropped = Utils.cropRectFromBitmap(activity, bitmap, face.boundingBox)
                        if (cropped != null) {
                            val buffer = convertBitmapToBuffer(cropped)
                            cont.resume(runFaceNet(buffer))
                        } else cont.resume(null)
                    } else cont.resume(null)
                }
                .addOnFailureListener { cont.resume(null) }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun startRecognition(
        faceGroupName: String?,
        isFromDialog: Boolean,
        resultCallback: (
            searchScore: Int,
            faceBitmap: Bitmap?,
            rect: Rect?,
            notFace: Boolean,
            notMask: Boolean
        ) -> Unit
    ) {
        this.isDialogCaller = isFromDialog
        listSampleFaceGroupData.clear()

        // LUÔN đặt detected = true khi bắt đầu nhận diện để hàm analyze thực hiện tính điểm
        detected = true

        CoroutineScope(Dispatchers.Default).launch {
            if (faceGroupName != null) {
                // Logic cũ cho Student (1:1): Tìm mẫu theo ID
                database.sampleFaceRecognitionDao().findSampleFaceByGroupName(faceGroupName)
                        ?.let { listSampleFaceGroupData.add(it) }

                // Nếu chưa tìm thấy mẫu trong DB, thử lại (giữ nguyên logic của bạn)
                if (listSampleFaceGroupData.size < 1) {
                    delay(500) // Thêm delay nhỏ để tránh loop quá nhanh
                    startRecognition(faceGroupName = faceGroupName, isFromDialog = isFromDialog, resultCallback = resultCallback)
                }
            } else {
                // Logic bổ sung cho Teacher (1:N): Tải TẤT CẢ mẫu khuôn mặt có trong DB local
                val allSamples = database.sampleFaceRecognitionDao().getAll()
                listSampleFaceGroupData.addAll(allSamples)
                android.util.Log.d("FaceRecog", "Teacher Login: Đã tải ${allSamples.size} mẫu khuôn mặt từ DB")
            }
        }

//        detected = false
//        faceGroupName?.let {
//            detected = true
//            CoroutineScope(Dispatchers.Default).launch {
//                database.sampleFaceRecognitionDao().findSampleFaceByGroupName(faceGroupName)
//                    ?.let { listSampleFaceGroupData.add(it) }
//                if (listSampleFaceGroupData.size < 1) {
//                    startRecognition(faceGroupName = faceGroupName, resultCallback = resultCallback)
//                }
//            }
//        }
        detectionInProgress = true
        this.resultRecognitionCallback = resultCallback
    }

    override fun stopRecognition() {
        detectionInProgress = false
        isDialogCaller = false
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
    private fun playFailSound() {
        try {
            // TONE_SUP_ERROR thường kéo dài khoảng 2-3 giây mặc định hoặc theo tham số duration
            // 1000 ở đây là thời gian phát (miliseconds)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 1000)

            Log.d("FACE_DEBUG", "Đang phát âm thanh lỗi...")
        } catch (e: Exception) {
            Log.e("FACE_DEBUG", "Không thể phát âm thanh: ${e.message}")
        }
        // KHÔNG gọi toneGen.release() ở đây!
    }

    fun releaseResources() {
        try {
            toneGen.release() // Giải phóng âm thanh
            interpreter.close() // Giải phóng TFLite (Rất quan trọng để tránh crash)
            mlKitFaceDetection.close() // Giải phóng ML Kit
            Log.d("FACE_DEBUG", "Đã giải phóng toàn bộ tài nguyên")
        } catch (e: Exception) {
            Log.e("FACE_DEBUG", "Lỗi khi giải phóng: ${e.message}")
        }
    }

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