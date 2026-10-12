package com.folioreader.bookreader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folioreader.bookreader.data.BookLayoutMode
import com.folioreader.bookreader.data.ReaderTheme
import com.folioreader.bookreader.ui.theme.LocalReaderColors

@Composable
fun ReaderTopBar(
    visible: Boolean,
    title: String,
    currentTheme: ReaderTheme,
    currentLayout: BookLayoutMode,
    onBack: () -> Unit,
    onSelectTheme: (ReaderTheme) -> Unit,
    onSelectLayout: (BookLayoutMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val readerColors = LocalReaderColors.current
    var showThemeMenu by remember { mutableStateOf(false) }
    var showLayoutMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 6.dp)
                .background(readerColors.toolbarBg)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBack() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        color = readerColors.inkColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = readerColors.inkColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Actions: Layout mode & Theme selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Layout Mode Button
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, readerColors.dividerColor, RoundedCornerShape(6.dp))
                                .clickable { showLayoutMenu = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = when (currentLayout) {
                                    BookLayoutMode.AUTO -> "Auto"
                                    BookLayoutMode.SINGLE -> "1-Page"
                                    BookLayoutMode.DUAL -> "2-Page"
                                },
                                color = readerColors.inkColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        DropdownMenu(
                            expanded = showLayoutMenu,
                            onDismissRequest = { showLayoutMenu = false },
                            modifier = Modifier.background(readerColors.toolbarBg)
                        ) {
                            BookLayoutMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.title,
                                            color = if (mode == currentLayout) readerColors.accentColor else readerColors.inkColor,
                                            fontWeight = if (mode == currentLayout) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectLayout(mode)
                                        showLayoutMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Theme selector button
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, readerColors.dividerColor, RoundedCornerShape(6.dp))
                                .clickable { showThemeMenu = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = currentTheme.title,
                                color = readerColors.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false },
                            modifier = Modifier.background(readerColors.toolbarBg)
                        ) {
                            ReaderTheme.values().forEach { theme ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (theme) {
                                                            ReaderTheme.PAPER -> Color(0xFFFBF0D9)
                                                            ReaderTheme.LIGHT -> Color(0xFFFFFFFF)
                                                            ReaderTheme.DARK -> Color(0xFF1E1E1E)
                                                        }
                                                    )
                                                    .border(1.dp, Color.Gray, CircleShape)
                                            )
                                            Text(
                                                text = theme.title,
                                                color = if (theme == currentTheme) readerColors.accentColor else readerColors.inkColor,
                                                fontWeight = if (theme == currentTheme) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectTheme(theme)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
