package tv.nuvio.lite.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import tv.nuvio.lite.addon.NavSection
import tv.nuvio.lite.addon.StreamLink

// Glass Theme Constants
val GlassSurface = Color(0x24FFFFFF)
val GlassSurfaceElevated = Color(0x38FFFFFF)
val GlassBorder = Color(0x40FFFFFF)
val GlassBorderActive = Color(0xFF60A5FA)
val BackgroundDeep = Color(0xFF090B10)

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
    onPlayInternal: (String) -> Unit,
    onPlayExternal: (String, String) -> Unit
) {
    var currentSection by remember { mutableStateOf(NavSection.MOVIES) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddonDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // --- 1. GLASS SIDEBAR ---
        GlassSidebar(
            selectedSection = currentSection,
            isLowRam = isLowRam,
            onSelectSection = {
                currentSection = it
                if (it == NavSection.ADDONS) {
                    showAddonDialog = true
                }
            }
        )

        // --- 2. MAIN CONTENT AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Frosted Glass Search Bar Header
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        if (it.isBlank()) onClearSearch() else onSearch(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Smooth Section Switching Animations
                AnimatedContent(
                    targetState = Pair(currentSection, searchQuery.isNotBlank()),
                    transitionSpec = {
                        fadeIn(spring(stiffness = Spring.StiffnessLow)) + slideInVertically { it / 8 } togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessLow))
                    },
                    label = "sectionTransition"
                ) { (section, isSearching) ->
                    if (isSearching) {
                        CatalogGrid(
                            title = "Search Results",
                            items = searchResults,
                            isLowRam = isLowRam,
                            onSelect = onSelectContent
                        )
                    } else {
                        when (section) {
                            NavSection.MOVIES -> CatalogGrid("Featured Movies", movies, isLowRam, onSelectContent)
                            NavSection.SERIES -> CatalogGrid("Trending Series", series, isLowRam, onSelectContent)
                            NavSection.WATCHLIST -> CatalogGrid("My Watchlist", watchlist, isLowRam, onSelectContent)
                            NavSection.ADDONS -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { showAddonDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = GlassBorderActive)
                                    ) {
                                        Text("Open Add-ons Manager (${installedAddons.size})")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Modal with Watchlist Toggle & Player Option
    selectedItem?.let { item ->
        DetailsGlassDialog(
            item = item,
            streams = availableStreams,
            isLoading = isStreamsLoading,
            isSaved = isItemInWatchlist,
            onToggleWatchlist = { onToggleWatchlist(item) },
            onDismiss = onCloseDetails,
            onPlayInternal = onPlayInternal,
            onPlayExternal = onPlayExternal
        )
    }

    // Addons Manager Modal
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

// --- GLASS SIDEBAR COMPONENT ---
@Composable
fun GlassSidebar(
    selectedSection: NavSection,
    isLowRam: Boolean,
    onSelectSection: (NavSection) -> Unit
) {
    val sidebarBg = if (isLowRam) Color(0xFF10131A) else Color(0x1AFFFFFF)

    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(sidebarBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
        horizontalAlignment = Alignment.CenterVertically,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))),
            contentAlignment = Alignment.Center
        ) {
            Text("N", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }

        // 4 Navigation Items
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterVertically
        ) {
            SidebarIcon(
                label = "MOV",
                name = "Movies",
                isSelected = selectedSection == NavSection.MOVIES,
                onClick = { onSelectSection(NavSection.MOVIES) }
            )
            SidebarIcon(
                label = "SER",
                name = "Series",
                isSelected = selectedSection == NavSection.SERIES,
                onClick = { onSelectSection(NavSection.SERIES) }
            )
            SidebarIcon(
                label = "WCH",
                name = "Watchlist",
                isSelected = selectedSection == NavSection.WATCHLIST,
                onClick = { onSelectSection(NavSection.WATCHLIST) }
            )
            SidebarIcon(
                label = "ADD",
                name = "Addons",
                isSelected = selectedSection == NavSection.ADDONS,
                onClick = { onSelectSection(NavSection.ADDONS) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SidebarIcon(
    label: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused || isSelected) 1.15f else 1.0f,
        label = "sidebarScale"
    )

    val background = when {
        isSelected -> Color(0xFF3B82F6)
        isFocused -> GlassSurfaceElevated
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (isSelected || isFocused) Color.White else Color.Gray
        )
        Text(
            text = name,
            fontSize = 8.sp,
            color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.LightGray.copy(alpha = 0.6f)
        )
    }
}

// --- GLASS SEARCH BAR ---
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search movies, TV shows, anime...", color = Color.Gray, fontSize = 14.sp) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = GlassSurfaceElevated,
            unfocusedContainerColor = GlassSurface,
            focusedBorderColor = GlassBorderActive,
            unfocusedBorderColor = GlassBorder
        ),
        modifier = modifier.height(52.dp)
    )
}

// --- CATALOG GRID ---
@Composable
fun CatalogGrid(
    title: String,
    items: List<CatalogItem>,
    isLowRam: Boolean,
    onSelect: (CatalogItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No content found", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(115.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    GlassCard(item = item, isLowRam = isLowRam, onSelect = { onSelect(item) })
                }
            }
        }
    }
}

// --- GLASS CARD ---
@Composable
fun GlassCard(
    item: CatalogItem,
    isLowRam: Boolean,
    onSelect: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        label = "cardScale"
    )

    val cardBg = if (isLowRam) Color(0xFF161A22) else GlassSurface
    val borderBrush = if (isFocused) {
        Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB)))
    } else {
        Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Glass gradient caption
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 90f
                    )
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// --- GLASS DETAILS & PLAYER SELECTION DIALOG ---
@Composable
fun DetailsGlassDialog(
    item: CatalogItem,
    streams: List<StreamLink>,
    isLoading: Boolean,
    isSaved: Boolean,
    onToggleWatchlist: () -> Unit,
    onDismiss: () -> Unit,
    onPlayInternal: (String) -> Unit,
    onPlayExternal: (String, String) -> Unit
) {
    var selectedStreamForAction by remember { mutableStateOf<StreamLink?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0131720),
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onToggleWatchlist,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) Color(0xFFEF4444) else Color(0x33FFFFFF)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(if (isSaved) "✓ Saved" else "+ Watchlist", fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(80.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.description ?: "No description provided.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 5,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Playable Streams (${streams.size})",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(28.dp))
                    }
                } else if (streams.isEmpty()) {
                    Text("No playable streams available.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(streams) { link ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x24FFFFFF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedStreamForAction = link }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(link.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(link.addonName, color = Color(0xFF60A5FA), fontSize = 10.sp)
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

    // Player Selection Chooser
    selectedStreamForAction?.let { stream ->
        AlertDialog(
            onDismissRequest = { selectedStreamForAction = null },
            containerColor = Color(0xF21E2430),
            shape = RoundedCornerShape(16.dp),
            title = { Text("Play Stream", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stream.title, color = Color.LightGray, fontSize = 12.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val url = stream.url
                            selectedStreamForAction = null
                            onPlayInt
