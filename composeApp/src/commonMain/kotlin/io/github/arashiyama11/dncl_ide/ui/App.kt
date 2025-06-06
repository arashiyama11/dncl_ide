package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.adapter.SelectFileScreenViewModel
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.ui.components.isImeVisible
import io.github.arashiyama11.dncl_ide.ui.components.rememberDarkThemeStateFlow
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

val showFabDist = setOf(
    Destination.SelectFileScreen::class.qualifiedName,
    Destination.SelectNotebookScreen::class.qualifiedName,
)

@Composable
fun AppFab(
    currentRoute: String?,
    notebookViewModel: NotebookViewModel = koinViewModel(),
    selectFileViewModel: SelectFileScreenViewModel = koinViewModel(),
    selectNotebookViewModel: SelectFileScreenViewModel = koinViewModel(),
) {
    val uiState by selectNotebookViewModel.uiState.collectAsStateWithLifecycle()
    println("Current route: $currentRoute, Notebook selected: ${uiState.selectedEntryPath?.isNotebookFile()}")
    when (currentRoute) {
        Destination.SelectFileScreen::class.qualifiedName -> {
            FloatingActionButton(onClick = { selectFileViewModel.onFileAddClicked() }) {
                Icon(Icons.Outlined.Add, contentDescription = "New File")
            }
        }

        Destination.SelectNotebookScreen::class.qualifiedName -> {
            FloatingActionButton(onClick = { selectNotebookViewModel.onFileAddClicked() }) {
                Icon(Icons.Outlined.Add, contentDescription = "New Notebook")
            }
        }

        Destination.CodingScreen::class.qualifiedName -> {
            if (uiState.selectedEntryPath?.isNotebookFile() == true) {
                val uiState by notebookViewModel.uiState.collectAsStateWithLifecycle()
                var showDropdown by remember { mutableStateOf(false) }
                Box {
                    FloatingActionButton(onClick = { showDropdown = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Cell")
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Code Cell") },
                            onClick = {
                                notebookViewModel.handleAction(
                                    io.github.arashiyama11.dncl_ide.adapter.NotebookAction.AddCellAfter(
                                        uiState.selectedCellId,
                                        CellType.CODE
                                    )
                                )
                                showDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Markdown Cell") },
                            onClick = {
                                notebookViewModel.handleAction(
                                    io.github.arashiyama11.dncl_ide.adapter.NotebookAction.AddCellAfter(
                                        uiState.selectedCellId,
                                        CellType.MARKDOWN
                                    )
                                )
                                showDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun App() {
    DnclIdeTheme {
        //val drawerViewModel = koinViewModel<DrawerViewModel>()
        val selectFileViewModel = koinViewModel<SelectFileScreenViewModel>()
        val selectNotebookViewModel = koinViewModel<SelectFileScreenViewModel>()
        val ideViewModel = koinViewModel<IdeViewModel>()
        val notebookViewModel = koinViewModel<NotebookViewModel>()
        val snackbarHostState = remember { SnackbarHostState() }
        val isDarkThemeStateFlow = rememberDarkThemeStateFlow()
        val navController = rememberNavController()
        val fileRepository = koinInject<FileRepository>()
        val selectedFile by fileRepository.selectedEntryPath.collectAsStateWithLifecycle()


        LaunchedEffect(Unit) {
            ideViewModel.onStart(isDarkThemeStateFlow)
            for (err in ideViewModel.errorChannel) {
                snackbarHostState.showSnackbar(err, "OK", false, SnackbarDuration.Indefinite)
            }
        }

        LaunchedEffect(Unit) {
            notebookViewModel.onStart()
            for (err in notebookViewModel.errorChannel) {
                snackbarHostState.showSnackbar(err, "OK", false, SnackbarDuration.Indefinite)
            }
        }


        var bottomAppBarHeight by remember { mutableStateOf(0.dp) }

        Scaffold(snackbarHost = {
            SnackbarHost(snackbarHostState)
        }, bottomBar = {
            val dense = LocalDensity.current
            BottomAppBar(modifier = Modifier.wrapContentHeight().onGloballyPositioned {
                bottomAppBarHeight = with(dense) { it.size.height.toDp() }
            }) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        navController.navigate(Destination.SelectFileScreen)
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null)
                    }

                    IconButton(onClick = {
                        navController.navigate(Destination.SelectNotebookScreen)
                    }) {
                        Icon(Icons.Outlined.FormatListNumbered, null)
                    }

                    IconButton(onClick = {
                        navController.navigate(Destination.CodingScreen)
                    }) {
                        Icon(Icons.Outlined.Code, null)
                    }


                    IconButton(onClick = {
                        navController.navigate(Destination.SettingsScreen)
                    }) {
                        Icon(Icons.Outlined.Settings, null)
                    }
                }
            }
        }, floatingActionButton = {
            val bse by navController.currentBackStackEntryAsState()
            val dist = bse?.destination
            AppFab(dist?.route)
            /*
            if (dist?.route in showFabDist) {
                val isNotebook =
                    dist?.route == Destination.SelectNotebookScreen::class.qualifiedName
                var isShowDetails by remember { mutableStateOf(false) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = bottomAppBarHeight + 12.dp)
                ) {
                    AnimatedVisibility(isShowDetails) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SmallFloatingActionButton(
                                onClick = {
                                    if (isNotebook) {
                                        selectNotebookViewModel.onFileAddClicked()
                                    } else {
                                        selectFileViewModel.onFileAddClicked()
                                    }
                                },
                                containerColor = Color.White
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            SmallFloatingActionButton(
                                onClick = {
                                    if (isNotebook) {
                                        selectNotebookViewModel.onFolderAddClicked()
                                    } else {
                                        selectFileViewModel.onFolderAddClicked()
                                    }
                                },
                                containerColor = Color.White
                            ) {

                                Icon(
                                    Icons.Outlined.Folder,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    FloatingActionButton(
                        onClick = { isShowDetails = !isShowDetails },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }*/
        }, floatingActionButtonPosition = FabPosition.EndOverlay) { contentPadding ->
            val padding = if (isImeVisible()) {
                PaddingValues(
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    top = contentPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 4.dp
                )
            } else {
                contentPadding
            }

            NavHost(navController, startDestination = Destination.App) {
                composable<Destination.App> {
                    if (selectedFile?.isNotebookFile() == true) {
                        NotebookScreen(modifier = Modifier.padding(padding))
                    } else DnclIDE(
                        modifier = Modifier.padding(padding), ideViewModel
                    )
                }

                composable<Destination.LicensesScreen> {
                    LicencesScreen(
                        onBack = { navController.popBackStack() }
                    ) {
                        navController.navigate(Destination.SingleLicenseScreen(it))
                    }
                }

                composable<Destination.SingleLicenseScreen> {
                    val license = it.toRoute<Destination.SingleLicenseScreen>()
                    SingleLicenseScreen(license.content) {
                        navController.popBackStack()
                    }
                }

                composable<Destination.SelectFileScreen> {
                    SelectFileScreen() {
                        navController.navigate(Destination.CodingScreen)
                    }
                }

                composable<Destination.SelectNotebookScreen> {
                    SelectNotebookScreen() {
                        navController.navigate(Destination.CodingScreen)
                    }
                }

                composable<Destination.CodingScreen> {
                    CodingScreen()
                }

                composable<Destination.SettingsScreen> {
                    SettingsScreen() {
                        navController.navigate(Destination.LicensesScreen)
                    }
                }
            }
        }
    }
}


object Destination {
    @Serializable
    object App

    @Serializable
    object SelectFileScreen

    @Serializable
    object SelectNotebookScreen

    @Serializable
    object CodingScreen

    @Serializable
    object SettingsScreen

    @Serializable
    object LicensesScreen

    @Serializable
    data class SingleLicenseScreen(
        val content: String,
    )
}
