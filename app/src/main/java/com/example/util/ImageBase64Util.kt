package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.ui.components.ImageCodecHelper

object ImageBase64Util {
    fun decodeBase64ToBitmap(base64Data: String?): Bitmap? {
        return ImageCodecHelper.decodeBase64ToBitmap(base64Data)
    }

    fun encodeUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 720,
        quality: Int = 72
    ): String? {
        return ImageCodecHelper.encodeUriToBase64Jpeg(context, uri, maxDimension, quality)
    }
}
