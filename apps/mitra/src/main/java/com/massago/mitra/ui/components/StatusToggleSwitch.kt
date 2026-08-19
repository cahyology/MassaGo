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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massago.mitra.data.model.DutyStatus
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.MassaGoMitraTheme
import com.massago.mitra.ui.theme.StatusBusyOrange
import com.massago.mitra.ui.theme.StatusOnlineGreen
import com.massago.mitra.ui.theme.TextMuted

@Composable
fun StatusToggleSwitch(
    currentStatus: DutyStatus,
    onStatusChange: (DutyStatus) -> Unit,
    autoAcceptOrders: Boolean = false,
    onAutoAcceptChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOnline = currentStatus == DutyStatus.ONLINE
    val isBusy = currentStatus == DutyStatus.ON_DUTY_BUSY

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Section: Online / Offline Duty Switch (Tap to Toggle)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isBusy) return@clickable
                        val next = if (isOnline) DutyStatus.OFFLINE else DutyStatus.ONLINE
                        onStatusChange(next)
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isBusy -> StatusBusyOrange
                                isOnline -> StatusOnlineGreen
                                else -> Color(0xFF94A3B8)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = when {
                            isBusy -> "Bertugas"
                            isOnline -> "ONLINE"
                            else -> "OFFLINE"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isBusy -> StatusBusyOrange
                            isOnline -> EmeraldPrimary
                            else -> Color(0xFF64748B)
                        },
                        fontSize = 12.sp
                    )
                    Text(
                        text = when {
                            isBusy -> "Sedang melayani"
                            isOnline -> "Siap terima order"
                            else -> "Ketuk untuk aktif"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.5.sp
                    )
                }
            }

            // Center Divider
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // Right Section: Compact Auto-Accept Switch
            Row(
                modifier = Modifier
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = if (autoAcceptOrders) EmeraldPrimary else Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Auto-Accept",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (autoAcceptOrders) EmeraldDark else Color(0xFF475569)
                        )
                    }
                    Text(
                        text = if (autoAcceptOrders) "Otomatis" else "Manual",
                        fontSize = 9.sp,
                        color = if (autoAcceptOrders) EmeraldPrimary else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = autoAcceptOrders,
                    onCheckedChange = { isChecked ->
                        onAutoAcceptChange?.invoke(isChecked)
                    },
                    modifier = Modifier.scale(0.8f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    )
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
