package team.codialo.wifix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.codialo.wifix.logic.LoginLogic

@Composable
fun LogViewerModal(
    loginLogic: LoginLogic,
    onDismiss: () -> Unit
) {
    val logs = loginLogic.getLogs()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clickable { },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WiFix Debug Logs",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF0D1117),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    items(logs) { log ->
                        LogEntry(log)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntry(log: LoginLogic.LogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when (log.level) {
                    "ERROR" -> Color.Red.copy(alpha = 0.1f)
                    "SUCCESS" -> Color.Green.copy(alpha = 0.1f)
                    "INFO" -> Color.Blue.copy(alpha = 0.1f)
                    "WARNING" -> Color.Yellow.copy(alpha = 0.1f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Row {
            Text(
                text = "[${log.timestamp}] ",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = log.level,
                color = when (log.level) {
                    "ERROR" -> Color.Red
                    "SUCCESS" -> Color.Green
                    "INFO" -> Color.Cyan
                    "WARNING" -> Color.Yellow
                    else -> Color.White
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        
        Text(
            text = log.message,
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        if (log.details.isNotEmpty()) {
            Text(
                text = log.details,
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

