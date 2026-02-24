package com.pixelmarket.app.domain.repository

import com.pixelmarket.app.domain.model.User
import com.pixelmarket.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, password: String): Flow<Resource<User>>
    fun register(email: String, password: String, username: String): Flow<Resource<User>>
    fun googleLogin(idToken: String): Flow<Resource<User>>
    fun logout()
    fun getCurrentUser(): User?
    fun isUserLoggedIn(): Boolean
}
