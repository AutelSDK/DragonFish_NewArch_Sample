package com.autel.sdk.debugtools;

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import okhttp3.internal.toLongOrDefault


object UriConvert {

    fun getPhotoPathFromContentUri(ctx: Context, uri: Uri): String? {
        var photoPath: String? = null
        if (DocumentsContract.isDocumentUri(ctx, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if (isExternalStorageDocument(uri)) {
                val split: List<String> = docId.split(":")
                if (split.size >= 2 && "primary".equals(split[0], ignoreCase = true)) {
                    photoPath =
                        Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    docId.toLongOrDefault(0)
                )
                photoPath = getDataColumn(ctx, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val contentUris: Uri? = when (type) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> null
                    }
                    if (contentUris == null) return null
                    val selection = MediaStore.Images.Media._ID + "=?"
                    val selectionArgs: Array<String> = arrayOf(split[1])
                    photoPath = getDataColumn(ctx, contentUris, selection, selectionArgs);
                }
            }
        } else if ("file" == uri.scheme?.lowercase()) {
            photoPath = uri.path
        } else {
            photoPath = getDataColumn(ctx, uri, null, null);
        }
        return photoPath
    }


    private fun isExternalStorageDocument(uri: Uri): Boolean =
        "com.android.externalstorage.documents" == uri.authority


    private fun isDownloadsDocument(uri: Uri): Boolean =
        "com.android.providers.downloads.documents" == uri.authority

    private fun isMediaDocument(uri: Uri): Boolean =
        "com.android.providers.media.documents" == uri.authority

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        return null
    }
}
