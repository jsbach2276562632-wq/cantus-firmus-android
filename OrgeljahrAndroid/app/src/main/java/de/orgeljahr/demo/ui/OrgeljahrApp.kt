package de.orgeljahr.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.orgeljahr.demo.data.AppRepository
import de.orgeljahr.demo.data.Hymn
import de.orgeljahr.demo.data.Melody
import de.orgeljahr.demo.data.OrganWork
import de.orgeljahr.demo.data.WorshipService

private data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun OrgeljahrApp(repository: AppRepository) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedHymn by remember { mutableStateOf<Hymn?>(null) }
    var selectedMelody by remember { mutableStateOf<Melody?>(null) }
    var melodyReturnHymn by remember { mutableStateOf<Hymn?>(null) }
    var hymnbookSearch by rememberSaveable { mutableStateOf("") }
    var hymnbookDirectory by rememberSaveable { mutableIntStateOf(0) }
    var editingService by remember { mutableStateOf<WorshipService?>(null) }
    var performingService by remember { mutableStateOf<WorshipService?>(null) }

    when {
        performingService != null -> PerformanceReaderScreen(
            service = performingService!!,
            repository = repository,
            onClose = {
                performingService = null
                selectedTab = 2
            }
        )
        selectedHymn != null -> {
            val hymn = selectedHymn!!
            val linkedMelodies = repository.melodies
                .filter { melody -> melody.linkedHymns.any { link -> link.hymn.id == hymn.id } }
                .sortedByDescending { melody ->
                    melody.linkedHymns.firstOrNull { link -> link.hymn.id == hymn.id }?.isPrimary == true
                }
            HymnDetailScreen(
                hymn = hymn,
                melodies = linkedMelodies,
                onBack = { selectedHymn = null },
                onMelody = { melody ->
                    melodyReturnHymn = hymn
                    selectedHymn = null
                    selectedMelody = melody
                }
            )
        }
        selectedMelody != null -> MelodyDetailScreen(
            melody = selectedMelody!!,
            onBack = {
                selectedMelody = null
                melodyReturnHymn?.let { hymn -> selectedHymn = hymn }
                melodyReturnHymn = null
            },
            onHymn = { selectedHymn = it }
        )
        editingService != null -> ServiceEditorScreen(
            initialService = editingService!!,
            repository = repository,
            onBack = { editingService = null },
            onSave = {
                repository.saveService(it)
                editingService = null
                selectedTab = 2
            },
            onStartPerformance = {
                repository.saveService(it)
                editingService = null
                performingService = it
            }
        )
        else -> {
            val tabs = listOf(
                TabItem("Diese Woche", Icons.Default.CalendarMonth),
                TabItem("Gesangbuch", Icons.Default.MenuBook),
                TabItem("Gottesdienste", Icons.Default.Piano)
            )
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                when (selectedTab) {
                    0 -> ThisWeekScreen(
                        repository, innerPadding,
                        onHymn = { selectedHymn = it },
                        onCreateService = { editingService = repository.newService() }
                    )
                    1 -> HymnbookScreen(
                        hymns = repository.hymns,
                        melodies = repository.melodies,
                        search = hymnbookSearch,
                        onSearchChange = { hymnbookSearch = it },
                        selectedDirectory = hymnbookDirectory,
                        onDirectoryChange = { hymnbookDirectory = it },
                        contentPadding = innerPadding,
                        onHymn = {
                            melodyReturnHymn = null
                            selectedHymn = it
                        },
                        onMelody = {
                            melodyReturnHymn = null
                            selectedMelody = it
                        }
                    )
                    else -> ServicesScreen(
                        repository = repository,
                        contentPadding = innerPadding,
                        onEdit = { editingService = it },
                        onPlay = { performingService = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThisWeekScreen(
    repository: AppRepository,
    contentPadding: PaddingValues,
    onHymn: (Hymn) -> Unit,
    onCreateService: () -> Unit
) {
    val week = repository.week
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Ivory),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Text("Cantus Firmus", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(LiturgicalGreen, CircleShape))
                        Text(
                            week.dateLabel.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(week.colorName, color = Color.White.copy(alpha = .75f))
                    }
                    Text(week.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(week.subtitle, color = Color.White.copy(alpha = .72f), fontSize = 18.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = .18f))
                    Text("WOCHENSPRUCH · BIBELSTELLE", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(
                        week.weeklyVerse.ifBlank { "Keine verifizierte Angabe verfügbar" },
                        color = Color.White,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 22.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        week.readings.forEach { reading ->
                            Text(
                                reading,
                                color = Color.White.copy(alpha = .78f),
                                fontSize = 10.sp,
                                modifier = Modifier.background(Color.White.copy(alpha = .10f), CircleShape).padding(7.dp)
                            )
                        }
                    }
                    Button(
                        onClick = onCreateService,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy)
                    ) {
                        Text("Gottesdienst für diesen Sonntag anlegen", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { SectionTitle("GESANGBUCH", "Wochenlied") }
        items(week.hymnNumbers) { number ->
            repository.hymn(number)?.let { hymn -> HymnRow(hymn) { onHymn(hymn) } }
        }
        item { SectionTitle("JOHANN SEBASTIAN BACH", "Bach der Woche") }
        if (week.bachWorks.isEmpty()) {
            item {
                Text(
                    "Für dieses Wochenlied ist noch kein geprüftes Orgelwerk verknüpft.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(week.bachWorks) { WorkCard(it, prominent = true) }
        }
        item { SectionTitle("PASSEND ZUM WOCHENLIED", "Weitere Orgelmusik") }
        if (week.otherWorks.isEmpty()) {
            item {
                Text(
                    "Weitere geprüfte Choralbearbeitungen werden schrittweise ergänzt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(week.otherWorks) { WorkCard(it, prominent = false) }
        }
    }
}

@Composable
fun SectionTitle(eyebrow: String, title: String) {
    Column {
        Text(eyebrow, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WorkCard(work: OrganWork, prominent: Boolean) {
    val catalogueLine = listOf(work.catalog, work.duration).filter(String::isNotBlank).joinToString(" · ")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(44.dp).background(
                    if (prominent) Gold.copy(alpha = .14f) else Sage.copy(alpha = .14f),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (prominent) Icons.Default.LibraryMusic else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (prominent) Gold else Sage
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(work.composer, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(work.title, fontWeight = FontWeight.Bold)
                if (catalogueLine.isNotBlank()) {
                    Text(catalogueLine, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Text(work.relation, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
