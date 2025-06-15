package com.fgobot.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServantCard(
    name: String,
    className: String,
    rarity: Int,
    level: Int,
    owned: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp,
        backgroundColor = if (owned) Color(0xFFE8F5E8) else Color.White
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
                RarityStars(rarity = rarity)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = className,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                if (owned) {
                    Text(
                        text = "Lv. $level",
                        fontSize = 14.sp,
                        color = Color.Blue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (!owned) {
                Text(
                    text = "Not Owned",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun RarityStars(rarity: Int) {
    Row {
        repeat(rarity) {
            Text(
                text = "★",
                color = Color(0xFFFFD700),
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServantCardPreview() {
    Column {
        ServantCard(
            name = "Artoria Pendragon",
            className = "Saber",
            rarity = 5,
            level = 90,
            owned = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        ServantCard(
            name = "Gilgamesh",
            className = "Archer",
            rarity = 5,
            level = 1,
            owned = false
        )
    }
} 