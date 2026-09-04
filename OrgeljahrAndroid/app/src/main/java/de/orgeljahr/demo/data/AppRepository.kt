package de.orgeljahr.demo.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.orgeljahr.demo.data.catalog.CatalogDatabase
import de.orgeljahr.demo.data.catalog.ChurchYearCalculator
import de.orgeljahr.demo.data.catalog.HymnEntity
import de.orgeljahr.demo.data.catalog.HymnTuneRow
import de.orgeljahr.demo.data.catalog.HymnWorkRow
import de.orgeljahr.demo.data.catalog.LiturgicalOccasionBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

class AppRepository(private val context: Context) {
    private val bachPersonId = "person:johann-sebastian-bach"
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val archiveFile = File(context.filesDir, "gottesdienste.json")
    private val attachmentsDirectory = File(context.filesDir, "attachments").apply { mkdirs() }
    private val readerPreferences = context.getSharedPreferences("performance_reader", Context.MODE_PRIVATE)
    private val catalogSnapshot = runCatching {
        runBlocking(Dispatchers.IO) { loadCatalogSnapshot() }
    }.getOrNull()

    var services by mutableStateOf(loadServices())
        private set

    val week: LiturgicalWeek = catalogSnapshot?.week ?: unverifiedWeek()
    val hymns: List<Hymn> = catalogSnapshot?.hymns?.ifEmpty { SampleData.hymns } ?: SampleData.hymns
    val melodies: List<Melody> = catalogSnapshot?.melodies.orEmpty()
    val templates: List<ServiceTemplate> = SampleData.templates

    fun hymn(egNumber: String): Hymn? = hymns.firstOrNull { it.egNumber == egNumber }

    fun newService(template: ServiceTemplate = templates.first()): WorshipService {
        val sections = template.sections.map { (title, type) ->
            if (title == "Wochenlied") {
                val number = week.hymnNumbers.firstOrNull().orEmpty()
                ServiceSection(title = title, type = type, hymnNumber = number, musicTitle = hymn(number)?.title.orEmpty())
            } else {
                ServiceSection(title = title, type = type)
            }
        }
        return WorshipService(
            date = week.isoDate,
            title = week.title,
            liturgicalColor = week.colorName,
            templateName = template.name,
            sections = sections
        )
    }

    fun saveService(service: WorshipService) {
        services = (services.filterNot { it.id == service.id } + service).sortedByDescending { it.date }
        archiveFile.writeText(gson.toJson(services))
    }

    fun deleteService(id: String) {
        val deletedService = services.firstOrNull { it.id == id }
        val remainingServices = services.filterNot { it.id == id }
        services = remainingServices
        archiveFile.writeText(gson.toJson(services))

        val filesStillInUse = remainingServices
            .flatMap { it.sections }
            .flatMap { it.attachments }
            .map { it.storedFileName }
            .toSet()
        deletedService?.sections
            ?.flatMap { it.attachments }
            ?.map { it.storedFileName }
            ?.distinct()
            ?.filterNot { it in filesStillInUse }
            ?.forEach(::deletePrivateAttachment)
        readerPreferences.edit().remove("page_$id").apply()
    }

    fun importFiles(uris: List<Uri>): List<ScoreAttachment> =
        uris.mapNotNull { uri -> runCatching { copyUri(uri, context.contentResolver) }.getOrNull() }

