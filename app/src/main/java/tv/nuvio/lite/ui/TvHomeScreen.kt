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

@Composable
fun TvHomeScreen(
    movies: List<CatalogItem>,
    series: List<CatalogItem>,
    installedAddons: List<InstalledAddon>,
    isLoading: Boolean,
    isLowRam: Boolean,
    onAddAddon: (String) -> Unit,
    onRemoveAddon: (String) -> Unit,
    onItemSelected: (CatalogItem) -> Unit
) {
    var showAddonDialog by remember { mutableStateOf(false) }

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
            contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
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
            }

            if (movies.isNotEmpty()) {
                item {
                    CatalogSection("Movies", movies, isLowRam, onItemSelected)
                }
            }

            if (series.isNotEmpty()) {
                item {
                    CatalogSection("Series", series, isLowRam, onItemSelected)
                }
            }
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
