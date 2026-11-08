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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tv.nuvio.lite.addon.AppTheme
import tv.nuvio.lite.addon.CatalogItem
import tv.nuvio.lite.addon.InstalledAddon
import tv.nuvio.lite.addon.NavSection
import tv.nuvio.lite.addon.StreamLink

@Composable
fun TvHomeScreen(
    movies: List<CatalogItem>,
    series: List<CatalogItem>,
    watchlist: List<CatalogItem>,
    searchResults: List<CatalogItem>,
    installedAddons: List<InstalledAddon>,
    selectedItem: CatalogItem?,
    availableStreams: List<StreamLink>,
    isStreamsLoading: Boolean,
    isItemInWatchlist: Boolean,
    isLoading: Boolean,
    currentTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onToggleWatchlist: (CatalogItem) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAddAddon: (String) -> Unit,
    onRemoveAddon: (String) -> Unit,
    onSelectContent: (CatalogItem) -> Unit,
    onCloseDetails: () -> Unit,
    onPlayInternal: (String, String) -> Unit,
    onPlayExternal: (String, String) -> Unit
) {
    var currentSection by remember { mutableStateOf(NavSection.MOVIES) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddonDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bg)
    ) {
        // DOCKED OTT SIDEBAR
        OttSidebar(
            selectedSection = currentSection,
            accentColor = currentTheme.accent,
            sidebarBg = currentTheme.surface,
            onSelectSection = { section -> currentSection = section }
        )

        // MAIN CONTENT
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp)
            ) {
                // Search bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isBlank()) onClearSearch() else onSearch(it)
                        },
                        placeholder = { Text("Search movies, series...", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentTheme.accent,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = currentTheme.surface,
                            unfocusedContainerColor = currentTheme.surface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 8.dp)
                            .height(48.dp)
                    )
                }

                if (searchQuery.isNotBlank()) {
                    item {
                        CleanCatalogRow("Search Results", searchResults, currentTheme.accent, onSelectContent)
                    }
                } else {
                    when (currentSection) {
                        NavSection.MOVIES -> {
                            item { CleanCatalogRow("Popular Movies", movies, currentTheme.accent, onSelectContent) }
                            item { CleanCatalogRow("Top Picks", movies.reversed(), currentTheme.accent, onSelectContent) }
                        }
                        NavSection.SERIES -> {
                            item { CleanCatalogRow("Trending Series", series, currentTheme.accent, onSelectContent) }
                        }
                        NavSection.WATCHLIST -> {
                            item { CleanCatalogRow("My Watchlist", watchlist, currentTheme.accent, onSelectContent) }
                        }
                        NavSection.SETTINGS -> {
                            item {
                                SettingsScreen(
                                    currentTheme = currentTheme,
                                    installedAddons = installedAddons,
                                    onSelectTheme = onSelectTheme,
                                    onManageAddons = { showAddonDialog = true }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = currentTheme.accent)
                }
            }
        }
    }

    // Stream Selection Details Dialog
    selectedItem?.let { item ->
        DetailsDialog(
            item = item,
            streams = availableStreams,
            isLoading = isStreamsLoading,
            isSaved = isItemInWatchlist,
            accentColor = currentTheme.accent,
            dialogBg = currentTheme.surface,
            onToggleWatchlist = { onToggleWatchlist(item) },
            onDismiss = onCloseDetails,
            onPlayInternal = { streamUrl -> onPlayInternal(streamUrl, item.title) },
            onPlayExternal = onPlayExternal
        )
    }

    // Addon Manager Dialog
    if (showAddonDialog) {
        AddonManagerDialog(
            addons = installedAddons,
            accentColor = currentTheme.accent,
            dialogBg = currentTheme.surface,
            onDismiss = { showAddonDialog = false },
            onAddUrl = { url ->
                onAddAddon(url)
                showAddonDialog = false
            },
            onRemove = { onRemoveAddon(it) }
        )
    }
}

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    installedAddons: List<InstalledAddon>,
    onSelectTheme: (AppTheme) -> Unit,
    onManageAddons: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Text("Appearance & Themes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppTheme.values().forEach { theme ->
                val isSelected = theme == currentTheme
                Surface(
                    color = theme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) theme.accent else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .clickable { onSelectTheme(theme) }
                        .padding(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(theme.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(if (isSelected) "Active" else "Select", color = if (isSelected) theme.accent else Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Add-ons & Catalogs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onManageAddons,
            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.surface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Manage Installed Add-ons (${installedAddons.size})", color = Color.White)
        }
    }
}

@Composable
fun OttSidebar(
    selectedSection: NavSection,
    accentColor: Color,
    sidebarBg: Color,
    onSelectSection: (NavSection) -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .fillMaxHeight()
            .background(sidebarBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎞", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            SidebarNavItem("🏠", selectedSection == NavSection.MOVIES, accentColor) { onSelectSection(NavSection.MOVIES) }
            Spacer(modifier = Modifier.height(18.dp))
            SidebarNavItem("🎬", selectedSection == NavSection.SERIES, accentColor) { onSelectSection(NavSection.SERIES) }
            Spacer(modifier = Modifier.height(18.dp))
            SidebarNavItem("🔖", selectedSection == NavSection.WATCHLIST, accentColor) { onSelectSection(NavSection.WATCHLIST) }
            Spacer(modifier = Modifier.height(18.dp))
            SidebarNavItem("⚙️", selectedSection == NavSection.SETTINGS, accentColor) { onSelectSection(NavSection.SETTINGS) }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SidebarNavItem(
    symbol: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else if (isFocused) Color(0x22FFFFFF) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 18.sp)
    }
}

@Composable
fun CleanCatalogRow(
    title: String,
    items: List<CatalogItem>,
    accentColor: Color,
    onSelect: (CatalogItem) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 28.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                CleanMovieCard(item, accentColor, onSelect = { onSelect(item) })
            }
        }
    }
}

