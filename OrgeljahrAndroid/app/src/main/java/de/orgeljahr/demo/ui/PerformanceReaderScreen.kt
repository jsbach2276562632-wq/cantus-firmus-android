package de.orgeljahr.demo.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.orgeljahr.demo.data.AppRepository
import de.orgeljahr.demo.data.PerformancePage
import de.orgeljahr.demo.data.WorshipService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PerformanceReaderScreen(
    service: WorshipService,
    repository: AppRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val plan = remember(service) { repository.performancePlan(service) }
    val renderer = remember(service.id) { PerformancePageRenderer(repository) }

    DisposableEffect(activity, renderer) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            renderer.close()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF111111)) {
        if (plan.pages.isEmpty()) {
            EmptyPerformancePlan(service.title, onClose)
        } else {
            val initialPage = repository.lastPerformancePage(service.id)
                .coerceIn(0, plan.pages.lastIndex)
            val pagerState = rememberPagerState(initialPage = initialPage) { plan.pages.size }
            val scope = rememberCoroutineScope()
            var viewport by remember { mutableStateOf(IntSize.Zero) }
            var controlsVisible by remember { mutableStateOf(true) }

            fun turnTo(page: Int) {
                if (page in plan.pages.indices) {
                    scope.launch { pagerState.animateScrollToPage(page, animationSpec = tween(130)) }
                }
            }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { repository.savePerformancePage(service.id, it) }
            }

            LaunchedEffect(pagerState.currentPage, viewport) {
                if (viewport != IntSize.Zero) {
                    val current = pagerState.currentPage
                    listOf(current + 2, current - 1, current + 1, current)
                        .filter { it in plan.pages.indices }
                        .forEach { renderer.load(plan.pages[it], viewport) }
                }
            }

            Box(modifier = Modifier.fillMaxSize().onSizeChanged { viewport = it }) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { index ->
                    PerformancePageContent(
                        page = plan.pages[index],
                        renderer = renderer,
                        viewport = viewport,
                        controlsVisible = controlsVisible,
                        onTap = { fraction ->
                            when {
                                fraction < .30f -> turnTo(index - 1)
                                fraction > .70f -> turnTo(index + 1)
                                else -> controlsVisible = !controlsVisible
                            }
                        }
                    )
                }

                val currentPage = plan.pages[pagerState.currentPage]
                ReaderControls(
                    page = currentPage,
                    pageIndex = pagerState.currentPage,
                    pageCount = plan.pages.size,
                    visible = controlsVisible,
                    onPrevious = { turnTo(pagerState.currentPage - 1) },
                    onNext = { turnTo(pagerState.currentPage + 1) },
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun PerformancePageContent(
    page: PerformancePage,
    renderer: PerformancePageRenderer,
    viewport: IntSize,
    controlsVisible: Boolean,
    onTap: (Float) -> Unit
) {
    val verticalPadding = if (controlsVisible) 58.dp else 0.dp
    Box(
        modifier = Modifier.fillMaxSize().padding(vertical = verticalPadding)
            .pointerInput(page.section.id, page.pageIndex) {
                detectTapGestures { position ->
                    onTap(if (size.width == 0) .5f else position.x / size.width)
                }
            }
    ) {
        if (page.isSectionPage) {
            SectionCuePage(page)
        } else {
            val cacheKey = renderer.cacheKey(page, viewport)
            val bitmap by produceState<Bitmap?>(
                initialValue = renderer.cached(page, viewport),
                cacheKey
            ) {
                value = renderer.load(page, viewport)
            }
            if (bitmap == null) {
                CircularProgressIndicator(color = Gold, modifier = Modifier.align(Alignment.Center))
            } else {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "${page.attachment?.displayName}, Seite ${page.pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SectionCuePage(page: PerformancePage) {
    val section = page.section
    val sectionTitle = section.title.trim().ifBlank { section.type.label }
    val hymnNumber = section.hymnNumber.trim()
    val musicTitle = section.musicTitle.trim()
    val workMetadata = listOf(section.composer.trim(), section.catalog.trim())
        .filter(String::isNotBlank)
        .joinToString(" · ")
    val hasDetails = hymnNumber.isNotBlank() ||
        musicTitle.isNotBlank() ||
        workMetadata.isNotBlank() ||
        section.durationMinutes > 0 ||
        section.registration.isNotBlank() ||
        section.notes.isNotBlank()
    var noteSize by remember(section.id, section.notes) { mutableStateOf(30.sp) }
    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 600.dp || maxHeight < 470.dp
            val horizontalPadding = if (compact) 28.dp else 52.dp
            val verticalPadding = if (compact) 24.dp else 38.dp
            val headingSize = if (compact) 34.sp else 44.sp

            Column(
                modifier = Modifier.fillMaxSize().padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                ),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "${(page.sectionIndex + 1).toString().padStart(2, '0')} · ${sectionTitle.uppercase(Locale.GERMAN)}",
                    color = Color(0xFF4B4B4D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!hasDetails) {
                    Spacer(Modifier.weight(1f))
                    return@Column
                }

                if (hymnNumber.isNotBlank()) {
                    Text(
                        "EG-Nummer: $hymnNumber",
                        color = Color.Black,
                        fontSize = headingSize,
                        lineHeight = headingSize * 1.08f,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (compact) 14.dp else 20.dp)
                    )
                } else if (musicTitle.isNotBlank()) {
                    Text(
                        musicTitle,
                        color = Color.Black,
                        fontSize = headingSize,
                        lineHeight = headingSize * 1.08f,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (compact) 14.dp else 20.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (hymnNumber.isNotBlank() && musicTitle.isNotBlank()) {
                    Text(
                        musicTitle,
                        color = Color(0xFF333335),
                        fontSize = if (compact) 18.sp else 23.sp,
                        lineHeight = if (compact) 23.sp else 29.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (workMetadata.isNotBlank()) {
                    Text(
                        workMetadata,
                        color = Color(0xFF4B4B4D),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 7.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = if (compact) 16.dp else 24.dp),
                    color = Color(0xFFB8B8BA)
                )

                val hasDuration = section.durationMinutes > 0
                val hasRegistration = section.registration.isNotBlank()
                if (hasDuration || hasRegistration) {
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (hasDuration) {
                                CueField("DAUER", "${section.durationMinutes} Minuten")
                            }
                            if (hasRegistration) {
                                CueField("REGISTRATION", section.registration)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(34.dp)
                        ) {
                            if (hasRegistration) {
                                CueField("REGISTRATION", section.registration, Modifier.weight(1f))
                            }
                            if (hasDuration) {
                                CueField("DAUER", "${section.durationMinutes} Minuten")
                            }
                        }
                    }
                }

                if (section.notes.isBlank()) {
                    Spacer(Modifier.weight(1f))
                } else {
                    Text(
                        "NOTIZ",
                        color = Color(0xFF4B4B4D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(
                            top = if (hasDuration || hasRegistration) 24.dp else 0.dp,
                            bottom = 9.dp
                        )
                    )
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            section.notes,
                            color = Color.Black,
                            fontSize = noteSize,
                            lineHeight = noteSize * 1.25f,
                            textAlign = TextAlign.Start,
                            onTextLayout = { result ->
                                if (result.hasVisualOverflow && noteSize.value > 13f) {
                                    noteSize = (noteSize.value - 1f).sp
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CueField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color(0xFF4B4B4D),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            value,
            color = Color.Black,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun ReaderControls(
    page: PerformancePage,
    pageIndex: Int,
    pageCount: Int,
    visible: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .84f))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Spielmodus schließen", tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    page.section.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    page.attachment?.displayName ?: "Hinweisseite",
                    color = Color.White.copy(alpha = .7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("${pageIndex + 1} / $pageCount", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .84f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, enabled = pageIndex > 0) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Vorherige Seite", tint = Color.White)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (page.section.registration.isNotBlank() && !page.isSectionPage) {
                        Text(
                            "Registration: ${page.section.registration}",
                            color = Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    page.nextSectionTitle?.let {
                        Text("Als Nächstes: $it", color = Color.White.copy(alpha = .72f), fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onNext, enabled = pageIndex < pageCount - 1) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Nächste Seite", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyPerformancePlan(title: String, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Dieser Ablauf enthält noch keine Abschnitte.", color = Color.White.copy(alpha = .72f))
        }
    }
}

private class PerformancePageRenderer(private val repository: AppRepository) : Closeable {
    private data class PdfSession(
        val descriptor: ParcelFileDescriptor,
        val renderer: PdfRenderer
    ) : Closeable {
        override fun close() {
            renderer.close()
            descriptor.close()
        }
    }

    private val cacheLimit = (Runtime.getRuntime().maxMemory() / 8L)
        .coerceIn(64L * 1024 * 1024, 128L * 1024 * 1024)
        .toInt()
    private val bitmapCache = object : LruCache<String, Bitmap>(cacheLimit) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val sessions = mutableMapOf<String, PdfSession>()
    private val renderMutex = Mutex()
    private val closeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var closed = false

    fun cacheKey(page: PerformancePage, viewport: IntSize): String {
        val attachment = page.attachment ?: return "section:${page.section.id}"
        return "${attachment.id}:${page.pageIndex}:${viewport.width}x${viewport.height}"
    }

    fun cached(page: PerformancePage, viewport: IntSize): Bitmap? =
        bitmapCache.get(cacheKey(page, viewport))

    suspend fun load(page: PerformancePage, viewport: IntSize): Bitmap? {
        val attachment = page.attachment ?: return null
        if (viewport == IntSize.Zero || closed) return null
        val key = cacheKey(page, viewport)
        bitmapCache.get(key)?.let { return it }

        return renderMutex.withLock {
            if (closed) return@withLock null
            bitmapCache.get(key)?.let { return@withLock it }
            val rendered = withContext(Dispatchers.IO) {
                runCatching {
                    val file = repository.attachmentFile(attachment)
                    if (attachment.mimeType == "application/pdf") {
                        renderPdfPage(file, page.pageIndex, viewport)
                    } else {
                        decodeImage(file, viewport)
                    }
                }.getOrNull()
            }
            rendered?.also { bitmapCache.put(key, it) }
        }
    }

    private fun renderPdfPage(file: File, pageIndex: Int, viewport: IntSize): Bitmap {
        val path = file.absolutePath
        val session = sessions.getOrPut(path) {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfSession(descriptor, PdfRenderer(descriptor))
        }
        session.renderer.openPage(pageIndex).use { pdfPage ->
            val renderWidth = (viewport.width * 1.15f).roundToInt().coerceIn(900, 1900)
            val renderHeight = (renderWidth * pdfPage.height.toFloat() / pdfPage.width)
                .roundToInt().coerceAtMost(3400)
            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(AndroidColor.WHITE)
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    private fun decodeImage(file: File, viewport: IntSize): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val targetWidth = max((viewport.width * 1.4f).roundToInt(), 1400)
        val targetHeight = max((viewport.height * 1.4f).roundToInt(), 1400)
        while (bounds.outWidth / sample > targetWidth || bounds.outHeight / sample > targetHeight) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    override fun close() {
        closed = true
        closeScope.launch {
            renderMutex.withLock {
                bitmapCache.evictAll()
                sessions.values.forEach { runCatching { it.close() } }
                sessions.clear()
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
