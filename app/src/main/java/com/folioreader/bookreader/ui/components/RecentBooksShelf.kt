package com.folioreader.bookreader.ui.components

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folioreader.bookreader.data.ReaderTheme
import com.folioreader.bookreader.data.RecentBook
import com.folioreader.bookreader.ui.theme.LocalReaderColors

@Composable
fun RecentBooksShelf(
    recentBooks: List<RecentBook>,
    currentTheme: ReaderTheme,
    onOpenPdfPicker: () -> Unit,
    onOpenSampleBook: () -> Unit,
    onOpenRecentBook: (RecentBook) -> Unit,
    onRemoveRecentBook: (RecentBook) -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val readerColors = LocalReaderColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(readerColors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title and Theme switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "FolioReader",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = readerColors.inkColor
                        )
                        Text(
                            text = "Physical Book Experience for Android",
                            fontSize = 13.sp,
                            color = readerColors.inkColor.copy(alpha = 0.7f)
                        )
                    }

                    // Theme selector pills
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReaderTheme.values().forEach { theme ->
                            val isSelected = theme == currentTheme
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) readerColors.accentColor else readerColors.toolbarBg
                                    )
                                    .clickable { onSelectTheme(theme) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = theme.title.substringBefore(" "),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else readerColors.inkColor
                                )
                            }
                        }
                    }
                }
            }

            // Hero Action Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(readerColors.pageColor)
                        .border(1.dp, readerColors.dividerColor, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "❦",
                            fontSize = 24.sp,
                            color = readerColors.accentColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open Any PDF File",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = readerColors.inkColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Read with realistic curved page edges, physics page turns, and facing pages on larger screens.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = readerColors.inkColor.copy(alpha = 0.75f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onOpenPdfPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = readerColors.accentColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Choose PDF File",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenSampleBook,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Read Classic Sample",
                                    color = readerColors.accentColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Feature Highlights Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FeaturePill(title = "Physical Curl", desc = "Canvas Shading")
                    FeaturePill(title = "Facing Pages", desc = "Auto / Dual")
                    FeaturePill(title = "100% Offline", desc = "Zero Telemetry")
                }
            }

            // Library / Recent Books Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Documents",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = readerColors.inkColor
                    )
                    Text(
                        text = "${recentBooks.size} books",
                        fontSize = 12.sp,
                        color = readerColors.inkColor.copy(alpha = 0.6f)
                    )
                }
            }

            if (recentBooks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent books yet.\nTap 'Choose PDF File' above to start reading.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = readerColors.inkColor.copy(alpha = 0.55f),
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                items(recentBooks, key = { it.uriString }) { book ->
                    RecentBookCard(
                        book = book,
                        onClick = { onOpenRecentBook(book) },
                        onRemove = { onRemoveRecentBook(book) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(title: String, desc: String) {
    val readerColors = LocalReaderColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(readerColors.toolbarBg)
            .border(1.dp, readerColors.dividerColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = readerColors.inkColor
            )
            Text(
                text = desc,
                fontSize = 9.sp,
                color = readerColors.inkColor.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun RecentBookCard(
    book: RecentBook,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val readerColors = LocalReaderColors.current
    val progress = if (book.totalPages > 0) {
        ((book.lastPage + 1).toFloat() / book.totalPages).coerceIn(0f, 1f)
    } else 0f
    val percentText = "${(progress * 100).toInt()}%"

    val timeAgo = try {
        DateUtils.getRelativeTimeSpanString(book.lastOpenedTime).toString()
    } catch (_: Exception) {
        "Recently"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(readerColors.pageColor)
            .border(1.dp, readerColors.dividerColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Book Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(readerColors.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, readerColors.accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📖",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = readerColors.inkColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Page ${book.lastPage + 1} of ${book.totalPages} • $percentText",
                    fontSize = 12.sp,
                    color = readerColors.inkColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = readerColors.accentColor,
                    trackColor = readerColors.dividerColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Opened $timeAgo",
                    fontSize = 10.sp,
                    color = readerColors.inkColor.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Delete / Remove button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onRemove() }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 14.sp,
                    color = readerColors.inkColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}
