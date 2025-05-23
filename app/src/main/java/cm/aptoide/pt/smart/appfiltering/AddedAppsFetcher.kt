package cm.aptoide.pt.smart.appfiltering

import android.content.Context
import android.os.Build
import android.util.Log
import cm.aptoide.pt.dataprovider.model.smart.*
import cm.aptoide.pt.dataprovider.ws.smart.SMARTAppsAdd
import cm.aptoide.pt.store.view.my.SMARTStore
import okhttp3.OkHttpClient
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.util.Collections.emptyList
import com.fasterxml.jackson.databind.ObjectMapper

class AddedAppsFetcher(httpClient: OkHttpClient, private val context: Context) {
    private companion object {
        const val TAG = "AddedAppsFetcher"
        const val JSON = "added_apps.json"
    }

    private var subscription: Subscription? = null
    private val appsAddClient = SMARTAppsAdd(httpClient)
    private val emptyAppsList: List<AddedApp>
        get() = emptyList()

    fun populateFilteredAppsAsync() {

        subscription?.unsubscribe()
        // Only read from the online file (i.e. the appsAddClient.observe() call) for production builds.
        // Use the local asset (i.e. getLocalAddedAppsFromJson()) if you want to do some dev work, but don't
        // check it in. As it stands, we only have one list for added apps, so if we try to include the local
        // asset + the online file we'll wind up overwriting one with the other - the last file read will "win".
        // Trying to merge the two lists would also be problematic since we're likely to update the online
        // one more often, which would then be out of sync with the local asset, meaning that we could wind up
        // overwriting the newer file with this one.

        subscription = appsAddClient.observe()
        //subscription = getLocalAddedAppsFromJson()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap { dto -> dto.asEntity() }
                .subscribe({ list ->
                    Log.d(TAG, "Received $list")
                    populateList(list)
                }, { throwable ->
                    Log.e(TAG, "Exception has occurred!", throwable)
                })
    }

    fun unsubscribe() {
        subscription?.unsubscribe()
    }

    private fun populateList(list: List<AppToAdd>) {
        runCatching {
            AddingList.populateAddingList(list)
        }
    }

    private fun FilteredAppsDto?.asEntity() = if (this == null) {
        Observable.fromCallable { emptyAppsList }
    } else {
        Observable.from(stores)
                .filter { store -> store.storeName == SMARTStore.getStoreName(context) }
                .map { store ->
                    Observable.from(store.platforms)
                            .filter { platform -> Build.MODEL == platform.platform }
                            .map { platform -> platform.added }
                            .toBlocking().singleOrDefault(emptyAppsList)
                }
    }.map { list ->
        return@map list.map { AppToAdd(it.json, it.appType, it.appCategory, it.isIncludeInTop, it.isIncludeInLatest) }
    }

    private fun getLocalAddedAppsFromJson() = Observable.fromCallable { loadJsonFromAsset() }
            .subscribeOn(Schedulers.io())
            .map { ObjectMapper().readValue(it, FilteredAppsDto::class.java) }

    private fun loadJsonFromAsset() = context.assets.open(JSON)
            .runCatching {
                val size: Int = available()
                val buffer = ByteArray(size)
                read(buffer)
                close()
                return@runCatching String(buffer, Charsets.UTF_8)
            }.fold({
                return@fold it
            }, {
                Log.e(TAG, "Cannot load $JSON", it)
                return@fold ""
            })

}