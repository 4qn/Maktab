package com.edu.maktab.book

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.ResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
private const val TAG = "AddDocumentViewModel"
class BookListViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()


    val canAddDocument: Boolean
        get() = canAddDocumentPermission(context)

    /**
     * Using lazy to instantiate the [OkHttpClient] only when accessing it, not when the viewmodel
     * is created
     */
    private val httpClient by lazy { OkHttpClient() }

    /**
     * We keep the current [FileEntry] in the savedStateHandle to re-render it if there is a
     * configuration change and we expose it as a [LiveData] to the UI
     */
    private var _isDownloading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDownloading: LiveData<Boolean> = _isDownloading

    /**
     * We keep the current [FileEntry] in the savedStateHandle to re-render it if there is a
     * configuration change and we expose it as a [LiveData] to the UI
     */
//    val currentFileEntry = savedStateHandle.getLiveData<FileEntry>("current_file")

    /**
     * Generate random filename when saving a new file
     */
    private fun generateFilename(extension: String) = "${System.currentTimeMillis()}.$extension"

    /**
     * Check if the app can writes on the shared storage
     *
     * On Android 10 (API 29), we can add files to the Downloads folder without having to request the
     * [WRITE_EXTERNAL_STORAGE] permission, so we only check on pre-API 29 devices
     */
    private fun canAddDocumentPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun addRandomFile() {
        _isDownloading.postValue(true)

        val randomRemoteUrl = "https://developer.android.com/images/jetpack/compose/compose-testing-cheatsheet.pdf"// SampleFiles.nonMedia.random()
        val extension = randomRemoteUrl.substring(randomRemoteUrl.lastIndexOf(".") + 1)
        val filename = generateFilename(extension)

        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val newFileUri = addFileToDownloadsApi29(filename)
                    val outputStream = context.contentResolver.openOutputStream(newFileUri, "w")
                        ?: throw Exception("ContentResolver couldn't open $newFileUri outputStream")

                    val responseBody = downloadFileFromInternet(randomRemoteUrl)

                    if (responseBody == null) {
                        _isDownloading.postValue(false)
                        return@withContext
                    }

                    // .use is an extension function that closes the output stream where we're
                    // saving the file content once its lambda is finished being executed
                    responseBody.use {
                        outputStream.use {
                            responseBody.byteStream().copyTo(it)
                        }
                    }

                    Log.d(TAG, "File downloaded uri ($newFileUri)")

                    val path = getMediaStoreEntryPathApi29(newFileUri)
                        ?: throw Exception("ContentResolver couldn't find $newFileUri")

                    // We scan the newly added file to make sure MediaStore.Downloads is always up
                    // to date
                    scanFilePath(path, responseBody.contentType().toString()) { uri ->
                        Log.d(TAG, "MediaStore updated ($path, $uri)")

                        viewModelScope.launch {
                            val fileDetails = getFileDetails(uri)
                            Log.d(TAG, "New file: $fileDetails")

//                            savedStateHandle["current_file"] = fileDetails
                            _isDownloading.postValue(false)
                        }
                    }
                } else {
                    val file = addFileToDownloadsApi21(filename)
                    val outputStream = file.outputStream()

                    val responseBody = downloadFileFromInternet(randomRemoteUrl)

                    if (responseBody == null) {
                        _isDownloading.postValue(false)
                        return@withContext
                    }

                    // .use is an extension function that closes the output stream where we're
                    // saving the file content once its lambda is finished being executed
                    responseBody.use {
                        outputStream.use {
                            responseBody.byteStream().copyTo(it)
                        }
                    }

                    Log.d(TAG, "File downloaded (${file.absolutePath})")

                    // We scan the newly added file to make sure MediaStore.Files is always up to
                    // date
                    scanFilePath(file.path, responseBody.contentType().toString()) { uri ->
                        Log.d(TAG, "MediaStore updated ($file.path, $uri)")

                        viewModelScope.launch {
                            val fileDetails = getFileDetails(uri)
                            Log.d(TAG, "New file: $fileDetails")

//                            savedStateHandle["current_file"] = fileDetails
                            _isDownloading.postValue(false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                _isDownloading.postValue(false)
            }
        }
    }

    /**
     * Downloads a random file from internet and saves its content to the specified outputStream
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadFileFromInternet(url: String): ResponseBody? {
        // We use OkHttp to create HTTP request
        val request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO) {
            val response = httpClient.newCall(request).execute()
            return@withContext response.body()
        }
    }

    /**
     * Create a file inside the Download folder using java.io API
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun addFileToDownloadsApi21(filename: String): File {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)

        // Get path of the destination where the file will be saved
        val newNonMediaFile = File(downloadsFolder, filename)

        return withContext(Dispatchers.IO) {
            // Create new file if it does not exist, throw exception otherwise
            if (!newNonMediaFile.createNewFile()) {
                throw Exception("File ${newNonMediaFile.name} already exists")
            }

            return@withContext newNonMediaFile
        }
    }

    /**
     * Create a file inside the Download folder using MediaStore API
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun addFileToDownloadsApi29(filename: String): Uri {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        return withContext(Dispatchers.IO) {
            val newFile = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
            }

            // This method will perform a binder transaction which is better to execute off the main
            // thread
            return@withContext context.contentResolver.insert(collection, newFile)
                ?: throw Exception("MediaStore Uri couldn't be created")
        }
    }

    /**
     * When adding a file (using java.io or ContentResolver APIs), MediaStore might not be aware of
     * the new entry or doesn't have an updated version of it. That's why some entries have 0 bytes
     * size, even though the file is definitely not empty. MediaStore will eventually scan the file
     * but it's better to do it ourselves to have a fresher state whenever we can
     */
    private suspend fun scanFilePath(path: String, mimeType: String, callback: (uri: Uri) -> Unit) {
        withContext(Dispatchers.IO) {
            MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType)) { _, uri ->
                callback(uri)
            }
        }
    }

    /**
     * Get a path for a MediaStore entry as it's needed when calling MediaScanner
     */
    private suspend fun getMediaStoreEntryPathApi29(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Files.FileColumns.DATA),
                null,
                null,
                null
            ) ?: return@withContext null

            cursor.use {
                if (!cursor.moveToFirst()) {
                    return@withContext null
                }

                return@withContext cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
            }
        }
    }

    /**
     * Get file details using the MediaStore API
     */
    private suspend fun getFileDetails(uri: Uri): FileEntry? {
        return withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.MIME_TYPE,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.DATA
                ),
                null,
                null,
                null
            ) ?: return@withContext null

            cursor.use {
                if (!cursor.moveToFirst()) {
                    return@withContext null
                }

                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                return@withContext FileEntry(
                    filename = cursor.getString(displayNameColumn),
                    size = cursor.getLong(sizeColumn),
                    mimeType = cursor.getString(mimeTypeColumn),
                    // FileColumns.DATE_ADDED is in seconds, not milliseconds
                    addedAt = cursor.getLong(dateAddedColumn) * 1000,
                    path = cursor.getString(dataColumn),
                )
            }
        }
    }
}

@Parcelize
data class FileEntry(
    val filename: String,
    val size: Long,
    val mimeType: String,
    val addedAt: Long,
    val path: String
) : Parcelable