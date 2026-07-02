package com.buannel.studio.pvt.ltd.zostream.repository;

import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.response.SeasonResponse;
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SeasonRepository {

    public interface SeasonCallback {
        void onSuccess(SeasonResponse response);
        void onError(String error);
    }

    public static void loadSeasons(
            String token,
            int movieNum,
            SeasonCallback callback
    ) {

        Call<SeasonResponse> call = Api.getApi().getSeasons(AuthHeader.bearer(token), movieNum);

        call.enqueue(new Callback<SeasonResponse>() {

            @Override
            public void onResponse(Call<SeasonResponse> call, Response<SeasonResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load seasons: " + response.code());
                }

            }

            @Override
            public void onFailure(Call<SeasonResponse> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Unknown error");
            }
        });
    }
}
