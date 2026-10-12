package com.folioreader.bookreader.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.folioreader.bookreader.data.BookLayoutMode
import com.folioreader.bookreader.data.PdfDocumentManager
import com.folioreader.bookreader.data.ReaderPreferences
import com.folioreader.bookreader.data.ReaderTheme
import com.folioreader.bookreader.ui.components.PageCurlCanvas
import com.folioreader.bookreader.ui.components.ReaderBottomBar
import com.folioreader.bookreader.ui.components.ReaderTopBar
import com.folioreader.bookreader.ui.theme.LocalReaderColors
import kotlinx.coroutines.launch

@Composable
fun BookReaderScreen(
    uri: Uri,
    pdfManager: PdfDocumentManager,
    preferences: ReaderPreferences,
    initialPage: Int,
    currentTheme: ReaderTheme,
    layoutModePreference: BookLayoutMode,
    onBack: () -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectLayout: (BookLayoutMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val readerColors = LocalReaderColors.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    var currentPage by remember { mutableIntStateOf(initialPage) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Rendered Bitmaps
    var leftBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rightBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var nextBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var prevBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val totalPages = pdfManager.totalPageCount
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    // Decide if dual-page mode should be active
    val isDualPageEffective = when (layoutModePreference) {
        BookLayoutMode.AUTO -> (isLandscape || isTablet) && totalPages > 1
        BookLayoutMode.DUAL -> totalPages > 1
        BookLayoutMode.SINGLE -> false
    }

    // Save reading progress whenever page changes
    LaunchedEffect(currentPage, totalPages) {
        if (totalPages > 0) {
            preferences.saveDocumentProgress(
                uriString = uri.toString(),
                title = pdfManager.documentTitle,
                page = currentPage,
                totalPages = totalPages
            )
        }
    }

    // Load PDF when entering screen
    LaunchedEffect(uri) {
        isLoading = true
        loadError = null
        val success = pdfManager.openDocument(uri)
        if (success) {
            val savedPage = preferences.getLastReadPage(uri.toString())
            currentPage = savedPage.coerceIn(0, (pdfManager.totalPageCount - 1).coerceAtLeast(0))
            isLoading = false
        } else {
            loadError = "Unable to open PDF document."
            isLoading = false
        }
    }

    BackHandler {
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            pdfManager.clearCache()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(readerColors.background)
    ) {
        val targetWidthPx = with(density) { maxWidth.toPx().toInt() }
        val targetHeightPx = with(density) { maxHeight.toPx().toInt() }
        val paperBgArgb = readerColors.pageColor.toArgb()

        // Page Bitmaps Loader
        LaunchedEffect(
            currentPage,
            isDualPageEffective,
            targetWidthPx,
            targetHeightPx,
            currentTheme,
            isLoading
        ) {
            if (isLoading || totalPages <= 0 || targetWidthPx <= 0 || targetHeightPx <= 0) return@LaunchedEffect

            val pageWidth = if (isDualPageEffective) targetWidthPx / 2 else targetWidthPx
            val pageHeight = targetHeightPx

            if (isDualPageEffective) {
                // In dual page mode, ensure even base page
                val basePage = if (currentPage % 2 == 0) currentPage else currentPage - 1
                leftBitmap = pdfManager.getPageBitmap(basePage, pageWidth, pageHeight, paperBgArgb)
                rightBitmap = if (basePage + 1 < totalPages) {
                    pdfManager.getPageBitmap(basePage + 1, pageWidth, pageHeight, paperBgArgb)
                } else null

                // For forward turn: next spread is basePage + 2, basePage + 3
                nextBitmap = if (basePage + 2 < totalPages) {
                    pdfManager.getPageBitmap(basePage + 2, pageWidth, pageHeight, paperBgArgb)
                } else null

                // For backward turn: prev spread is basePage - 2, basePage - 1
                prevBitmap = if (basePage - 1 >= 0) {
                    pdfManager.getPageBitmap(basePage - 1, pageWidth, pageHeight, paperBgArgb)
                } else null

                // Background prefetch
                pdfManager.prefetchPages(basePage, pageWidth, pageHeight, paperBgArgb)
            } else {
                // Single page mode
                leftBitmap = pdfManager.getPageBitmap(currentPage, pageWidth, pageHeight, paperBgArgb)
                rightBitmap = null

                nextBitmap = if (currentPage + 1 < totalPages) {
                    pdfManager.getPageBitmap(currentPage + 1, pageWidth, pageHeight, paperBgArgb)
                } else null

                prevBitmap = if (currentPage - 1 >= 0) {
                    pdfManager.getPageBitmap(currentPage - 1, pageWidth, pageHeight, paperBgArgb)
                } else null

                pdfManager.prefetchPages(currentPage, pageWidth, pageHeight, paperBgArgb)
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = readerColors.accentColor)
            }
        } else if (loadError != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = loadError ?: "Error loading file",
                    color = readerColors.inkColor
                )
            }
        } else {
            // Main Canvas Reading Surface
            PageCurlCanvas(
                currentPage = currentPage,
                totalPages = totalPages,
                layoutMode = layoutModePreference,
                isDualPageEffective = isDualPageEffective,
                leftBitmap = leftBitmap,
                rightBitmap = rightBitmap,
                nextBitmap = nextBitmap,
                prevBitmap = prevBitmap,
                onTurnForward = {
                    val step = if (isDualPageEffective) 2 else 1
                    if (currentPage + step < totalPages) {
                        currentPage += step
                    } else if (currentPage + 1 < totalPages) {
                        currentPage += 1
                    }
                },
                onTurnBackward = {
                    val step = if (isDualPageEffective) 2 else 1
                    currentPage = (currentPage - step).coerceAtLeast(0)
                },
                onToggleControls = {
                    controlsVisible = !controlsVisible
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar
            ReaderTopBar(
                visible = controlsVisible,
                title = pdfManager.documentTitle,
                currentTheme = currentTheme,
                currentLayout = layoutModePreference,
                onBack = onBack,
                onSelectTheme = onSelectTheme,
                onSelectLayout = onSelectLayout,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Bottom Bar
            ReaderBottomBar(
                visible = controlsVisible,
                currentPage = currentPage,
                totalPages = totalPages,
                isDualPage = isDualPageEffective,
                onSeekToPage = { targetPage ->
                    currentPage = if (isDualPageEffective && targetPage % 2 != 0) {
                        targetPage - 1
                    } else targetPage
                },
                onPrevPage = {
                    val step = if (isDualPageEffective) 2 else 1
                    currentPage = (currentPage - step).coerceAtLeast(0)
                },
                onNextPage = {
                    val step = if (isDualPageEffective) 2 else 1
                    if (currentPage + step < totalPages) {
                        currentPage += step
                    } else if (currentPage + 1 < totalPages) {
                        currentPage += 1
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
