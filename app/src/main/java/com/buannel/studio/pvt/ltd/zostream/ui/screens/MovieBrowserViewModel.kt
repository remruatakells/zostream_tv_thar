package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.response.MovieFilterResponse
import com.buannel.studio.pvt.ltd.zostream.response.HomeRecommendationResponse
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieBrowserViewModel : ViewModel() {

    val movies = mutableStateOf<List<Movie>>(emptyList())
    val isLoading = mutableStateOf(false)
    val isLoadingMore = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    private var currentPage = 0
    private var lastPage = Int.MAX_VALUE
    private var requestInFlight = false
    private var token = ""
    private var userId = ""
    private var ageRestriction = false
    private var isChildMode = false
    private var category = "latest update"

    fun loadFirstPage(
        accessToken: String,
        uid: String,
        age: Boolean,
        childMode: Boolean,
        categoryName: String?
    ) {
        token = accessToken
        userId = uid
        ageRestriction = age
        isChildMode = childMode
        category = categoryName?.takeIf { it.isNotBlank() } ?: "latest update"
        currentPage = 0
        lastPage = Int.MAX_VALUE
        movies.value = emptyList()
        error.value = null
        loadPage(1, append = false)
    }

    fun loadNextPage() {
        if (requestInFlight || currentPage >= lastPage) {
            return
        }
        loadPage(currentPage + 1, append = true)
    }

    private fun loadPage(page: Int, append: Boolean) {
        if (token.isBlank() || userId.isBlank()) {
            return
        }

        requestInFlight = true
        if (append) {
            isLoadingMore.value = true
        } else {
            isLoading.value = true
        }

        if (category in RECOMMENDATION_SECTIONS || category.startsWith("custom_")) {
            loadRecommendationPage(page, append)
            return
        }

        Api.getApi().getMovie(
            AuthHeader.bearer(token),
            ageRestriction,
            isChildMode,
            userId,
            category,
            null,
            page
        ).enqueue(object : Callback<MovieFilterResponse> {
            override fun onResponse(
                call: Call<MovieFilterResponse>,
                response: Response<MovieFilterResponse>
            ) {
                requestInFlight = false
                isLoading.value = false
                isLoadingMore.value = false

                if (!response.isSuccessful || response.body() == null) {
                    error.value = "API Error: ${response.code()}"
                    return
                }

                val body = response.body()!!
                currentPage = body.pagination?.current_page ?: page
                lastPage = body.pagination?.last_page ?: currentPage
                val newMovies = body.data ?: emptyList()
                movies.value = if (append) movies.value + newMovies else newMovies
            }

            override fun onFailure(call: Call<MovieFilterResponse>, t: Throwable) {
                requestInFlight = false
                isLoading.value = false
                isLoadingMore.value = false
                error.value = t.message ?: "Unable to load movies"
            }
        })
    }

    private fun loadRecommendationPage(page: Int, append: Boolean) {
        val contentMode = if (isChildMode) "kids" else "adult"
        Api.getApi().homeRecommendations(
            AuthHeader.bearer(token),
            contentMode,
            ageRestriction,
            category,
            page,
            PAGE_SIZE
        ).enqueue(object : Callback<HomeRecommendationResponse> {
            override fun onResponse(
                call: Call<HomeRecommendationResponse>,
                response: Response<HomeRecommendationResponse>
            ) {
                requestInFlight = false
                isLoading.value = false
                isLoadingMore.value = false

                val body = response.body()
                val section = body?.getSection(category)
                // Production may return either a V4 envelope or the direct
                // recommendation payload, so do not require envelope.success.
                if (!response.isSuccessful || body == null || !body.hasSections() || section == null) {
                    error.value = body?.message ?: "Recommendation API Error: ${response.code()}"
                    return
                }

                currentPage = section.pagination?.currentPage ?: page
                lastPage = if (section.pagination?.hasMore == true) Int.MAX_VALUE else currentPage
                val newMovies = section.publishedMovies()
                movies.value = if (append) movies.value + newMovies else newMovies
            }

            override fun onFailure(call: Call<HomeRecommendationResponse>, throwable: Throwable) {
                requestInFlight = false
                isLoading.value = false
                isLoadingMore.value = false
                error.value = throwable.message ?: "Unable to load recommendations"
            }
        })
    }

    private companion object {
        const val PAGE_SIZE = 20
        val RECOMMENDATION_SECTIONS = setOf(
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
