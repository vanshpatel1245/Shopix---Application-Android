package com.example.seller

import android.net.Uri
import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CloudinaryUploader {

    // ✅ Paste YOUR values from Cloudinary Dashboard here
    private const val CLOUD_NAME   = "diddjxukp"    // e.g. dxyz1234
    private const val UPLOAD_PRESET = "shopix_preset"      // the unsigned preset you created

    // Upload image and return the URL via callback
    fun uploadImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Thread {
            try {
                // Convert Uri to File
                val inputStream = context.contentResolver.openInputStream(imageUri)!!
                val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(tempFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                // Build multipart request
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody)
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val imageUrl = json.getString("secure_url") // ✅ this is the image URL
                    onSuccess(imageUrl)
                } else {
                    onFailure("Upload failed: ${response.code}")
                }

                tempFile.delete() // clean up temp file

            } catch (e: Exception) {
                onFailure("Error: ${e.message}")
            }
        }.start()
    }
}