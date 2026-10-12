package com.folioreader.bookreader.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.folioreader.bookreader.data.BookLayoutMode
import com.folioreader.bookreader.ui.theme.LocalReaderColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class TurnDirection {
    FORWARD,
    BACKWARD,
    NONE
}

@Composable
fun PageCurlCanvas(
    currentPage: Int,
    totalPages: Int,
    layoutMode: BookLayoutMode,
    isDualPageEffective: Boolean,
    leftBitmap: Bitmap?,
    rightBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    prevBitmap: Bitmap?,
    onTurnForward: () -> Unit,
    onTurnBackward: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val readerColors = LocalReaderColors.current
    val coroutineScope = rememberCoroutineScope()

    // Animation progress 0f..1f
    val animProgress = remember { Animatable(0f) }
    var turnDirection by remember { mutableStateOf(TurnDirection.NONE) }
    var touchY by remember { mutableFloatStateOf(0.5f) } // normalized 0..1 touch height
    var isDragging by remember { mutableStateOf(false) }

    val bgArgb = readerColors.background.toArgb()
    val pageArgb = readerColors.pageColor.toArgb()
    val spineArgb = readerColors.spineColor.toArgb()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentPage, totalPages, isDualPageEffective) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    val leftZone = width * 0.22f
                    val rightZone = width * 0.78f

                    if (offset.x < leftZone) {
                        // Turn backward
                        if (currentPage > 0 && !animProgress.isRunning) {
                            turnDirection = TurnDirection.BACKWARD
                            touchY = offset.y / size.height
                            coroutineScope.launch {
                                animProgress.snapTo(0f)
                                animProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                                )
                                onTurnBackward()
                                animProgress.snapTo(0f)
                                turnDirection = TurnDirection.NONE
                            }
                        }
                    } else if (offset.x > rightZone) {
                        // Turn forward
                        val canForward = if (isDualPageEffective) currentPage + 2 < totalPages || (currentPage + 1 < totalPages) else currentPage + 1 < totalPages
                        if (canForward && !animProgress.isRunning) {
                            turnDirection = TurnDirection.FORWARD
                            touchY = offset.y / size.height
                            coroutineScope.launch {
                                animProgress.snapTo(0f)
                                animProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                                )
                                onTurnForward()
                                animProgress.snapTo(0f)
                                turnDirection = TurnDirection.NONE
                            }
                        }
                    } else {
                        // Center tap: toggle controls
                        onToggleControls()
                    }
                }
            }
            .pointerInput(currentPage, totalPages, isDualPageEffective) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        if (animProgress.isRunning) return@detectDragGestures
                        isDragging = true
                        touchY = startOffset.y / size.height
                    },
                    onDrag = { change, dragAmount ->
                        if (!isDragging) return@detectDragGestures
                        change.consume()

                        val width = size.width.toFloat()
                        val activeWidth = if (isDualPageEffective) width * 0.5f else width

                        if (turnDirection == TurnDirection.NONE) {
                            if (dragAmount.x < -4) {
                                // dragging left -> turning forward
                                val canForward = if (isDualPageEffective) currentPage + 2 < totalPages || currentPage + 1 < totalPages else currentPage + 1 < totalPages
                                if (canForward) {
                                    turnDirection = TurnDirection.FORWARD
                                }
                            } else if (dragAmount.x > 4) {
                                // dragging right -> turning backward
                                if (currentPage > 0) {
                                    turnDirection = TurnDirection.BACKWARD
                                }
                            }
                        }

                        if (turnDirection != TurnDirection.NONE) {
                            val deltaProgress = -dragAmount.x / activeWidth
                            val newProgress = if (turnDirection == TurnDirection.FORWARD) {
                                (animProgress.value + deltaProgress).coerceIn(0f, 1f)
                            } else {
                                (animProgress.value - deltaProgress).coerceIn(0f, 1f)
                            }
                            coroutineScope.launch {
                                animProgress.snapTo(newProgress)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        if (turnDirection != TurnDirection.NONE) {
                            coroutineScope.launch {
                                val current = animProgress.value
                                if (current > 0.35f) {
                                    animProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                                    )
                                    if (turnDirection == TurnDirection.FORWARD) {
                                        onTurnForward()
                                    } else {
                                        onTurnBackward()
                                    }
                                } else {
                                    // snap back
                                    animProgress.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                                    )
                                }
                                animProgress.snapTo(0f)
                                turnDirection = TurnDirection.NONE
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        coroutineScope.launch {
                            animProgress.snapTo(0f)
                            turnDirection = TurnDirection.NONE
                        }
                    }
                )
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { composeCanvas ->
                val canvas = composeCanvas.nativeCanvas
                val width = size.width
                val height = size.height

                // 1. Draw outer table / background
                val bgPaint = Paint().apply { color = bgArgb }
                canvas.drawRect(0f, 0f, width, height, bgPaint)

                if (isDualPageEffective) {
                    renderDualPageSpread(
                        canvas = canvas,
                        width = width,
                        height = height,
                        currentPage = currentPage,
                        totalPages = totalPages,
                        leftBitmap = leftBitmap,
                        rightBitmap = rightBitmap,
                        nextBitmap = nextBitmap,
                        prevBitmap = prevBitmap,
                        turnDirection = turnDirection,
                        turnProgress = animProgress.value,
                        touchY = touchY,
                        pageColor = pageArgb,
                        spineColor = spineArgb
                    )
                } else {
                    renderSinglePageSpread(
                        canvas = canvas,
                        width = width,
                        height = height,
                        currentPage = currentPage,
                        totalPages = totalPages,
                        currentBitmap = if (currentPage % 2 == 0) leftBitmap ?: rightBitmap else rightBitmap ?: leftBitmap,
                        nextBitmap = nextBitmap,
                        prevBitmap = prevBitmap,
                        turnDirection = turnDirection,
                        turnProgress = animProgress.value,
                        touchY = touchY,
                        pageColor = pageArgb,
                        spineColor = spineArgb
                    )
                }
            }
        }
    }
}

