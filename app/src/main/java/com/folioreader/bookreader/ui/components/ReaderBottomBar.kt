package com.folioreader.bookreader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folioreader.bookreader.ui.theme.LocalReaderColors
import kotlin.math.roundToInt

@Composable
fun ReaderBottomBar(
    visible: Boolean,
    currentPage: Int,
    totalPages: Int,
    isDualPage: Boolean,
    onSeekToPage: (Int) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val readerColors = LocalReaderColors.current
    val maxPageIndex = (totalPages - 1).coerceAtLeast(0)
    var sliderValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp)
                .background(readerColors.toolbarBg)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page slider
                if (maxPageIndex > 0) {
                    Slider(
                        value = sliderValue.coerceIn(0f, maxPageIndex.toFloat()),
                        onValueChange = {
                            sliderValue = it
                        },
                        onValueChangeFinished = {
                            onSeekToPage(sliderValue.roundToInt())
                        },
                        valueRange = 0f..maxPageIndex.toFloat(),
                        steps = if (maxPageIndex > 1) maxPageIndex - 1 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = readerColors.accentColor,
                            activeTrackColor = readerColors.accentColor,
                            inactiveTrackColor = readerColors.dividerColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom row: Prev button, Page indicator, Next button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Previous button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = currentPage > 0) { onPrevPage() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹ Prev",
                            color = if (currentPage > 0) readerColors.inkColor else readerColors.dividerColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Page & Progress text
                    val displayPageText = if (isDualPage && totalPages > 1) {
                        val p1 = currentPage + 1
                        val p2 = (currentPage + 2).coerceAtMost(totalPages)
                        if (p1 == p2) "Page $p1 of $totalPages" else "Pages $p1–$p2 of $totalPages"
                    } else {
                        "Page ${currentPage + 1} of $totalPages"
                    }
                    val percentage = if (totalPages > 0) {
                        ((currentPage + 1).toFloat() / totalPages * 100).toInt()
                    } else 0

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayPageText,
                            color = readerColors.inkColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "$percentage% completed",
                            color = readerColors.inkColor.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }

                    // Next button
                    val canNext = if (isDualPage) currentPage + 2 < totalPages || currentPage + 1 < totalPages else currentPage + 1 < totalPages
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = canNext) { onNextPage() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Next ›",
                            color = if (canNext) readerColors.inkColor else readerColors.dividerColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
