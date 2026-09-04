package de.orgeljahr.demo.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.orgeljahr.demo.data.Hymn
import de.orgeljahr.demo.data.Melody
import de.orgeljahr.demo.data.MelodyHymnLink

@Composable
fun HymnbookScreen(
    hymns: List<Hymn>,
    melodies: List<Melody>,
    search: String,
    onSearchChange: (String) -> Unit,
    selectedDirectory: Int,
    onDirectoryChange: (Int) -> Unit,
    contentPadding: PaddingValues,
    onHymn: (Hymn) -> Unit,
    onMelody: (Melody) -> Unit
) {
    val filteredHymns = remember(search, hymns) {
        if (search.isBlank()) hymns else hymns.filter { hymn ->
            listOf(hymn.egNumber, hymn.title, hymn.tune, hymn.themes.joinToString(" "))
                .any { it.contains(search, ignoreCase = true) }
        }
    }
    val filteredMelodies = remember(search, melodies) {
        if (search.isBlank()) melodies else melodies.filter { melody ->
            listOf(
                melody.name,
                melody.origin,
                melody.year,
                melody.linkedHymns.joinToString(" ") { "${it.hymn.egNumber} ${it.hymn.title}" }
            ).any { it.contains(search, ignoreCase = true) }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Ivory).padding(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding()
        )
    ) {
        Text("Gesangbuch", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selectedDirectory == 0) {
                Button(
                    onClick = { onDirectoryChange(0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy)
                ) { Text("Lieder") }
            } else {
                OutlinedButton(onClick = { onDirectoryChange(0) }, modifier = Modifier.weight(1f)) {
                    Text("Lieder")
                }
            }
            if (selectedDirectory == 1) {
                Button(
                    onClick = { onDirectoryChange(1) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy)
                ) { Text("Melodien") }
            } else {
                OutlinedButton(onClick = { onDirectoryChange(1) }, modifier = Modifier.weight(1f)) {
                    Text("Melodien")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = {
                Text(if (selectedDirectory == 0) "EG-Nummer, Titel oder Melodie" else "Melodie oder EG-Nummer")
            },
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selectedDirectory == 0) {
                items(filteredHymns, key = { it.id }) { hymn -> HymnRow(hymn) { onHymn(hymn) } }
            } else {
                items(filteredMelodies, key = { it.id }) { melody ->
                    MelodyRow(melody) { onMelody(melody) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MelodyRow(melody: Melody, onClick: () -> Unit) {
    val egNumbers = melody.linkedHymns.joinToString(" · ") { it.hymn.egNumber }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(Gold.copy(alpha = .16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("♫", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(melody.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "EG $egNumbers",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HymnRow(hymn: Hymn, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(Sage, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("EG\n${hymn.egNumber}", color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(hymn.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    hymn.themes.firstOrNull().orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HymnDetailScreen(
    hymn: Hymn,
    melodies: List<Melody>,
    onBack: () -> Unit,
    onMelody: (Melody) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EG ${hymn.egNumber}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Text("EG ${hymn.egNumber}", color = Gold, fontWeight = FontWeight.Bold)
                Text(hymn.title, fontSize = 31.sp, fontWeight = FontWeight.Bold, lineHeight = 35.sp)
                Text(
                    hymn.themes.firstOrNull().orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            if (!hymn.hasScore) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Text(
                            if (hymn.tune.isBlank()) {
                                "Melodie und geprüfter Notensatz sind für diesen Verzeichniseintrag noch nicht hinterlegt."
                            } else {
                                "Die Melodie ist geprüft; ein verifizierter Notensatz ist noch nicht hinterlegt."
                            },
                            modifier = Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        MetadataRow("Textverfasser", hymn.textAuthor)
                        HorizontalDivider()
                        MelodyMetadataRow(hymn.tune, melodies, onMelody)
                        HorizontalDivider()
                        MetadataRow("Melodieautor / Quelle", hymn.tuneSource)
                        HorizontalDivider()
                        MetadataRow("Entstehungsjahr", hymn.year)
                    }
                }
            }
            item { SectionTitle("LIEDTEXT", "Text") }
            if (hymn.verses.isEmpty()) {
                item {
                    Text(
                        "Der vollständige Liedtext wird nach Quellen- und Rechteprüfung ergänzt.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
            } else {
                items(hymn.verses) { verse -> Text(verse, lineHeight = 24.sp) }
            }
            item { SectionTitle("HINTERGRUND", "Geschichte") }
            item { Text(hymn.history, lineHeight = 24.sp) }
            if (hymn.relatedWorks.isNotEmpty()) {
                item { SectionTitle("CHORALBEZUG", "Werke zu diesem Choral") }
                items(hymn.relatedWorks) { WorkCard(it, prominent = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelodyDetailScreen(
    melody: Melody,
    onBack: () -> Unit,
    onHymn: (Hymn) -> Unit
) {
    val linkedCountLabel = if (melody.linkedHymns.size == 1) {
        "1 verknüpftes EG-Lied"
    } else {
        "${melody.linkedHymns.size} verknüpfte EG-Lieder"
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Melodie") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("MELODIENVERZEICHNIS", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(melody.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            }
            if (melody.origin.isNotBlank() || melody.year.isNotBlank()) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            MetadataRow("Herkunft", melody.origin)
                            HorizontalDivider()
                            MetadataRow("Entstehung", melody.year)
                        }
                    }
                }
            }
            item { SectionTitle("VERKNÜPFUNGEN", linkedCountLabel) }
            items(melody.linkedHymns, key = { it.hymn.id }) { link ->
                MelodyHymnRow(link = link, onClick = { onHymn(link.hymn) })
            }
        }
    }
}

@Composable
private fun MelodyHymnRow(link: MelodyHymnLink, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(Sage, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("EG\n${link.hymn.egNumber}", color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(link.hymn.title, fontWeight = FontWeight.Bold)
                Text(
                    if (link.isPrimary) "Hauptmelodie" else "Alternativmelodie",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(160.dp))
        Text(value.ifBlank { "Noch nicht erfasst" }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MelodyMetadataRow(
    fallbackName: String,
    melodies: List<Melody>,
    onMelody: (Melody) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Text(
            "Melodie",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(160.dp)
        )
        if (melodies.isEmpty()) {
            Text(fallbackName.ifBlank { "Noch nicht erfasst" }, modifier = Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                melodies.forEach { melody ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onMelody(melody) }.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            melody.name,
                            color = Navy,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text("›", color = Navy, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}
