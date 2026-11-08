package tv.nuvio.lite.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tv.nuvio.lite.addon.CatalogItem
import tv.nuvio.lite.addon.InstalledAddon
import tv.nuvio.lite.addon.StreamLink

@Composable
fun TvHomeScreen(
    movies: List<CatalogItem>,
    series: List<CatalogItem>,
    searchResults: List<CatalogItem>,
    installedAddons: List<InstalledAddon>,
    selectedItem: CatalogItem?,
    availableStreams: List<StreamLink>,
    isStreamsLoading: Boolean,
    isLoading: Boolean,
    isLowRam: Boolean,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAddAddon: (String) -> Unit,
    onRemoveAddon: (String) -> Unit,
    onSelectContent: (CatalogItem) -> Unit,
    onCloseDetails: () -> Unit,
    onPlayInternal: (String) -> Unit,
    onPlayExternal: (String, String) -> Unit
) {
    var showAddonDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D12)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF3B82F6))
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D12)),
            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NUVIO LITE",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Button(
                            onClick = { showAddonDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Addons (${installedAddons.size})", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isBlank()) onClearSearch() else onSearch(it)
                        },
                        placeholder = { Text("Search movies, series...", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                item {
                    CatalogSection("Search Results", searchResults, isLowRam, onSelectContent)
                }
            } else {
                if (movies.isNotEmpty()) {
                    item {
                        CatalogSection("Movies", movies, isLowRam, onSelectContent)
                    }
                }
                if (series.isNotEmpty()) {
                    item {
                        CatalogSection("Series", series, isLowRam, onSelectContent)
                    }
                }
            }
        }

        selectedItem?.let { item ->
            DetailsDialog(
                item = item,
                streams = availableStreams,
                isLoading = isStreamsLoading,
                onDismiss = onCloseDetails,
                onPlayInternal = onPlayInternal,
                onPlayExternal = onPlayExternal
            )
        }

        if (showAddonDialog) {
            AddonManagerDialog(
                addons = installedAddons,
                onDismiss = { showAddonDialog = false },
                onAddUrl = { url ->
                    onAddAddon(url)
                    showAddonDialog = false
                },
                onRemove = { onRemoveAddon(it) }
            )
        }
    }
}

@Composable
fun DetailsDialog(
    item: CatalogItem,
    streams: List<StreamLink>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onPlayInternal: (String) -> Unit,
    onPlayExternal: (String, String) -> Unit
) {
    var selectedStreamForAction by remember { mutableStateOf<StreamLink?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161A22),
        title = {
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(80.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.description ?: "No description available.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 6,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Streams (${streams.size})",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(28.dp))
                    }
                } else if (streams.isEmpty()) {
                    Text(
                        text = "No playable stream links found from active add-ons.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(streams) { link ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF222834)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedStreamForAction = link }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = link.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = link.addonName,
                                        color = Color(0xFF60A5FA),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.LightGray)
            }
        }
    )

    // Player Selection Chooser Dialog
    selectedStreamForAction?.let { stream ->
        AlertDialog(
            onDismissRequest = { selectedStreamForAction = null },
            containerColor = Color(0xFF1E2430),
            title = {
                Text("Choose Player", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stream.title, color = Color.LightGray, fontSize = 12.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val url = stream.url
                            selectedStreamForAction = null
                            onPlayInternal(url)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Internal Player (Low-RAM)", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val url = stream.url
                            val title = item.title
                            selectedStreamForAction = null
                            onPlayExternal(url, title)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("External Player (VLC / MX / Just Player)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedStreamForAction = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AddonManagerDialog(
    addons: List<InstalledAddon>,
    onDismiss: () -> Unit,
    onAddUrl: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Installed Add-ons", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF181C24),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("Paste Addon Manifest URL") },
                    placeholder = { Text("https://.../manifest.json") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(addons) { addon ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(addon.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(addon.transportUrl, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                            }
                            if (addon.id != "cinemeta") {
                                TextButton(onClick = { onRemove(addon.transportUrl) }) {
                                    Text("Delete", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (inputUrl.isNotBlank()) onAddUrl(inputUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Install Addon")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.LightGray)
            }
        }
    )
}

@Composable
fun CatalogSection(
    title: String,
    items: List<CatalogItem>,
    isLowRam: Boolean,
    onItemSelected: (CatalogItem) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.id }) { item ->
                TvCard(item, isLowRam) { onItemSelected(item) }
            }
        }
    }
}

@Composable
fun TvCard(
    item: CatalogItem,
    isLowRam: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.06f else 1.0f, label = "cardScale")
    val cardBg = if (isLowRam) Color(0xFF161A22) else Color(0x33FFFFFF)

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(
                width = if (isFocused) 2.dp else 0.5.dp,
                color = if (isFocused) Color.White else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 120f
                    )
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = item.title,
                fontSize = 11.sp,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}
