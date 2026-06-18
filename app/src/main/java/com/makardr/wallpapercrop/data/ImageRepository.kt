package com.makardr.wallpapercrop.data

import android.content.Context
import android.net.Uri
import com.makardr.wallpapercrop.common.Tags
import com.makardr.wallpapercrop.common.utils.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class ImageRepository(private val context: Context) {
    private val folderName = "images"
    private val imageDir = File(context.filesDir, folderName)

    fun saveImage(uri: Uri) {
        val guid = UUID.randomUUID().toString()
        val fileName = "${guid}.png"

        Logger.logInfo(Tags.FileSystem, "Saving image to $fileName")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream for $uri")

        //Create images folder if it doesn't exist
        val imagesDir = imageDir.apply { mkdirs() }
        val outputFile = File(imagesDir, fileName)

        input.use { stream ->
            FileOutputStream(outputFile).use { output ->
                stream.copyTo(output)
            }
        }
        printAllFiles()
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
        context.filesDir.walkTopDown().forEach { file ->
            val relativePath = file.relativeTo(context.filesDir).path
            if (file.isDirectory) {
                Logger.logDebug(Tags.FileSystem, "[DIR] ${file.relativeTo(context.filesDir).path}")
            } else {
                Logger.logDebug(Tags.FileSystem, relativePath)
            }
        }
    }


    fun deleteImage(fileName: String) {
        val file = File(imageDir, fileName)
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted){
                Logger.logInfo(Tags.FileSystem, "File deleted: $fileName")
            }else{
                Logger.logError(Tags.FileSystem, "Failed to delete file: $fileName")
            }
        } else {
            Logger.logError(Tags.FileSystem, "File not found: $fileName")
        }
    }

    fun deleteAllFiles(){
        val imagesDir = File(context.filesDir, "images")
        if (imagesDir.deleteRecursively()){
            Logger.logInfo(Tags.FileSystem, "Deleted all files")
        }else{
            Logger.logInfo(Tags.FileSystem, "Failed to delete all files")
        }
    }

}