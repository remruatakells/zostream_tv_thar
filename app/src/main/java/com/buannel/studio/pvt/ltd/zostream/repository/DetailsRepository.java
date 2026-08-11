package com.buannel.studio.pvt.ltd.zostream.repository;

import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.response.MovieDetailsResponse;
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailsRepository {

    public interface DetailsCallback {
        void onSuccess(MovieDetailsResponse response);
        void onError(String error);
    }

    public static void loadDetailsData(
            String accessToken,
            String userId,
            String deviceId,
            String movieId,
            String type,
            DetailsCallback callback
    ) {

        Call<MovieDetailsResponse> call =
                Api.getApi().getDetails(AuthHeader.bearer(accessToken), movieId, userId, deviceId, "tv", type, movieId);

        call.enqueue(new Callback<MovieDetailsResponse>() {
            @Override
            public void onResponse(Call<MovieDetailsResponse> call,
                                   Response<MovieDetailsResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Response error");
                }
            }

            @Override
            public void onFailure(Call<MovieDetailsResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}
