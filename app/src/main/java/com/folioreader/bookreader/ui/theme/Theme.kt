package com.folioreader.bookreader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.folioreader.bookreader.data.ReaderTheme

data class ReaderColors(
    val background: Color,
    val pageColor: Color,
    val inkColor: Color,
    val spineColor: Color,
    val accentColor: Color,
    val toolbarBg: Color,
    val dividerColor: Color
)

val LocalReaderColors = staticCompositionLocalOf {
    ReaderColors(
        background = PaperBackground,
        pageColor = PaperPage,
        inkColor = PaperInk,
        spineColor = PaperSpine,
        accentColor = PaperAccent,
        toolbarBg = PaperToolbar,
        dividerColor = PaperDivider
    )
}

@Composable
fun BookReaderTheme(
    selectedTheme: ReaderTheme = ReaderTheme.PAPER,
    content: @Composable () -> Unit
) {
    val readerColors = when (selectedTheme) {
        ReaderTheme.PAPER -> ReaderColors(
            background = PaperBackground,
            pageColor = PaperPage,
            inkColor = PaperInk,
            spineColor = PaperSpine,
            accentColor = PaperAccent,
            toolbarBg = PaperToolbar,
            dividerColor = PaperDivider
        )
        ReaderTheme.LIGHT -> ReaderColors(
            background = LightBackground,
            pageColor = LightPage,
            inkColor = LightInk,
            spineColor = LightSpine,
            accentColor = LightAccent,
            toolbarBg = LightToolbar,
            dividerColor = LightDivider
        )
        ReaderTheme.DARK -> ReaderColors(
            background = DarkBackground,
            pageColor = DarkPage,
            inkColor = DarkInk,
            spineColor = DarkSpine,
            accentColor = DarkAccent,
            toolbarBg = DarkToolbar,
            dividerColor = DarkDivider
        )
    }

    val materialColors = if (selectedTheme == ReaderTheme.DARK) {
        darkColorScheme(
            background = readerColors.background,
            surface = readerColors.toolbarBg,
            primary = readerColors.accentColor,
            onBackground = readerColors.inkColor,
            onSurface = readerColors.inkColor
        )
    } else {
        lightColorScheme(
            background = readerColors.background,
            surface = readerColors.toolbarBg,
            primary = readerColors.accentColor,
            onBackground = readerColors.inkColor,
            onSurface = readerColors.inkColor
        )
    }

    CompositionLocalProvider(LocalReaderColors provides readerColors) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content
        )
    }
}
