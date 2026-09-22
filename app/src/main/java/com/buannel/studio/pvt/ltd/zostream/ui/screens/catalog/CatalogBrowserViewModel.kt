package com.buannel.studio.pvt.ltd.zostream.ui.screens.catalog

import android.util.Log
import androidx.lifecycle.ViewModel
import com.buannel.studio.pvt.ltd.zostream.data.Category
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.repository.HomeRepository
import com.buannel.studio.pvt.ltd.zostream.response.BannerResponse
import com.buannel.studio.pvt.ltd.zostream.response.HomeRecommendationResponse
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

    // The personalized endpoint supplies both AI shelves and live shelves in one response.
    fun loadHomeFromApi(
        token: String,
        xmode: String,
        userId: String,
        ageRestriction: Boolean = false
    ) {

        Log.d("API_DEBUG", "Recommendation home API call started")

        val repository = HomeRepository()

        repository.getRecommendationHome(
            token,
            xmode,
            ageRestriction,
            object : HomeRepository.RecommendationHomeCallback {

                override fun onSuccess(data: HomeRecommendationResponse) {
                    val layout = data.sectionOrder.takeIf { it.isNotEmpty() }
                        ?: RECOMMENDATION_SECTION_ORDER.mapIndexed { position, key ->
                            HomeRecommendationResponse.SectionDefinition().apply {
                                this.key = key
                                this.position = position
                            }
                        }
                    val categories = layout.mapNotNull { definition ->
                        val sectionId = definition.key ?: return@mapNotNull null
                        val section = data.getSection(sectionId) ?: return@mapNotNull null
                        val movies = section.publishedMovies()
                        if (movies.isEmpty()) return@mapNotNull null

                        Category(
                            id = sectionId,
                            name = definition.title
                                ?.takeIf { it.isNotBlank() }
                                ?: recommendationTitle(sectionId, section),
                            movieList = movies
                        )
                    }

                    if (categories.isEmpty()) {
                        loadLegacyHome(repository, token, xmode, ageRestriction, userId)
                        return
                    }

                    _categoryList.value = categories
                    _featuredMovieList.value = categories.first().movieList
                    Log.d("API_DEBUG", "Recommendation category size: ${categories.size}")
                }

                override fun onError(message: String) {
                    Log.w("API_ERROR", "Recommendation home unavailable; using catalog home: $message")
                    loadLegacyHome(repository, token, xmode, ageRestriction, userId)
                }
            }
        )
    }

    private fun loadLegacyHome(
        repository: HomeRepository,
        token: String,
        xmode: String,
        ageRestriction: Boolean,
        userId: String
    ) {
        repository.getHomeSections(
            token,
            xmode,
            ageRestriction,
            userId,
            object : HomeRepository.HomeCallback {
                override fun onSuccess(data: Map<String, List<Movie>>) {
                    val categories = data.map { entry ->
                        Category(
                            id = entry.key,
                            name = entry.key,
                            movieList = entry.value
                        )
                    }
                    _categoryList.value = categories
                    _featuredMovieList.value = categories.firstOrNull()?.movieList ?: emptyList()
                }

                override fun onError(message: String) {
                    Log.e("API_ERROR", "Catalog home fallback failed: $message")
                    _categoryList.value = emptyList()
                }
            }
        )
    }

    private fun recommendationTitle(
        sectionId: String,
        section: HomeRecommendationResponse.Section
    ): String = when (sectionId) {
        "latest_update" -> "Latest Update"
        "continue_watching" -> "Continue Watching"
        "because_you_watched" -> section.anchor?.title
            ?.takeIf { it.isNotBlank() }
            ?.let { "Because You Watched $it" }
            ?: "Because You Watched"
        "top_picks_for_you" -> "Top Picks for You"
        "similar_movies" -> "Similar Movies"
        "trending_now" -> "Trending Now"
        "new_releases" -> "New Releases"
        "your_wishlist" -> "Your Wishlist"
        "next_episode" -> "Next Episode"
        else -> sectionId
    }

    private companion object {
        val RECOMMENDATION_SECTION_ORDER = listOf(
            "latest_update",
            "continue_watching",
            "because_you_watched",
            "top_picks_for_you",
            "similar_movies",
            "trending_now",
            "new_releases",
            "your_wishlist",
            "next_episode"
        )
    }
}
