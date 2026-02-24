package com.pixelmarket.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelmarket.app.domain.model.User
import com.pixelmarket.app.domain.repository.AuthRepository
import com.pixelmarket.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>?>(null)
    val authState: StateFlow<Resource<User>?> = _authState

    fun login(email: String, password: String) {
        repository.login(email, password).onEach { result ->
            _authState.value = result
        }.launchIn(viewModelScope)
    }

    fun register(email: String, password: String, username: String) {
        repository.register(email, password, username).onEach { result ->
            _authState.value = result
        }.launchIn(viewModelScope)
    }

    fun googleLogin(idToken: String) {
        repository.googleLogin(idToken).onEach { result ->
            _authState.value = result
        }.launchIn(viewModelScope)
    }

    private val _isUserLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn

    fun resetAuthState() {
        _authState.value = null
    }
}
