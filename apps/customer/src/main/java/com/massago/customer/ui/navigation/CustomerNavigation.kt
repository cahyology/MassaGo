package com.massago.customer.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.massago.customer.data.repository.CustomerAuthRepository
import com.massago.customer.ui.screens.auth.CustomerForgotPasswordScreen
import com.massago.customer.ui.screens.auth.CustomerLoginScreen
import com.massago.customer.ui.screens.auth.CustomerOtpScreen
import com.massago.customer.ui.screens.auth.CustomerRegisterScreen
import com.massago.customer.ui.screens.chat.CustomerChatScreen
import com.massago.customer.ui.screens.checkout.CheckoutScreen
import com.massago.customer.ui.screens.detail.ServiceDetailScreen
import com.massago.customer.ui.screens.history.CustomerHistoryScreen
import com.massago.customer.ui.screens.home.CustomerHomeScreen
import com.massago.customer.ui.screens.profile.CustomerProfileScreen
import com.massago.customer.ui.screens.tracking.OrderTrackingScreen
import com.massago.customer.ui.screens.wallet.CustomerWalletScreen
import com.massago.customer.ui.screens.splash.CustomerSplashScreen
import com.massago.customer.ui.theme.EmeraldDark
import com.massago.customer.ui.theme.EmeraldLight
import com.massago.customer.ui.theme.EmeraldPrimary
import com.massago.customer.ui.theme.TextMuted

