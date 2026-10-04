package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NoteEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEntity>> = _searchQuery.flatMapLatest { query ->
        repository.searchNotes(query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveNote(id: Long, title: String, content: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (title.isNotBlank() || content.isNotBlank()) {
                val effectiveTitle = if (title.isBlank()) "Başlıksız Not" else title.trim()
                repository.saveNote(id, effectiveTitle, content.trim())
                onComplete()
            }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun getExportText(note: NoteEntity): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date(note.updatedAt))
        return "${note.title}\n" +
                "Tarih: $dateStr\n" +
                "----------------------------------------\n\n" +
                note.content
    }

    fun getAllNotesExportText(notesList: List<NoteEntity>): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        val sb = StringBuilder()
        sb.append("=== GÜNDEM PORTALI - TÜM NOTLAR ===\n\n")
        notesList.forEachIndexed { index, note ->
            sb.append("${index + 1}. NOT: ${note.title}\n")
            sb.append("Tarih: ${dateFormat.format(Date(note.updatedAt))}\n")
            sb.append("----------------------------------------\n")
            sb.append(note.content)
            sb.append("\n\n========================================\n\n")
        }
        return sb.toString()
    }

    fun importNotesFromText(text: String, onImported: (Int) -> Unit) {
        viewModelScope.launch {
            val lines = text.lines()
            val imported = mutableListOf<NoteEntity>()
            var currentTitle = ""
            val currentContent = StringBuilder()

            for (line in lines) {
                if (line.startsWith("=== ") || line.startsWith("---") || line.startsWith("Tarih:")) {
                    continue
                }
                if (line.matches(Regex("^\\d+\\.\\s*NOT:.*")) || (line.startsWith("# ") && currentTitle.isNotEmpty())) {
                    if (currentTitle.isNotBlank() || currentContent.isNotBlank()) {
                        imported.add(
                            NoteEntity(
                                title = if (currentTitle.isBlank()) "İçe Aktarılan Not" else currentTitle,
                                content = currentContent.toString().trim(),
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        currentContent.clear()
                    }
                    currentTitle = line.replace(Regex("^\\d+\\.\\s*NOT:\\s*"), "").trim()
                } else {
                    if (currentTitle.isBlank() && line.isNotBlank()) {
                        currentTitle = line.trim()
                    } else {
                        currentContent.append(line).append("\n")
                    }
                }
            }

            if (currentTitle.isNotBlank() || currentContent.isNotBlank()) {
                imported.add(
                    NoteEntity(
                        title = if (currentTitle.isBlank()) "İçe Aktarılan Not" else currentTitle,
                        content = currentContent.toString().trim(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            if (imported.isNotEmpty()) {
                repository.importNotes(imported)
                onImported(imported.size)
            } else {
                onImported(0)
            }
        }
    }
}
