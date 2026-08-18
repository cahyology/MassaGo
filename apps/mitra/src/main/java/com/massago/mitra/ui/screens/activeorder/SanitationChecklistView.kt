package com.massago.mitra.ui.screens.activeorder

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
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.SingleBed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massago.mitra.data.model.ChecklistItemType
import com.massago.mitra.data.model.Order
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.TextSecondary

@Composable
fun SanitationChecklistView(
    order: Order,
    onUpdateChecklist: (ChecklistItemType, Boolean) -> Unit,
    onStartTreatment: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Health & Safety Icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "SOP Higienitas & Persiapan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Wajib diselesaikan demi kenyamanan & keamanan klien",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Checklist Items
            ChecklistRow(
                icon = Icons.Default.CleanHands,
                title = "Sanitasi & Cuci Tangan",
                description = "Cuci tangan dengan sabun steril / gunakan antiseptic gel sebelum menyentuh klien.",
                isChecked = order.isHandsSanitized,
                onCheckedChange = { onUpdateChecklist(ChecklistItemType.HANDS_SANITIZED, it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ChecklistRow(
                icon = Icons.Default.SingleBed,
                title = "Ganti Alas Matras Bersih",
                description = "Pasang seprai disposable / kain penutup bersih baru di tempat terapi klien.",
                isChecked = order.isMatCoverReplaced,
                onCheckedChange = { onUpdateChecklist(ChecklistItemType.MAT_COVER_REPLACED, it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ChecklistRow(
                icon = Icons.Default.Opacity,
                title = "Konfirmasi Pilihan Minyak Pijat",
                description = "Tawarkan aroma minyak aromaterapi (Zaitun, Lavender, Rempah Hangat) ke klien.",
                isChecked = order.isOilAromaConfirmed,
                onCheckedChange = { onUpdateChecklist(ChecklistItemType.OIL_AROMA_CONFIRMED, it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ChecklistRow(
                icon = Icons.Default.SettingsSuggest,
                title = "Cek Tekanan & Titik Pegal",
                description = "Tanyakan preferensi tekanan (Sedang/Kuat) dan bagian tubuh yang ingin difokuskan.",
                isChecked = order.isPressurePreferenceChecked,
                onCheckedChange = { onUpdateChecklist(ChecklistItemType.PRESSURE_CHECKED, it) }
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Start Button
            Button(
                onClick = onStartTreatment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary
                ),
                enabled = order.isPrepComplete
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (order.isPrepComplete) "Mulai Sesi Pemijatan" else "Lengkapi Checklist SOP (4/4)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    icon: ImageVector,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!isChecked) },
        shape = RoundedCornerShape(14.dp),
        color = if (isChecked) EmeraldLight.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isChecked) EmeraldPrimary.copy(alpha = 0.4f) else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = EmeraldPrimary,
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
