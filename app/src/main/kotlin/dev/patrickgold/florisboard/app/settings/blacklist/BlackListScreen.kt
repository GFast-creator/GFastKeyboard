package dev.patrickgold.florisboard.app.settings.blacklist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.apptheme.FlorisAppTheme
import dev.patrickgold.florisboard.app.apptheme.ItemGreen
import dev.patrickgold.florisboard.app.apptheme.ItemRed
import dev.patrickgold.florisboard.app.settings.blacklist.exchanger.WordContentResolver
import dev.patrickgold.florisboard.app.settings.blacklist.room.Word
import dev.patrickgold.florisboard.app.settings.blacklist.room.WordViewModel
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.ui.theme.FlorisBoardTheme


@AndroidEntryPoint
class BlackListActivity : ComponentActivity() {

    private val viewModel: WordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlorisBoardTheme {
                BlackListScreen(viewModel, onBackPressedDispatcher)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun WordCardPreview() {
        FlorisAppTheme(theme = AppTheme.AUTO) {
            WordCard(
                Modifier,
                Word(word = stringResource(R.string.hello_world)),
                selected = false,
                selectedForDelete = true
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BlackListScreen(
    viewModel: WordViewModel,
    onBackPressedDispatcher: OnBackPressedDispatcher
) = FlorisScreen {

    title = "Чёрный список"
    previewFieldVisible = true
    iconSpaceReserved = false
    navigationIconVisible = true
    scrollable = false

    val words by viewModel.words.collectAsState(initial = emptyList())
    val selectedWords by viewModel.selectedWords.collectAsState(initial = emptyList())
    val isDialog by viewModel.dialogStateObj.isDialogStateFlow.collectAsState()
    val selectedForDelete by viewModel.deleteWordsObj.stateFlowInstant.collectAsState()



    val lifecycleOwner = LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    var multipleDeleteFlag by rememberSaveable {
        mutableStateOf(false)
    }


    val context = LocalContext.current
    LaunchedEffect(state) {
        Log.i("killzoid", "Updated: $state")

        when (state) {
            //Отправка данных
            Lifecycle.State.CREATED,Lifecycle.State.STARTED -> {
                WordContentResolver(context).run {
                    if (multipleDeleteFlag) { //обновление остольных только при удалении
                        deleteAll()
                        multipleDeleteFlag = false
                    }
                    words.forEach {
                        insertWord(it)
                    }
                }
            }
            //Получение данных
            Lifecycle.State.RESUMED -> {
                val newWords = WordContentResolver(context).allWordsRecords
                if (newWords != null) {
                    viewModel.deleteAll()
                    viewModel.saveAll(*newWords.toTypedArray())
                }
            }

            else -> {/* Nothing */}
        }
    }

    actions {
        if (selectedForDelete.isNotEmpty()) {
            IconButton(onClick = {
                viewModel.deleteWordsObj.update(emptyList())
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.cancel)
                )
            }

            IconButton(onClick = {
                viewModel.deleteWordsObj.update(words)
            }) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = stringResource(id = R.string.select_all),
                )
            }

            IconButton(onClick = {

                multipleDeleteFlag = true

                viewModel.delete(*selectedForDelete.toTypedArray())
                viewModel.deleteWordsObj.update(emptyList())


            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_selected)
                )
            }
        } else {
//            IconButton(onClick = {
//                val newWords = WordContentResolver(content).allWordsRecords
//                viewModel.deleteAll()
//                viewModel.saveAll(*newWords.toTypedArray())
//            }) {
//                Icon(
//                    imageVector = Icons.Default.SystemUpdateAlt,
//                    contentDescription = "Обновить"
//                )
//            }
//            IconButton(onClick = {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    WordContentResolver(content).run {
//                        deleteAll()
//                        words.forEach {
//                            insertWord(it)
//                        }
//                    }
//                }
//
//            }) {
//                Icon(
//                    imageVector = Icons.Default.Airplay,
//                    contentDescription = "Отправить"
//                )
//            }
        }
    }

    content {
        //viewModel.updateFoundation(selectedWords, text) // нужен был для прошлой версии приложения
        onBackPressedDispatcher.addCallback(LocalLifecycleOwner.current, viewModel.onBackPressedCallback)

        //диалог добавления слова
        if (isDialog) {
            var newWord by rememberSaveable {
                mutableStateOf("")
            }

            AlertDialog(
                onDismissRequest = { viewModel.dialogStateObj.update(false) },
                title = { Text(stringResource(R.string.adding_element)) },
                text = {
                    Column {
                        if (words.any { p -> p.word == newWord }) {
                            Text(stringResource(R.string.word_exist), color = Color.Red)
                        }
                        TextField(
                            value = newWord,
                            onValueChange = { newWord = it },
                            label = {
                                Text(
                                    stringResource(R.string.new_word)
                                )
                            },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newWord.isNotBlank() && words.none { p -> p.word == newWord }) {
                            viewModel.saveAll(Word(word = newWord))
                            viewModel.dialogStateObj.update(false)
                        }
                    }) {
                        Text(text = stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        viewModel.dialogStateObj.update(false)
                    }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Column {
                /*TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { s ->
                        text = s
                        //updateFoundElements(selectedWords.value, text)
                    },
                    label = { Text(stringResource(R.string.search)) })*/

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                    onClick = { viewModel.dialogStateObj.update(true) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add)
                    )
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    //items(words.value.size, key = { it }) { index ->
                    words.forEach { word ->
                        WordCard(
                            modifier = Modifier
                                .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                                .wrapContentSize()
                                .combinedClickable(
                                    onClick = {
                                        if (selectedForDelete.isNotEmpty()) {
                                            if (word !in selectedForDelete)
                                                viewModel.deleteWordsObj += word
                                            else
                                                viewModel.deleteWordsObj -= word

                                            viewModel.vibrate()
                                        } else {
                                            viewModel.updateWordState(
                                                word = word,
                                                newState = word !in selectedWords
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (selectedForDelete.isEmpty()) {
                                            viewModel.deleteWordsObj += word
                                            viewModel.onBackPressedCallback.isEnabled = true
                                            viewModel.vibrate()
                                        }
                                    }
                                ),
                            wordObj = word,
                            selected = word in selectedWords,
                            selectedForDelete = word in selectedForDelete)
                    }

                    if (words.isEmpty()) {

                        val rowModifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)

                        Column(
                            Modifier
                                .padding(8.dp)
                        ) {
                            Row(
                                rowModifier,
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Для добавления элементов нажмите значёк - ")
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Значёт добавить")
                            }
                            Row(
                                rowModifier,
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WordCard(wordObj = Word(word = "Пример"), selected = false, selectedForDelete = false)
                                Text(text = " - так выгладит элемент")
                            }
                            Row(
                                rowModifier,
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = "Чтобы элемент учитывался при вводе текста, нажмите на него"
                                )
                                WordCard(wordObj = Word(word = "Пример"), selected = true, selectedForDelete = false)
                            }
                            Row(
                                rowModifier,
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = "Чтоб выделить элемент для удаления - удерживайте"
                                )
                                WordCard(wordObj = Word(word = "Пример"), selected = false, selectedForDelete = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCard(
    modifier: Modifier = Modifier,
    wordObj: Word,
    selected: Boolean,
    selectedForDelete: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.run {
            if (selectedForDelete) {
                cardColors(
                    containerColor = ItemRed
                )
            } else if (selected) {
                cardColors(
                    containerColor = ItemGreen
                )
            } else cardColors()
        }

    ) {
        // Тело карточки
        Row {
            Text(
                wordObj.word,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}


/*
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun BlackListView(
    viewModel: WordViewModel,
    onBackPressedDispatcher: OnBackPressedDispatcher
) {
    val words by viewModel.words.collectAsState(initial = emptyList())
    val selectedWords by viewModel.selectedWords.collectAsState(initial = emptyList())
    val isDialog by viewModel.dialogStateObj.isDialogStateFlow.collectAsState()
    val selectedForDelete by viewModel.deleteWordsObj.stateFlowInstant.collectAsState()

    //viewModel.updateFoundation(selectedWords, text) // нужен был для прошлой версии приложения
    onBackPressedDispatcher.addCallback(LocalLifecycleOwner.current, viewModel.onBackPressedCallback)

    //диалог добавления слова
    if (isDialog) {
        var newWord by rememberSaveable {
            mutableStateOf("")
        }

        AlertDialog(
            onDismissRequest = { viewModel.dialogStateObj.update(false) },
            title = { Text(stringResource(R.string.adding_element)) },
            text = {
                Column {
                    if (words.any { p -> p.word == newWord }) {
                        Text(stringResource(R.string.word_exist), color = Color.Red)
                    }
                    TextField(
                        value = newWord,
                        onValueChange = { newWord = it },
                        label = {
                            Text(
                                stringResource(R.string.new_word)
                            )
                        },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newWord.isNotBlank() && words.none { p -> p.word == newWord }) {
                        viewModel.saveAll(Word(word = newWord))
                        viewModel.dialogStateObj.update(false)
                    }
                }) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = {
                    viewModel.dialogStateObj.update(false)
                }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        */
/*topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.black_list)
                    )
                },
                actions = {
                    if (selectedForDelete.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.deleteWordsObj.update(emptyList())
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.cancel)
                            )
                        }

                        IconButton(onClick = {
                            viewModel.deleteWordsObj.update(words)
                        }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = stringResource(id = R.string.select_all),
                            )
                        }
                        IconButton(onClick = {
                            viewModel.delete(*selectedForDelete.toTypedArray())

                            viewModel.deleteWordsObj.update(emptyList())
                            //updateFoundElements(selectedWords.value, text)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_selected)
                            )
                        }
                    }
                }
            )
        }*//*

    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding), color = MaterialTheme.colorScheme.background
        ) {
            Column {
                */
/*TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { s ->
                        text = s
                        //updateFoundElements(selectedWords.value, text)
                    },
                    label = { Text(stringResource(R.string.search)) })*//*


                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                    onClick = { viewModel.dialogStateObj.update(true) }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add)
                    )
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    //items(words.value.size, key = { it }) { index ->
                    words.forEach { word ->
                        WordCard(
                            modifier = Modifier
                                .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                                .wrapContentSize()
                                .combinedClickable(
                                    onClick = {
                                        if (selectedForDelete.isNotEmpty()) {
                                            if (word !in selectedForDelete)
                                                viewModel.deleteWordsObj += word
                                            else
                                                viewModel.deleteWordsObj -= word

                                            viewModel.vibrate()
                                        } else {
                                            viewModel.updateWordState(
                                                word = word,
                                                newState = word !in selectedWords
                                            )

                                            //updateFoundElements(selectedWords.value, text)
                                        }
                                    },
                                    onLongClick = {
                                        if (selectedForDelete.isEmpty()) {
                                            viewModel.deleteWordsObj += word
                                            viewModel.onBackPressedCallback.isEnabled = true
                                            viewModel.vibrate()
                                        }
                                    }
                                ),
                            wordObj = word,
                            selected = word in selectedWords,
                            selectedForDelete = word in selectedForDelete)
                    }

                    if (words.isEmpty()) {
                        Column {
                            Row {
                                //TODO: Доделать пример при пустом списке
                            }
                        }
                    }
                }
            }
        }
    }
}
*/
