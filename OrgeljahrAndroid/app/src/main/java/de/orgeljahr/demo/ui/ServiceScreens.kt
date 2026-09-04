package de.orgeljahr.demo.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.orgeljahr.demo.data.AppRepository
import de.orgeljahr.demo.data.ScoreAttachment
import de.orgeljahr.demo.data.SectionType
import de.orgeljahr.demo.data.ServiceSection
import de.orgeljahr.demo.data.WorshipService

@Composable
fun ServicesScreen(
    repository: AppRepository,
    contentPadding: PaddingValues,
    onEdit: (WorshipService) -> Unit,
    onPlay: (WorshipService) -> Unit
) {
    var servicePendingDeletion by remember { mutableStateOf<WorshipService?>(null) }

    servicePendingDeletion?.let { service ->
        AlertDialog(
            onDismissRequest = { servicePendingDeletion = null },
            title = { Text("Gottesdienst löschen?") },
            text = {
                Text(
                    "„${service.title}“ und die dazugehörigen lokalen Notendateien werden dauerhaft gelöscht."
                )
            },
            dismissButton = {
                TextButton(onClick = { servicePendingDeletion = null }) {
                    Text("Abbrechen")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.deleteService(service.id)
                        servicePendingDeletion = null
                    }
                ) {
                    Text("Endgültig löschen", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().background(Ivory).padding(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding()
        )
    ) {
        Text("Meine Gottesdienste", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Ablauf, Noten und Registrierungen an einem Ort",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repository.templates.forEach { template ->
                AssistChip(
                    onClick = { onEdit(repository.newService(template)) },
                    label = { Text(if (template.name == "Evangelischer Hauptgottesdienst") "Hauptgottesdienst" else template.name) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (repository.services.isEmpty()) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Piano, contentDescription = null, modifier = Modifier.size(44.dp), tint = Gold)
                    Text("Noch kein Gottesdienst", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Wählen Sie oben den Hauptgottesdienst oder einen leeren Ablauf.")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(repository.services, key = { it.id }) { service ->
                    ServiceCard(
                        service = service,
                        onClick = { onEdit(service) },
                        onPlay = { onPlay(service) },
                        onDelete = { servicePendingDeletion = service }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    service: WorshipService,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(service.title, fontWeight = FontWeight.Bold)
                Text(
                    service.date.replace("T", " · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    "${service.sections.size} Abschnitte · ${service.attachmentCount} Dateien · ${service.totalDuration} Min.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onPlay, enabled = service.sections.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Spielmodus starten", tint = Gold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceEditorScreen(
    initialService: WorshipService,
    repository: AppRepository,
    onBack: () -> Unit,
    onSave: (WorshipService) -> Unit,
    onStartPerformance: (WorshipService) -> Unit
) {
    var draft by remember(initialService.id) { mutableStateOf(initialService) }
    var editingSectionIndex by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val index = editingSectionIndex
    if (index != null) {
        SectionEditorScreen(
            initialSection = draft.sections[index],
            repository = repository,
            onBack = { editingSectionIndex = null },
            onSave = { section ->
                draft = draft.copy(sections = draft.sections.toMutableList().also { it[index] = section })
                editingSectionIndex = null
            }
        )
        return
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Abschnitt hinzufügen") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(SectionType.entries) { type ->
                        TextButton(
                            onClick = {
                                draft = draft.copy(sections = draft.sections + ServiceSection(title = type.label, type = type))
                                showAddDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(type.label, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gottesdienstmappe") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Zurück") }
                },
                actions = {
                    IconButton(onClick = { onSave(draft) }) {
                        Icon(Icons.Default.Save, contentDescription = "Sichern")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Ivory),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = 20.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = { draft = draft.copy(title = it) },
                            label = { Text("Bezeichnung") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.location,
                            onValueChange = { draft = draft.copy(location = it) },
                            label = { Text("Kirche / Ort") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.date,
                            onValueChange = { draft = draft.copy(date = it) },
                            label = { Text("Datum und Uhrzeit") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { onStartPerformance(draft) },
                    enabled = draft.sections.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Spielmodus starten", modifier = Modifier.padding(start = 8.dp))
                }
            }
            item { SectionTitle("FREI SORTIERBAR", "Ablauf") }
            items(draft.sections.size, key = { draft.sections[it].id }) { sectionIndex ->
                val section = draft.sections[sectionIndex]
                SectionCard(
                    section = section,
                    onClick = { editingSectionIndex = sectionIndex },
                    onUp = {
                        if (sectionIndex > 0) {
                            val list = draft.sections.toMutableList()
                            val moved = list.removeAt(sectionIndex)
                            list.add(sectionIndex - 1, moved)
                            draft = draft.copy(sections = list)
                        }
                    },
                    onDown = {
                        if (sectionIndex < draft.sections.lastIndex) {
                            val list = draft.sections.toMutableList()
                            val moved = list.removeAt(sectionIndex)
                            list.add(sectionIndex + 1, moved)
                            draft = draft.copy(sections = list)
                        }
                    },
                    onDelete = {
                        draft = draft.copy(sections = draft.sections.filterNot { it.id == section.id })
                    }
                )
            }
            item {
                Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Abschnitt hinzufügen", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: ServiceSection,
    onClick: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(section.title, fontWeight = FontWeight.Bold)
                val detail = when {
                    section.musicTitle.isNotBlank() -> section.musicTitle
                    section.hymnNumber.isNotBlank() -> "EG ${section.hymnNumber}"
                    else -> section.type.label
                }
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                if (section.attachments.isNotEmpty()) {
                    Text("${section.attachments.size} Datei(en)", color = Gold, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "Nach oben") }
            IconButton(onClick = onDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "Nach unten") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Löschen") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionEditorScreen(
    initialSection: ServiceSection,
    repository: AppRepository,
    onBack: () -> Unit,
    onSave: (ServiceSection) -> Unit
) {
    var draft by remember(initialSection.id) { mutableStateOf(initialSection) }
    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            draft = draft.copy(attachments = draft.attachments + repository.importFiles(uris))
        }
    }

    fun openAttachment(attachment: ScoreAttachment) {
        val uri = repository.shareableUri(attachment)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Keine passende Anzeige-App gefunden.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(draft.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Zurück") }
                },
                actions = {
                    IconButton(onClick = { onSave(draft) }) {
                        Icon(Icons.Default.Save, contentDescription = "Sichern")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Ivory),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = 20.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = { draft = draft.copy(title = it) },
                            label = { Text("Bezeichnung") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Art: ${draft.type.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionTitle("WERK UND ABLAUF", "Musik") }
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (draft.type.acceptsHymnNumber) {
                            OutlinedTextField(
                                value = draft.hymnNumber,
                                onValueChange = { draft = draft.copy(hymnNumber = it) },
                                label = { Text("EG-Nummer") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = draft.musicTitle,
                            onValueChange = { draft = draft.copy(musicTitle = it) },
                            label = { Text("Werk / Titel") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.composer,
                            onValueChange = { draft = draft.copy(composer = it) },
                            label = { Text("Komponist:in") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.catalog,
                            onValueChange = { draft = draft.copy(catalog = it) },
                            label = { Text("Werkverzeichnis") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = if (draft.durationMinutes == 0) "" else draft.durationMinutes.toString(),
                            onValueChange = { value ->
                                draft = draft.copy(durationMinutes = value.filter(Char::isDigit).toIntOrNull() ?: 0)
                            },
                            label = { Text("Dauer in Minuten") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item { SectionTitle("PERSÖNLICH", "Orgel") }
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = draft.registration,
                            onValueChange = { draft = draft.copy(registration = it) },
                            label = { Text("Registrierung") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.notes,
                            onValueChange = { draft = draft.copy(notes = it) },
                            label = { Text("Notizen") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item { SectionTitle("PRIVAT GESPEICHERT", "Eigene Noten") }
            item {
                Button(
                    onClick = { fileLauncher.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Text("PDF oder Bild auswählen", modifier = Modifier.padding(start = 8.dp))
                }
            }
            items(draft.attachments.size, key = { draft.attachments[it].id }) { attachmentIndex ->
                val attachment = draft.attachments[attachmentIndex]
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { openAttachment(attachment) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (attachment.mimeType == "application/pdf") Icons.Default.Description else Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = Gold
                        )
                        Text(
                            attachment.displayName,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = {
                                if (attachmentIndex > 0) {
                                    val files = draft.attachments.toMutableList()
                                    val moved = files.removeAt(attachmentIndex)
                                    files.add(attachmentIndex - 1, moved)
                                    draft = draft.copy(attachments = files)
                                }
                            },
                            enabled = attachmentIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Datei nach oben")
                        }
                        IconButton(
                            onClick = {
                                if (attachmentIndex < draft.attachments.lastIndex) {
                                    val files = draft.attachments.toMutableList()
                                    val moved = files.removeAt(attachmentIndex)
                                    files.add(attachmentIndex + 1, moved)
                                    draft = draft.copy(attachments = files)
                                }
                            },
                            enabled = attachmentIndex < draft.attachments.lastIndex
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Datei nach unten")
                        }
                        IconButton(
                            onClick = {
                                draft = draft.copy(attachments = draft.attachments.filterNot { it.id == attachment.id })
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Entfernen")
                        }
                    }
                }
            }
            item {
                Text(
                    "Die Dateien werden in den privaten App-Speicher kopiert und nicht veröffentlicht.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