@Composable
fun CustomerNavigation(
    navController: NavHostController = rememberNavController()
) {
    val authRepo = CustomerAuthRepository.instance
    val isLoggedIn by authRepo.isLoggedIn.collectAsState()
    val tempPhone by authRepo.tempPhoneNumber.collectAsState()
    val sessionTerminatedMsg by authRepo.sessionTerminatedMessage.collectAsState()

    if (sessionTerminatedMsg != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                authRepo.clearSessionTerminatedMessage()
                navController.navigate(CustomerScreen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                    contentDescription = "Sesi Berakhir",
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Sesi Akun Berakhir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = sessionTerminatedMsg ?: "",
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        authRepo.clearSessionTerminatedMessage()
                        navController.navigate(CustomerScreen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Masuk Kembali", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        )
    }

    val bottomNavItems = listOf(
        CustomerScreen.Home,
        CustomerScreen.History,
        CustomerScreen.Wallet,
        CustomerScreen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        CustomerScreen.Home.route,
        CustomerScreen.History.route,
        CustomerScreen.Wallet.route,
        CustomerScreen.Profile.route
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    screen.icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            startDestination = CustomerScreen.Splash.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Splash Route
            composable(CustomerScreen.Splash.route) {
                CustomerSplashScreen(
                    onSplashFinished = {
                        val destination = if (isLoggedIn) CustomerScreen.Home.route else CustomerScreen.Login.route
                        navController.navigate(destination) {
                            popUpTo(CustomerScreen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Auth Routes
            composable(CustomerScreen.Login.route) {
                CustomerLoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(CustomerScreen.Register.route)
                    },
                    onNavigateToForgotPassword = { phone ->
                        authRepo.setTempPhone(phone)
                        navController.navigate("customer_forgot_password")
                    },
                    onLoginSuccess = {
                        navController.navigate(CustomerScreen.Home.route) {
                            popUpTo(CustomerScreen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable("customer_forgot_password") {
                CustomerForgotPasswordScreen(
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
                CustomerOtpScreen(
                    phoneNumber = tempPhone.ifEmpty { "081234567890" },
                    isForgotPassword = isForgot,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToRegister = {
                        navController.navigate(CustomerScreen.Register.route)
                    },
                    onNavigateToResetPassword = {
                        navController.navigate(CustomerScreen.ResetPassword.route)
                    },
                    onVerificationSuccess = {
                        navController.navigate(CustomerScreen.Home.route) {
                            popUpTo(CustomerScreen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(CustomerScreen.Register.route) {
                CustomerRegisterScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onRegistrationSuccess = {
                        navController.navigate(CustomerScreen.Home.route) {
                            popUpTo(CustomerScreen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(CustomerScreen.ResetPassword.route) {
                com.massago.customer.ui.screens.auth.CustomerResetPasswordScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onResetSuccess = {
                        navController.navigate(CustomerScreen.Home.route) {
                            popUpTo(CustomerScreen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home Screen
            composable(CustomerScreen.Home.route) {
                CustomerHomeScreen(
                    onNavigateToDetail = { serviceId ->
                        navController.navigate(CustomerScreen.Detail.createRoute(serviceId))
                    },
                    onNavigateToTracking = {
                        navController.navigate(CustomerScreen.Tracking.route)
                    },
                    onNavigateToWallet = {
                        navController.navigate(CustomerScreen.Wallet.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(CustomerScreen.History.route)
                    },
                    onNavigateToCheckout = { serviceId, therapistId, therapistName ->
                        navController.navigate(
                            CustomerScreen.Checkout.createRoute(
                                serviceId = serviceId,
                                preferredTherapistId = therapistId,
                                preferredTherapistName = therapistName
                            )
                        )
                    }
                )
            }

            // Service Detail Screen
            composable(
                route = CustomerScreen.Detail.route,
                arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val serviceId = backStackEntry.arguments?.getString("serviceId") ?: "SRV-TRAD"
                ServiceDetailScreen(
                    serviceId = serviceId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCheckout = { srvId, duration, aromaId, focusAreas, pressure, gender ->
                        navController.navigate(
                            CustomerScreen.Checkout.createRoute(
                                srvId, duration, aromaId, focusAreas, pressure, gender
                            )
                        )
                    }
                )
            }

            // Checkout Screen
            composable(
                route = CustomerScreen.Checkout.route,
                arguments = listOf(
                    navArgument("serviceId") { type = NavType.StringType; defaultValue = "SRV-TRAD" },
                    navArgument("duration") { type = NavType.IntType; defaultValue = 90 },
                    navArgument("aromaId") { type = NavType.StringType; defaultValue = "aroma-olive" },
                    navArgument("focusAreas") { type = NavType.StringType; defaultValue = "Pundak" },
                    navArgument("pressure") { type = NavType.StringType; defaultValue = "MEDIUM" },
                    navArgument("gender") { type = NavType.StringType; defaultValue = "Bebas" },
                    navArgument("preferredTherapistId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("preferredTherapistName") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val serviceId = backStackEntry.arguments?.getString("serviceId") ?: "SRV-TRAD"
                val duration = backStackEntry.arguments?.getInt("duration") ?: 90
                val rawAromaId = backStackEntry.arguments?.getString("aromaId") ?: "aroma-olive"
                val rawFocusAreas = backStackEntry.arguments?.getString("focusAreas") ?: "Pundak"
                val pressure = backStackEntry.arguments?.getString("pressure") ?: "MEDIUM"
                val rawGender = backStackEntry.arguments?.getString("gender") ?: "Bebas"
                val rawTherapistId = backStackEntry.arguments?.getString("preferredTherapistId") ?: ""
                val rawTherapistName = backStackEntry.arguments?.getString("preferredTherapistName") ?: ""

                val aromaId = try { java.net.URLDecoder.decode(rawAromaId, "UTF-8") } catch (_: Exception) { rawAromaId }
                val focusAreas = try { java.net.URLDecoder.decode(rawFocusAreas, "UTF-8") } catch (_: Exception) { rawFocusAreas }
                val gender = try { java.net.URLDecoder.decode(rawGender, "UTF-8") } catch (_: Exception) { rawGender }
                val therapistId = try { java.net.URLDecoder.decode(rawTherapistId, "UTF-8") } catch (_: Exception) { rawTherapistId }
                val therapistName = try { java.net.URLDecoder.decode(rawTherapistName, "UTF-8") } catch (_: Exception) { rawTherapistName }

                CheckoutScreen(
                    serviceId = serviceId,
                    durationMinutes = duration,
                    aromaId = aromaId,
                    focusAreasStr = focusAreas,
                    pressureStr = pressure,
                    genderPreference = gender,
                    preferredTherapistId = therapistId,
                    preferredTherapistName = therapistName,
                    onNavigateBack = { navController.popBackStack() },
                    onOrderPlaced = {
                        navController.navigate(CustomerScreen.Tracking.route) {
                            popUpTo(CustomerScreen.Home.route)
                        }
                    }
                )
            }

            // Tracking Screen
            composable(CustomerScreen.Tracking.route) {
                OrderTrackingScreen(
                    onNavigateBack = { navController.navigate(CustomerScreen.Home.route) },
                    onNavigateToChat = { navController.navigate(CustomerScreen.Chat.route) }
                )
            }

            // Chat Screen
            composable(CustomerScreen.Chat.route) {
                CustomerChatScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // History Screen
            composable(CustomerScreen.History.route) {
                CustomerHistoryScreen(
                    onNavigateBack = { navController.navigate(CustomerScreen.Home.route) },
                    onReorder = { serviceId, therapistId, therapistName ->
                        if (therapistId.isNotBlank()) {
                            navController.navigate(
                                CustomerScreen.Checkout.createRoute(
                                    serviceId = serviceId,
                                    preferredTherapistId = therapistId,
                                    preferredTherapistName = therapistName
                                )
                            )
                        } else {
                            navController.navigate(CustomerScreen.Detail.createRoute(serviceId))
                        }
                    }
                )
            }

            // Wallet Screen
            composable(CustomerScreen.Wallet.route) {
                CustomerWalletScreen(
                    onNavigateBack = { navController.navigate(CustomerScreen.Home.route) }
                )
            }

            // Profile Screen
            composable(CustomerScreen.Profile.route) {
                CustomerProfileScreen(
                    onNavigateBack = { navController.navigate(CustomerScreen.Home.route) },
                    onLogout = {
                        navController.navigate(CustomerScreen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
