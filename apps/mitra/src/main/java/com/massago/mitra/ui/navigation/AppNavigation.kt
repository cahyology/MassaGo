package com.massago.mitra.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.massago.mitra.data.repository.AuthRepository
import com.massago.mitra.ui.screens.activeorder.ActiveOrderScreen
import com.massago.mitra.ui.screens.auth.LoginScreen
import com.massago.mitra.ui.screens.auth.MitraForgotPasswordScreen
import com.massago.mitra.ui.screens.auth.OtpVerificationScreen
import com.massago.mitra.ui.screens.auth.RegisterMitraScreen
import com.massago.mitra.ui.screens.chat.ChatScreen
import com.massago.mitra.ui.screens.history.OrderHistoryScreen
import com.massago.mitra.ui.screens.home.HomeScreen
import com.massago.mitra.ui.screens.profile.ProfileScreen
import com.massago.mitra.ui.screens.profile.SafetyCenterDialog
import com.massago.mitra.ui.screens.services.ServicesManagementScreen
import com.massago.mitra.ui.screens.splash.MitraSplashScreen
import com.massago.mitra.ui.screens.wallet.WalletScreen
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.TextSecondary

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authRepo = AuthRepository.instance
    val isLoggedIn by authRepo.isLoggedIn.collectAsState()
    val tempPhone by authRepo.tempPhoneNumber.collectAsState()

    var showSafetyDialog by remember { mutableStateOf(false) }

    val shouldShowBottomBar = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomBar) {
                Surface(
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    screen.icon?.let { icon ->
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Splash Route
            composable(Screen.Splash.route) {
                MitraSplashScreen(
                    onSplashFinished = {
                        val destination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Auth Routes
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgotPassword = { phone ->
                        authRepo.setTempPhone(phone)
                        navController.navigate("mitra_forgot_password")
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable("mitra_forgot_password") {
                MitraForgotPasswordScreen(
                    initialPhone = tempPhone,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOtpSent = { phone ->
                        authRepo.setTempPhone(phone)
                        navController.navigate("otp_verification?isForgot=true")
                    }
                )
            }

            composable(
                route = "otp_verification?isForgot={isForgot}",
                arguments = listOf(
                    androidx.navigation.navArgument("isForgot") {
                        type = androidx.navigation.NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val isForgot = backStackEntry.arguments?.getBoolean("isForgot") ?: false
                OtpVerificationScreen(
                    phoneNumber = tempPhone.ifEmpty { "081234567890" },
                    isForgotPassword = isForgot,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToResetPassword = {
                        navController.navigate(Screen.ResetPassword.route)
                    },
                    onVerificationSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterMitraScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onRegistrationSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ResetPassword.route) {
                com.massago.mitra.ui.screens.auth.MitraResetPasswordScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onResetSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Core Main Routes
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToActiveOrder = {
                        navController.navigate(Screen.ActiveOrder.route)
                    },
                    onNavigateToWallet = {
                        navController.navigate(Screen.Wallet.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onSosClick = {
                        showSafetyDialog = true
                    }
                )
            }

            composable(Screen.ActiveOrder.route) {
                ActiveOrderScreen(
                    onNavigateToChat = {
                        navController.navigate(Screen.Chat.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack(Screen.Home.route, false)
                    },
                    onSosClick = {
                        showSafetyDialog = true
                    }
                )
            }

            composable(Screen.Chat.route) {
                ChatScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Wallet.route) {
                WalletScreen()
            }

            composable(Screen.History.route) {
                OrderHistoryScreen()
            }

            composable(Screen.Services.route) {
                ServicesManagementScreen()
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSosClick = {
                        showSafetyDialog = true
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    if (showSafetyDialog) {
        SafetyCenterDialog(
            onDismiss = { showSafetyDialog = false }
        )
    }
}
