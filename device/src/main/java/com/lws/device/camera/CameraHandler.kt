package com.lws.device.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.lws.type.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CameraHandler: CameraDevice {

    private var cameraEvent: CameraEvent? = null
    private lateinit var photoURI: Uri

    private val reqPermissionChannel = Channel<Boolean>()
    private var reqPermissionScope: CoroutineScope? = null

    companion object {
        private const val REQUEST_TAKE_PICTURE = 1

        private const val WRITE_STORAGE_PERMISSION_REQUEST_CODE = 2
        val LIST_PERMISSION_REQUIRED: List<Pair<String, Int>> = listOf(
            Pair(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_STORAGE_PERMISSION_REQUEST_CODE)
        )
    }

    override suspend fun takePicture(activity: Activity, cameraEvent: CameraEvent) {
        Logger.d("dispatchTakePicture")

        reqPermissionScope = CoroutineScope(Dispatchers.Default)

        reqPermissionScope?.launch {
            checkPermissionRequired(activity)
        }
        var limitTimes = 10
        while (!reqPermissionChannel.receive() && limitTimes > 0) {
            Logger.w("Exist permissions must be required by function's mandatory!")
            limitTimes--
        }
        if (limitTimes <= 0) {
            reqPermissionScope?.cancel()
            return
        }

        this.cameraEvent = cameraEvent
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(activity.packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile(activity)
                } catch (ex: IOException) {
                    cameraEvent.onTakenPicture(ResultStatus.ERROR, null)
                    Logger.e("Error occurred while creating the File")
                    null
                }
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(
                        activity,
                        activity.application.packageName + ".provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    activity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PICTURE)
                    activity.contentResolver
                }
            }
        }
    }

    override var activityResultCallback: (activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) -> Unit =
        { activity: Activity, requestCode: Int, resultCode: Int, data: Intent? ->
            Logger.d("activityResultCallback requestCode: $requestCode | resultCode: $resultCode | data: $data")
            if (requestCode == REQUEST_TAKE_PICTURE) {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val bitmap =
                            MediaStore.Images.Media.getBitmap(activity.contentResolver, photoURI)
                        cameraEvent?.onTakenPicture(ResultStatus.SUCCESS, bitmap)
                    }
                    Activity.RESULT_CANCELED -> cameraEvent?.onTakenPicture(
                        ResultStatus.CANCELED,
                        null
                    )
                }
            }
        }

    override var requestPermissionResultCallback: (
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) -> Unit =
        { activity: Activity, requestCode: Int, permissions: Array<out String>, grantResults: IntArray ->
            Logger.d("requestPermissionResultCallback requestCode: $requestCode | permissions: $permissions | grantResults: $grantResults")
            if (LIST_PERMISSION_REQUIRED.map { it.second }.contains(requestCode)) {
                reqPermissionScope?.launch {
                    checkPermissionRequired(activity)
                }
            }
        }

    private suspend fun checkPermissionRequired(activity: Activity) {
        reqPermissionScope?.run {
            if (isActive) {
                LIST_PERMISSION_REQUIRED.firstOrNull {
                    activity.checkSelfPermission(it.first) != PackageManager.PERMISSION_GRANTED
                }?.run {
                    activity.requestPermissions(
                        arrayOf(this.first), this.second
                    )
                    reqPermissionChannel.send(false)
                } ?: reqPermissionChannel.send(true)
            }
        }
    }

    lateinit var currentPhotoPath: String

    private fun createImageFile(activity: Activity): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
}