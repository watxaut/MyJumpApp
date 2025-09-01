package com.watxaut.myjumpapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val users: List<User> = emptyList(),
    val selectedUserId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository
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
                            )
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
    }
    
    fun refreshUsers() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadUsers()
    }
}