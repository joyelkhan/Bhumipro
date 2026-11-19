package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.WorkspaceMode
import com.example.ui.screens.AnaGondaCalculatorScreen
import com.example.ui.screens.BhumiProBottomTab
import com.example.ui.screens.BontonnamaScreen
import com.example.ui.screens.DigitalCompassScreen
import com.example.ui.screens.ExportCenterScreen
import com.example.ui.screens.FeedTabScreen
import com.example.ui.screens.FilesTabScreen
import com.example.ui.screens.MapWorkspaceScreen
import com.example.ui.screens.MouzaMapLibraryScreen
import com.example.ui.screens.ToolDashboardScreen
import com.example.ui.screens.UnitConverterScreen
import com.example.ui.screens.UnknownSideAreaScreen
import com.example.ui.screens.print.LivePrintPreviewScreen
import com.example.ui.screens.print.PrintTicketScreen
import com.example.ui.settings.AdvancedSettingsScreen
import com.example.ui.theme.BhumiOrangeCta
import com.example.ui.theme.BhumiProTheme
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.LandSurveyViewModel
import com.example.ui.viewmodel.PrintTicketViewModel
import com.example.ui.viewmodel.SettingsViewModel

object BhumiProRoutes {
    const val DASHBOARD = "dashboard"
    const val FILES_TAB = "files_tab"
    const val BONTONNAMA_TAB = "bontonnama_tab"
    const val FEED_TAB = "feed_tab"
    const val WORKSPACE_PATTERN = "workspace/{projectId}?mode={mode}"
    const val PRINT_PREVIEW = "print_preview"
    const val PRINT_TICKET = "print_ticket"
    const val PRINT_EXPORT_CENTER = "print_export_center"
    const val ANA_GONDA = "ana_gonda"
    const val UNKNOWN_SIDE_AREA = "unknown_side_area"
    const val UNIT_CONVERTER = "unit_converter"
    const val COMPASS = "compass"
    const val MOUZA_LIBRARY = "mouza_library"
    const val SETTINGS = "settings"

