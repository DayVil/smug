package com.github.smugapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow



@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF6200),
        Color(0xFF4CAF50),
        Color(0xFFFFEB3B),
        Color(0xFF2196F3),
        Color(0xFFE91E63), // Red
        Color(0xFF009688), // Green
        Color(0xFF4363D8), // Blue
        Color(0xFFF58231), // Orange
        Color(0xFF911EB4), // Purple
        Color(0xFF46F0F0), // Cyan
        Color(0xFFF032E6), // Magenta
        Color(0xFFBCF60C), // Lime
        Color(0xFFFABEBE), // Pink
        Color(0xFFAAFFAF), // Mint
        Color(0xFF9A6324), // Brown
        Color(0xFFFFFFA7), // Pale Yellow
        Color(0xFF800000), // Maroon
        Color(0xFFAAC8A0), // Olive
        Color(0xFF808080), // Grey
        Color(0xFF000000)  // Black (use sparingly for charts if possible)
    )
) {
    if (data.isEmpty()) return

    val total = data.values.sum()
    val angles = data.values.map { (it / total * 360).toFloat() }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2
            var startAngle = 0f

            angles.forEachIndexed { index, angle ->
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = angle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += angle
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Column(modifier = Modifier.padding(start = 16.dp)) {
            data.entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                colors[index % colors.size],
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key}: ${entry.value.toInt()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// --- MODIFIED: Added yAxisUnit parameter and Y-axis labels ---
@Composable
fun StackedBarChart(
    data: Map<String, Map<String, Double>>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF6200),
        Color(0xFF4CAF50),
        Color(0xFFFFEB3B),
        Color(0xFF2196F3),
        Color(0xFFE91E63), // Red
        Color(0xFF009688), // Green
        Color(0xFF4363D8), // Blue
        Color(0xFFF58231), // Orange
        Color(0xFF911EB4), // Purple
        Color(0xFF46F0F0), // Cyan
        Color(0xFFF032E6), // Magenta
        Color(0xFFBCF60C), // Lime
        Color(0xFFFABEBE), // Pink
        Color(0xFFAAFFAF), // Mint
        Color(0xFF9A6324), // Brown
        Color(0xFFFFFFA7), // Pale Yellow
        Color(0xFF800000), // Maroon
        Color(0xFFAAC8A0), // Olive
        Color(0xFF808080), // Grey
        Color(0xFF000000)  // Black (use sparingly for charts if possible)
    ),
    yAxisUnit: String = "" // New parameter for units like "ml" or "kcal"
) {
    if (data.isEmpty()) return

    val allTypes = data.values.flatMap { it.keys }.distinct()
    val maxValue = data.values.maxOfOrNull { it.values.sum() } ?: 1.0
    val yAxisLabelsCount = 4

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                (yAxisLabelsCount downTo 0).forEach { i ->
                    val value = maxValue / yAxisLabelsCount * i
                    Text(
                        text = "${value.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chart Bars
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.entries.forEach { (day, dayData) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val totalHeight = 180.dp // Adjusted height to align with axis
                        val total = dayData.values.sum()

                        if (total > 0) {
                            Column(
                                modifier = Modifier
                                    .width(35.dp)
                                    .height(totalHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                allTypes.forEach { type ->
                                    val value = dayData[type] ?: 0.0
                                    if (value > 0) {
                                        val height = (value / maxValue * totalHeight.value).dp
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(height)
                                                .background(colors[allTypes.indexOf(type) % colors.size])
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Legend
        // Legend (multi-row, always shows all types with correct colors)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            allTypes.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowItems.forEach { type ->
                        val colorIndex = allTypes.indexOf(type) % colors.size
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(colors[colorIndex])
                                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
// --- End of modification ---

@Composable
fun SimpleBarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return

    val maxValue = data.values.maxOrNull() ?: 1.0
    val yAxisLabelsCount = 4

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            (yAxisLabelsCount downTo 0).forEach { i ->
                val value = maxValue / yAxisLabelsCount * i
                Text(
                    text = value.toInt().toString(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Bars
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.entries.forEach { (day, value) ->
                Column(
                    modifier = Modifier
                        .wrapContentWidth()
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val height = (value / maxValue * 160).dp
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(height)
                            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = day,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


