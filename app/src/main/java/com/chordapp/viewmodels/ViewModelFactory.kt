package com.chordapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chordapp.about.AboutCacheStore
import com.chordapp.about.AboutRepository
import com.chordapp.about.HttpAboutRemoteSource
import com.chordapp.about.bundledAboutText
import com.chordapp.about.runtimeAppMetadata
import com.chordapp.catalog.CatalogDatabase
import com.chordapp.charts.ChartCacheStore
import com.chordapp.charts.ChartRepositoryImpl
import com.chordapp.charts.HttpChartRemoteSource
import com.chordapp.charts.SharedPreferencesChartPlatformStore
import com.chordapp.repositories.CatalogRepository
import java.io.File

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userCatalogRepo = com.chordapp.usercatalog.UserCatalogRepository(context)
        val repository = CatalogRepository(
            daoProvider = { CatalogDatabase.getDatabase(context).catalogDao() },
            userCatalogRepoProvider = { userCatalogRepo }
        )
        val chartRepository = ChartRepositoryImpl(
            remote = HttpChartRemoteSource(),
            cache = ChartCacheStore(File(context.filesDir, "charts"))
        )

        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val userDb = com.chordapp.userdata.UserdataDatabase.getDatabase(context)
            val userRepo = com.chordapp.repositories.UserdataRepository(userDb.userdataDao()) {
                CatalogDatabase.getDatabase(context).catalogDao()
            }
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                userRepo,
                repository,
                chartRepository,
                SharedPreferencesChartPlatformStore(context),
                catalogSizeBytes = {
                    context.getDatabasePath("catalog.db").takeIf(File::exists)?.length() ?: 0L
                }
            ) as T
        }
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExploreViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(AboutViewModel::class.java)) {
            val aboutRepository = AboutRepository(
                remote = HttpAboutRemoteSource(),
                cache = AboutCacheStore(File(context.filesDir, "about/about.txt")),
                fallback = bundledAboutText(context)
            )
            @Suppress("UNCHECKED_CAST")
            return AboutViewModel(runtimeAppMetadata(context), aboutRepository) as T
        }
        if (modelClass.isAssignableFrom(PromoViewModel::class.java)) {
            val promoRepository = com.chordapp.promo.PromoRepository(
                remote = com.chordapp.promo.HttpPromoRemoteSource(),
                cache = com.chordapp.promo.PromoCacheStore(File(context.filesDir, "promo/promo.json"))
            )
            @Suppress("UNCHECKED_CAST")
            return PromoViewModel(promoRepository) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context, repository) as T
        }
        if (modelClass.isAssignableFrom(ArtistDirectoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArtistDirectoryViewModel(repository) as T
        }
                if (modelClass.isAssignableFrom(SongVersionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongVersionsViewModel(repository) as T
        }
                if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            val userDb = com.chordapp.userdata.UserdataDatabase.getDatabase(context)
            val userRepo = com.chordapp.repositories.UserdataRepository(userDb.userdataDao()) {
                CatalogDatabase.getDatabase(context).catalogDao()
            }
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(
                repository,
                userRepo,
                com.chordapp.settings.PlayerPreferences(context)
            ) as T
        }
        if (modelClass.isAssignableFrom(SetlistsViewModel::class.java)) {
            val userDb = com.chordapp.userdata.UserdataDatabase.getDatabase(context)
            val userRepo = com.chordapp.repositories.UserdataRepository(userDb.userdataDao()) {
                CatalogDatabase.getDatabase(context).catalogDao()
            }
            @Suppress("UNCHECKED_CAST")
            return SetlistsViewModel(userRepo) as T
        }
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            val userDb = com.chordapp.userdata.UserdataDatabase.getDatabase(context)
            val userRepo = com.chordapp.repositories.UserdataRepository(userDb.userdataDao()) {
                CatalogDatabase.getDatabase(context).catalogDao()
            }
            @Suppress("UNCHECKED_CAST")
            return FavoritesViewModel(userRepo) as T
        }
        if (modelClass.isAssignableFrom(SetlistTracklistViewModel::class.java)) {
            val userDb = com.chordapp.userdata.UserdataDatabase.getDatabase(context)
            val userRepo = com.chordapp.repositories.UserdataRepository(userDb.userdataDao()) {
                CatalogDatabase.getDatabase(context).catalogDao()
            }
            @Suppress("UNCHECKED_CAST")
            return SetlistTracklistViewModel(userRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
