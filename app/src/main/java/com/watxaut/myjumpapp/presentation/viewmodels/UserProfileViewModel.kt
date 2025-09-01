package com.watxaut.myjumpapp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watxaut.myjumpapp.data.database.entities.JumpSession
import com.watxaut.myjumpapp.data.database.entities.User
import com.watxaut.myjumpapp.data.repository.JumpSessionRepository
import com.watxaut.myjumpapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val user: User? = null,
    val recentSessions: List<JumpSession> = emptyList(),
    val totalSessions: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val jumpSessionRepository: JumpSessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()
    
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load user data
                val user = userRepository.getUserById(userId)
                if (user == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "User not found"
                        ) 
                    }
                    return@launch
                }
                
                // Load session statistics - get all sessions for this user and count them
                val allSessionsFlow = jumpSessionRepository.getSessionsByUserId(userId)
                val allSessions = mutableListOf<JumpSession>()
                allSessionsFlow.collect { sessions ->
                    allSessions.clear()
                    allSessions.addAll(sessions)
                }
                
                val recentSessions = allSessions.take(5)
                val totalSessions = allSessions.size
                
                _uiState.update {
                    it.copy(
                        user = user,
                        recentSessions = recentSessions,
                        totalSessions = totalSessions,
                        isLoading = false,
                        error = null
                    )
                }
                
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user profile"
                    )
                }
            }
        }
    }
    
    fun refreshProfile() {
        val currentUser = _uiState.value.user
        if (currentUser != null) {
            loadUserProfile(currentUser.userId)
        }
    }
}