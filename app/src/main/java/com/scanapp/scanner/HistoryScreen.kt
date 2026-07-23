package com.scanapp.scanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.scanapp.scanner.data.ScanHistoryEntity
import com.scanapp.scanner.data.ScanSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    items: List<ScanHistoryEntity>,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFFF7F4EF),
            darkIcons = true
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F4EF))
            .padding(WindowInsets.statusBars.asPaddingValues()),
    ) {
        HistoryTopBar(onBack = onBack)
        if (items.isEmpty()) {
            EmptyHistory(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 18.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    HistoryItem(
                        item = item,
                        onCopy = { onCopy(item.content) },
                        onOpenBrowser = { onOpenBrowser(item.content) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFC4612F).copy(alpha = 0.1f))
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFC4612F).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back_hand_drawn),
                contentDescription = "返回",
                modifier = Modifier.size(24.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF1F2421)),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "扫描历史",
            color = Color(0xFF1F2421),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📋", color = Color(0xFF5C635D), fontSize = 52.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有扫描记录",
                color = Color(0xFF1F2421),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "扫描二维码或从相册识别后会显示在这里",
                color = Color(0xFF5C635D),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun HistoryItem(
    item: ScanHistoryEntity,
    onCopy: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    val canOpen = item.content.normalizedWebUrl() != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Hand-drawn style badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (item.source == ScanSource.CAMERA) 
                                Color(0xFFF2E3D6) 
                            else 
                                Color(0xFFE8F5E9)
                        )
                        .border(
                            width = 1.dp,
                            color = if (item.source == ScanSource.CAMERA) 
                                Color(0xFFC4612F).copy(alpha = 0.3f) 
                            else 
                                Color(0xFF4CAF50).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.source.displayName(),
                        color = if (item.source == ScanSource.CAMERA) 
                            Color(0xFFC4612F) 
                        else 
                            Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = item.scannedAt.displayTime(),
                    color = Color(0xFF5C635D),
                    fontSize = 12.sp,
                )
            }
            Text(
                text = item.content,
                color = Color(0xFF1F2421),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(Color(0xFFFBF9F5), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE7E1D7), RoundedCornerShape(14.dp))
                    .padding(13.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCopy) {
                    Text(text = "复制", color = Color(0xFF1F2421), fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenBrowser,
                    enabled = canOpen,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE7E1D7),
                        disabledContentColor = Color(0xFF5C635D),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Text(text = "访问", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun ScanSource.displayName(): String = when (this) {
    ScanSource.CAMERA -> "相机扫描"
    ScanSource.GALLERY -> "相册识别"
}

private fun Long.displayTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(this))
}