    fun shareableUri(attachment: ScoreAttachment): Uri {
        val file = File(attachmentsDirectory, attachment.storedFileName)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun attachmentFile(attachment: ScoreAttachment): File =
        File(attachmentsDirectory, attachment.storedFileName)

    fun performancePlan(service: WorshipService): PerformancePlan =
        buildPerformancePlan(service, ::attachmentPageCount)

    fun lastPerformancePage(serviceId: String): Int =
        readerPreferences.getInt("page_$serviceId", 0)

    fun savePerformancePage(serviceId: String, pageIndex: Int) {
        readerPreferences.edit().putInt("page_$serviceId", pageIndex).apply()
    }

    private fun attachmentPageCount(attachment: ScoreAttachment): Int {
        val file = attachmentFile(attachment)
        if (!file.exists()) return 0
        if (attachment.mimeType != "application/pdf") return 1
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
            }
        }.getOrDefault(0)
    }

    private data class CatalogSnapshot(
        val hymns: List<Hymn>,
        val melodies: List<Melody>,
        val week: LiturgicalWeek
    )

    private suspend fun loadCatalogSnapshot(): CatalogSnapshot {
        val dao = CatalogDatabase.get(context).catalogDao()
        val tunesByHymn = dao.allHymnTunes().groupBy(HymnTuneRow::hymnId)
        val worksByHymn = dao.allHymnWorks().groupBy(HymnWorkRow::hymnId)
        val catalogHymns = dao.hymns().map { hymn ->
            catalogHymn(
                entity = hymn,
                tunes = tunesByHymn[hymn.id].orEmpty(),
                works = worksByHymn[hymn.id].orEmpty()
            )
        }
        val catalogMelodies = buildMelodyIndex(catalogHymns, tunesByHymn.values.flatten())
        val sunday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val occasion = dao.allLiturgicalOccasions()
            .firstOrNull { bundle ->
                bundle.dateRules.any { rule -> ChurchYearCalculator.resolve(rule, sunday.year - 1) == sunday } ||
                    bundle.dateRules.any { rule -> ChurchYearCalculator.resolve(rule, sunday.year) == sunday }
            }
        val catalogWeek = occasion?.let { catalogWeek(it, sunday, dao.recommendationsForOccasion(it.occasion.id)) }
            ?: unverifiedWeek(sunday)
        return CatalogSnapshot(hymns = catalogHymns, melodies = catalogMelodies, week = catalogWeek)
    }

    private fun catalogHymn(
        entity: HymnEntity,
        tunes: List<HymnTuneRow>,
        works: List<HymnWorkRow>
    ): Hymn {
        val tunePresentation = buildHymnTunePresentation(tunes)
        return Hymn(
        id = entity.id,
        egNumber = entity.numberLabel,
        title = entity.title,
        tune = tunePresentation.names,
        textAuthor = "",
        tuneSource = tunePresentation.origins,
        year = tunePresentation.years,
        history = entity.history.ifBlank {
            "Katalogeintrag des bundesweit gemeinsamen EG-Stammteils. Text-, Melodie- und Geschichtsdaten werden quellen- und rechtegeprüft ergänzt."
        },
        verses = emptyList(),
        themes = listOfNotNull(entity.category.takeIf(String::isNotBlank)),
        relatedWorks = works.distinctBy(HymnWorkRow::workId).map { item ->
            OrganWork(
                id = item.workId,
                composer = item.composerDisplayName,
                title = item.workTitle,
                catalog = item.catalogNumber,
                category = item.genre,
                relation = "Choralbezug zur Melodie „${item.tuneName}“",
                duration = "",
                composerPersonId = item.composerPersonId
            )
        },
        hasScore = tunePresentation.hasScore
        )
    }

    private fun catalogWeek(
        bundle: LiturgicalOccasionBundle,
        sunday: LocalDate,
        recommendations: List<de.orgeljahr.demo.data.catalog.HymnRecommendation>
    ): LiturgicalWeek {
        val dateLabel = sunday.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() }
        val works = recommendations.groupBy { it.workId }.values.map { rows ->
            val item = rows.first()
            val egNumbers = rows.map { it.numberLabel }.distinct().joinToString(", ")
            OrganWork(
                id = item.workId,
                composer = item.composerDisplayName,
                title = item.workTitle,
                catalog = item.catalogNumber,
                category = item.genre,
                relation = "Choralbezug zu EG $egNumbers · ${item.tuneName}",
                duration = "",
                composerPersonId = item.composerPersonId
            )
        }
        return LiturgicalWeek(
            dateLabel = dateLabel,
            isoDate = "${sunday}T10:00",
            title = bundle.occasion.name,
            subtitle = bundle.occasion.season,
            colorName = bundle.occasion.defaultColor,
            weeklyVerse = bundle.lectionary?.weeklyVerseReference.orEmpty(),
            readings = listOfNotNull(
                bundle.lectionary?.oldTestamentReference?.takeIf(String::isNotBlank),
                bundle.lectionary?.epistleReference?.takeIf(String::isNotBlank),
                bundle.lectionary?.gospelReference?.takeIf(String::isNotBlank)
            ),
            hymnNumbers = bundle.hymns.sortedBy { it.position }
                .filter { it.collectionCode == "EG" }
                .flatMap { it.numberLabel.split("/") }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct(),
            bachWorks = works.filter { it.composerPersonId == bachPersonId },
            otherWorks = works.filterNot { it.composerPersonId == bachPersonId }
        )
    }

    private fun unverifiedWeek(
        sunday: LocalDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    ): LiturgicalWeek = LiturgicalWeek(
        dateLabel = sunday.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() },
        isoDate = "${sunday}T10:00",
        title = "Kirchenjahr-Daten nicht verfügbar",
        subtitle = "Es werden keine ungeprüften Ersatzangaben angezeigt.",
        colorName = "",
        weeklyVerse = "",
        readings = emptyList(),
        hymnNumbers = emptyList(),
        bachWorks = emptyList(),
        otherWorks = emptyList()
    )

    private fun deletePrivateAttachment(storedFileName: String) {
        runCatching {
            val attachmentRoot = attachmentsDirectory.canonicalFile
            val target = File(attachmentRoot, storedFileName).canonicalFile
            if (target.parentFile == attachmentRoot && target.isFile) {
                target.delete()
            }
        }
    }

    private fun copyUri(uri: Uri, resolver: ContentResolver): ScoreAttachment {
        val displayName = queryDisplayName(uri) ?: "Noten-${UUID.randomUUID()}"
        val extension = displayName.substringAfterLast('.', "")
        val storedName = "${UUID.randomUUID()}${if (extension.isBlank()) "" else ".$extension"}"
        val target = File(attachmentsDirectory, storedName)
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Datei konnte nicht geöffnet werden." }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return ScoreAttachment(
            displayName = displayName.substringBeforeLast('.', displayName),
            storedFileName = storedName,
            mimeType = resolver.getType(uri) ?: "application/octet-stream"
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun loadServices(): List<WorshipService> {
        if (!archiveFile.exists()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<WorshipService>>() {}.type
            gson.fromJson<List<WorshipService>>(archiveFile.readText(), type)
        }.getOrDefault(emptyList())
    }
}

