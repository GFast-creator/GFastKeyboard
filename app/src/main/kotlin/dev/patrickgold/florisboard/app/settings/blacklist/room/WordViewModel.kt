package dev.patrickgold.florisboard.app.settings.blacklist.room

import android.app.NotificationManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.patrickgold.florisboard.app.settings.blacklist.room.CONSTANTS.NOTIFICATION_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordViewModel @Inject constructor(room: AppDatabase) : ViewModel() {

    @Inject lateinit var notification: NotificationCompat.Builder
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var vibrator: Vibrator

    var onBackPressedCallback = object : OnBackPressedCallback(false){
        override fun handleOnBackPressed() {
            viewModelScope.launch {
                if (deleteWordsObj.stateFlowInstant.value.isNotEmpty()){
                    deleteWordsObj.update(emptyList())
                    isEnabled = false
                }
            }
        }
    }

    private val dao = room.wordDao

    val words = dao.getAll()

    val selectedWords = dao.getSelected()

    object DialogStateObject{
        private val _isDialog = MutableStateFlow(false)
        val isDialogStateFlow: StateFlow<Boolean>
            get() = _isDialog.asStateFlow()

        fun update(newState : Boolean){
            _isDialog.value = newState
        }
    }
    val dialogStateObj = DialogStateObject

    object DeleteWordsObj{
        private val _wordsForDelete = MutableStateFlow(emptyList<Word>())
        val stateFlowInstant : StateFlow<List<Word>>
            get() = _wordsForDelete.asStateFlow()

        operator fun plusAssign(w: Word) {
            _wordsForDelete.value += w
        }

        operator fun minusAssign(w: Word) {
            _wordsForDelete.value -= w
        }

        fun update(l : List<Word>){
            _wordsForDelete.value = l
        }
    }
    val deleteWordsObj = DeleteWordsObj

    fun save(word: Word){
        viewModelScope.launch { dao.insertAll(word) }
    }
    fun delete(vararg word: Word){
        viewModelScope.launch{
            dao.delete(*word)
        }
    }
    fun deleteAll(){
        viewModelScope.launch{ dao.deleteAll() }
    }

    fun updateWordState(word: Word, newState: Boolean){
        word.isSelected = newState
        viewModelScope.launch { dao.update(word) }
    }

    fun updateFoundation(selectedWords: List<Word>, text: String) {

        notificationManager.cancel(NOTIFICATION_ID)

        val founded = selectedWords.filter { p -> text.contains(p.word) }
        if (founded.isEmpty()) return

        notification.setContentText("Найденные элементы:\n"
                + founded.joinToString(separator = ",\n") {
                it.word
            }
        )

        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    fun vibrate(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(VibrationEffect.createOneShot(50, 255))
    }
}
