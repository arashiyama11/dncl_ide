package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.adapter.SelectFileScreenViewModel
import io.github.arashiyama11.dncl_ide.adapter.SelectNotebookScreenViewModel
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.ui.components.isImeVisible
import io.github.arashiyama11.dncl_ide.ui.components.rememberDarkThemeStateFlow
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FabsWith2Options(onFileAddClicked: () -> Unit, onFolderAddClicked: () -> Unit) {
    var isShowDetails by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(isShowDetails) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SmallFloatingActionButton(
                    onClick = {
                        onFileAddClicked()
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
                        onFolderAddClicked()
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

}

@Composable
fun AppFab(
    currentRoute: String?,
    notebookViewModel: NotebookViewModel = koinViewModel(),
    selectFileViewModel: SelectFileScreenViewModel = koinViewModel(),
    selectNotebookViewModel: SelectNotebookScreenViewModel = koinViewModel(),
) {
    val uiState by selectNotebookViewModel.uiState.collectAsStateWithLifecycle()
    when (currentRoute) {
        Destination.SelectFileScreen::class.qualifiedName -> {
            FabsWith2Options(
                onFileAddClicked = { selectFileViewModel.onFileAddClicked() },
                onFolderAddClicked = {
                    selectFileViewModel.onFolderAddClicked()
                }
            )
        }

        Destination.SelectNotebookScreen::class.qualifiedName -> {
            FabsWith2Options(
                onFileAddClicked = {
                    selectNotebookViewModel.onFileAddClicked()
                },
                onFolderAddClicked = {
                    selectNotebookViewModel.onFolderAddClicked()
                }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    DnclIdeTheme {
        val ideViewModel = koinViewModel<IdeViewModel>()
        val notebookViewModel = koinViewModel<NotebookViewModel>()
        val snackbarHostState = remember { SnackbarHostState() }
        val isDarkThemeStateFlow = rememberDarkThemeStateFlow()
        val navController = rememberNavController()
        val bse by navController.currentBackStackEntryAsState()




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

        Scaffold(
            topBar = {
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route
                when (currentRoute) {
                    Destination.LicensesScreen::class.qualifiedName -> {
                        TopAppBar(
                            title = { Text("ライセンス表示") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    }

                    Destination.SingleLicenseScreen::class.qualifiedName -> {
                        TopAppBar(
                            title = { Text("License") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        )
                    }

                    else -> {}
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }, bottomBar = {
                val dense = LocalDensity.current
                BottomAppBar(modifier = Modifier.height(100.dp).onGloballyPositioned {
                    bottomAppBarHeight = with(dense) { it.size.height.toDp() }
                }) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = {
                            navController.navigate(Destination.SelectFileScreen)
                        }) {
                            val icon =
                                if (bse?.destination?.route == Destination.SelectFileScreen::class.qualifiedName) {
                                    Icons.AutoMirrored.Filled.InsertDriveFile
                                } else {
                                    Icons.AutoMirrored.Outlined.InsertDriveFile
                                }
                            Icon(icon, null)
                        }

                        run {
                            val isSelected =
                                remember(bse?.destination?.route) { bse?.destination?.route == Destination.SelectNotebookScreen::class.qualifiedName }
                            IconButton(
                                onClick = {
                                    navController.navigate(Destination.SelectNotebookScreen)
                                },
                                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent),
                            ) {
                                val icon =
                                    if (bse?.destination?.route == Destination.SelectNotebookScreen::class.qualifiedName) {
                                        Icons.Filled.FormatListNumbered
                                    } else {
                                        Icons.Outlined.FormatListNumbered
                                    }
                                Icon(
                                    icon,
                                    null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.surface else LocalContentColor.current
                                )
                            }
                        }

                        run {
                            val isSelected =
                                remember(bse?.destination?.route) { bse?.destination?.route == Destination.CodingScreen::class.qualifiedName }
                            IconButton(
                                onClick = {
                                    navController.navigate(Destination.CodingScreen)
                                },
                                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent),
                            ) {
                                val icon =
                                    if (bse?.destination?.route == Destination.CodingScreen::class.qualifiedName) {
                                        Icons.Filled.Code
                                    } else {
                                        Icons.Outlined.Code
                                    }
                                Icon(
                                    icon,
                                    null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.surface else LocalContentColor.current
                                )
                            }
                        }

                        IconButton(onClick = {
                            navController.navigate(Destination.SettingsScreen)
                        }) {
                            val icon =
                                if (bse?.destination?.route == Destination.SettingsScreen::class.qualifiedName) {
                                    Icons.Filled.Settings
                                } else {
                                    Icons.Outlined.Settings
                                }
                            Icon(icon, null)
                        }
                    }
                }
            }, floatingActionButton = {
                val bse by navController.currentBackStackEntryAsState()
                val dist = bse?.destination
                Box(
                    modifier = Modifier.wrapContentSize()
                        .padding(bottom = bottomAppBarHeight)
                ) {
                    AppFab(dist?.route)
                }
            }, floatingActionButtonPosition = FabPosition.EndOverlay
        ) { contentPadding ->
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

            NavHost(
                navController,
                modifier = Modifier.padding(padding),
                startDestination = Destination.CodingScreen
            ) {
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


sealed interface Destination {
    @Serializable
    object SelectFileScreen : Destination

    @Serializable
    object SelectNotebookScreen : Destination

    @Serializable
    object CodingScreen : Destination

    @Serializable
    object SettingsScreen : Destination

    @Serializable
    object LicensesScreen : Destination

    @Serializable
    data class SingleLicenseScreen(
        val content: String,
    ) : Destination
}
