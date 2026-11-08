package com.buannel.studio.pvt.ltd.zostream.utils

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
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
    private val ppvRentalStatuses = mutableStateMapOf<String, CheckPpvRentalResponse.Data>()

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
        ppvRentalStatuses.clear()
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

                        loadAlsoLike(accessToken, userId, movieData.id, movieData.title)
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
                    val seasonList = sortSeasons(response.data ?: emptyList())
                    seasons.value = seasonList

                    if (seasonList.isNotEmpty()) {
                        val firstSeason = seasonList.first()
                        selectedSeason.value = firstSeason
                        val firstSeasonEpisodes = sortEpisodes(firstSeason.episodes ?: emptyList())
                        episodes.value = firstSeasonEpisodes

                        val firstEpisode = firstSeasonEpisodes.firstOrNull()
                        if (firstEpisode?.isPayPerView == true) {
                            checkPpvRental(
                                accessToken = accessToken,
                                userId = userId,
                                contentId = firstEpisode.id,
                                seasonId = firstEpisode.seasonId.ifBlank { firstSeason.id },
                                type = "episode"
                            ) {
                                isLoading.value = false
                            }

                            firstSeasonEpisodes
                                .drop(1)
                                .filter { it.isPayPerView }
                                .forEach { episode ->
                                    checkPpvRental(
                                        accessToken = accessToken,
                                        userId = userId,
                                        contentId = episode.id,
                                        seasonId = episode.seasonId.ifBlank { firstSeason.id },
                                        type = "episode"
                                    )
                                }
                        } else {
                            isLoading.value = false
                        }
                    } else {
                        selectedSeason.value = null
                        episodes.value = emptyList()
                        isLoading.value = false
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
        contentId: String,
        title: String?
    ) {
        if (title.isNullOrBlank()) {
            alsoLikeMovies.value = emptyList()
            isLoading.value = false
            return
        }

        val call = Api.getApi().getAlsoLike(
            AuthHeader.bearer(accessToken),
            contentId,
            userId,
            title,
            false
        ) ?: run {
            alsoLikeMovies.value = emptyList()
            isLoading.value = false
            return
        }

        call.enqueue(object : Callback<List<Movie?>?> {
            override fun onResponse(
                call: Call<List<Movie?>?>,
                response: Response<List<Movie?>?>
            ) {
                alsoLikeMovies.value = if (response.isSuccessful) {
                    response.body().orEmpty().filterNotNull()
                } else {
                    emptyList()
                }
                isLoading.value = false
            }

            override fun onFailure(call: Call<List<Movie?>?>, t: Throwable) {
                alsoLikeMovies.value = emptyList()
                isLoading.value = false
            }
        })
    }

    fun selectSeason(season: Season) {
        selectedSeason.value = season
        episodes.value = sortEpisodes(season.episodes ?: emptyList())
    }

    fun preloadSeasonRentalStatuses(
        accessToken: String,
        userId: String,
        season: Season
    ) {
        sortEpisodes(season.episodes ?: emptyList())
            .filter { it.isPayPerView }
            .forEach { episode ->
                checkPpvRental(
                    accessToken = accessToken,
                    userId = userId,
                    contentId = episode.id,
                    seasonId = episode.seasonId.ifBlank { season.id },
                    type = "episode"
                )
            }
    }

    fun rentalStatus(
        type: String,
        contentId: String,
        seasonId: String? = null
    ): CheckPpvRentalResponse.Data? {
        return ppvRentalStatuses[rentalKey(type, contentId, seasonId)]
    }

    private fun sortSeasons(source: List<Season>): List<Season> {
        return source.sortedWith(
            compareBy<Season> { if (it.seasonNumber > 0) it.seasonNumber else Int.MAX_VALUE }
                .thenBy { it.title ?: "" }
        )
    }

    private fun sortEpisodes(source: List<Episode>): List<Episode> {
        return source.sortedWith(
            compareBy<Episode> { if (it.episodeNumber > 0) it.episodeNumber else Int.MAX_VALUE }
                .thenBy { it.title ?: "" }
        )
    }

    fun checkPpvRental(
        accessToken: String,
        userId: String,
        contentId: String,
        seasonId: String?,
        type: String,
        force: Boolean = false,
        onResult: (CheckPpvRentalResponse.Data?) -> Unit = {}
    ) {
        val key = rentalKey(type, contentId, seasonId)
        if (!force) {
            ppvRentalStatuses[key]?.let { cached ->
                onResult(cached)
                return
            }
        }

        val call = Api.getApi().checkPayPerViewRental(
            AuthHeader.bearer(accessToken),
            contentId,
            type,
            contentId,
            seasonId,
            userId,
            "tv"
        ) ?: run {
            onResult(null)
            return
        }

        call.enqueue(object : Callback<CheckPpvRentalResponse?> {
            override fun onResponse(
                call: Call<CheckPpvRentalResponse?>,
                response: Response<CheckPpvRentalResponse?>
            ) {
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    ppvRentalStatuses[key] = data
                }
                onResult(data)
            }

            override fun onFailure(call: Call<CheckPpvRentalResponse?>, t: Throwable) {
                onResult(null)
            }
        })
    }

    private fun rentalKey(type: String, contentId: String, seasonId: String?): String {
        return "${type.lowercase()}:$contentId:${seasonId.orEmpty()}"
    }
}
