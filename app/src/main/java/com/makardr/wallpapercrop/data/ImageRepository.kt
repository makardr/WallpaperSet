package com.makardr.wallpapercrop.data

import android.content.Context
import android.net.Uri
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class ImageRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val folderName = "images"
    private val imageDir = File(context.filesDir, folderName)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveImage(uri: Uri) {
        repositoryScope.launch {
            val guid = UUID.randomUUID().toString()
            val fileName = "${guid}.png"

            Logger.logInfo(Tags.FileSystem, "Saving image to $fileName")
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
            printAllFiles()
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
                        Tags.FileSystem,
                        "[DIR] ${file.relativeTo(appContext.filesDir).path}"
                    )
                } else {
                    Logger.logDebug(Tags.FileSystem, relativePath)
                }
            }
        }
    }


    fun deleteImage(fileName: String) {
        repositoryScope.launch {
            val file = File(imageDir, fileName)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Logger.logInfo(Tags.FileSystem, "File deleted: $fileName")
                } else {
                    Logger.logError(Tags.FileSystem, "Failed to delete file: $fileName")
                }
            } else {
                Logger.logError(Tags.FileSystem, "File not found: $fileName")
            }
        }

    }

    fun deleteAllFiles() {
        repositoryScope.launch {
            val imagesDir = File(appContext.filesDir, "images")
            if (imagesDir.deleteRecursively()) {
                Logger.logInfo(Tags.FileSystem, "Deleted all files")
            } else {
                Logger.logInfo(Tags.FileSystem, "Failed to delete all files")
            }
        }

    }

    companion object {
        @Volatile
        private var INSTANCE: ImageRepository? = null

        fun getInstance(context: Context): ImageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageRepository(context).also { INSTANCE = it }
            }
        }
    }
}