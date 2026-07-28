package com.troxzy.xploit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.theme.AmoledBlack
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.DarkSurface
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.TextSecondary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Settings

/**
 * Data class representing a single navigation item in the bottom bar.
 */
private data class NavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

/**
 * A composable bottom navigation bar with 5 items:
 * Dashboard, AI Chat, Tools, Terminal, and Settings.
 *
 * Each item uses neon cyan when selected, and a muted color when unselected.
 *
 * @param currentRoute The currently active route string.
 * @param onNavigate Callback invoked with the route string when an item is tapped.
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // Define the 5 navigation items
    val navItems = listOf(
        NavItem(
            label = "Dashboard",
            route = "dashboard",
            icon = Icons.Filled.Home
        ),
        NavItem(
            label = "AI Chat",
            route = "ai_chat",
            icon = Icons.Filled.SmartToy
        ),
        NavItem(
            label = "Tools",
            route = "tools",
            icon = Icons.Filled.Build
        ),
        NavItem(
            label = "Terminal",
            route = "terminal",
            icon = Icons.Filled.Terminal
        ),
        NavItem(
            label = "Settings",
            route = "settings",
            icon = Icons.Filled.Settings
        )
    )

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = NeonCyan,
        tonalElevation = 0.dp,
        modifier = Modifier
    ) {
        navItems.forEach { item ->
            val isSelected = currentRoute == item.route

            // Animate the icon color transition
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) NeonCyan else TextSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "nav_icon_color_${item.route}"
            )

            // Animate the text color transition
            val textColor by animateColorAsState(
                targetValue = if (isSelected) NeonCyan else TextSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "nav_text_color_${item.route}"
            )

            // Animate the indicator color
            val indicatorColor by animateColorAsState(
                targetValue = if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "nav_indicator_color_${item.route}"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = textColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                },
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonCyan,
                    selectedTextColor = NeonCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = NeonCyan.copy(alpha = 0.12f)
                )
            )
        }
    }
}