internal data class HymnTunePresentation(
    val names: String,
    val origins: String,
    val years: String,
    val hasScore: Boolean
)

internal fun buildHymnTunePresentation(tunes: List<HymnTuneRow>): HymnTunePresentation {
    val orderedTunes = tunes.sortedWith(
        compareByDescending(HymnTuneRow::isPrimary).thenBy(HymnTuneRow::tuneName)
    )
    return HymnTunePresentation(
        names = orderedTunes.map(HymnTuneRow::tuneName)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" · "),
        origins = orderedTunes.map(HymnTuneRow::origin)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" · "),
        years = orderedTunes.map(HymnTuneRow::yearLabel)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" · "),
        hasScore = orderedTunes.any {
            !it.scoreAssetPath.isNullOrBlank() || !it.musicXmlAssetPath.isNullOrBlank()
        }
    )
}

internal fun buildMelodyIndex(
    hymns: List<Hymn>,
    tuneRows: List<HymnTuneRow>
): List<Melody> {
    val hymnsById = hymns.associateBy(Hymn::id)
    return tuneRows
        .groupBy(HymnTuneRow::melodyId)
        .mapNotNull { (melodyId, rows) ->
            val linkedHymns = rows
                .distinctBy(HymnTuneRow::hymnId)
                .mapNotNull { row ->
                    hymnsById[row.hymnId]?.let { hymn ->
                        MelodyHymnLink(hymn = hymn, isPrimary = row.isPrimary)
                    }
                }
                .sortedWith(
                    compareBy<MelodyHymnLink>({ it.hymn.egNumber.toIntOrNull() ?: Int.MAX_VALUE })
                        .thenBy { it.hymn.egNumber }
                )
            if (linkedHymns.isEmpty()) return@mapNotNull null

            val representative = rows.first()
            Melody(
                id = melodyId,
                name = representative.tuneName,
                origin = rows.firstOrNull { it.origin.isNotBlank() }?.origin.orEmpty(),
                year = rows.firstOrNull { it.yearLabel.isNotBlank() }?.yearLabel.orEmpty(),
                linkedHymns = linkedHymns
            )
        }
        .sortedBy { it.name.lowercase(Locale.GERMAN) }
}

internal fun buildPerformancePlan(
    service: WorshipService,
    attachmentPageCount: (ScoreAttachment) -> Int
): PerformancePlan {
    val pages = service.sections.flatMapIndexed { sectionIndex, section ->
        val nextSectionTitle = service.sections.getOrNull(sectionIndex + 1)?.title
        val cuePage = PerformancePage(
            sectionIndex = sectionIndex,
            section = section,
            nextSectionTitle = nextSectionTitle
        )
        val scorePages = section.attachments.flatMap { attachment ->
            val pageCount = attachmentPageCount(attachment).coerceAtLeast(0)
            (0 until pageCount).map { pageIndex ->
                PerformancePage(
                    sectionIndex = sectionIndex,
                    section = section,
                    attachment = attachment,
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    nextSectionTitle = nextSectionTitle
                )
            }
        }
        listOf(cuePage) + scorePages
    }
    return PerformancePlan(serviceId = service.id, pages = pages)
}
