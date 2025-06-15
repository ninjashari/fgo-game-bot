package com.fgobot.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TeamConfigCard(
    name: String,
    servants: List<String>,
    questType: String?,
    isActive: Boolean,
    onPlayClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp,
        backgroundColor = if (isActive) Color(0xFFE3F2FD) else Color.White
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Row {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Use Team",
                            tint = Color.Green
                        )
                    }
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Edit Team",
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Servants: ${servants.joinToString(", ")}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                questType?.let {
                    Text(
                        text = "Quest Type: $it",
                        fontSize = 12.sp,
                        color = Color.Blue
                    )
                }
                
                if (isActive) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 12.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TeamConfigCardPreview() {
    Column {
        TeamConfigCard(
            name = "Farming Team",
            servants = listOf("Artoria", "Waver", "Merlin"),
            questType = "Daily Quests",
            isActive = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TeamConfigCard(
            name = "Boss Team",
            servants = listOf("Cu Alter", "Tamamo", "Mash"),
            questType = "Story Quests",
            isActive = false
        )
    }
} 