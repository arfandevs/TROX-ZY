package com.troxzy.xploit.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.theme.AmoledBlack
import com.troxzy.xploit.ui.theme.DarkSurface
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.TextPrimary
import kotlinx.coroutines.launch

/**
 * A composable scaffold wrapper that combines:
 * - **TopAppBar** with a back button, hamburger menu, and title
 * - **SideDrawer** with all tool modules
 * - **BottomNavBar** with the 5 main navigation items
 *
 * This provides the common shell for all screens in the app.
 *
 * @param title The title displayed in the top app bar.
 * @param currentRoute The currently active route string for navigation highlighting.
 * @param onNavigate Callback invoked with a route string when navigation occurs.
 * @param onBack Callback invoked when the back button is pressed.
 * @param content The main screen content composable, receiving the inner padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonScaffold(
    title: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // The SideDrawer wraps everything and provides the drawer functionality
    SideDrawer(
        drawerState = drawerState,
        onNavigate = onNavigate
    ) {
        // The Scaffold provides the top bar, bottom bar, and content area
        Scaffold(
            containerColor = AmoledBlack,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        GlitchText(
                            text = title,
                            isAnimating = false,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            defaultColor = TextPrimary
                        )
                    },
                    navigationIcon = {
                        // Back button
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    actions = {
                        // Hamburger menu button to open the drawer
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = NeonCyan,
                        actionIconContentColor = NeonCyan
                    )
                )
            },
            bottomBar = {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate
                )
            },
            content = content
        )
    }
}
