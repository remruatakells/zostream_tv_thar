package com.buannel.studio.pvt.ltd.zostream.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.response.StreamResult;
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreamRepository {

    public interface StreamCallback {
        void onSuccess(StreamResult result);
        void onError(String title, String message);
        void onFailure(Throwable throwable);
    }

    public static void startStream(
            @NonNull String accessToken,
            @NonNull String deviceId,
            @NonNull String userId,
            int subscriptionId,
            @NonNull String contentId,
            @NonNull String seasonId,
            @NonNull String type,
            @NonNull StreamCallback callback
    ) {

        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("movie_id", contentId);
        body.addProperty("type", type);
        body.addProperty("season_id", seasonId);
        body.addProperty("subscription_id", subscriptionId);

        Api.getApi().startStream(AuthHeader.bearer(accessToken), deviceId, body)
                .enqueue(new Callback<JsonObject>() {

                    @OptIn(markerClass = UnstableApi.class)
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call,
                                           @NonNull Response<JsonObject> response) {

                        if (!response.isSuccessful()) {
                            handleHttpError(response, callback);
                            return;
                        }

                        JsonObject res = response.body();

                        if (res != null
                                && res.has("status")
                                && "success".equalsIgnoreCase(res.get("status").getAsString())) {

                            JsonObject movieLinks = res.has("movie_links") && res.get("movie_links").isJsonObject()
                                    ? res.getAsJsonObject("movie_links")
                                    : null;

                            String streamUrl = null;
                            String streamToken = res.has("stream_token") && !res.get("stream_token").isJsonNull()
                                    ? res.get("stream_token").getAsString()
                                    : "";
                            String maxQuality = res.has("max_quality") && !res.get("max_quality").isJsonNull()
                                    ? res.get("max_quality").getAsString()
                                    : "";
                            Long watchPosition = res.has("watch_position") && !res.get("watch_position").isJsonNull()
                                    ? res.get("watch_position").getAsLong()
                                    : null;

                            if (movieLinks != null && movieLinks.has("links") && !movieLinks.get("links").isJsonNull()) {
                                try {
                                    streamUrl = movieLinks.get("links").getAsString();
                                } catch (Exception e) {
                                    Log.e("StreamRepo", "Invalid stream URL", e);
                                }
                            }

                            if (streamUrl != null && !streamUrl.trim().isEmpty()) {
                                callback.onSuccess(new StreamResult(streamUrl, streamToken, maxQuality, watchPosition));
                            } else {
                                callback.onError("Stream Error", "No stream URL found");
                            }

                        } else {
                            String title = (res != null && res.has("title") && !res.get("title").isJsonNull())
                                    ? res.get("title").getAsString()
                                    : "Error";

                            String message = (res != null && res.has("message") && !res.get("message").isJsonNull())
                                    ? res.get("message").getAsString()
                                    : "Unknown error";

                            callback.onError(title, message);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call,
                                          @NonNull Throwable throwable) {
                        callback.onFailure(throwable);
                    }
                });
    }

    private static void handleHttpError(
            @NonNull Response<JsonObject> response,
            @NonNull StreamCallback callback
    ) {
        try {
            String errorJson = response.errorBody() != null
                    ? response.errorBody().string()
                    : null;

            JsonObject errorObj = new Gson().fromJson(errorJson, JsonObject.class);

            String title = (errorObj != null && errorObj.has("title") && !errorObj.get("title").isJsonNull())
                    ? errorObj.get("title").getAsString()
                    : "Error";

            String message = (errorObj != null && errorObj.has("message") && !errorObj.get("message").isJsonNull())
                    ? errorObj.get("message").getAsString()
                    : "Something went wrong";

            callback.onError(title, message);

        } catch (Exception e) {
            Log.e("StreamRepo", "Error parsing HTTP error", e);
            callback.onError("Error", "Unable to parse error response");
        }
    }
}
