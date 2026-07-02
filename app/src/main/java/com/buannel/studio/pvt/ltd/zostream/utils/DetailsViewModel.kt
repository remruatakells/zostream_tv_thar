package com.buannel.studio.pvt.ltd.zostream.utils

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.model.Episode
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.model.Season
import com.buannel.studio.pvt.ltd.zostream.model.Subscription
import com.buannel.studio.pvt.ltd.zostream.repository.DetailsRepository
import com.buannel.studio.pvt.ltd.zostream.repository.SeasonRepository
import com.buannel.studio.pvt.ltd.zostream.response.CheckPpvRentalResponse
import com.buannel.studio.pvt.ltd.zostream.response.MovieDetailsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.collections.firstOrNull

class DetailsViewModel : ViewModel() {

    var subscription = mutableStateOf<Subscription?>(null)
    var ppv = mutableStateOf<CheckPpvRentalResponse.Data?>(null)

    var movie = mutableStateOf<Movie?>(null)
    var seasons = mutableStateOf<List<Season>>(emptyList())
    var selectedSeason = mutableStateOf<Season?>(null)
    var episodes = mutableStateOf<List<Episode>>(emptyList())

    // ✅ for movie playlist
    var alsoLikeMovies = mutableStateOf<List<Movie>>(emptyList())

    // ✅ helper state
    var isSeriesContent = mutableStateOf(false)

    var isLoading = mutableStateOf(true)
    var error = mutableStateOf<String?>(null)

    fun loadData(
        accessToken: String,
        userId: String,
        deviceId: String,
        movieId: String,
        type: String = "movie"
    ) {
        isLoading.value = true
        error.value = null

        // reset old state before loading new content
        movie.value = null
        subscription.value = null
        ppv.value = null
        seasons.value = emptyList()
        selectedSeason.value = null
        episodes.value = emptyList()
        alsoLikeMovies.value = emptyList()
        isSeriesContent.value = false

        DetailsRepository.loadDetailsData(
            accessToken,
            userId,
            deviceId,
            movieId,
            type,
            object : DetailsRepository.DetailsCallback {

                override fun onSuccess(response: MovieDetailsResponse) {
                    val movieData = response.movie
                    val subData = response.subscription

                    if (movieData == null) {
                        error.value = "Movie not found"
                        isLoading.value = false
                        return
                    }

                    movie.value = movieData
                    subscription.value = subData
                    isSeriesContent.value = movieData.isSeason

                    if (movieData.isSeason) {
                        loadSeasons(userId, accessToken, movieData.num)
                    } else {
                        if (movieData.isPayPerView) {

                            checkPpvRental(
                                accessToken,
                                userId,
                                movieData.id,
                                null,
                                "movie"
                            )
                        }

                        loadAlsoLike(accessToken, userId, movieData.title)
                    }
                }

                override fun onError(errorMsg: String) {
                    error.value = errorMsg
                    isLoading.value = false
                }
            }
        )
    }

    private fun loadSeasons(userId: String, accessToken: String, movieNum: Int) {
        SeasonRepository.loadSeasons(
            accessToken,
            movieNum,
            object : SeasonRepository.SeasonCallback {
                override fun onSuccess(response: com.buannel.studio.pvt.ltd.zostream.response.SeasonResponse) {
                    val seasonList = response.data ?: emptyList()
                    isLoading.value = false
                    seasons.value = seasonList

                    if (seasonList.isNotEmpty()) {
                        val firstSeason = seasonList.first()
                        selectedSeason.value = firstSeason
                        episodes.value = firstSeason.episodes ?: emptyList()
                    } else {
                        selectedSeason.value = null
                        episodes.value = emptyList()
                    }

                    val firstSeason = seasonList.firstOrNull()
                    val firstEpisode = firstSeason?.episodes?.firstOrNull()

                    if (firstEpisode?.isPayPerView == true) {
                        checkPpvRental(
                            accessToken,
                            userId,
                            firstEpisode.id,
                            firstSeason.id,
                            "episode"
                        )
                    }
                }

                override fun onError(errorMsg: String) {
                    error.value = errorMsg
                    isLoading.value = false
                }
            }
        )
    }

    private fun loadAlsoLike(
        accessToken: String,
        userId: String,
        title: String?
    ) {
        if (title.isNullOrBlank()) {
            alsoLikeMovies.value = emptyList()
            isLoading.value = false
            return
        }

        val call = Api.getApi().getAlsoLike(AuthHeader.bearer(accessToken), userId, title, false)
        call?.enqueue(object : Callback<List<Movie>> {
            override fun onResponse(call: Call<List<Movie>>, response: Response<List<Movie>>) {
                alsoLikeMovies.value = if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    emptyList()
                }
                isLoading.value = false
            }

            override fun onFailure(call: Call<List<Movie>>, t: Throwable) {
                alsoLikeMovies.value = emptyList()
                isLoading.value = false
            }
        } as Callback<List<Movie?>?>?)
    }

    fun selectSeason(season: Season) {
        selectedSeason.value = season
        episodes.value = season.episodes ?: emptyList()
    }

    private fun checkPpvRental(
        accessToken: String,
        userId: String,
        contentId: String?,
        seasonId: String?,
        type: String
    ) {
        val call = Api.getApi().checkPayPerViewRental(
            AuthHeader.bearer(accessToken),
            type,
            contentId,
            seasonId,
            userId,
            "tv" // or dynamic
        )

        call?.enqueue(object : Callback<CheckPpvRentalResponse> {
            override fun onResponse(
                call: Call<CheckPpvRentalResponse>,
                response: Response<CheckPpvRentalResponse>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val data = response.body()!!.data
                    isLoading.value = false
                    ppv.value = data
                }
            }

            override fun onFailure(call: Call<CheckPpvRentalResponse>, t: Throwable) {
                isLoading.value = false
            }
        } as Callback<CheckPpvRentalResponse?>?)
    }
}
