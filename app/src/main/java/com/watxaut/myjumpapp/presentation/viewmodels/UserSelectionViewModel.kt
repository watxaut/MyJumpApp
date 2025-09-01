package com.watxaut.myjumpapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserSelectionUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class UserSelectionViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserSelectionUiState())
    val uiState: StateFlow<UserSelectionUiState> = _uiState.asStateFlow()
    
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
                                error = null
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
    
    fun createUser(name: String, height: Int?, weight: Double?) {
        viewModelScope.launch {
            try {
                val user = User(
                    userName = name.trim(),
                    heightCm = height,
                    weightKg = weight
                )
                userRepository.insertUser(user)
                // Users list will be automatically updated via Flow
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = exception.message ?: "Failed to create user"
                    )
                }
            }
        }
    }
    
    fun deleteUser(user: User) {
        viewModelScope.launch {
            try {
                userRepository.deactivateUser(user.userId)
                // Users list will be automatically updated via Flow
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = exception.message ?: "Failed to delete user"
                    )
                }
            }
        }
    }
    
    fun selectUser(userId: String) {
        // This could be used to navigate or store selection
        // For now, it's handled by the calling screen
    }
}