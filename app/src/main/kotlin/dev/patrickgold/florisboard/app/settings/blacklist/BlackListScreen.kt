package dev.patrickgold.florisboard.app.settings.blacklist

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.Greeting
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.FlorisAppActivity
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.apptheme.FlorisAppTheme
import dev.patrickgold.florisboard.app.apptheme.ItemGreen
import dev.patrickgold.florisboard.app.apptheme.ItemRed
import dev.patrickgold.florisboard.app.apptheme.MyApplicationTheme
import dev.patrickgold.florisboard.app.settings.blacklist.room.CONSTANTS.NOTIFICATION_ID
import dev.patrickgold.florisboard.app.settings.blacklist.room.Word
import dev.patrickgold.florisboard.app.settings.blacklist.room.WordViewModel
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.ui.theme.FlorisBoardTheme


private lateinit var onBackPressedCallback: OnBackPressedCallback

@AndroidEntryPoint
class BlackListActivity : ComponentActivity() {

    private val viewModel: WordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlorisBoardTheme {
                // A surface container using the 'background' color from the theme
                MainView()
            }
        }
    }

    @Composable
    fun BlackListScreen(
        viewModel: WordViewModel
    ) = FlorisScreen {

        title = "123"

        content {
            MainView()
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

    @OptIn(
        ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
        ExperimentalFoundationApi::class
    )
    @Composable
    private fun MainView() {
        var text by rememberSaveable { mutableStateOf("") }

        val words = viewModel.words.collectAsState(initial = emptyList())
        val selectedWords =
            viewModel.selectedWords.collectAsState(initial = emptyList())//words.value.filter { word -> word.isSelected }
        val isDialog = viewModel.dialogStateObj.isDialogStateFlow.collectAsState()
        val selectedForDelete = viewModel.deleteWordsObj.stateFlowInstant.collectAsState()

        viewModel.updateFoudation(selectedWords.value, text)


        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.deleteWordsObj.update(emptyList())
                isEnabled = false
            }
        }

        //TODO: onBackPressedDispatcher.addCallback(LocalLifecycleOwner.current, onBackPressedCallback)

        //диалог добавления слова
        if (isDialog.value) {
            var newWord by rememberSaveable {
                mutableStateOf("")
            }

            AlertDialog(onDismissRequest = { viewModel.dialogStateObj.update(false) },
                title = { Text(stringResource(R.string.adding_element)) },
                text = {
                    Column {
                        if (words.value.any { p -> p.word == newWord }) {
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
                        if (newWord.isNotBlank() && words.value.none { p -> p.word == newWord }) {
                            viewModel.save(Word(word = newWord))
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
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name)
                        )
                    },
                    actions = {
                        if (selectedForDelete.value.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.deleteWordsObj.update(emptyList())
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.cancel)
                                )
                            }

                            IconButton(onClick = {
                                viewModel.deleteWordsObj.update(words.value)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = stringResource(id = R.string.select_all),
                                )
                            }
                            IconButton(onClick = {
                                viewModel.delete(*selectedForDelete.value.toTypedArray())

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
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding), color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = text,
                        onValueChange = { s ->
                            text = s
                            //updateFoundElements(selectedWords.value, text)
                        },
                        label = { Text(stringResource(R.string.search)) })

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
                        words.value.forEachIndexed { index, word ->
                            WordCard(
                                modifier = Modifier
                                    .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                                    .wrapContentSize()
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedForDelete.value.isNotEmpty()) {
                                                if (word !in selectedForDelete.value)
                                                    viewModel.deleteWordsObj += word
                                                else
                                                    viewModel.deleteWordsObj -= word

                                                viewModel.vibrate()
                                            } else {
                                                viewModel.updateWordState(
                                                    word = word,
                                                    newState = word !in selectedWords.value
                                                )

                                                //updateFoundElements(selectedWords.value, text)
                                            }
                                        },
                                        onLongClick = {
                                            if (selectedForDelete.value.isEmpty()) {
                                                viewModel.deleteWordsObj += word
                                                onBackPressedCallback.isEnabled = true
                                                viewModel.vibrate()
                                            }
                                        }
                                    ),
                                wordObj = word,
                                selected = word in selectedWords.value,
                                selectedForDelete = word in selectedForDelete.value)
                        }
                    }
                }
            }
        }
    }

    //вибрация
    private fun vibrate(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(50, 255))
    }


    @Composable
    private fun WordCard(
        modifier: Modifier,
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
}
