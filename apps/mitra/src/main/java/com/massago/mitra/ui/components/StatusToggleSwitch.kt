package com.massago.mitra.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massago.mitra.data.model.DutyStatus
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.MassaGoMitraTheme
import com.massago.mitra.ui.theme.StatusBusyOrange
import com.massago.mitra.ui.theme.StatusOnlineGreen
import com.massago.mitra.ui.theme.TextMuted

@Composable
fun StatusToggleSwitch(
    currentStatus: DutyStatus,
    onStatusChange: (DutyStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = currentStatus == DutyStatus.ONLINE
    val isBusy = currentStatus == DutyStatus.ON_DUTY_BUSY

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isBusy -> StatusBusyOrange
            isOnline -> EmeraldPrimary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(350),
        label = "bgColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isOnline || isBusy -> Color.White
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(350),
        label = "textColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        shadowElevation = if (isOnline) 4.dp else 0.dp,
        border = if (!isOnline) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isBusy) return@clickable
                    val next = if (isOnline) DutyStatus.OFFLINE else DutyStatus.ONLINE
                    onStatusChange(next)
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isOnline || isBusy) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.ElectricBolt else Icons.Default.PowerSettingsNew,
                        contentDescription = "Status",
                        tint = if (isOnline || isBusy) Color.White else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = when {
                            isBusy -> "Sedang Bertugas"
                            isOnline -> "SIAP TERIMA PESANAN"
                            else -> "STATUS OFFLINE"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        letterSpacing = 0.5.sp,
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = when {
                            isBusy -> "Pesanan sedang berlangsung di lokasi klien"
                            isOnline -> "Radar aktif • Menunggu pesanan terdekat"
                            else -> "Ketuk tombol untuk mulai online"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline || isBusy) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }
            }

            // Status Indicator Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isOnline || isBusy) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = if (isOnline) "ON" else "OFF",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Status Toggle - Online (Light)")
@Composable
fun StatusToggleOnlinePreview() {
    MassaGoMitraTheme(darkTheme = false) {
        StatusToggleSwitch(
            currentStatus = DutyStatus.ONLINE,
            onStatusChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Status Toggle - Offline (Dark)")
@Composable
fun StatusToggleOfflinePreview() {
    MassaGoMitraTheme(darkTheme = true) {
        StatusToggleSwitch(
            currentStatus = DutyStatus.OFFLINE,
            onStatusChange = {}
        )
    }
}
