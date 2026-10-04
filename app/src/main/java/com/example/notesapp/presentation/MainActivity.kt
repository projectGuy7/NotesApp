package com.example.notesapp.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.notesapp.ui.theme.NotesAppTheme
import kotlinx.serialization.Serializable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import com.example.notesapp.domain.DBRepository
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.flow.StateFlow

const val VIEW_NOTE_ANALYTICS = "view_note"

class MainActivity : ComponentActivity() {
    lateinit var analytics: FirebaseAnalytics
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                MainViewModel(DBRepository(application.applicationContext))
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics = Firebase.analytics
        enableEdgeToEdge()
        setContent {
            NotesAppTheme {
                NavigationRoot(
                    stateFlow = viewModel.notesListState,
                    onEvent = viewModel::onEvent,
                    analytics = analytics
                )
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NavigationRoot(
    stateFlow: StateFlow<NotesListState>,
    onEvent: (MainIntent) -> Unit,
    analytics: FirebaseAnalytics,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(Route.NotesList)
    val snackBarHostState = remember { SnackbarHostState() }
    val state by stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(state.error) {
        state.error?.let { errorMessage ->
            snackBarHostState.showSnackbar(message = errorMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { _ ->
        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = { key ->
                when (key) {
                    is Route.NotesList -> {
                        NavEntry(key) {
                            NotesListScreen(
                                state = state,
                                onCreateNoteClick = { backStack.safeTransition(Route.CreateNote) },
                                onNoteClick = { note ->
                                    backStack.safeTransition(
                                        Route.NoteDetail(
                                            note
                                        )
                                    )
                                }
                            )
                        }
                    }

                    is Route.CreateNote -> {
                        NavEntry(key) {
                            CreateNoteScreen(
                                onSave = { title, content ->
                                    onEvent(MainIntent.CreateNote(title, content))
                                    backStack.remove(Route.CreateNote)
                                }
                            )
                        }
                    }

                    is Route.NoteDetail -> {
                        NavEntry(key) {
                            LaunchedEffect(Unit) {
                                analytics.logEvent(VIEW_NOTE_ANALYTICS, null)
                            }
                            NoteDetailScreen(
                                note = key.note,
                                onEditClick = { backStack.safeTransition(Route.EditNote(key.note)) },
                                onDeleteClick = {
                                    onEvent(MainIntent.DeleteNote(key.note.id))
                                    backStack.remove(Route.NoteDetail(key.note))
                                }
                            )
                        }
                    }

                    is Route.EditNote -> {
                        NavEntry(key) {
                            EditNoteScreen(
                                note = key.note,
                                onSave = { updatedContent ->
                                    onEvent(
                                        MainIntent.UpdateNote(
                                            id = key.note.id,
                                            content = updatedContent
                                        )
                                    )
                                    backStack.clear()
                                    backStack.safeTransition(Route.NotesList)
                                }
                            )
                        }
                    }

                    else -> error("Unknown route $key")
                }
            }
        )
    }
}

@Serializable
sealed interface Route : NavKey {

    @Serializable
    object NotesList : Route

    @Serializable
    object CreateNote : Route

    @Serializable
    data class NoteDetail(val note: NoteUI) : Route

    @Serializable
    data class EditNote(val note: NoteUI) : Route

}

@Composable
fun NotesListScreen(
    state: NotesListState,
    onCreateNoteClick: () -> Unit,
    onNoteClick: (NoteUI) -> Unit
) {
    Scaffold(
        topBar = { TopHeader(title = "Note list") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(state.notes, key = { it.id }) { note ->
                    NoteItem(note = note, onClick = { onNoteClick(note) })
                }
            }

            PurpleButton(
                text = "CREATE NOTE",
                onClick = onCreateNoteClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CreateNoteScreen(
    onSave: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopHeader(title = "Add note") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            PurpleButton(
                text = "SAVE",
                onClick = {
                    isSaving = true
                    onSave(title, content)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
        }
    }
}

@Composable
fun NoteDetailScreen(
    note: NoteUI,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Scaffold(
        topBar = { TopHeader(title = "View Note") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = note.title,
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Text(
                text = note.content,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            PurpleButton(
                text = "EDIT",
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            )

            PurpleButton(
                text = "DELETE",
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun EditNoteScreen(
    note: NoteUI,
    onSave: (updatedContent: String) -> Unit
) {
    var content by remember { mutableStateOf(note.content) }

    Scaffold(
        topBar = { TopHeader(title = "Edit note") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = note.title,
                enabled = false,
                onValueChange = { _ -> },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            PurpleButton(
                text = "SAVE",
                onClick = { onSave(content) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==========================================
// REUSABLE UI COMPONENTS
// ==========================================

@Composable
private fun TopHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NoteItem(
    note: NoteUI,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            text = note.title,
            fontSize = 16.sp,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = note.date,
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun PurpleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.extraSmall,
        enabled = enabled
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = Color(0xFFE0E0E0),
    focusedContainerColor = Color(0xFFE0E0E0),
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = Color.Transparent
)

@Preview(showBackground = true, name = "1. Notes List Screen")
@Composable
fun NotesListScreenPreview() {
    MaterialTheme {
        NotesListScreen(
            state = NotesListState(
                notes = listOf(
                    NoteUI(
                        id = 1,
                        title = "Another note",
                        date = "Apr 1, 2021 4:34:24 PM",
                        content = "Content for another note"
                    ),
                    NoteUI(
                        id = 2,
                        title = "One more note",
                        date = "Apr 1, 2021 4:34:38 PM",
                        content = "Content for one more note"
                    )
                )
            ),
            onCreateNoteClick = {},
            onNoteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Create Note Screen")
@Composable
fun CreateNoteScreenPreview() {
    MaterialTheme {
        CreateNoteScreen(
            onSave = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "3. View Note Screen")
@Composable
fun NoteDetailScreenPreview() {
    MaterialTheme {
        NoteDetailScreen(
            note = NoteUI(
                id = 1,
                title = "Test",
                date = "Apr 1, 2021 4:33:59 PM",
                content = "Some note content"
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "4. Edit Note Screen")
@Composable
fun EditNoteScreenPreview() {
    MaterialTheme {
        EditNoteScreen(
            note = NoteUI(
                id = 1,
                title = "Test",
                date = "Apr 1, 2021 4:33:59 PM",
                content = "Some note content"
            ),
            onSave = { }
        )
    }
}

fun NavBackStack<NavKey>.safeTransition(navKey: NavKey) {
    if (lastOrNull() != navKey) {
        add(navKey)
    }
}