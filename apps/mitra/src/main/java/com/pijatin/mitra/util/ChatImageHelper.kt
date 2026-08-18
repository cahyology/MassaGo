package com.pijatin.mitra.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.media.ExifInterface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ChatImageHelper {

    /**
     * Compress bitmap to crisp high-definition resolution (max 1280px) and encode to base64 string
     */
    fun compressBitmapToBase64(bitmap: Bitmap, maxDimension: Int = 1280, quality: Int = 85): String {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()

        val finalWidth: Int
        val finalHeight: Int
        if (width > height) {
            finalWidth = width.coerceAtMost(maxDimension)
            finalHeight = (finalWidth / ratio).toInt().coerceAtLeast(1)
        } else {
            finalHeight = height.coerceAtMost(maxDimension)
            finalWidth = (finalHeight * ratio).toInt().coerceAtLeast(1)
        }

        val scaled = if (finalWidth != width || finalHeight != height) {
            Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Read image from content Uri with orientation fix, high quality scaling, and base64 encoding
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            val maxBound = 1440
            while (origWidth / (sampleSize * 2) >= maxBound && origHeight / (sampleSize * 2) >= maxBound) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val initialBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (initialBitmap == null) return null
            var resultBitmap: Bitmap = initialBitmap

            // Check EXIF Orientation
            try {
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (!matrix.isIdentity) {
                        val currentBmp = resultBitmap
                        val rotated = Bitmap.createBitmap(
                            currentBmp, 0, 0, currentBmp.width, currentBmp.height, matrix, true
                        )
                        resultBitmap = rotated
                    }
                }
            } catch (_: Exception) {}

            compressBitmapToBase64(resultBitmap, maxDimension = 1280, quality = 85)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decode base64 string back to Compose ImageBitmap
     */
    fun base64ToImageBitmap(base64Str: String): ImageBitmap? {
        return try {
            val cleanBase64 = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
