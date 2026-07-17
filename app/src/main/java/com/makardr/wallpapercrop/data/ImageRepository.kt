package com.makardr.wallpapercrop.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.makardr.wallpapercrop.data.model.LogTags
import com.makardr.wallpapercrop.common.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class ImageRepository private constructor(context: Context) {
    private val appContext: Context = context.applicationContext
    private val imageDir = File(context.filesDir, FOLDER_NAME)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveImage(uri: Uri?) {
        uri?.let {
            repositoryScope.launch {
                val guid = UUID.randomUUID().toString()
                val fileName = "${guid}.png"
                val input = appContext.contentResolver.openInputStream(uri)
                    ?: throw IOException("Unable to open input stream for $uri")

                //Create images folder if it does not exist
                val imagesDir = imageDir.apply { mkdirs() }
                val outputFile = File(imagesDir, fileName)

                input.use { stream ->
                    FileOutputStream(outputFile).use { output ->
                        stream.copyTo(output)
                    }
                }
                Logger.logInfo(LogTags.FileSystem, "Image saved to $fileName")
            }
        }
    }

    fun listSavedImages(): List<File> {
        return imageDir.listFiles { file -> file.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun listSavedImageUris(): List<Uri> {
        return listSavedImages().map { file ->
            Uri.fromFile(file)
        }
    }

    fun printAllFiles() {
        repositoryScope.launch {
            appContext.filesDir.walkTopDown().forEach { file ->
                val relativePath = file.relativeTo(appContext.filesDir).path
                if (file.isDirectory) {
                    Logger.logDebug(
                        LogTags.FileSystem,
                        "[DIR] ${file.relativeTo(appContext.filesDir).path}"
                    )
                } else {
                    Logger.logDebug(LogTags.FileSystem, relativePath)
                }
            }
        }
    }


    suspend fun deleteImages(uriList: Set<Uri>): Boolean {
        var failedToDelete = false
        withContext(Dispatchers.IO) {
            for (uri in uriList) {
                val deleted = when (uri.scheme) {
                    ContentResolver.SCHEME_CONTENT ->
                        appContext.contentResolver.delete(uri, null, null) > 0

                    ContentResolver.SCHEME_FILE ->
                        uri.path?.let { File(it).delete() } ?: false

                    else -> {
                        Logger.logError(LogTags.FileSystem, "Unsupported URI scheme: $uri")
                        continue
                    }
                }

                if (deleted) {
                    Logger.logInfo(LogTags.FileSystem, "File deleted: $uri")
                } else {
                    Logger.logError(LogTags.FileSystem, "Failed to delete file: $uri")
                    failedToDelete = true
                }
            }
        }
        return !failedToDelete
    }

    suspend fun deleteAllFiles() {
        withContext(Dispatchers.IO) {
            if (imageDir.deleteRecursively()) {
                Logger.logInfo(LogTags.FileSystem, "Deleted all files")
            } else {
                Logger.logInfo(LogTags.FileSystem, "Failed to delete all files")
            }
        }

    }

    companion object {
        private const val FOLDER_NAME = "images"

        @Volatile
        private var INSTANCE: ImageRepository? = null

        fun getInstance(context: Context): ImageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageRepository(context).also { INSTANCE = it }
            }
        }
    }
}