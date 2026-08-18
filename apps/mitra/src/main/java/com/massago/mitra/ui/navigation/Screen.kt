package com.massago.mitra.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Login : Screen("login", "Masuk Mitra")
    object OtpVerification : Screen("otp_verification?isForgot={isForgot}", "Verifikasi OTP")
    object Register : Screen("register", "Pendaftaran Terapis")
    object ResetPassword : Screen("reset_password", "Atur Ulang Password")
    
    object Home : Screen("home", "Beranda", Icons.Default.Home)
    object ActiveOrder : Screen("active_order", "Pesanan Aktif")
    object Chat : Screen("chat", "Pesan")
    object Wallet : Screen("wallet", "Dompet", Icons.Default.AccountBalanceWallet)
    object History : Screen("history", "Riwayat", Icons.Default.History)
    object Services : Screen("services", "Layanan", Icons.Default.Spa)
    object Profile : Screen("profile", "Profil", Icons.Default.Person)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Wallet,
    Screen.History,
    Screen.Services,
    Screen.Profile
)
