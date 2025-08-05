package com.metrolist.music.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.metrolist.music.constants.DownloadLocationKey
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val defaultDir = context.filesDir.resolve("download")
    private val _directory = MutableStateFlow(defaultDir)
    val directory: StateFlow<File> = _directory

    init {
        scope.launch {
            context.dataStore.data.collectLatest { prefs ->
                val uriString = prefs[DownloadLocationKey]
                val file = uriString?.takeIf { it.isNotBlank() }?.let { toFile(it) } ?: defaultDir
                _directory.value = file
            }
        }
    }

    private fun toFile(uriString: String): File {
        val uri = Uri.parse(uriString)
        return when (uri.scheme) {
            "file" -> File(uri.path!!)
            "content" -> DocumentFile.fromTreeUri(context, uri)?.uri?.path?.let(::File) ?: defaultDir
            else -> File(uriString)
        }
    }

    fun current(): File = directory.value
}
