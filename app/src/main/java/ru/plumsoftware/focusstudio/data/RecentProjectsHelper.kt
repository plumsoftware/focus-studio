package ru.plumsoftware.focusstudio.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecentProject(
    val uri: Uri,
    val isVideo: Boolean,
    val dateLabel: String
)

object RecentProjectsHelper {
    private const val LIMIT = 10
    private fun dateFormat() = SimpleDateFormat("d MMM", Locale.getDefault())

    suspend fun loadRecent(context: Context): List<RecentProject> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<Long, RecentProject>>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.RELATIVE_PATH
            ),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%FocusStudio%"),
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            var count = 0
            while (cursor.moveToNext() && count < LIMIT) {
                val id = cursor.getLong(idCol)
                val dateSec = cursor.getLong(dateCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                results.add(
                    dateSec to RecentProject(
                        uri = uri,
                        isVideo = false,
                        dateLabel = dateFormat().format(Date(dateSec * 1000))
                    )
                )
                count++
            }
        }

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.RELATIVE_PATH
            ),
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%FocusStudio%"),
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            var count = 0
            while (cursor.moveToNext() && count < LIMIT) {
                val id = cursor.getLong(idCol)
                val dateSec = cursor.getLong(dateCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                results.add(
                    dateSec to RecentProject(
                        uri = uri,
                        isVideo = true,
                        dateLabel = dateFormat().format(Date(dateSec * 1000))
                    )
                )
                count++
            }
        }

        results
            .sortedByDescending { it.first }
            .take(LIMIT)
            .map { it.second }
    }
}