/**
 * Renders Dual Facing Pages Spread (Classic Open Hardcover Book)
 */
private fun renderDualPageSpread(
    canvas: Canvas,
    width: Float,
    height: Float,
    currentPage: Int,
    totalPages: Int,
    leftBitmap: Bitmap?,
    rightBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    prevBitmap: Bitmap?,
    turnDirection: TurnDirection,
    turnProgress: Float,
    touchY: Float,
    pageColor: Int,
    spineColor: Int
) {
    val bookPaddingH = width * 0.02f
    val bookPaddingV = height * 0.02f
    val bookWidth = width - (bookPaddingH * 2)
    val bookHeight = height - (bookPaddingV * 2)
    val halfWidth = bookWidth / 2f
    val spineX = bookPaddingH + halfWidth

    val leftRect = RectF(bookPaddingH, bookPaddingV, spineX, bookPaddingV + bookHeight)
    val rightRect = RectF(spineX, bookPaddingV, bookPaddingH + bookWidth, bookPaddingV + bookHeight)

    // Book Drop Shadow on desk/background
    val outerShadowPaint = Paint().apply {
        color = 0x33000000
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(leftRect.left + 4f, leftRect.top + 6f, rightRect.right + 8f, rightRect.bottom + 8f),
        10f, 10f, outerShadowPaint
    )

    // Layered Page Edge Stack on Left & Right (simulates 200+ stacked pages)
    drawStackedPageEdges(canvas, leftRect, rightRect, pageColor)

    // Draw Left Page Flat
    drawBookPageSurface(canvas, leftRect, pageColor)
    if (turnDirection == TurnDirection.BACKWARD && turnProgress > 0f) {
        // Turning backward: Left page is curling rightward to reveal prev page!
        // Underneath left rect is prevBitmap
        prevBitmap?.let { drawBitmapFitted(canvas, it, leftRect) }
        drawPageCurlEffect(
            canvas = canvas,
            pageRect = leftRect,
            topBitmap = leftBitmap,
            revealedBitmap = prevBitmap,
            turnProgress = turnProgress,
            isTurnFromRight = false,
            touchY = touchY,
            pageColor = pageColor
        )
    } else {
        leftBitmap?.let { drawBitmapFitted(canvas, it, leftRect) }
    }

    // Draw Right Page Flat or Curled
    drawBookPageSurface(canvas, rightRect, pageColor)
    if (turnDirection == TurnDirection.FORWARD && turnProgress > 0f) {
        // Turning forward: Right page is curling leftward to reveal nextBitmap!
        nextBitmap?.let { drawBitmapFitted(canvas, it, rightRect) }
        drawPageCurlEffect(
            canvas = canvas,
            pageRect = rightRect,
            topBitmap = rightBitmap,
            revealedBitmap = nextBitmap,
            turnProgress = turnProgress,
            isTurnFromRight = true,
            touchY = touchY,
            pageColor = pageColor
        )
    } else {
        rightBitmap?.let { drawBitmapFitted(canvas, it, rightRect) }
    }

    // Realistic Gutter & Spine Binding Shadows
    drawSpineGutterShadow(canvas, spineX, bookPaddingV, bookHeight, spineColor)
}

