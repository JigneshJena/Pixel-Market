package com.pixelmarket.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.FirebaseDatabase
import com.pixelmarket.app.data.repository.AssetRepositoryImpl
import com.pixelmarket.app.domain.repository.AssetRepository
import com.pixelmarket.app.data.repository.AuthRepositoryImpl
import com.pixelmarket.app.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepositoryImpl(auth, firestore)

    @Provides
    @Singleton
    fun provideAssetRepository(
        firestore: FirebaseFirestore,
        database: FirebaseDatabase
    ): AssetRepository = AssetRepositoryImpl(firestore, database)
    
    @Provides
    @Singleton
    fun provideWalletRepository(
        firestore: FirebaseFirestore,
        database: FirebaseDatabase
    ): com.pixelmarket.app.domain.repository.WalletRepository = 
        com.pixelmarket.app.data.repository.WalletRepositoryImpl(firestore, database)
    
    @Provides
    @Singleton
    fun provideDeveloperRepository(
        firestore: FirebaseFirestore
    ): com.pixelmarket.app.domain.repository.DeveloperRepository = 
        com.pixelmarket.app.data.repository.DeveloperRepositoryImpl(firestore)
    
    @Provides
    @Singleton
    fun provideSalesRepository(
        firestore: FirebaseFirestore
    ): com.pixelmarket.app.domain.repository.SalesRepository = 
        com.pixelmarket.app.data.repository.SalesRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideCartRepository(
        firestore: FirebaseFirestore
    ): com.pixelmarket.app.domain.repository.CartRepository =
        com.pixelmarket.app.data.repository.CartRepositoryImpl(firestore)
}
