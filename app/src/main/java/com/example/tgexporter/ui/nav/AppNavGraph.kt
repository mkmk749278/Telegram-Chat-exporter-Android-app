package com.example.tgexporter.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tgexporter.di.AppContainer
import com.example.tgexporter.ui.auth.AuthScreen
import com.example.tgexporter.ui.auth.AuthViewModel
import com.example.tgexporter.ui.chats.ChatsScreen
import com.example.tgexporter.ui.chats.ChatsViewModel
import com.example.tgexporter.ui.export.ExportPreviewScreen
import com.example.tgexporter.ui.export.ExportPreviewViewModel
import com.example.tgexporter.ui.messages.MessagesScreen
import com.example.tgexporter.ui.messages.MessagesViewModel

private const val ROUTE_AUTH = "auth"
private const val ROUTE_CHATS = "chats"
private const val ROUTE_MESSAGES = "messages/{chatId}/{title}"
private const val ROUTE_EXPORT = "export/{title}"

@Composable
fun AppNavGraph(container: AppContainer) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = ROUTE_AUTH) {
        composable(ROUTE_AUTH) {
            val vm: AuthViewModel = viewModel(
                factory = viewModelFactory { initializer { AuthViewModel(container.telegram) } },
            )
            AuthScreen(viewModel = vm) {
                nav.navigate(ROUTE_CHATS) {
                    popUpTo(ROUTE_AUTH) { inclusive = true }
                }
            }
        }

        composable(ROUTE_CHATS) {
            val vm: ChatsViewModel = viewModel(
                factory = viewModelFactory { initializer { ChatsViewModel(container.chats) } },
            )
            ChatsScreen(viewModel = vm) { chatId, title ->
                nav.navigate("messages/$chatId/${Uri.encode(title)}")
            }
        }

        composable(
            route = ROUTE_MESSAGES,
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val title = backStackEntry.arguments?.getString("title").orEmpty()
            val vm: MessagesViewModel = viewModel(
                key = "messages-$chatId",
                factory = viewModelFactory {
                    initializer { MessagesViewModel(chatId, container.messages) }
                },
            )
            MessagesScreen(
                chatTitle = title,
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onExport = {
                    container.pendingExport = vm.selectedMessages()
                    vm.clearSelection()
                    nav.navigate("export/${Uri.encode(title)}")
                },
            )
        }

        composable(
            route = ROUTE_EXPORT,
            arguments = listOf(navArgument("title") { type = NavType.StringType }),
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title").orEmpty()
            val pending = remember { container.pendingExport }
            val vm: ExportPreviewViewModel = viewModel(
                key = "export-${pending.firstOrNull()?.id ?: 0}",
                factory = viewModelFactory {
                    initializer { ExportPreviewViewModel(title, pending) }
                },
            )
            ExportPreviewScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }
    }
}
