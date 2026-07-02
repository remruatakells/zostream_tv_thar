package com.buannel.studio.pvt.ltd.zostream.ui.screens.catalog

import android.util.Log
import androidx.lifecycle.ViewModel
import com.buannel.studio.pvt.ltd.zostream.data.Category
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.repository.HomeRepository
import com.buannel.studio.pvt.ltd.zostream.response.BannerResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class CatalogBrowserViewModel @Inject constructor() : ViewModel() {

    // ✅ Full category list from API
    private val _categoryList = MutableStateFlow<List<Category>>(emptyList())
    val categoryList: StateFlow<List<Category>> = _categoryList

    // ✅ Carousel list (first category OR "featured")
    private val _featuredMovieList = MutableStateFlow<List<Movie>>(emptyList())
    val featuredMovieList: StateFlow<List<Movie>> = _featuredMovieList

    private val _bannerList = MutableStateFlow<List<BannerResponse.Banner>>(emptyList())
    val bannerList: StateFlow<List<BannerResponse.Banner>> = _bannerList

    fun loadBannersFromApi() {
        val repository = HomeRepository()

        repository.getBanners(
            object : HomeRepository.BannerCallback {
                override fun onSuccess(data: MutableList<BannerResponse.Banner>) {
                    _bannerList.value = data
                        .filter { it.isActive }
                        .sortedBy { it.priority }

                    Log.d("API_DEBUG", "Banner size: ${_bannerList.value.size}")
                }

                override fun onError(message: String) {
                    Log.e("API_ERROR", message)
                }
            }
        )
    }

    // ✅ API call
    fun loadHomeFromApi(
        token: String,
        xmode: String,
        userId: String,
        ageRestriction: Boolean = false
    ) {

        Log.d("API_DEBUG", "API CALL STARTED")

        val repository = HomeRepository()

        repository.getHomeSections(
            token,
            xmode,
            ageRestriction,
            userId,
            object : HomeRepository.HomeCallback {

                override fun onSuccess(data: Map<String, List<Movie>>) {

                    Log.d("API_DEBUG", "Keys: ${data.keys}")

                    // ✅ Convert Map → List<Category>
                    val categories = data.map { entry ->
                        Category(
                            name = entry.key,
                            movieList = entry.value
                        )
                    }

                    _categoryList.value = categories

                    // ✅ Set carousel list
                    val featured = data["featured"]
                        ?: data.values.firstOrNull()
                        ?: emptyList()

                    _featuredMovieList.value = featured

                    Log.d("API_DEBUG", "Category size: ${categories.size}")
                    Log.d("API_DEBUG", "Featured size: ${featured.size}")
                }

                override fun onError(message: String) {
                    Log.e("API_ERROR", message)
                }
            }
        )
    }
}
