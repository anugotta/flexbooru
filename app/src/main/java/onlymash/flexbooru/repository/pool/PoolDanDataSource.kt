package onlymash.flexbooru.repository.pool

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import onlymash.flexbooru.api.ApiUrlHelper
import onlymash.flexbooru.api.DanbooruApi
import onlymash.flexbooru.entity.PoolDan
import onlymash.flexbooru.entity.Search
import onlymash.flexbooru.repository.NetworkState
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.Executor

class PoolDanDataSource(private val danbooruApi: DanbooruApi,
                        private val search: Search,
                        private val retryExecutor: Executor) : PageKeyedDataSource<Int, PoolDan>() {

    // keep a function reference for the retry event
    private var retry: (() -> Any)? = null

    /**
     * There is no sync on the state because paging will always call loadInitial first then wait
     * for it to return some success value before calling loadAfter.
     */
    val networkState = MutableLiveData<NetworkState>()

    val initialLoad = MutableLiveData<NetworkState>()

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }


    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, PoolDan>) {
        val request = danbooruApi.getPools(ApiUrlHelper.getDanPoolUrl(search = search, page = 1))
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)
        val host = search.host
        val keyword = search.keyword
        try {
            val response =  request.execute()
            val data = response.body() ?: mutableListOf()
            retry = null
            networkState.postValue(NetworkState.LOADED)
            initialLoad.postValue(NetworkState.LOADED)
            callback.onResult(data, null, 2)
        } catch (ioException: IOException) {
            retry = {
                loadInitial(params, callback)
            }
            val error = NetworkState.error(ioException.message ?: "unknown error")
            networkState.postValue(error)
            initialLoad.postValue(error)
        }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, PoolDan>) {
        networkState.postValue(NetworkState.LOADING)
        val page = params.key
        danbooruApi.getPools(ApiUrlHelper.getDanPoolUrl(search = search, page = page))
            .enqueue(object : retrofit2.Callback<MutableList<PoolDan>> {
                override fun onFailure(call: Call<MutableList<PoolDan>>, t: Throwable) {
                    retry = {
                        loadAfter(params, callback)
                    }
                    networkState.postValue(NetworkState.error(t.message ?: "unknown err"))
                }
                override fun onResponse(call: Call<MutableList<PoolDan>>, response: Response<MutableList<PoolDan>>) {
                    if (response.isSuccessful) {
                        val data = response.body() ?: mutableListOf()
                        retry = null
                        callback.onResult(data, page + 1)
                    } else {
                        retry = {
                            loadAfter(params, callback)
                        }
                        networkState.postValue(NetworkState.error("error code: ${response.code()}"))
                    }
                }
            })
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, PoolDan>) {

    }
}