package tv.nuvio.lite.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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

@Composable
fun TvHomeScreen(
    movies: List<CatalogItem>,
    series: List<CatalogItem>,
    isLoading: Boolean,
    isLowRam: Boolean,
    onItemSelected: (CatalogItem) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1115)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF3B82F6))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D12)),
        contentPadding = PaddingValues(top = 32.dp, bottom = 48.dp)
    ) {
        item {
            Text(
                text = "NUVIO LITE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
            )
        }

        if (movies.isNotEmpty()) {
            item {
                CatalogSection("Popular Movies", movies, isLowRam, onItemSelected)
            }
        }

        if (series.isNotEmpty()) {
            item {
                CatalogSection("Trending Series", series, isLowRam, onItemSelected)
            }
        }
    }
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
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, label = "cardScale")
    val cardBg = if (isLowRam) Color(0xFF161A22) else Color(0x33FFFFFF)

    Box(
        modifier = Modifier
            .width(130.dp)
            .height(195.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(
                width = if (isFocused) 2.5.dp else 0.5.dp,
                color = if (isFocused) Color.White else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
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
                        startY = 140f
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
