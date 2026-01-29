package com.pixelmarket.app.di

import android.content.Context
import com.pixelmarket.app.data.remote.CloudinaryManager
import com.pixelmarket.app.data.repository.StorageRepositoryImpl
import com.pixelmarket.app.domain.repository.StorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideCloudinaryManager(@ApplicationContext context: Context): CloudinaryManager {
        return CloudinaryManager().apply {
            init(context)
        }
    }

    @Provides
    @Singleton
    fun provideStorageRepository(
        cloudinaryManager: CloudinaryManager
    ): StorageRepository {
        return StorageRepositoryImpl(cloudinaryManager)
    }
}
