package com.pijatin.mitra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pijatin.mitra.data.model.DutyStatus
import com.pijatin.mitra.data.model.TherapistProfile
import com.pijatin.mitra.ui.theme.AmberGold
import com.pijatin.mitra.ui.theme.EmeraldDark
import com.pijatin.mitra.ui.theme.EmeraldLight
import com.pijatin.mitra.ui.theme.EmeraldPrimary
import com.pijatin.mitra.ui.theme.PijatInMitraTheme
import com.pijatin.mitra.ui.theme.StatusAlertRed
import com.pijatin.mitra.ui.theme.StatusBusyOrange
import com.pijatin.mitra.ui.theme.StatusOnlineGreen
import com.pijatin.mitra.ui.theme.TextMuted
import com.pijatin.mitra.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun TopHeaderBar(
    profile: TherapistProfile,
    onSosClick: () -> Unit,
    onWalletClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
    val formattedBalance = "Rp " + currencyFormat.format(profile.mainBalance)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Therapist Avatar & Info (Gojek Driver Profile Chip)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onProfileClick() }
                    .padding(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(EmeraldPrimary, EmeraldDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }

                    // Online indicator dot with white border
                    val statusColor = when (profile.dutyStatus) {
                        DutyStatus.ONLINE -> StatusOnlineGreen
                        DutyStatus.ON_DUTY_BUSY -> StatusBusyOrange
                        DutyStatus.OFFLINE -> TextMuted
                    }
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = profile.name.split(" ").take(2).joinToString(" "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = AmberGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${profile.rating}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• ${profile.tier.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            // Right: Wallet pill & SOS button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wallet Quick Capsule
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onWalletClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Dompet",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedBalance,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.5.sp
                        )
                    }
                }

                // SOS Safety Button
                IconButton(
                    onClick = onSosClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StatusAlertRed.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Pusat Keamanan & SOS",
                        tint = StatusAlertRed,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Top Header Bar - Light")
@Composable
fun TopHeaderBarPreview() {
    PijatInMitraTheme(darkTheme = false) {
        TopHeaderBar(
            profile = TherapistProfile(),
            onSosClick = {},
            onWalletClick = {},
            onProfileClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Top Header Bar - Dark")
@Composable
fun TopHeaderBarDarkPreview() {
    PijatInMitraTheme(darkTheme = true) {
        TopHeaderBar(
            profile = TherapistProfile(),
            onSosClick = {},
            onWalletClick = {},
            onProfileClick = {}
        )
    }
}
