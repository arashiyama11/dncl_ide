package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.entity.Organization
import com.mikepenz.aboutlibraries.entity.Scm
import io.github.arashiyama11.dncl_ide.util.getAllLicenses
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicencesScreen(onBack: () -> Unit = {}, navigateToSingleLicense: (String) -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var libs by remember { mutableStateOf<Libs?>(null) }

    LaunchedEffect(Unit) {
        libs = getAllLicenses()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ライセンス表示") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (libs != null) {
                LicencesContent(libs!!, navigateToSingleLicense)
            } else {
                Text("Failed to load licenses", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun LicencesContent(libs: Libs, navigateToSingleLicense: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // License Summary Section
        item {
            Text(
                """このアプリは以下のオープンソースソフトウェアを使用しています。
一部のライブラリには追加情報（NOTICE等）が付属している場合があります。
""",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "使用ライブラリ",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Libraries List
        items(libs.libraries.toList()) { library ->
            LibraryItem(
                library = library,
                onLicenseClick = { content ->
                    navigateToSingleLicense(content)
                }
            )
        }
    }
}

@Composable
private fun LibraryItem(
    library: Library,
    onLicenseClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current


    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).padding(vertical = 8.dp)
            .clickable {
                val licenses =
                    library.licenses.mapNotNull { it.licenseContent }.joinToString("\n\n")
                onLicenseClick(licenses)
            }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f, fill = true)) {
                Text(
                    library.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        if (library.website != null) {
                            uriHandler.openUri(library.website!!)
                        }
                    }.wrapContentSize()
                )


                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Copyright © " + library.developers.joinToString(", ") { it.name ?: "Unknown" },
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )




                Spacer(modifier = Modifier.height(4.dp))

                library.licenses.forEach { license ->
                    Text(
                        license.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Text(
                library.artifactVersion.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.wrapContentSize(),
                color = Color.Gray,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleLicenseScreen(content: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("License") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(
                rememberScrollState()
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}


@Preview
@Composable
fun LicencesViewPreview() {
    LicencesScreen {}
}

@Preview
@Composable
fun LibraryItemPreview() {
    val mockLibrary = Library(
        uniqueId = "mock-library",
        artifactVersion = "1.0.0",
        name = "Mock Library",
        description = "This is a mock library for testing purposes.",
        website = "https://mocklibrary.example.com",
        developers = persistentListOf(
            Developer(name = "John Doe", null),
            Developer(name = "Jane Smith", null)
        ),
        organization = Organization(
            name = "Mock Organization",
            url = "https://mockorganization.example.com"
        ),
        scm = Scm(
            url = "https://github.com/mocklibrary",
            connection = "scm:git:git://github.com/mocklibrary.git",
            developerConnection = "scm:git:ssh://github.com/mocklibrary.git"
        ),
        licenses = persistentSetOf(
            License(name = "MIT", url = "https://opensource.org/licenses/MIT", hash = "MIT"),
            License(
                name = "Apache-2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0",
                hash = "Apache-2.0"
            )
        ),
        funding = persistentSetOf(),
        tag = "mock"
    )
    LibraryItem(
        library = mockLibrary,
        onLicenseClick = {}
    )
}