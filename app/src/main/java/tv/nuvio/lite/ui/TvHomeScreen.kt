package tv.nuvio.lite.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tv.nuvio.lite.addon.CatalogItem
import tv.nuvio.lite.addon.InstalledAddon
import tv.nuvio.lite.addon.NavSection
import tv.nuvio.lite.addon.StreamLink

val DarkSidebarBg = Color(0xFF0F0E17)
val MainContentBg = Color(0xFF0A0910)
val AmberAccent = Color(0xFFF59E0B)

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
    isLowRam: Boolean,
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

    val featuredItem = remember(movies) { movies.firstOrNull() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MainContentBg)
    ) {
        OttSidebar(
            selectedSection = currentSection,
            onSelectSection = { section ->
                currentSection = section
                if (section == NavSection.ADDONS) {
                    showAddonDialog = true
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isBlank()) onClearSearch() else onSearch(it)
                        },
                        placeholder = { Text("Search title, actor or genre...", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x22000000),
                            unfocusedContainerColor = Color(0x11000000)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                            .height(48.dp)
                    )
                }

                if (searchQuery.isNotBlank()) {
                    item {
                        CatalogRow(
                            title = "Search Results",
                            items = searchResults,
                            isLowRam = isLowRam,
                            onSelect = onSelectContent
                        )
                    }
                } else {
                    if (featuredItem != null) {
                        item {
                            FeaturedHeroBanner(item = featuredItem, onPlay = { onSelectContent(featuredItem) })
                        }
                    }

                    when (currentSection) {
                        NavSection.MOVIES -> {
                            item { CatalogRow("Popular", movies, isLowRam, onSelectContent) }
                            item { CatalogRow("Top Rated", movies.reversed(), isLowRam, onSelectContent) }
                        }
                        NavSection.SERIES -> {
                            item { CatalogRow("Trending Series", series, isLowRam, onSelectContent) }
                        }
                        NavSection.WATCHLIST -> {
                            item { CatalogRow("My Watchlist", watchlist, isLowRam, onSelectContent) }
                        }
                        NavSection.ADDONS -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { showAddonDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                                    ) {
                                        Text("Manage Installed Add-ons (${installedAddons.size})", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
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
                    CircularProgressIndicator(color = AmberAccent)
                }
            }
        }
    }

    selectedItem?.let { item ->
        DetailsDialog(
            item = item,
            streams = availableStreams,
            isLoading = isStreamsLoading,
            isSaved = isItemInWatchlist,
            onToggleWatchlist = { onToggleWatchlist(item) },
            onDismiss = onCloseDetails,
            onPlayInternal = { streamUrl -> onPlayInternal(streamUrl, item.title) },
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

@Composable
fun OttSidebar(
    selectedSection: NavSection,
    onSelectSection: (NavSection) -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .fillMaxHeight()
            .background(DarkSidebarBg),
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
                    .background(Color(0xFF231F2D)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎞", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            SidebarNavItem("🏠", "Home", selectedSection == NavSection.MOVIES) { onSelectSection(NavSection.MOVIES) }
            Spacer(modifier = Modifier.height(18.dp))
            SidebarNavItem("🎬", "Series", selectedSection == NavSection.SERIES) { onSelectSection(NavSection.SERIES) }
            Spacer(modifier = Modifier.height(18.dp))
            SidebarNavItem("🔖", "Watchlist", selectedSection == NavSection.WATCHLIST) { onSelectSection(NavSection.WATCHLIST) }
            Spacer(modifier = Modifier.height(18.dp))
            SidebarNavItem("🧩", "Addons", selectedSection == NavSection.ADDONS) { onSelectSection(NavSection.ADDONS) }
        }

        Box(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .clickable { onSelectSection(NavSection.ADDONS) }
        ) {
            Text("⚙️", fontSize = 16.sp)
        }
    }
}

@Composable
fun SidebarNavItem(
    symbol: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0x33F59E0B) else if (isFocused) Color(0x22FFFFFF) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) AmberAccent else Color.Transparent,
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
fun FeaturedHeroBanner(
    item: CatalogItem,
    onPlay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 140.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MainContentBg, MainContentBg.copy(alpha = 0.95f), Color.Transparent),
                        startX = 0f,
                        endX = 900f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MainContentBg),
                        startY = 140f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(480.dp)
                .padding(start = 32.dp, top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FEATURED",
                color = AmberAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.title,
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Film  •  2026  •  ", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                Text("★ 7.9", color = AmberAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("  •  🍅 94%", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description ?: "Embark on an epic journey across mysterious lands.",
                color = Color(0xFFCCCCCC),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 3
            )
        }
    }
}

@Composable
fun CatalogRow(
    title: String,
    items: List<CatalogItem>,
    isLowRam: Boolean,
    onSelect: (CatalogItem) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 32.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                OttMovieCard(item, onSelect = { onSelect(item) })
            }
        }
    }
}

@Composable
fun OttMovieCard(
    item: CatalogItem,
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
                    color = if (isFocused) AmberAccent else Color.Transparent,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("★ 7.5", color = AmberAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("🍅 82%", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailsDialog(
    item: CatalogItem,
    streams: List<StreamLink>,
    isLoading: Boolean,
    isSaved: Boolean,
    onToggleWatchlist: () -> Unit,
    onDismiss: () -> Unit,
    onPlayInternal: (String) -> Unit,
    onPlayExternal: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161520),
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
                Text("Select Stream Source (${streams.size}):", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AmberAccent, modifier = Modifier.size(24.dp))
                    }
                } else if (streams.isEmpty()) {
                    Text("No direct stream links found.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(streams) { link ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF222030)),
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
    onDismiss: () -> Unit,
    onAddUrl: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Installed Add-ons", color = Color.White, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF161520),
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
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
            ) {
                Text("Install", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.LightGray) }
        }
    )
}