@Composable
fun CleanMovieCard(
    item: CatalogItem,
    accentColor: Color,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.06f else 1.0f, label = "cardScale")

    Column(
        modifier = Modifier
            .width(135.dp)
            .scale(scale)
            .clickable { onSelect() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) accentColor else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun DetailsDialog(
    item: CatalogItem,
    streams: List<StreamLink>,
    isLoading: Boolean,
    isSaved: Boolean,
    accentColor: Color,
    dialogBg: Color,
    onToggleWatchlist: () -> Unit,
    onDismiss: () -> Unit,
    onPlayInternal: (String) -> Unit,
    onPlayExternal: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Button(
                    onClick = onToggleWatchlist,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSaved) Color(0xFFEF4444) else Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(if (isSaved) "Saved" else "+ Watchlist", fontSize = 11.sp, color = Color.White)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                Text(item.description ?: "No overview available.", color = Color.LightGray, fontSize = 12.sp, maxLines = 4)
                Spacer(modifier = Modifier.height(14.dp))
                Text("Select Stream Source (${streams.size}):", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                    }
                } else if (streams.isEmpty()) {
                    Text("No direct stream links found.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(streams) { link ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onPlayInternal(link.url) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(link.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Source: ${link.addonName}", color = Color.Gray, fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { onPlayExternal(link.url, item.title) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Ext ↗", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.Gray) }
        }
    )
}
@Composable
fun AddonManagerDialog(
    addons: List<InstalledAddon>,
    accentColor: Color,
    dialogBg: Color,
    onDismiss: () -> Unit,
    onAddUrl: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Installed Add-ons", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = dialogBg,
        shape = RoundedCornerShape(12.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("Addon Manifest URL") },
                    placeholder = { Text("https://.../manifest.json") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(addons) { addon ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(addon.name, color = Color.White, fontSize = 13.sp)
                                Text(addon.transportUrl, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                            }
                            if (addon.id != "cinemeta") {
                                TextButton(onClick = { onRemove(addon.transportUrl) }) {
                                    Text("Remove", color = Color.Red, fontSize = 11.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Install", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.LightGray) }
        }
    )
}
