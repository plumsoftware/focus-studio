package ru.plumsoftware.focusstudio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

fun copyUriToCache(context: Context, uri: Uri): Uri? {
    return try {
        val fileName = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
            }
        } catch (e: Exception) {
            null
        } ?: uri.lastPathSegment ?: "temp_file"

        val cacheFile = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(cacheFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Uri.fromFile(cacheFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}