    fun workspaceRoute(
        projectId: String = "draft_raujan_4",
        mode: WorkspaceMode = WorkspaceMode.MOUZA_SURVEY
    ): String = "workspace/$projectId?mode=${mode.id}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = applicationContext as Application
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(app))
            val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

            BhumiProTheme(themeMode = prefs.themeMode) {
                val navController = rememberNavController()
                val dashboardViewModel: DashboardViewModel = viewModel()
                val surveyViewModel: LandSurveyViewModel = viewModel(factory = LandSurveyViewModel.Factory(app))
                val ticketViewModel: PrintTicketViewModel = viewModel()
                var showFilesCreateDialog by remember { mutableStateOf(false) }

                val navigateBottomTab: (BhumiProBottomTab) -> Unit = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(BhumiProRoutes.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = BhumiProRoutes.DASHBOARD
                ) {
                    composable(BhumiProRoutes.DASHBOARD) {
                        ToolDashboardScreen(
                            viewModel = dashboardViewModel,
                            language = prefs.language,
                            onSelectTab = navigateBottomTab,
                            onOpenWorkspace = { projectId ->
                                navController.navigate(BhumiProRoutes.workspaceRoute(projectId, WorkspaceMode.MOUZA_SURVEY))
                            },
                            onOpenWorkspaceWithMode = { projectId, mode ->
                                navController.navigate(BhumiProRoutes.workspaceRoute(projectId, mode))
                            },
                            onOpenRoute = { route ->
                                navController.navigate(route)
                            },
                            onOpenSettings = {
                                navigateBottomTab(BhumiProBottomTab.SETTINGS)
                            }
                        )
                    }

                    composable(BhumiProRoutes.FILES_TAB) {
                        FilesTabScreen(
                            viewModel = dashboardViewModel,
                            language = prefs.language,
                            onSelectTab = navigateBottomTab,
                            onOpenWorkspace = { projectId ->
                                navController.navigate(BhumiProRoutes.workspaceRoute(projectId, WorkspaceMode.MOUZA_SURVEY))
                            },
                            onOpenCreateDialog = { showFilesCreateDialog = true }
                        )
                    }

                    composable(BhumiProRoutes.BONTONNAMA_TAB) {
                        BontonnamaScreen(
                            viewModel = dashboardViewModel,
                            defaultShotokFactor = prefs.appDefaultShotokSqFt,
                            language = prefs.language,
                            onSelectTab = navigateBottomTab,
                            onOpenWorkspace = { projectId ->
                                navController.navigate(BhumiProRoutes.workspaceRoute(projectId, WorkspaceMode.MOUZA_SURVEY))
                            }
                        )
                    }

                    composable(BhumiProRoutes.FEED_TAB) {
                        FeedTabScreen(
                            viewModel = dashboardViewModel,
                            language = prefs.language,
                            onSelectTab = navigateBottomTab,
                            onOpenWorkspace = { projectId ->
                                navController.navigate(BhumiProRoutes.workspaceRoute(projectId, WorkspaceMode.MOUZA_SURVEY))
                            },
                            onOpenMouzaLibrary = {
                                navController.navigate(BhumiProRoutes.MOUZA_LIBRARY)
                            }
                        )
                    }

                    composable(
                        route = BhumiProRoutes.WORKSPACE_PATTERN,
                        arguments = listOf(
                            navArgument("projectId") {
                                type = NavType.StringType
                                defaultValue = "draft_raujan_4"
                            },
                            navArgument("mode") {
                                type = NavType.StringType
                                defaultValue = WorkspaceMode.MOUZA_SURVEY.id
                            }
                        )
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId") ?: "draft_raujan_4"
                        val modeStr = backStackEntry.arguments?.getString("mode")
                        val mode = WorkspaceMode.fromId(modeStr)
                        MapWorkspaceScreen(
                            projectId = projectId,
                            mode = mode,
                            viewModel = surveyViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenPrintPreview = {
                                surveyViewModel.observePrintJobState()
                                navController.navigate(BhumiProRoutes.PRINT_PREVIEW)
                            },
                            onOpenExportCenter = {
                                navController.navigate(BhumiProRoutes.PRINT_EXPORT_CENTER)
                            }
                        )
                    }

                    composable(BhumiProRoutes.PRINT_PREVIEW) {
                        LivePrintPreviewScreen(
                            viewModel = surveyViewModel,
                            ticketViewModel = ticketViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenTicket = { navController.navigate(BhumiProRoutes.PRINT_TICKET) },
                            onOpenExportCenter = { navController.navigate(BhumiProRoutes.PRINT_EXPORT_CENTER) }
                        )
                    }

                    composable(BhumiProRoutes.PRINT_TICKET) {
                        PrintTicketScreen(
                            ticketViewModel = ticketViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(BhumiProRoutes.PRINT_EXPORT_CENTER) {
                        ExportCenterScreen(
                            viewModel = surveyViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(BhumiProRoutes.ANA_GONDA) {
                        AnaGondaCalculatorScreen(
                            defaultShotokFactor = prefs.appDefaultShotokSqFt,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(BhumiProRoutes.UNKNOWN_SIDE_AREA) {
                        UnknownSideAreaScreen(
                            defaultShotokFactor = prefs.appDefaultShotokSqFt,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(BhumiProRoutes.UNIT_CONVERTER) {
                        UnitConverterScreen(
                            defaultShotokFactor = prefs.appDefaultShotokSqFt,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(BhumiProRoutes.COMPASS) {
                        DigitalCompassScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(BhumiProRoutes.MOUZA_LIBRARY) {
                        MouzaMapLibraryScreen(
                            dashboardViewModel = dashboardViewModel,
                            onOpenProjectInWorkspace = { projectId ->
                                navController.navigate(BhumiProRoutes.workspaceRoute(projectId, WorkspaceMode.MOUZA_SURVEY))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(BhumiProRoutes.SETTINGS) {
                        AdvancedSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                            onSelectTab = navigateBottomTab,
                            onOpenPrintTicket = { navController.navigate(BhumiProRoutes.PRINT_TICKET) },
                            onOpenFeed = { navigateBottomTab(BhumiProBottomTab.FEED) }
                        )
                    }
                }

                if (showFilesCreateDialog) {
                    var title by remember { mutableStateOf("") }
                    var mouza by remember { mutableStateOf("") }
                    var jl by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showFilesCreateDialog = false },
                        title = { Text("নতুন জরিপ প্রজেক্ট তৈরি") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("প্রজেক্ট শিরোনাম") }, singleLine = true)
                                OutlinedTextField(value = mouza, onValueChange = { mouza = it }, label = { Text("মৌজার নাম") }, singleLine = true)
                                OutlinedTextField(value = jl, onValueChange = { jl = it }, label = { Text("জে.এল নম্বর") }, singleLine = true)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showFilesCreateDialog = false
                                    dashboardViewModel.createNewProject(title, mouza, jlNumber = jl) { newId ->
                                        navController.navigate(BhumiProRoutes.workspaceRoute(newId, WorkspaceMode.MOUZA_SURVEY))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BhumiOrangeCta)
                            ) { Text("তৈরি করুন") }
                        },
                        dismissButton = { TextButton(onClick = { showFilesCreateDialog = false }) { Text("বাতিল") } }
                    )
                }
            }
        }
    }
}
