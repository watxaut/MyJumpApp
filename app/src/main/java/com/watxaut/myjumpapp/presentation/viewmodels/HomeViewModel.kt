package com.watxaut.myjumpapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.data.repository.UserRepository
import com.watxaut.myjumpapp.data.repository.JumpSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val users: List<User> = emptyList(),
    val selectedUserId: String? = null,
    val recentSessions: List<JumpSession> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val jumpSessionRepository: JumpSessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadUsers()
    }
    
    private fun loadUsers() {
        viewModelScope.launch {
            try {
                userRepository.getAllActiveUsers()
                    .catch { exception ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                error = exception.message ?: "Unknown error occurred"
                            )
                        }
                    }
                    .collect { users ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                users = users,
                                isLoading = false,
                                error = null,
                                selectedUserId = currentState.selectedUserId 
                                    ?: users.firstOrNull()?.userId
                            ).also {
                                // Load recent sessions for the selected user
                                it.selectedUserId?.let { userId ->
                                    loadRecentSessions(userId)
                                }
                            }
                        }
                    }
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
    
    fun selectUser(userId: String) {
        _uiState.update { currentState ->
            currentState.copy(selectedUserId = userId)
        }
        loadRecentSessions(userId)
    }
    
    fun refreshUsers() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadUsers()
    }
    
    private fun loadRecentSessions(userId: String) {
        viewModelScope.launch {
            try {
                val allSessions = jumpSessionRepository.getSessionsByUserIdList(userId)
                val recentSessions = allSessions
                    .filter { it.isCompleted }
                    .sortedByDescending { it.startTime }
                    .take(5)
                _uiState.update { currentState ->
                    currentState.copy(recentSessions = recentSessions)
                }
            } catch (exception: Exception) {
                // Handle error silently for now, keep existing recent sessions
            }
        }
    }
}