package com.attendance.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(studentId: String, password: String, onLoginSuccess: () -> Unit) {
        if (studentId.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Student ID and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.fetchAttendance(studentId, password)
            if (result.isSuccess) {
                _uiState.value = LoginUiState.Success
                onLoginSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed"
                _uiState.value = LoginUiState.Error(errorMsg)
            }
        }
    }
}
