package com.folioreader.bookreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class PdfDocumentManager(private val context: Context) {

    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val renderMutex = Mutex()

    var totalPageCount: Int = 0
        private set

    var documentTitle: String = "Untitled"
        private set

    var currentUri: Uri? = null
        private set

    // LRU Bitmap cache: max 8 rendered page bitmaps to balance high performance and strict RAM bounds
    private val bitmapCache = object : LruCache<Int, Bitmap>(8) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted && oldValue != null && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    suspend fun openDocument(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        close()
        currentUri = uri
        documentTitle = queryFileName(uri) ?: "Document"

        try {
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
            } catch (_: Exception) {
                // Ignore and fall back to copying
            }

            // If not seekable or null, copy stream to local cache
            if (pfd == null) {
                val tempFile = File(context.cacheDir, "current_reading.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            val safePfd = pfd ?: return@withContext false
            parcelFileDescriptor = safePfd
            pdfRenderer = PdfRenderer(safePfd)
            totalPageCount = pdfRenderer?.pageCount ?: 0
            true
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    suspend fun getPageBitmap(
        pageIndex: Int,
        targetWidth: Int,
        targetHeight: Int,
        paperBackgroundColor: Int = Color.WHITE
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (pageIndex < 0 || pageIndex >= totalPageCount) return@withContext null

        // Check cache first
        val cached = bitmapCache.get(pageIndex)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        renderMutex.withLock {
            // Re-check after lock
            val secondCheck = bitmapCache.get(pageIndex)
            if (secondCheck != null && !secondCheck.isRecycled) {
                return@withLock secondCheck
            }

            val renderer = pdfRenderer ?: return@withLock null
            if (targetWidth <= 0 || targetHeight <= 0) return@withLock null

            try {
                val page = renderer.openPage(pageIndex)
                val pageWidth = page.width
                val pageHeight = page.height

                // Calculate aspect-preserving dimensions
                val scale = min(
                    targetWidth.toFloat() / pageWidth.toFloat(),
                    targetHeight.toFloat() / pageHeight.toFloat()
                )
                val finalWidth = (pageWidth * scale).toInt().coerceAtLeast(1)
                val finalHeight = (pageHeight * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(paperBackgroundColor)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                bitmapCache.put(pageIndex, bitmap)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun prefetchPages(centerPage: Int, targetWidth: Int, targetHeight: Int, paperBg: Int) {
        withContext(Dispatchers.Default) {
            val pagesToPrefetch = listOf(
                centerPage - 1,
                centerPage + 1,
                centerPage + 2
            ).filter { it in 0 until totalPageCount }

            for (p in pagesToPrefetch) {
                if (bitmapCache.get(p) == null) {
                    getPageBitmap(p, targetWidth, targetHeight, paperBg)
                }
            }
        }
    }

    fun clearCache() {
        bitmapCache.evictAll()
    }

    fun close() {
        clearCache()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null

        try {
            parcelFileDescriptor?.close()
        } catch (_: Exception) {}
        parcelFileDescriptor = null

        totalPageCount = 0
    }

    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "file") {
            return File(uri.path ?: "").name
        }
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }

    companion object {
        /**
         * Creates a built-in literary sample book using Android's native PdfDocument.
         * Zero external assets, zero download size, instant offline sample testing.
         */
        suspend fun createSampleBook(context: Context): Uri = withContext(Dispatchers.IO) {
            val sampleFile = File(context.cacheDir, "sample_classic_book.pdf")
            if (sampleFile.exists() && sampleFile.length() > 0) {
                return@withContext Uri.fromFile(sampleFile)
            }

            val doc = PdfDocument()
            val pageWidth = 595 // Standard A4 points (72 dpi)
            val pageHeight = 842
            val margin = 56

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2C221E")
                textSize = 15f
                typeface = Typeface.SERIF
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1C1512")
                textSize = 26f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#7D634C")
                textSize = 14f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
            }
            val chapterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#443224")
                textSize = 18f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val ornamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C8A878")
                textSize = 16f
                textAlign = Paint.Align.CENTER
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#998270")
                textSize = 11f
                typeface = Typeface.SERIF
                textAlign = Paint.Align.CENTER
            }
            val bgPaint = Paint().apply {
                color = Color.parseColor("#FDFBF7")
            }

            val pagesContent = listOf(
                // Page 1: Cover / Title Page
                PageData(
                    isCover = true,
                    title = "ALICE'S ADVENTURES\nIN WONDERLAND",
                    subtitle = "By Lewis Carroll\nWith Classic Typography",
                    paragraphs = listOf(
                        "\"Curiouser and curiouser!\" cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English).",
                        "A classic demonstration book specially prepared for FolioReader with realistic physical page curl, dual facing pages, and warm paper rendering."
                    )
                ),
                // Page 2: Chapter 1
                PageData(
                    isCover = false,
                    chapter = "CHAPTER I",
                    title = "Down the Rabbit-Hole",
                    paragraphs = listOf(
                        "Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, 'and what is the use of a book,' thought Alice 'without pictures or conversations?'",
                        "So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.",
                        "There was nothing so very remarkable in that; nor did Alice think it so very much out of the way to hear the Rabbit say to itself, 'Oh dear! Oh dear! I shall be late!' (when she thought it over afterwards, it occurred to her that she ought to have wondered at this, but at the time it all seemed quite natural)."
                    )
                ),
                // Page 3: The Rabbit Hole
                PageData(
                    isCover = false,
                    chapter = "CHAPTER I (Continued)",
                    title = "The Deep Well",
                    paragraphs = listOf(
                        "In another moment down went Alice after it, never once considering in the world how she was to get out again.",
                        "The rabbit-hole went straight on like a tunnel for some way, and then dipped suddenly down, so suddenly that Alice had not a moment to think about stopping herself before she found herself falling down a very deep well.",
                        "Either the well was very deep, or she fell very slowly, for she had plenty of time as she went down to look about her and to wonder what was going to happen next. First, she tried to look down and make out what she was coming to, but it was too dark to see anything; then she looked at the sides of the well, and noticed that they were filled with cupboards and book-shelves; here and there she saw maps and pictures hung upon pegs."
                    )
                ),
                // Page 4: Orange Marmalade
                PageData(
                    isCover = false,
                    chapter = "CHAPTER I (Continued)",
                    title = "Through the Cupboards",
                    paragraphs = listOf(
                        "She took down a jar from one of the shelves as she passed; it was labelled 'ORANGE MARMALADE', but to her great disappointment it was empty: she did not like to drop the jar for fear of killing somebody, so managed to put it into one of the cupboards as she fell past it.",
                        "'Well!' thought Alice to herself, 'after such a fall as this, I shall think nothing of tumbling down stairs! How brave they'll all think me at home! Why, I wouldn't say anything about it, even if I fell off the top of the house!' (Which was very likely true.)",
                        "Down, down, down. Would the fall never come to an end! 'I wonder how many miles I've fallen by this time?' she said aloud. 'I must be getting somewhere near the centre of the earth. Let me see: that would be four thousand miles down, I think--'"
                    )
                ),
                // Page 5: Chapter 2
                PageData(
                    isCover = false,
                    chapter = "CHAPTER II",
                    title = "The Pool of Tears",
                    paragraphs = listOf(
                        "'Curiouser and curiouser!' cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English); 'now I'm opening out like the largest telescope that ever was! Good-bye, feet!' (for when she looked down at her feet, they seemed to be almost out of sight, they were getting so far off).",
                        "'Oh, my poor little feet, I wonder who will put on your shoes and stockings for you now, dears? I'm sure I shan't be able! I shall be a great deal too far off to trouble myself about you: you must manage the best way you can;--but I must be kind to them,' thought Alice, 'or perhaps they won't walk the way I want to go!'",
                        "And she went on planning to herself how she would manage it. 'They must go by the carrier,' she thought; 'and how funny it'll seem, sending presents to one's own feet!'"
                    )
                ),
                // Page 6: The Art of Physical Reading
                PageData(
                    isCover = false,
                    chapter = "EPILOGUE",
                    title = "The Art of the Physical Page",
                    paragraphs = listOf(
                        "There is a timeless intimacy in holding a book—the weight of pages resting against the palm, the gentle rustle of paper curving between thumb and fingers, and the soft shadow dancing down the spine gutter.",
                        "FolioReader brings that physical tactile presence to digital screens. Turn pages naturally with realistic curved physics, read side-by-side spreads on wider displays, and let words breathe in warm, gentle paper tones.",
                        "Select any PDF from your local device storage at any time to immerse yourself in your own library."
                    )
                )
            )

            for (index in pagesContent.indices) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                // Page Border frame
                val framePaint = Paint().apply {
                    color = Color.parseColor("#EDE4D3")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f
                }
                canvas.drawRect(
                    (margin - 16).toFloat(),
                    (margin - 16).toFloat(),
                    (pageWidth - margin + 16).toFloat(),
                    (pageHeight - margin + 16).toFloat(),
                    framePaint
                )

                val content = pagesContent[index]
                var y = if (content.isCover) margin + 100f else margin + 40f

                if (content.isCover) {
                    canvas.drawText("✦  ❦  ✦", (pageWidth / 2).toFloat(), y - 30f, ornamentPaint)
                    val titleLines = content.title.split("\n")
                    for (line in titleLines) {
                        canvas.drawText(line, (pageWidth / 2).toFloat(), y, titlePaint)
                        y += 34f
                    }
                    y += 18f
                    canvas.drawText("————— ◈ —————", (pageWidth / 2).toFloat(), y, ornamentPaint)
                    y += 36f

                    content.subtitle?.split("\n")?.forEach { sub ->
                        canvas.drawText(sub, (pageWidth / 2).toFloat(), y, subtitlePaint)
                        y += 24f
                    }
                    y += 40f
                } else {
                    if (content.chapter != null) {
                        canvas.drawText(content.chapter, (pageWidth / 2).toFloat(), y, subtitlePaint)
                        y += 26f
                    }
                    canvas.drawText(content.title, (pageWidth / 2).toFloat(), y, chapterPaint)
                    y += 16f
                    canvas.drawText("— ❦ —", (pageWidth / 2).toFloat(), y, ornamentPaint)
                    y += 36f
                }

                // Paragraphs with word wrapping
                val maxWidth = pageWidth - (margin * 2)
                for (paragraph in content.paragraphs) {
                    val lines = wrapText(paragraph, textPaint, maxWidth.toFloat())
                    for (line in lines) {
                        canvas.drawText(line, margin.toFloat(), y, textPaint)
                        y += 22f
                    }
                    y += 14f // paragraph spacing
                }

                // Footer page number
                val footerText = "— ${index + 1} —"
                canvas.drawText(footerText, (pageWidth / 2).toFloat(), (pageHeight - margin + 2).toFloat(), footerPaint)

                doc.finishPage(page)
            }

            FileOutputStream(sampleFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()

            Uri.fromFile(sampleFile)
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
            return lines
        }

        private data class PageData(
            val isCover: Boolean,
            val title: String,
            val subtitle: String? = null,
            val chapter: String? = null,
            val paragraphs: List<String>
        )
    }
}
