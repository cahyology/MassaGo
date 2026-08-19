package com.massago.mitra.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massago.mitra.data.model.Order
import com.massago.mitra.data.repository.OrderRepository
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.TextMuted
import com.massago.mitra.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    orderRepository: OrderRepository = OrderRepository.instance
) {
    val history by orderRepository.orderHistory.collectAsState()
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    val currencyFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    LaunchedEffect(Unit) {
        orderRepository.fetchOrderHistoryFromSupabase()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Riwayat Pesanan Pijat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat pesanan.",
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(history) { order ->
                    OrderHistoryCard(
                        order = order,
                        currencyFormat = currencyFormat,
                        onClick = { selectedOrderForDetail = order }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    selectedOrderForDetail?.let { order ->
        OrderDetailDialog(
            order = order,
            onDismiss = { selectedOrderForDetail = null }
        )
    }
}

@Composable
private fun OrderHistoryCard(
    order: Order,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
    val formattedDate = sdf.format(Date(order.createdAtMillis))
    val netEarning = "Rp " + currencyFormat.format(order.therapistNetEarnings + order.tipAmount)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EmeraldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = order.servicePackage.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = netEarning,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${order.client.name} (${order.servicePackage.durationMinutes} mnt)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = order.client.address.take(28) + if (order.client.address.length > 28) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
