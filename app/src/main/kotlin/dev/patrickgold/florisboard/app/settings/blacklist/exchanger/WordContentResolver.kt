package dev.patrickgold.florisboard.app.settings.blacklist.exchanger

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import dev.patrickgold.florisboard.app.settings.blacklist.room.Word

class WordContentResolver(private var mContext: Context){

    companion object{
        private const val TABLE_NAME = "words"
        private const val CONTENT_AUTHORITY = "ru.gfastg98.myapplication.provider.StorageProvider"
        const val URL = "content://$CONTENT_AUTHORITY/$TABLE_NAME"
        val CONTENT_URI: Uri = Uri.parse(URL)
        private val TAG: String = WordContentResolver::class.java.simpleName
    }

    private var contentResolver: ContentResolver = mContext.contentResolver

    val allWordsRecords: List<Word>?
        get() {
            val words: ArrayList<Word> = arrayListOf()
            val projection = arrayOf("id", "word", "isSelected")
            val cursor : Cursor? = contentResolver.query(CONTENT_URI, projection, null, null, null)

            if (cursor != null) {
                if (cursor.count > 0) {
                    while (cursor.moveToNext()) {
                        words.add(
                            Word(
                                cursor.getInt(0),
                                cursor.getString(1),
                                cursor.getInt(2) > 0
                            )
                        )
                    }
                }
                cursor.close()
            } else {
                return null
            }
            return words
        }

    fun getWordById(id: Int): Word?{
        var word : Word? = null
        val contentResolver = mContext.contentResolver
        val uri: Uri = CONTENT_URI
        val projection = arrayOf("id", "word", "isSelected")

        val cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                word = Word(cursor.getInt(0), cursor.getString(1), cursor.getString(2).toBoolean())
            }
            cursor.close()
        }

        return word
    }

    fun insertWord(word: Word) {
        val contentValues = ContentValues()
        contentValues.put("id", word.id)
        contentValues.put("word", word.word)
        contentValues.put("isSelected", word.isSelected)
        try {
            contentResolver.insert(CONTENT_URI, contentValues)
        }catch (e : IllegalArgumentException){
            Log.e(TAG, "insertWord: Provider not found")
        }
    }

    fun deleteAll(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                contentResolver.query( Uri.parse("$URL/deleteAll"), null,null,null,null)?.close()
            } catch (e: Exception) {
                Log.e(TAG, "insertWord: Provider not found")
            }
        }
    }
}
