package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.MediaItemEntity
import com.example.domain.model.SearchResultItem
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentInfo
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.VideoLensBlueLight
import com.example.ui.theme.VideoLensCyanLight
import com.example.ui.theme.VideoLensPurpleLight

@Composable
fun MediaItemCard(
    media: MediaItemEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("media_card_${media.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container based on media type
            val (icon, tintBg, tintIcon) = getMediaVisuals(media.mediaType)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tintBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = media.mediaType,
                    tint = tintIcon,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatFileSize(media.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = media.topicTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Indexing status badge
            IndexingStatusBadge(status = media.indexingStatus)
        }
    }
}

@Composable
fun SearchResultCard(
    result: SearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("search_result_${result.segment.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (icon, tintBg, tintIcon) = getMediaVisuals(result.mediaItem.mediaType)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tintBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintIcon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.mediaItem.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.mediaItem.topicTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Similarity badge
                SimilarityScoreBadge(percent = result.similarityPercent)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Match snippet & concept
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Text(
                    text = result.matchSnippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            // Video moment or audio timestamp pill
            if (result.segment.timestampMs != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "پخش لحظه",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "پرش به لحظه: ${formatMs(result.segment.timestampMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (result.segment.pageNumber != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "صفحه ${result.segment.pageNumber} در سند",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SimilarityScoreBadge(percent: Int) {
    val (badgeBg, badgeText) = when {
        percent >= 80 -> Pair(AccentSuccess.copy(alpha = 0.2f), AccentSuccess)
        percent >= 60 -> Pair(AccentInfo.copy(alpha = 0.2f), AccentInfo)
        percent >= 40 -> Pair(AccentWarning.copy(alpha = 0.2f), AccentWarning)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(badgeBg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$percent٪ تطابق",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = badgeText
        )
    }
}

@Composable
fun IndexingStatusBadge(status: String) {
    val (icon, bg, fg, text) = when (status) {
        "COMPLETED" -> Quad(Icons.Default.CheckCircle, AccentSuccess.copy(alpha = 0.15f), AccentSuccess, "آماده")
        "EXTRACTING" -> Quad(Icons.Default.Sync, AccentInfo.copy(alpha = 0.15f), AccentInfo, "استخراج")
        "EMBEDDING" -> Quad(Icons.Default.Sync, VideoLensPurpleLight.copy(alpha = 0.2f), VideoLensPurpleLight, "بردارها")
        "PAUSED" -> Quad(Icons.Default.HourglassEmpty, AccentWarning.copy(alpha = 0.15f), AccentWarning, "متوقف")
        "FAILED" -> Quad(Icons.Default.Error, AccentError.copy(alpha = 0.15f), AccentError, "خطا")
        else -> Quad(Icons.Default.HourglassEmpty, Color.Gray.copy(alpha = 0.15f), Color.Gray, "در صف")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun getMediaVisuals(mediaType: String): Triple<ImageVector, Color, Color> {
    return when (mediaType) {
        "VIDEO" -> Triple(Icons.Default.Videocam, VideoLensBlueLight.copy(alpha = 0.2f), VideoLensBlueLight)
        "AUDIO" -> Triple(Icons.Default.Audiotrack, VideoLensCyanLight.copy(alpha = 0.2f), VideoLensCyanLight)
        "IMAGE" -> Triple(Icons.Default.Image, VideoLensPurpleLight.copy(alpha = 0.2f), VideoLensPurpleLight)
        else -> Triple(Icons.Default.Description, AccentWarning.copy(alpha = 0.2f), AccentWarning)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "۰ مگابایت"
    val mb = bytes / (1024 * 1024)
    return if (mb > 0) "$mb مگابایت" else "${bytes / 1024} کیلوبایت"
}

fun formatMs(ms: Long): String {
    val sec = ms / 1000
    val min = sec / 60
    val remSec = sec % 60
    return "%02d:%02d".format(min, remSec)
}
