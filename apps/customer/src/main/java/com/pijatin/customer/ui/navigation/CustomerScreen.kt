package com.pijatin.customer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class CustomerScreen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Splash : CustomerScreen("splash", "Splash")
    object Login : CustomerScreen("login", "Masuk")
    object OtpVerification : CustomerScreen("otp_verification?isForgot={isForgot}", "Verifikasi OTP") {
        fun createRoute(isForgot: Boolean = false) = "otp_verification?isForgot=$isForgot"
    }
    object Register : CustomerScreen("register", "Lengkapi Profil")
    object ResetPassword : CustomerScreen("reset_password", "Atur Ulang Password")

    object Home : CustomerScreen("home", "Beranda", Icons.Default.Home)
    object Detail : CustomerScreen("detail/{serviceId}") {
        fun createRoute(serviceId: String) = "detail/$serviceId"
    }
    object Checkout : CustomerScreen("checkout/{serviceId}/{duration}/{aromaId}/{focusAreas}/{pressure}/{gender}") {
        fun createRoute(
            serviceId: String,
            duration: Int,
            aromaId: String,
            focusAreas: String,
            pressure: String,
            gender: String
        ) = "checkout/$serviceId/$duration/$aromaId/$focusAreas/$pressure/$gender"
    }
    object Tracking : CustomerScreen("tracking", "Tracking")
    object Chat : CustomerScreen("chat", "Chat")
    object History : CustomerScreen("history", "Aktivitas", Icons.Default.History)
    object Wallet : CustomerScreen("wallet", "Dompet", Icons.Default.AccountBalanceWallet)
    object Profile : CustomerScreen("profile", "Profil", Icons.Default.Person)
}
