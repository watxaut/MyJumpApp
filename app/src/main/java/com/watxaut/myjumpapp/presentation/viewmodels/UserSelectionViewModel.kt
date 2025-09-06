package com.watxaut.myjumpapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
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
        // Test database connectivity
        viewModelScope.launch {
            val dbTest = userRepository.testDatabaseConnection()
            Log.d("UserSelectionVM", "Database connectivity test: $dbTest")
        }
    }
    
    private fun loadUsers() {
        Log.d("UserSelectionVM", "Loading users...")
        viewModelScope.launch {
            try {
                userRepository.getAllActiveUsers()
                    .catch { exception ->
                        Log.e("UserSelectionVM", "Error loading users", exception)
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                error = exception.message ?: "Unknown error occurred"
                            )
                        }
                    }
                    .collect { users ->
                        Log.d("UserSelectionVM", "Loaded ${users.size} users: ${users.map { it.userName }}")
                        _uiState.update { currentState ->
                            currentState.copy(
                                users = users,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (exception: Exception) {
                Log.e("UserSelectionVM", "Exception loading users", exception)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
    
    fun createUser(name: String, height: Int?, weight: Double?, eyeToHeadVertexCm: Double?, heelToHandReachCm: Double?) {
        Log.d("UserSelectionVM", "Creating user: $name, height: $height, weight: $weight, eyeToHead: $eyeToHeadVertexCm, heelToHand: $heelToHandReachCm")
        viewModelScope.launch {
            try {
                val user = User(
                    userName = name.trim(),
                    heightCm = height,
                    weightKg = weight,
                    eyeToHeadVertexCm = eyeToHeadVertexCm,
                    heelToHandReachCm = heelToHandReachCm
                )
                Log.d("UserSelectionVM", "User object created: $user")
                userRepository.insertUser(user)
                Log.d("UserSelectionVM", "User inserted successfully")
                // Users list will be automatically updated via Flow
            } catch (exception: Exception) {
                Log.e("UserSelectionVM", "Failed to create user", exception)
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