/**
 * Renders Single Page Mode (Mobile Portrait)
 */
private fun renderSinglePageSpread(
    canvas: Canvas,
    width: Float,
    height: Float,
    currentPage: Int,
    totalPages: Int,
    currentBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    prevBitmap: Bitmap?,
    turnDirection: TurnDirection,
    turnProgress: Float,
    touchY: Float,
    pageColor: Int,
    spineColor: Int
) {
    val paddingH = width * 0.02f
    val paddingV = height * 0.015f
    val pageRect = RectF(paddingH, paddingV, width - paddingH, height - paddingV)

    // Outer desk drop shadow
    val outerShadowPaint = Paint().apply {
        color = 0x2A000000
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(
        RectF(pageRect.left + 3f, pageRect.top + 4f, pageRect.right + 5f, pageRect.bottom + 6f),
        8f, 8f, outerShadowPaint
    )

    // Base surface
    drawBookPageSurface(canvas, pageRect, pageColor)

    when {
        turnDirection == TurnDirection.FORWARD && turnProgress > 0f -> {
            // Draw revealed next page underneath
            nextBitmap?.let { drawBitmapFitted(canvas, it, pageRect) }
            // Draw curling current page on top
            drawPageCurlEffect(
                canvas = canvas,
                pageRect = pageRect,
                topBitmap = currentBitmap,
                revealedBitmap = nextBitmap,
                turnProgress = turnProgress,
                isTurnFromRight = true,
                touchY = touchY,
                pageColor = pageColor
            )
        }
        turnDirection == TurnDirection.BACKWARD && turnProgress > 0f -> {
            // Draw revealed prev page underneath
            prevBitmap?.let { drawBitmapFitted(canvas, it, pageRect) }
            // Draw curling page from left
            drawPageCurlEffect(
                canvas = canvas,
                pageRect = pageRect,
                topBitmap = currentBitmap,
                revealedBitmap = prevBitmap,
                turnProgress = turnProgress,
                isTurnFromRight = false,
                touchY = touchY,
                pageColor = pageColor
            )
        }
        else -> {
            currentBitmap?.let { drawBitmapFitted(canvas, it, pageRect) }
        }
    }

    // Subtle left binding crease
    val spineLinePaint = Paint().apply {
        color = 0x18000000
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }
    canvas.drawLine(pageRect.left + 8f, pageRect.top, pageRect.left + 8f, pageRect.bottom, spineLinePaint)
}

/**
 * Draws the realistic physical page curl:
 * - Curved page fold line based on drag progress and corner touch
 * - Cylinder gradient highlight & shadow simulating paper curvature
 * - Drop shadow cast onto the revealed page underneath
 * - Back-face page fold with warm shading
 */
private fun drawPageCurlEffect(
    canvas: Canvas,
    pageRect: RectF,
    topBitmap: Bitmap?,
    revealedBitmap: Bitmap?,
    turnProgress: Float,
    isTurnFromRight: Boolean,
    touchY: Float,
    pageColor: Int
) {
    val w = pageRect.width()
    val h = pageRect.height()

    // Corner offset effect: dragging near top or bottom angles the fold!
    val cornerSkew = (touchY - 0.5f) * 0.28f * (1f - turnProgress)
    val progressClamped = turnProgress.coerceIn(0.001f, 0.999f)

    if (isTurnFromRight) {
        // Curling from right edge leftward
        val curlX = pageRect.right - (w * progressClamped)
        val topCurlX = (curlX + (cornerSkew * w)).coerceIn(pageRect.left, pageRect.right)
        val bottomCurlX = (curlX - (cornerSkew * w)).coerceIn(pageRect.left, pageRect.right)

        val cylinderWidth = (w * 0.14f * (1f - abs(progressClamped - 0.5f) * 0.5f)).coerceAtLeast(16f)

        // 1. Draw flat portion of top page: clipped from pageRect.left to curl fold line
        canvas.save()
        val flatPath = Path().apply {
            moveTo(pageRect.left, pageRect.top)
            lineTo(topCurlX, pageRect.top)
            quadTo(curlX, pageRect.top + h * 0.5f, bottomCurlX, pageRect.bottom)
            lineTo(pageRect.left, pageRect.bottom)
            close()
        }
        canvas.clipPath(flatPath)
        drawBookPageSurface(canvas, pageRect, pageColor)
        topBitmap?.let { drawBitmapFitted(canvas, it, pageRect) }
        canvas.restore()

        // 2. Drop Shadow cast by the curled page onto the exposed page underneath
        val shadowWidth = cylinderWidth * 1.4f
        val shadowLeft = (curlX - shadowWidth).coerceAtLeast(pageRect.left)
        val shadowRight = (curlX + shadowWidth * 0.8f).coerceAtMost(pageRect.right)

        if (shadowRight > shadowLeft) {
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    shadowLeft, 0f, shadowRight, 0f,
                    intArrayOf(0x00000000, 0x33000000, 0x66000000, 0x00000000),
                    floatArrayOf(0f, 0.45f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(shadowLeft, pageRect.top, shadowRight, pageRect.bottom, shadowPaint)
        }

        // 3. The 3D Roll / Cylinder Highlight & Shading of the curved edge
        val rollLeft = (curlX - cylinderWidth * 0.5f).coerceAtLeast(pageRect.left)
        val rollRight = (curlX + cylinderWidth * 0.7f).coerceAtMost(pageRect.right)

        if (rollRight > rollLeft) {
            val rollPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    rollLeft, 0f, rollRight, 0f,
                    intArrayOf(0x00000000, 0x44FFFFFF, 0x22000000, 0x55000000),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val curlCurve = Path().apply {
                moveTo(topCurlX - cylinderWidth * 0.5f, pageRect.top)
                lineTo(topCurlX + cylinderWidth * 0.5f, pageRect.top)
                quadTo(curlX + cylinderWidth * 0.5f, pageRect.top + h * 0.5f, bottomCurlX + cylinderWidth * 0.5f, pageRect.bottom)
                lineTo(bottomCurlX - cylinderWidth * 0.5f, pageRect.bottom)
                quadTo(curlX - cylinderWidth * 0.5f, pageRect.top + h * 0.5f, topCurlX - cylinderWidth * 0.5f, pageRect.top)
                close()
            }
            canvas.drawPath(curlCurve, rollPaint)
        }

        // 4. Back-side of turning page (verso paper tint with reverse specular reflection)
        val versoWidth = (w * progressClamped * 0.55f).coerceAtMost(cylinderWidth * 2.5f)
        val versoLeft = curlX
        val versoRight = (curlX + versoWidth).coerceAtMost(pageRect.right)
        if (versoRight > versoLeft) {
            val versoPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    versoLeft, 0f, versoRight, 0f,
                    intArrayOf(0xEED9C2, 0xDDDEC4, 0xBBBAA0),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val versoPath = Path().apply {
                moveTo(topCurlX, pageRect.top)
                lineTo(versoRight, pageRect.top)
                lineTo(versoRight, pageRect.bottom)
                lineTo(bottomCurlX, pageRect.bottom)
                quadTo(curlX, pageRect.top + h * 0.5f, topCurlX, pageRect.top)
                close()
            }
            canvas.drawPath(versoPath, versoPaint)
        }

    } else {
        // Curling backward: Left edge peeling rightward
        val curlX = pageRect.left + (w * progressClamped)
        val topCurlX = (curlX - (cornerSkew * w)).coerceIn(pageRect.left, pageRect.right)
        val bottomCurlX = (curlX + (cornerSkew * w)).coerceIn(pageRect.left, pageRect.right)

        val cylinderWidth = (w * 0.14f * (1f - abs(progressClamped - 0.5f) * 0.5f)).coerceAtLeast(16f)

        // 1. Flat portion of top page: clipped from curl line to pageRect.right
        canvas.save()
        val flatPath = Path().apply {
            moveTo(pageRect.right, pageRect.top)
            lineTo(topCurlX, pageRect.top)
            quadTo(curlX, pageRect.top + h * 0.5f, bottomCurlX, pageRect.bottom)
            lineTo(pageRect.right, pageRect.bottom)
            close()
        }
        canvas.clipPath(flatPath)
        drawBookPageSurface(canvas, pageRect, pageColor)
        topBitmap?.let { drawBitmapFitted(canvas, it, pageRect) }
        canvas.restore()

        // 2. Drop Shadow cast by curl
        val shadowLeft = (curlX - cylinderWidth * 0.8f).coerceAtLeast(pageRect.left)
        val shadowRight = (curlX + cylinderWidth * 1.4f).coerceAtMost(pageRect.right)

        if (shadowRight > shadowLeft) {
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    shadowLeft, 0f, shadowRight, 0f,
                    intArrayOf(0x00000000, 0x66000000, 0x33000000, 0x00000000),
                    floatArrayOf(0f, 0.25f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(shadowLeft, pageRect.top, shadowRight, pageRect.bottom, shadowPaint)
        }

        // 3. Cylinder highlight & shadow
        val rollLeft = (curlX - cylinderWidth * 0.7f).coerceAtLeast(pageRect.left)
        val rollRight = (curlX + cylinderWidth * 0.5f).coerceAtMost(pageRect.right)
        if (rollRight > rollLeft) {
            val rollPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    rollLeft, 0f, rollRight, 0f,
                    intArrayOf(0x55000000, 0x22000000, 0x44FFFFFF, 0x00000000),
                    floatArrayOf(0f, 0.3f, 0.65f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val curlCurve = Path().apply {
                moveTo(topCurlX - cylinderWidth * 0.5f, pageRect.top)
                lineTo(topCurlX + cylinderWidth * 0.5f, pageRect.top)
                quadTo(curlX + cylinderWidth * 0.5f, pageRect.top + h * 0.5f, bottomCurlX + cylinderWidth * 0.5f, pageRect.bottom)
                lineTo(bottomCurlX - cylinderWidth * 0.5f, pageRect.bottom)
                quadTo(curlX - cylinderWidth * 0.5f, pageRect.top + h * 0.5f, topCurlX - cylinderWidth * 0.5f, pageRect.top)
                close()
            }
            canvas.drawPath(curlCurve, rollPaint)
        }
    }
}

/**
 * Draws the realistic book spine gutter shadow between two facing pages.
 */
private fun drawSpineGutterShadow(
    canvas: Canvas,
    spineX: Float,
    top: Float,
    height: Float,
    spineColor: Int
) {
    val gutterWidth = 32f

    // Center dark line (book stitch/binding groove)
    val seamPaint = Paint().apply {
        color = 0x55000000
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    canvas.drawLine(spineX, top, spineX, top + height, seamPaint)

    // Left shadow into left page
    val leftGutterPaint = Paint().apply {
        isAntiAlias = true
        shader = LinearGradient(
            spineX - gutterWidth, 0f, spineX, 0f,
            intArrayOf(0x00000000, 0x1A000000, 0x4D000000),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(spineX - gutterWidth, top, spineX, top + height, leftGutterPaint)

    // Right shadow into right page
    val rightGutterPaint = Paint().apply {
        isAntiAlias = true
        shader = LinearGradient(
            spineX, 0f, spineX + gutterWidth, 0f,
            intArrayOf(0x4D000000, 0x1A000000, 0x00000000),
            floatArrayOf(0f, 0.3f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(spineX, top, spineX + gutterWidth, top + height, rightGutterPaint)
}

/**
 * Draws stacked layered page edges on outer borders simulating 200+ paper leaves.
 */
private fun drawStackedPageEdges(
    canvas: Canvas,
    leftRect: RectF,
    rightRect: RectF,
    pageColor: Int
) {
    val edgePaint = Paint().apply {
        color = 0x22000000
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    val edgeFillPaint = Paint().apply {
        color = pageColor
        style = Paint.Style.FILL
    }

    // Left outer edge stack
    for (i in 1..4) {
        val offset = i * 2f
        val rect = RectF(
            leftRect.left - offset,
            leftRect.top + offset,
            leftRect.left,
            leftRect.bottom - offset
        )
        canvas.drawRect(rect, edgeFillPaint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.bottom, edgePaint)
    }

    // Right outer edge stack
    for (i in 1..4) {
        val offset = i * 2f
        val rect = RectF(
            rightRect.right,
            rightRect.top + offset,
            rightRect.right + offset,
            rightRect.bottom - offset
        )
        canvas.drawRect(rect, edgeFillPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, edgePaint)
    }
}

/**
 * Draws base page rectangle with warm paper color and subtle rounded corner.
 */
private fun drawBookPageSurface(canvas: Canvas, rect: RectF, pageColor: Int) {
    val pagePaint = Paint().apply {
        color = pageColor
        style = Paint.Style.FILL
    }
    canvas.drawRect(rect, pagePaint)
}

/**
 * Scales and centers the rendered PDF bitmap cleanly inside target page rect.
 */
private fun drawBitmapFitted(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
    if (bitmap.isRecycled) return

    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()
    val dw = destRect.width()
    val dh = destRect.height()

    val scale = min(dw / bw, dh / bh)
    val rw = bw * scale
    val rh = bh * scale

    val rx = destRect.left + (dw - rw) / 2f
    val ry = destRect.top + (dh - rh) / 2f

    val src = Rect(0, 0, bitmap.width, bitmap.height)
    val dst = RectF(rx, ry, rx + rw, ry + rh)

    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(bitmap, src, dst, paint)
}
