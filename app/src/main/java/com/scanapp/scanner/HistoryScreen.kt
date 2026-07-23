package com.scanapp.scanner

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
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            color = Color(0xFFF3F6FA),
            darkIcons = true
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FA))
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
        IconButton(onClick = onBack) {
            Text(
                text = "←",
                color = Color(0xFF151820),
                fontSize = 31.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = "扫描历史",
            color = Color(0xFF151820),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "▣", color = Color(0xFF9BA5B7), fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "还没有扫描记录",
                color = Color(0xFF3F4658),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "扫描二维码或从相册识别后会显示在这里",
                color = Color(0xFF8A93A6),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.source.displayName(),
                    color = Color(0xFF2F7DFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.scannedAt.displayTime(),
                    color = Color(0xFF8A93A6),
                    fontSize = 12.sp,
                )
            }
            Text(
                text = item.content,
                color = Color(0xFF303746),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(Color(0xFFF2F5FA), RoundedCornerShape(14.dp))
                    .padding(13.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCopy) {
                    Text(text = "复制", color = Color(0xFF2F7DFF))
                }
                Button(
                    onClick = onOpenBrowser,
                    enabled = canOpen,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A67E),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFD5DBE6),
                        disabledContentColor = Color(0xFF8A93A6),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(text = "访问")
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
