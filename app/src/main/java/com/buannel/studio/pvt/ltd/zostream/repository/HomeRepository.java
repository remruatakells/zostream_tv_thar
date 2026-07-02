package com.buannel.studio.pvt.ltd.zostream.repository;

import androidx.annotation.NonNull;

import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.model.Episode;
import com.buannel.studio.pvt.ltd.zostream.model.Movie;
import com.buannel.studio.pvt.ltd.zostream.response.BannerResponse;
import com.buannel.studio.pvt.ltd.zostream.response.WatchContinueResponse;
import com.buannel.studio.pvt.ltd.zostream.response.WatchHistoryItem;
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeRepository {

    public interface HomeCallback {
        void onSuccess(Map<String, List<Movie>> data);
        void onError(String message);
    }

    public interface BannerCallback {
        void onSuccess(List<BannerResponse.Banner> data);
        void onError(String message);
    }

    public void getBanners(@NonNull BannerCallback callback) {
        Call<BannerResponse> call = Api.getApi().getBanner();

        call.enqueue(new Callback<BannerResponse>() {
            @Override
            public void onResponse(Call<BannerResponse> call,
                                   Response<BannerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BannerResponse.Banner> banners = response.body().getData();
                    callback.onSuccess(banners == null ? new ArrayList<>() : banners);
                } else {
                    callback.onError("Banner API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<BannerResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getHomeSections(
            String token,
            String mode,
            boolean ageRestriction,
            String userId,
            @NonNull HomeCallback callback
    ) {

        String bearerToken = AuthHeader.bearer(token);

        Call<WatchContinueResponse> watchContinueCall =
                Api.getApi().getWatchContinue(bearerToken, userId, mode, ageRestriction);

        watchContinueCall.enqueue(new Callback<WatchContinueResponse>() {
            @Override
            public void onResponse(Call<WatchContinueResponse> call,
                                   Response<WatchContinueResponse> response) {

                List<Movie> continueWatching = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    continueWatching = getContinueWatchingMovies(response.body());
                }

                loadHomeSections(bearerToken, mode, ageRestriction, userId, continueWatching, callback);
            }

            @Override
            public void onFailure(Call<WatchContinueResponse> call, Throwable t) {
                loadHomeSections(bearerToken, mode, ageRestriction, userId, new ArrayList<>(), callback);
            }
        });
    }

    private void loadHomeSections(
            String bearerToken,
            String mode,
            boolean ageRestriction,
            String userId,
            List<Movie> continueWatching,
            @NonNull HomeCallback callback
    ) {

        Call<Map<String, List<Movie>>> call =
                Api.getApi().homeSection(bearerToken, mode, ageRestriction, userId);

        call.enqueue(new Callback<Map<String, List<Movie>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Movie>>> call,
                                   Response<Map<String, List<Movie>>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, List<Movie>> orderedSections = new LinkedHashMap<>();
                    if (!continueWatching.isEmpty()) {
                        orderedSections.put("Continue Watching", continueWatching);
                    }

                    for (Map.Entry<String, List<Movie>> entry : response.body().entrySet()) {
                        if (orderedSections.containsKey(entry.getKey())) {
                            continue;
                        }
                        orderedSections.put(entry.getKey(), entry.getValue());
                    }

                    callback.onSuccess(orderedSections);
                } else {
                    callback.onError("API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Movie>>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private List<Movie> getContinueWatchingMovies(WatchContinueResponse response) {
        List<WatchHistoryItem> history = response.getData();
        if (history == null || history.isEmpty()) {
            history = response.getWatchHistory();
        }

        List<Movie> movies = new ArrayList<>();
        if (history == null) {
            return movies;
        }

        for (WatchHistoryItem item : history) {
            if (item == null || item.getMovie() == null) {
                continue;
            }

            Movie movie = item.getMovie();
            Long position = item.getPosition();
            if (position != null) {
                movie.watchPosition = position > Integer.MAX_VALUE ? Integer.MAX_VALUE : position.intValue();
            }
            Long duration = item.getDuration();
            if (duration != null) {
                movie.watchDuration = duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : duration.intValue();
            }
            applyEpisodeTitle(movie, item.getEpisode());
            movies.add(movie);
        }

        return movies;
    }

    private void applyEpisodeTitle(Movie movie, Episode episode) {
        if (episode == null) {
            return;
        }

        String movieTitle = movie.title == null ? "" : movie.title;
        String episodeLabel = episode.episodeNumber > 0
                ? "Episode " + episode.episodeNumber
                : episode.title;

        if (episodeLabel == null || episodeLabel.isEmpty()) {
            return;
        }

        movie.title = movieTitle.isEmpty()
                ? episodeLabel
                : movieTitle + " · " + episodeLabel;
    }
}
