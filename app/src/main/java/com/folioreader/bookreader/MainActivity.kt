package com.folioreader.bookreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.folioreader.bookreader.data.BookLayoutMode
import com.folioreader.bookreader.data.PdfDocumentManager
import com.folioreader.bookreader.data.ReaderPreferences
import com.folioreader.bookreader.data.ReaderPreferencesState
import com.folioreader.bookreader.data.ReaderTheme
import com.folioreader.bookreader.ui.BookReaderScreen
import com.folioreader.bookreader.ui.components.RecentBooksShelf
import com.folioreader.bookreader.ui.theme.BookReaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var pdfManager: PdfDocumentManager
    private lateinit var preferences: ReaderPreferences

    private var activeDocumentUri by mutableStateOf<Uri?>(null)

    // Storage Access Framework PDF Picker launcher
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Persist read permission across reboots if content URI
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                // Some providers or file schemes don't support takePersistableUriPermission
            }
            activeDocumentUri = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pdfManager = PdfDocumentManager(applicationContext)
        preferences = ReaderPreferences(applicationContext)

        // Handle intent if opened via file manager or another app
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_EDIT) {
                activeDocumentUri = uri
            }
        }

        setContent {
            val prefState by preferences.preferencesFlow.collectAsState(initial = ReaderPreferencesState())
            val coroutineScope = rememberCoroutineScope()

            BookReaderTheme(selectedTheme = prefState.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUri = activeDocumentUri

                    if (currentUri != null) {
                        BookReaderScreen(
                            uri = currentUri,
                            pdfManager = pdfManager,
                            preferences = preferences,
                            initialPage = 0,
                            currentTheme = prefState.theme,
                            layoutModePreference = prefState.layoutMode,
                            onBack = {
                                activeDocumentUri = null
                            },
                            onSelectTheme = { newTheme ->
                                coroutineScope.launch {
                                    preferences.setTheme(newTheme)
                                }
                            },
                            onSelectLayout = { newLayout ->
                                coroutineScope.launch {
                                    preferences.setLayoutMode(newLayout)
                                }
                            }
                        )
                    } else {
                        RecentBooksShelf(
                            recentBooks = prefState.recentBooks,
                            currentTheme = prefState.theme,
                            onOpenPdfPicker = {
                                openDocumentLauncher.launch(arrayOf("application/pdf"))
                            },
                            onOpenSampleBook = {
                                coroutineScope.launch {
                                    val sampleUri = PdfDocumentManager.createSampleBook(applicationContext)
                                    activeDocumentUri = sampleUri
                                }
                            },
                            onOpenRecentBook = { book ->
                                activeDocumentUri = Uri.parse(book.uriString)
                            },
                            onRemoveRecentBook = { book ->
                                coroutineScope.launch {
                                    preferences.removeRecentBook(book.uriString)
                                }
                            },
                            onSelectTheme = { newTheme ->
                                coroutineScope.launch {
                                    preferences.setTheme(newTheme)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            activeDocumentUri = uri
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfManager.close()
    }
}
