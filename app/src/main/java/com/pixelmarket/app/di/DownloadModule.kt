package com.pixelmarket.app.di

import android.content.Context
import com.pixelmarket.app.data.service.AssetDownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Provides
    @Singleton
    fun provideAssetDownloadManager(
        @ApplicationContext context: Context
    ): AssetDownloadManager {
        return AssetDownloadManager(context)
    }
}
