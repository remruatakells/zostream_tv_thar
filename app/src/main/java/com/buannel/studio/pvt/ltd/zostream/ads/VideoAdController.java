package com.buannel.studio.pvt.ltd.zostream.ads;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.utils.DeviceUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Plays one planned pre-, mid-, or post-roll ad over the content player. */
public final class VideoAdController {
    private static final long START_TIMEOUT_MS = 15_000L;
    private final Context context;
    private final FrameLayout host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExoPlayer adPlayer;
    private View overlay;
    private TextView skipButton;
    private VideoAdApi api;
    private ImageAd ad;
    private String impressionEventId;
    private boolean playing;
    private boolean sent25;
    private boolean sent50;
    private boolean sent75;
    private Runnable completion;
    private Map<String, ImageAd> plannedAds = new HashMap<>();

    public VideoAdController(Context context, FrameLayout host) {
        this.context = context;
        this.host = host;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void pause() {
        if (adPlayer != null) adPlayer.pause();
    }

    public void resume() {
        if (adPlayer != null && playing) adPlayer.play();
    }

    public void setPlannedAds(Map<String, ImageAd> ads) {
        plannedAds = ads == null ? new HashMap<>() : new HashMap<>(ads);
    }

    public boolean hasPlannedAd(String placement) {
        ImageAd candidate = plannedAds.get(placement);
        return candidate != null
                && "video".equalsIgnoreCase(candidate.getType())
                && first(candidate.getMediaUrl(), candidate.getProxyMediaUrl()) != null;
    }

    public void play(String placement, Runnable after) {
        if (playing) {
            if (after != null) after.run();
            return;
        }

        completion = after;
        try {
            api = Api.createService(VideoAdApi.class);
        } catch (IllegalStateException ignored) {
            finish();
            return;
        }

        ImageAd candidate = plannedAds.remove(placement);
        String media = candidate == null
                ? null
                : first(candidate.getMediaUrl(), candidate.getProxyMediaUrl());
        if (candidate == null
                || !"video".equalsIgnoreCase(candidate.getType())
                || media == null) {
            finish();
            return;
        }

        show(candidate, media);
    }

    private void show(ImageAd candidate, String media) {
        ad = candidate;
        playing = true;
        sent25 = sent50 = sent75 = false;

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.BLACK);
        root.setFocusable(true);
        overlay = root;

        PlayerView playerView = new PlayerView(context);
        playerView.setUseController(false);
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        TextView badge = chip("Advertisement");
        badge.setFocusable(false);
        root.addView(badge, chipParams(Gravity.TOP | Gravity.START));

        skipButton = chip("Skip ad");
        skipButton.setVisibility(View.GONE);
        skipButton.setContentDescription("Skip advertisement");
        skipButton.setOnClickListener(view -> {
            record("skip");
            finish();
        });
        root.addView(skipButton, chipParams(Gravity.TOP | Gravity.END));

        View advertiser = advertiserCta(candidate);
        root.addView(advertiser, advertiserCtaParams());
        host.addView(root, new FrameLayout.LayoutParams(-1, -1));

        adPlayer = new ExoPlayer.Builder(context).build();
        playerView.setPlayer(adPlayer);
        adPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    ensureImpression();
                    scheduleMilestones();
                    scheduleSkip();
                } else if (state == Player.STATE_ENDED) {
                    record("video_complete");
                    finish();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                finish();
            }
        });

        MediaItem.Builder item = new MediaItem.Builder().setUri(media);
        if (isDash(media)) item.setMimeType(MimeTypes.APPLICATION_MPD);
        adPlayer.setMediaItem(item.build());
        adPlayer.prepare();
        adPlayer.play();
        handler.postDelayed(() -> {
            if (playing && adPlayer != null
                    && adPlayer.getPlaybackState() != Player.STATE_READY) {
                finish();
            }
        }, START_TIMEOUT_MS);
    }

    private void ensureImpression() {
        if (impressionEventId != null || ad == null || api == null) return;
        String id = UUID.randomUUID().toString();
        impressionEventId = id;
        api.record(event("impression", id, null)).enqueue(emptyCallback(() -> record("video_start")));
    }

    private void scheduleMilestones() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!playing || adPlayer == null) return;
                long duration = adPlayer.getDuration();
                double percent = duration <= 0
                        ? 0
                        : adPlayer.getCurrentPosition() * 100d / duration;
                if (!sent25 && percent >= 25) {
                    sent25 = true;
                    record("video_25");
                }
                if (!sent50 && percent >= 50) {
                    sent50 = true;
                    record("video_50");
                }
                if (!sent75 && percent >= 75) {
                    sent75 = true;
                    record("video_75");
                }
                handler.postDelayed(this, 500L);
            }
        }, 500L);
    }

    private void scheduleSkip() {
        if (!Boolean.TRUE.equals(ad.isSkippable())) return;
        int seconds = Math.max(0, ad.getSkipAfterSeconds() == null
                ? 0
                : ad.getSkipAfterSeconds());
        handler.postDelayed(() -> {
            if (!playing || skipButton == null) return;
            skipButton.setVisibility(View.VISIBLE);
            skipButton.requestFocus();
        }, seconds * 1000L);
    }

    private View advertiserCta(ImageAd candidate) {
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(24), dp(16), dp(16), dp(16));
        panel.setBackgroundColor(0xdf111111);

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView name = adText(textOr(candidate.getName(), "Advertisement"), 18, Color.WHITE, Typeface.BOLD);
        name.setMaxLines(2);
        copy.addView(name);
        copy.addView(adText(
                "Sponsored · " + textOr(candidate.getAdvertiserName(), "Advertisement"),
                14,
                0xffd1d5db,
                Typeface.NORMAL
        ));
        TextView description = adText(overlaySummary(candidate), 14, 0xffe5e7eb, Typeface.NORMAL);
        description.setMaxLines(2);
        copy.addView(description);
        panel.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        if (first(candidate.getTargetUrl(), candidate.getAdUrl()) != null) {
            TextView visit = chip("Visit");
            visit.setContentDescription("Visit advertiser");
            visit.setOnClickListener(view -> openTarget());
            panel.addView(visit, new LinearLayout.LayoutParams(-2, -2));
            visit.post(visit::requestFocus);
        }
        return panel;
    }

    private void openTarget() {
        if (ad == null) return;
        String target = first(ad.getTargetUrl(), ad.getAdUrl());
        if (target == null) return;
        record("click");
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
        } catch (Exception ignored) {
        }
    }

    private void record(String kind) {
        if (impressionEventId == null || ad == null || api == null) return;
        api.record(event(kind, UUID.randomUUID().toString(), impressionEventId))
                .enqueue(emptyCallback(null));
    }

    private Map<String, Object> event(String kind, String eventId, String impressionId) {
        Map<String, Object> body = new HashMap<>();
        body.put("tracking_token", ad.getTrackingToken());
        body.put("event_id", eventId);
        body.put("event", kind);
        body.put("device_id", DeviceUtils.getDeviceId(context));
        if (impressionId != null) body.put("impression_event_id", impressionId);
        if (!"impression".equals(kind) && !"click".equals(kind)) {
            body.put("watched_seconds", adPlayer == null
                    ? 0
                    : (int) (adPlayer.getCurrentPosition() / 1000L));
        }
        return body;
    }

    private Callback<AdEnvelope<Map<String, Object>>> emptyCallback(Runnable success) {
        return new Callback<AdEnvelope<Map<String, Object>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<AdEnvelope<Map<String, Object>>> call,
                    @NonNull Response<AdEnvelope<Map<String, Object>>> response
            ) {
                if (response.isSuccessful() && success != null) success.run();
            }

            @Override
            public void onFailure(
                    @NonNull Call<AdEnvelope<Map<String, Object>>> call,
                    @NonNull Throwable throwable
            ) {
            }
        };
    }

    private TextView chip(String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(24), dp(12), dp(24), dp(12));
        view.setFocusable(true);
        view.setClickable(true);
        view.setBackground(focusBackground(false));
        view.setOnFocusChangeListener((focusedView, hasFocus) ->
                focusedView.setBackground(focusBackground(hasFocus))
        );
        return view;
    }

    private GradientDrawable focusBackground(boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(focused ? 0xff2563eb : 0xd9000000);
        drawable.setStroke(dp(2), focused ? 0xffbfdbfe : 0xff94a3b8);
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private TextView adText(String value, int size, int color, int style) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private FrameLayout.LayoutParams chipParams(int gravity) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2, gravity);
        params.setMargins(dp(20), dp(20), dp(20), dp(20));
        return params;
    }

    private FrameLayout.LayoutParams advertiserCtaParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        params.setMargins(dp(24), dp(24), dp(24), dp(30));
        return params;
    }

    private String overlaySummary(ImageAd candidate) {
        if (hasText(candidate.getDescription())) return candidate.getDescription().trim();
        String target = first(candidate.getTargetUrl(), candidate.getAdUrl());
        if (target != null) {
            try {
                String hostName = Uri.parse(target).getHost();
                if (hasText(hostName)) return "Visit " + hostName.replaceFirst("^www\\.", "");
            } catch (Exception ignored) {
            }
        }
        return "Learn more about this advertisement";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String textOr(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String first(String first, String second) {
        return hasText(first) ? first : hasText(second) ? second : null;
    }

    private boolean isDash(String url) {
        return url != null && url.toLowerCase(Locale.US).split("\\?")[0].endsWith(".mpd");
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public void release() {
        handler.removeCallbacksAndMessages(null);
        if (adPlayer != null) {
            adPlayer.release();
            adPlayer = null;
        }
        if (overlay != null) {
            host.removeView(overlay);
            overlay = null;
        }
        playing = false;
        impressionEventId = null;
        ad = null;
    }

    private void finish() {
        Runnable after = completion;
        completion = null;
        release();
        if (after != null) after.run();
    }
}
