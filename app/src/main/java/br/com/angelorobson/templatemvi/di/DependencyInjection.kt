package br.com.angelorobson.templatemvi.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import br.com.angelorobson.templatemvi.R
import br.com.angelorobson.templatemvi.model.database.ApplicationDatabase
import br.com.angelorobson.templatemvi.model.services.BannerService
import br.com.angelorobson.templatemvi.model.services.PurchaseService
import br.com.angelorobson.templatemvi.model.services.SpotlightService
import br.com.angelorobson.templatemvi.view.gamedetail.GameDetailViewModel
import br.com.angelorobson.templatemvi.view.home.HomeViewModel
import br.com.angelorobson.templatemvi.view.searchgame.SearchGameViewModel
import br.com.angelorobson.templatemvi.view.shoppingcart.ShoppingCartViewModel
import br.com.angelorobson.templatemvi.view.utils.ActivityService
import br.com.angelorobson.templatemvi.view.utils.IdlingResource
import br.com.angelorobson.templatemvi.view.utils.Navigator
import br.com.angelorobson.templatemvi.view.utils.customDateAdapter
import com.squareup.moshi.Moshi
import dagger.*
import dagger.multibindings.IntoMap
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.reflect.KClass


@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
internal annotation class ViewModelKey(val value: KClass<out ViewModel>)

@MustBeDocumented
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ApiBaseUrl

interface ApplicationComponent {

    fun viewModelFactory(): ViewModelProvider.Factory

    fun activityService(): ActivityService

}

@Singleton
@Component(modules = [ApplicationModule::class, ViewModelModule::class, ApiModule::class, DatabaseModule::class, RealModule::class])
interface RealComponent : ApplicationComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): RealComponent
    }
}

@Module
object ApplicationModule {

    @Provides
    @Singleton
    @JvmStatic
    fun viewModels(viewModels: MutableMap<Class<out ViewModel>, Provider<ViewModel>>):
            ViewModelProvider.Factory {
        return ViewModelFactory(viewModels)
    }

    @Provides
    @Singleton
    @JvmStatic
    fun activityService(): ActivityService = ActivityService()

    @Provides
    @Singleton
    @JvmStatic
    fun navigator(activityService: ActivityService): Navigator {
        return Navigator(R.id.navigationHostFragment, activityService)
    }

    @Provides
    @ApiBaseUrl
    @JvmStatic
    fun apiBaseUrl(context: Context): String = context.getString(R.string.api_base_url)
}

@Module
abstract class ViewModelModule {


    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun homeViewModel(viewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GameDetailViewModel::class)
    abstract fun gameDetailViewModel(viewModel: GameDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchGameViewModel::class)
    abstract fun searchGameViewModel(viewModel: SearchGameViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ShoppingCartViewModel::class)
    abstract fun shoppingCartViewModel(viewModel: ShoppingCartViewModel): ViewModel
}


@Module
object ApiModule {

    @Provides
    @Singleton
    @JvmStatic
    fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
    }

    @Provides
    @Singleton
    @JvmStatic
    fun moshi(): Moshi = Moshi.Builder().add(customDateAdapter).build()

    @Provides
    @Singleton
    @JvmStatic
    fun retrofit(@ApiBaseUrl apiBaseUrl: String, okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(okHttpClient)
                .build()
    }

    @Provides
    @Singleton
    @JvmStatic
    fun bannerService(retrofit: Retrofit): BannerService {
        return retrofit.create(BannerService::class.java)
    }

    @Provides
    @Singleton
    @JvmStatic
    fun spotlightService(retrofit: Retrofit): SpotlightService {
        return retrofit.create(SpotlightService::class.java)
    }

    @Provides
    @Singleton
    @JvmStatic
    fun purchaseService(retrofit: Retrofit): PurchaseService {
        return retrofit.create(PurchaseService::class.java)
    }

}

@Module
object DatabaseModule {

    @Provides
    @Singleton
    @JvmStatic
    fun applicationDatabase(context: Context): ApplicationDatabase {
        return Room.databaseBuilder(
                context,
                ApplicationDatabase::class.java,
                "cr_challenge_database"
        )
                .build()
    }

}


@Module
object RealModule {

    @Provides
    @Singleton
    @JvmStatic
    fun idlingResource(): IdlingResource = object : IdlingResource {
        override fun increment() {}
        override fun decrement() {}
    }

    @Provides
    @Singleton
    @JvmStatic
    fun shoppingCartDao(database: ApplicationDatabase) = database.shoppingCartDao()
}
