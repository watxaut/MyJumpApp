package com.watxaut.myjumpapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.data.repository.UserRepository
import com.watxaut.myjumpapp.utils.SecureLogger
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
            SecureLogger.d("UserSelectionVM", "Database connectivity test completed")
        }
    }
    
    private fun loadUsers() {
        SecureLogger.d("UserSelectionVM", "Loading users...")
        viewModelScope.launch {
            try {
                userRepository.getAllActiveUsers()
                    .catch { exception ->
                        SecureLogger.e("UserSelectionVM", "Error loading users", exception)
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                error = exception.message ?: "Unknown error occurred"
                            )
                        }
                    }
                    .collect { users ->
                        SecureLogger.d("UserSelectionVM", "Loaded ${users.size} users")
                        _uiState.update { currentState ->
                            currentState.copy(
                                users = users,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (exception: Exception) {
                SecureLogger.e("UserSelectionVM", "Exception loading users", exception)
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
        SecureLogger.d("UserSelectionVM", "Creating user: ${SecureLogger.redactSensitiveData(name)}, height: ${SecureLogger.redactMeasurement(height)}, weight: ${SecureLogger.redactMeasurement(weight)}, eyeToHead: ${SecureLogger.redactMeasurement(eyeToHeadVertexCm)}, heelToHand: ${SecureLogger.redactMeasurement(heelToHandReachCm)}")
        
        // Input validation
        try {
            validateUserInput(name, height, weight, eyeToHeadVertexCm, heelToHandReachCm)
        } catch (e: IllegalArgumentException) {
            _uiState.update { currentState ->
                currentState.copy(error = e.message)
            }
            return
        }
        
        viewModelScope.launch {
            try {
                val user = User(
                    userName = name.trim(),
                    heightCm = height,
                    weightKg = weight,
                    eyeToHeadVertexCm = eyeToHeadVertexCm,
                    heelToHandReachCm = heelToHandReachCm
                )
                SecureLogger.d("UserSelectionVM", "User object created successfully")
                userRepository.insertUser(user)
                SecureLogger.d("UserSelectionVM", "User inserted successfully")
                // Users list will be automatically updated via Flow
            } catch (exception: Exception) {
                SecureLogger.e("UserSelectionVM", "Failed to create user", exception)
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
    
    private fun validateUserInput(
        name: String,
        height: Int?,
        weight: Double?,
        eyeToHeadVertexCm: Double?,
        heelToHandReachCm: Double?
    ) {
        // Validate name
        if (name.isBlank()) {
            throw IllegalArgumentException("Name cannot be empty")
        }
        if (name.length > 50) {
            throw IllegalArgumentException("Name must be 50 characters or less")
        }
        
        // Validate height (reasonable human range)
        height?.let { 
            if (it < 100 || it > 250) {
                throw IllegalArgumentException("Height must be between 100-250 cm")
            }
        }
        
        // Validate weight (reasonable human range)
        weight?.let {
            if (it < 20.0 || it > 500.0) {
                throw IllegalArgumentException("Weight must be between 20-500 kg")
            }
        }
        
        // Validate biometric measurements
        eyeToHeadVertexCm?.let {
            if (it < 0 || it > 30) {
                throw IllegalArgumentException("Eye-to-head distance must be between 0-30 cm")
            }
        }
        
        heelToHandReachCm?.let {
            if (it < 150 || it > 300) {
                throw IllegalArgumentException("Heel-to-hand reach must be between 150-300 cm")
            }
        }
    }
}