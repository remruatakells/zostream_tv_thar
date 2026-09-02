package com.buannel.studio.pvt.ltd.zostream.ui.player;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.buannel.studio.pvt.ltd.zostream.utils.AppDialog.show;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.buannel.studio.pvt.ltd.zostream.R;
import com.buannel.studio.pvt.ltd.zostream.adapter.PlaylistAdapter;
import com.buannel.studio.pvt.ltd.zostream.api.Api;
import com.buannel.studio.pvt.ltd.zostream.model.Episode;
import com.buannel.studio.pvt.ltd.zostream.model.Movie;
import com.buannel.studio.pvt.ltd.zostream.model.Season;
import com.buannel.studio.pvt.ltd.zostream.payment.AmazonIapActivity;
import com.buannel.studio.pvt.ltd.zostream.payment.PaymentFeatureConfig;
import com.buannel.studio.pvt.ltd.zostream.request.QrPaymentRequest;
import com.buannel.studio.pvt.ltd.zostream.repository.SeasonRepository;
import com.buannel.studio.pvt.ltd.zostream.repository.StreamRepository;
import com.buannel.studio.pvt.ltd.zostream.response.QrLoginResponse;
import com.buannel.studio.pvt.ltd.zostream.response.SeasonResponse;
import com.buannel.studio.pvt.ltd.zostream.response.StreamResult;
import com.buannel.studio.pvt.ltd.zostream.utils.AppDialog;
import com.buannel.studio.pvt.ltd.zostream.utils.AuthHeader;
import com.buannel.studio.pvt.ltd.zostream.utils.QRUtils;
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
public class PlayerActivity extends ComponentActivity {

    private static final int AMAZON_IAP_REQUEST_CODE = 4201;

    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private DefaultTimeBar seekBar;

    private View loader;
    private TextView titleView;
    private TextView episodeTitle;

    private String streamUrl;
    private String title;
    private ImageView pauPlayBtn;

    private static final long SEEK_INCREMENT_MS = 10000; // 10 sec
    private static final long PLAYLIST_ANIMATION_DURATION_MS = 180L;

    private String streamToken;
    private int subscriptionId;
    private String movieId;
    private String maxQuality;
    private String type;
    private int movieNum;
    private int selectedQualityHeight = 0;
    private float selectedPlaybackSpeed = 1f;
    private boolean preferredAudioApplied = false;
    private boolean audioSelectionTouched = false;
    private long watchPosition = 0L;

    ArrayList<Season> season;
    ArrayList<Movie> movies;
    ArrayList<Episode> episodes;
    private boolean isEpisode;

    private ImageView speedBtn, qualityBtn, audioTrackBtn;
    private View prevBtn, nextBtn;
    private View prevIcon, nextIcon;
    private View playlistArrow;
    private View playlistLinear;
    private View seasonPlaylistScroll;
    private LinearLayout seasonPlaylistContainer;
    private RecyclerView playlistRecyclerView;
    private long playbackDuration = 0;
    private int currentEpisodeNumber = 0;
    private int currentEpisodeIndex = -1;
    private Dialog qrPaymentDialog;
    private DatabaseReference qrPaymentRef;
    private ValueEventListener qrPaymentListener;
    private final Handler qrPaymentHandler = new Handler();
    private Runnable qrPaymentCountdownRunnable;
    private boolean pendingQrIsPpv;
    @Nullable
    private Episode pendingQrEpisode;
    @Nullable
    private Movie pendingQrMovie;
    @Nullable
    private String pendingQrType;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setPlayerScreenAwake(true);
        setContentView(R.layout.activity_player);

        // 🔥 Get data
        streamUrl = getIntent().getStringExtra("streamUrl");
        title = getIntent().getStringExtra("title");
        type = getIntent().getStringExtra("type");
        movieNum = getIntent().getIntExtra("movieNum", 0);
        streamToken = getIntent().getStringExtra("streamToken");
        subscriptionId = getIntent().getIntExtra("subscriptionId", 0);
        movieId = getIntent().getStringExtra("movieId");
        maxQuality = getIntent().getStringExtra("maxQuality");
        isEpisode = getIntent().getBooleanExtra("isEpisode", false);
        currentEpisodeNumber = getIntent().getIntExtra("episode", 0);
        watchPosition = parseWatchPosition(getIntent().getStringExtra("watchPosition"));

        // 🎬 Views
        playerView = findViewById(R.id.playerView);
        playerView.setKeepScreenOn(true);
        playerView.setFocusable(true);
        playerView.setFocusableInTouchMode(true);
        loader = findViewById(R.id.loader);
        titleView = playerView.findViewById(R.id.movie_title);
        episodeTitle = playerView.findViewById(R.id.movie_episode);
        seekBar = playerView.findViewById(R.id.exo_progress);
        seekBar.setKeyTimeIncrement(SEEK_INCREMENT_MS);
        pauPlayBtn = playerView.findViewById(R.id.player_pause_play);

        speedBtn = playerView.findViewById(R.id.player_speed);
        qualityBtn = playerView.findViewById(R.id.quality_btn);
        audioTrackBtn = playerView.findViewById(R.id.player_audio_tracks);
        prevBtn = playerView.findViewById(R.id.prev_btn);
        nextBtn = playerView.findViewById(R.id.next_btn);
        prevIcon = playerView.findViewById(R.id.prev_icon);
        nextIcon = playerView.findViewById(R.id.next_icon);
        playlistArrow = playerView.findViewById(R.id.playlist_arrow);

        playlistLinear = playerView.findViewById(R.id.playlistLinear);
        seasonPlaylistScroll = playerView.findViewById(R.id.seasonPlaylistScroll);
        seasonPlaylistContainer = playerView.findViewById(R.id.seasonPlaylistContainer);
        playlistRecyclerView =  playerView.findViewById(R.id.playlistRecyclerView);

        setupTopControlDownFocus(speedBtn);
        setupTopControlDownFocus(qualityBtn);
        setupTopControlDownFocus(audioTrackBtn);
        setupTopControlDownFocus(prevIcon);
        setupTopControlDownFocus(nextIcon);

        movies = new ArrayList<>();
        season = new ArrayList<>();
        episodes = new ArrayList<>();

        playlistRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        );

        titleView.setText(title);

        // 🎬 Title
        if (isEpisode) {
            episodeTitle.setVisibility(View.VISIBLE);
            episodeTitle.setText("Episode " + currentEpisodeNumber);
        } else {
            episodeTitle.setVisibility(View.GONE);
        }

        pauPlayBtn.setOnClickListener(v -> pausePlayBtnAction());
        speedBtn.setOnClickListener(v -> showSpeedDialog());
        qualityBtn.setOnClickListener(v -> showQualityDialog());
        audioTrackBtn.setOnClickListener(v -> showAudioTrackDialog());
        prevBtn.setOnClickListener(v -> prev_btn());
        nextBtn.setOnClickListener(v -> next_btn());
        prevIcon.setOnClickListener(v -> prev_btn());
        nextIcon.setOnClickListener(v -> next_btn());
        updateEpisodeNavButtons();

        if (isEpisode) {
            loadSeasonData(movieNum);
        }

        // 🔥 Init player
        initPlayer(watchPosition);
    }

    private void setPlayerScreenAwake(boolean enabled) {
        if (enabled) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (playerView != null) {
            playerView.setKeepScreenOn(enabled);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && isControllerRevealKey(event.getKeyCode())
                && playerView != null
                && !playerView.isControllerFullyVisible()) {
            playerView.showController();
            focusPrimaryPlayerControl();
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private boolean isControllerRevealKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    private void focusPrimaryPlayerControl() {
        if (pauPlayBtn != null && pauPlayBtn.getVisibility() == VISIBLE) {
            pauPlayBtn.post(() -> pauPlayBtn.requestFocus());
        } else if (seekBar != null) {
            seekBar.post(() -> seekBar.requestFocus());
        }
    }

    private void loadSeasonData(int movieNum) {

        String accessToken = SessionManager.getAccessToken(this);
        setPlaylistArrowVisible(false);


        SeasonRepository.loadSeasons(AuthHeader.bearer(accessToken), movieNum, new SeasonRepository.SeasonCallback() {
            @Override
            public void onSuccess(SeasonResponse response) {
                movies.clear();
                season.clear();
                episodes.clear();

                if (response.data != null) {
                    season.addAll(response.data);
                }

                sortSeasonData();
                rebuildEpisodeList();

                currentEpisodeIndex = findCurrentEpisodeIndex();
                setupSeasonPlaylist();
                updateEpisodeNavButtons();
            }

            @Override
            public void onError(String error) {
                setPlaylistArrowVisible(false);
                Log.e("PlayerActivity", "Season load failed: " + error);
            }
        });

    }

    private void sortSeasonData() {
        Collections.sort(season, (left, right) -> {
            if (left == null && right == null) return 0;
            if (left == null) return 1;
            if (right == null) return -1;
            return Integer.compare(left.seasonNumber, right.seasonNumber);
        });

        for (Season item : season) {
            if (item != null && item.episodes != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Collections.sort(item.episodes, Comparator.comparingInt(episode -> episode.episodeNumber));
                }
            }
        }
    }

    private void rebuildEpisodeList() {
        episodes.clear();

        for (Season item : season) {
            if (item != null && item.episodes != null) {
                episodes.addAll(item.episodes);
            }
        }
    }

    private void setupSeasonPlaylist() {
        playlistRecyclerView.setAdapter(null);
        playlistRecyclerView.setVisibility(GONE);
        seasonPlaylistScroll.setVisibility(VISIBLE);
        seasonPlaylistContainer.removeAllViews();

        boolean firstSeasonRow = true;
        boolean hasSeasonRows = false;
        for (Season item : season) {
            if (item == null || item.episodes == null || item.episodes.isEmpty()) {
                continue;
            }

            hasSeasonRows = true;
            TextView seasonTitle = new TextView(this);
            seasonTitle.setText(getSeasonTitle(item));
            seasonTitle.setTextColor(Color.WHITE);
            seasonTitle.setTextSize(18);
            seasonTitle.setTypeface(seasonTitle.getTypeface(), android.graphics.Typeface.BOLD);
            seasonTitle.setPadding(0, dp(8), 0, dp(8));
            seasonPlaylistContainer.addView(seasonTitle, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            RecyclerView row = getRecyclerView(item, firstSeasonRow);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, dp(18));
            seasonPlaylistContainer.addView(row, rowParams);
            firstSeasonRow = false;
        }

        setPlaylistArrowVisible(hasSeasonRows);
    }

    private void setPlaylistArrowVisible(boolean visible) {
        if (playlistArrow != null) {
            playlistArrow.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    @NonNull
    private RecyclerView getRecyclerView(Season item, boolean hideOnUp) {
        RecyclerView row = new RecyclerView(this);
        row.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        row.setOnKeyListener((v, keyCode, event) -> {
            if (hideOnUp
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                return hidePlaylistAndFocusControls();
            }
            return false;
        });
        row.setAdapter(new PlaylistAdapter(
                item.episodes,
                selectedItem -> {
                    Episode selectedEpisode = (Episode) selectedItem;
                    playEpisode(selectedEpisode);
                },
                hideOnUp ? this::hidePlaylistAndFocusControls : null
        ));
        return row;
    }

    private String getSeasonTitle(@NonNull Season item) {
        if (item.title != null && !item.title.trim().isEmpty()) {
            return item.title;
        }

        if (item.seasonNumber > 0) {
            return "Season " + item.seasonNumber;
        }

        return "Season";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int findCurrentEpisodeIndex() {
        for (int i = 0; i < episodes.size(); i++) {
            Episode episode = episodes.get(i);
            if (episode == null) {
                continue;
            }

            if (movieId != null && movieId.equals(episode.id)) {
                return i;
            }

            if (currentEpisodeNumber > 0 && currentEpisodeNumber == episode.episodeNumber) {
                return i;
            }
        }

        return episodes.isEmpty() ? -1 : 0;
    }

    private void playEpisode(@NonNull Episode episode) {
        startStream(episode, null, "episode");
    }

    private void prev_btn() {
        if (!isEpisode || episodes.isEmpty()) {
            return;
        }

        if (currentEpisodeIndex <= 0) {
            Toast.makeText(this, "No previous episode", Toast.LENGTH_SHORT).show();
            return;
        }

        playEpisode(episodes.get(currentEpisodeIndex - 1));
    }

    private void next_btn() {
        if (!isEpisode || episodes.isEmpty()) {
            return;
        }

        if (currentEpisodeIndex >= episodes.size() - 1) {
            Toast.makeText(this, "No next episode", Toast.LENGTH_SHORT).show();
            return;
        }

        playEpisode(episodes.get(currentEpisodeIndex + 1));
    }

    private void updateEpisodeNavButtons() {
        boolean hasEpisodes = isEpisode && !episodes.isEmpty() && currentEpisodeIndex >= 0;

        setEpisodeNavButtonEnabled(prevBtn, prevIcon, hasEpisodes && currentEpisodeIndex > 0);

        setEpisodeNavButtonEnabled(nextBtn, nextIcon, hasEpisodes && currentEpisodeIndex < episodes.size() - 1);
    }

    private void setEpisodeNavButtonEnabled(View button, View icon, boolean enabled) {
        button.setEnabled(enabled);
        button.setFocusable(false);
        button.setFocusableInTouchMode(false);
        button.setAlpha(enabled ? 1f : 0.45f);

        icon.setEnabled(enabled);
        icon.setFocusable(enabled);
        icon.setFocusableInTouchMode(enabled);
        icon.setClickable(enabled);
    }

    private void setupTopControlDownFocus(View control) {
        control.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!hasPlaylistContent()) {
                    return false;
                }

                setPlaylistVisible(true);

                focusFirstPlaylistItem();
                return true;
            }
            return false;
        });
    }

    private boolean hasPlaylistContent() {
        if (seasonPlaylistScroll != null && seasonPlaylistScroll.getVisibility() == VISIBLE) {
            return hasSeasonPlaylistRows();
        }

        if (playlistRecyclerView == null || playlistRecyclerView.getVisibility() != VISIBLE) {
            return false;
        }

        RecyclerView.Adapter<?> adapter = playlistRecyclerView.getAdapter();
        return adapter != null && adapter.getItemCount() > 0;
    }

    private boolean hasSeasonPlaylistRows() {
        if (seasonPlaylistContainer == null) {
            return false;
        }

        for (int i = 0; i < seasonPlaylistContainer.getChildCount(); i++) {
            View child = seasonPlaylistContainer.getChildAt(i);
            if (child instanceof RecyclerView) {
                RecyclerView.Adapter<?> adapter = ((RecyclerView) child).getAdapter();
                if (adapter != null && adapter.getItemCount() > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private void focusFirstPlaylistItem() {
        if (seasonPlaylistScroll != null && seasonPlaylistScroll.getVisibility() == VISIBLE) {
            focusFirstSeasonPlaylistItem();
            return;
        }

        if (playlistRecyclerView == null) return;

        playlistRecyclerView.post(() -> {
            RecyclerView.LayoutManager lm = playlistRecyclerView.getLayoutManager();
            RecyclerView.Adapter<?> adapter = playlistRecyclerView.getAdapter();

            if (lm == null || adapter == null || adapter.getItemCount() == 0) return;

            lm.scrollToPosition(0);

            playlistRecyclerView.postDelayed(() -> {
                View firstItem = lm.findViewByPosition(0);
                if (firstItem != null) {
                    firstItem.requestFocus();
                } else {
                    playlistRecyclerView.requestFocus();
                }
            }, 80);
        });
    }

    private void focusFirstSeasonPlaylistItem() {
        if (seasonPlaylistContainer == null || seasonPlaylistScroll == null) return;

        seasonPlaylistContainer.post(() -> {
            seasonPlaylistScroll.scrollTo(0, 0);

            RecyclerView firstRow = null;

            for (int i = 0; i < seasonPlaylistContainer.getChildCount(); i++) {
                View child = seasonPlaylistContainer.getChildAt(i);
                if (child instanceof RecyclerView) {
                    firstRow = (RecyclerView) child;
                    break;
                }
            }

            if (firstRow == null) return;

            RecyclerView targetRow = firstRow;
            RecyclerView.LayoutManager lm = targetRow.getLayoutManager();
            RecyclerView.Adapter<?> adapter = targetRow.getAdapter();

            if (lm == null || adapter == null || adapter.getItemCount() == 0) return;

            lm.scrollToPosition(0);

            targetRow.postDelayed(() -> {
                View firstItem = lm.findViewByPosition(0);
                Objects.requireNonNullElse(firstItem, targetRow).requestFocus();

                seasonPlaylistScroll.postDelayed(() -> seasonPlaylistScroll.scrollTo(0, 0), 40);
            }, 80);
        });
    }

    private boolean hidePlaylistAndFocusControls() {
        if (playlistLinear == null || playlistLinear.getVisibility() != View.VISIBLE) {
            return false;
        }

        setPlaylistVisible(false);

        if (qualityBtn != null) {
            qualityBtn.requestFocus();
        }

        return true;
    }

    private void setPlaylistVisible(boolean visible) {
        if (playlistLinear != null) {
            playlistLinear.animate().cancel();

            if (visible) {
                playlistLinear.setVisibility(VISIBLE);
                playlistLinear.setAlpha(0f);
                playlistLinear.setTranslationY(dp(48));
                playlistLinear.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(PLAYLIST_ANIMATION_DURATION_MS)
                        .start();
            } else {
                playlistLinear.animate()
                        .alpha(0f)
                        .translationY(dp(48))
                        .setDuration(PLAYLIST_ANIMATION_DURATION_MS)
                        .withEndAction(() -> {
                            playlistLinear.setVisibility(GONE);
                            playlistLinear.setTranslationY(0f);
                            playlistLinear.setAlpha(1f);
                        })
                        .start();
            }
        }

        if (playlistArrow != null) {
            playlistArrow.animate()
                    .rotation(visible ? -90f : 90f)
                    .setDuration(120)
                    .start();
        }

        if (pauPlayBtn != null) {
            pauPlayBtn.setVisibility(visible ? GONE : VISIBLE);
        }
    }

    private void pausePlayBtnAction() {
        if (player.isPlaying()) {
            player.pause();
            animatePlayPause(false);
        } else {
            player.play();
            animatePlayPause(true);
        }
    }

    private void animatePlayPause(boolean isPlaying) {
        pauPlayBtn.animate()
                .scaleX(0.6f)
                .scaleY(0.6f)
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    pauPlayBtn.setImageResource(
                            isPlaying ? R.drawable.pause : R.drawable.play
                    );

                    pauPlayBtn.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(180)
                            .start();
                })
                .start();
    }

    private void initPlayer() {
        initPlayer(0L);
    }

    private void initPlayer(long resumePosition) {

        if (player != null) {
            player.release();
        }

        preferredAudioApplied = false;
        audioSelectionTouched = false;
        updateAudioTrackButtonVisibility();

        trackSelector = new DefaultTrackSelector(this);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();
        playerView.setPlayer(player);
        // Ensure initial adaptive selection respects server-side max quality
        setAutoQuality();
        player.setPlaybackSpeed(selectedPlaybackSpeed);

        // 🔄 Listener
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    loader.setVisibility(View.VISIBLE);
                } else {
                    loader.setVisibility(View.GONE);
                }

                if (state == Player.STATE_READY) {
                    playbackDuration = getSafeDuration();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {

                animatePlayPause(isPlaying);
            }

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                updateAudioTrackButtonVisibility();
                applyPreferredAudioTrackIfNeeded();
            }
        });

        // 🎬 Load video after listener setup so dialog choices can start playback reliably.
        if (streamUrl != null && !streamUrl.isEmpty()) {
            if (shouldShowResumeDialog(resumePosition)) {
                showResumeDialog(resumePosition);
            } else {
                startPlayback(0L);
            }
        }
    }

    private boolean shouldShowResumeDialog(long resumePositionMs) {
        return resumePositionMs > 10L;
    }

    private void showResumeDialog(long resumePosition) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#99000000"));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(28), dp(28), dp(28), dp(28));
        panel.setBackground(createDialogPanelBackground());

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                dp(420),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        overlay.addView(panel, panelParams);

        TextView title = new TextView(this);
        title.setText("Resume playback?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView message = new TextView(this);
        message.setText("Continue from " + formatWatchPosition(resumePosition) + " or start over?");
        message.setTextColor(Color.parseColor("#D1D5DB"));
        message.setTextSize(16);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, dp(18), 0, dp(22));
        panel.addView(message, messageParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        panel.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView startOverButton = createDialogButton("Start over", dialog, () -> startPlaybackAfterDialog(0L));
        TextView resumeButton = createDialogButton("Resume", dialog, () -> startPlaybackAfterDialog(resumePosition));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(44)
        );
        buttonParams.setMargins(0, 0, dp(12), 0);
        actions.addView(startOverButton, buttonParams);
        actions.addView(resumeButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(44)
        ));

        dialog.setContentView(overlay);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
        }

        dialog.setOnShowListener(d -> resumeButton.requestFocus());
        dialog.show();
    }

    private GradientDrawable createDialogPanelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#101827"));
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private TextView createDialogButton(String label, Dialog dialog, Runnable action) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(112));
        button.setPadding(dp(18), 0, dp(18), 0);
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setClickable(true);
        setDialogButtonBackground(button, false);
        button.setOnFocusChangeListener((v, hasFocus) -> setDialogButtonBackground((TextView) v, hasFocus));
        button.setOnClickListener(v -> {
            dialog.dismiss();
            action.run();
        });
        return button;
    }

    private void setDialogButtonBackground(TextView button, boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(focused ? "#2563EB" : "#1E293B"));
        drawable.setStroke(dp(2), Color.parseColor(focused ? "#BFDBFE" : "#334155"));
        drawable.setCornerRadius(dp(8));
        button.setBackground(drawable);
    }

    private void showOptionDialog(String titleText, List<DialogOption> options) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#99000000"));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(28), dp(28), dp(28), dp(28));
        panel.setBackground(createDialogPanelBackground());

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                dp(420),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        overlay.addView(panel, panelParams);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        listParams.setMargins(0, dp(18), 0, 0);
        panel.addView(list, listParams);

        ArrayList<TextView> optionViews = new ArrayList<>();
        TextView initialFocus = null;

        for (DialogOption option : options) {
            TextView optionView = createDialogOptionView(dialog, option);
            LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(46)
            );
            optionParams.setMargins(0, 0, 0, dp(10));
            list.addView(optionView, optionParams);
            optionViews.add(optionView);

            if (option.enabled && option.selected) {
                initialFocus = optionView;
            } else if (option.enabled && initialFocus == null) {
                initialFocus = optionView;
            }
        }

        TextView focusTarget = initialFocus;
        dialog.setOnShowListener(d -> {
            if (focusTarget != null) {
                focusTarget.requestFocus();
            }
        });

        dialog.setContentView(overlay);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
        }

        dialog.show();
    }

    private TextView createDialogOptionView(Dialog dialog, DialogOption option) {
        TextView optionView = new TextView(this);
        optionView.setText(option.label);
        optionView.setTextSize(15);
        optionView.setTypeface(optionView.getTypeface(), android.graphics.Typeface.BOLD);
        optionView.setGravity(Gravity.CENTER_VERTICAL);
        optionView.setPadding(dp(16), 0, dp(16), 0);
        optionView.setEnabled(option.enabled);
        optionView.setAlpha(option.enabled ? 1f : 0.55f);
        optionView.setTextColor(option.enabled ? Color.WHITE : Color.GRAY);
        optionView.setFocusable(option.enabled);
        optionView.setFocusableInTouchMode(option.enabled);
        optionView.setClickable(option.enabled);
        setDialogOptionBackground(optionView, false, option.selected, option.enabled);
        optionView.setOnFocusChangeListener((v, hasFocus) ->
                setDialogOptionBackground((TextView) v, hasFocus, option.selected, option.enabled)
        );
        optionView.setOnClickListener(v -> {
            if (!option.enabled) {
                return;
            }
            dialog.dismiss();
            option.action.run();
        });
        return optionView;
    }

    private void setDialogOptionBackground(TextView optionView, boolean focused, boolean selected, boolean enabled) {
        GradientDrawable drawable = new GradientDrawable();
        String color = focused ? "#2563EB" : selected ? "#1E3A8A" : "#1E293B";
        String stroke = focused ? "#BFDBFE" : selected ? "#60A5FA" : "#334155";
        drawable.setColor(Color.parseColor(enabled ? color : "#111827"));
        drawable.setStroke(dp(2), Color.parseColor(enabled ? stroke : "#263244"));
        drawable.setCornerRadius(dp(8));
        optionView.setBackground(drawable);
    }

    private void startPlaybackAfterDialog(long startPosition) {
        playerView.postDelayed(() -> startPlayback(startPosition), 100L);
    }

    private void startPlayback(long startPosition) {
        if (player == null) {
            return;
        }

        MediaItem mediaItem = MediaItem.fromUri(streamUrl);
        if (startPosition > 0L) {
            player.setMediaItem(mediaItem, startPosition);
        } else {
            player.setMediaItem(mediaItem, 0L);
        }

        player.setPlayWhenReady(true);
        player.prepare();
        player.play();
    }

    private long parseWatchPosition(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private String formatWatchPosition(long positionMs) {
        long totalSeconds = Math.max(0L, positionMs / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    private long getSafeDuration() {
        if (player == null) return 0L;

        long duration = player.getContentDuration();

        if (duration <= 0) {
            duration = player.getDuration();
        }

        if (duration <= 0) {
            return 0L;
        }

        return duration;
    }

    private void showQualityDialog() {
        if (player == null || trackSelector == null) {
            Toast.makeText(this, "Player is not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        List<QualityOption> qualityOptions = getAvailableQualityOptions();

        if (qualityOptions.isEmpty()) {
            Toast.makeText(this, "No quality options available", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<DialogOption> options = new ArrayList<>();
        options.add(new DialogOption(
                getAutoQualityLabel(),
                true,
                selectedQualityHeight <= 0,
                () -> {
                    setAutoQuality();
                    Toast.makeText(this, "Quality: " + getAutoQualityText(), Toast.LENGTH_SHORT).show();
                }
        ));

        for (QualityOption option : qualityOptions) {
            options.add(new DialogOption(
                    getQualityLabel(option),
                    !option.restricted,
                    option.height == selectedQualityHeight,
                    () -> {
                        setFixedQuality(option);
                        Toast.makeText(this, "Quality: " + option.label, Toast.LENGTH_SHORT).show();
                    }
            ));
        }

        showOptionDialog("Video Quality", options);
    }


    private void showAudioTrackDialog() {
        if (player == null || trackSelector == null) {
            Toast.makeText(this, "Player is not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AudioOption> audioOptions = getAvailableAudioOptions();
        if (audioOptions.size() <= 1) {
            Toast.makeText(this, "No other audio tracks available", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<DialogOption> options = new ArrayList<>();
        for (AudioOption option : audioOptions) {
            options.add(new DialogOption(
                    getAudioOptionLabel(option),
                    true,
                    option.selected,
                    () -> {
                        setFixedAudioTrack(option);
                        Toast.makeText(this, "Audio: " + option.label, Toast.LENGTH_SHORT).show();
                    }
            ));
        }

        showOptionDialog("Audio", options);
    }

    private void updateAudioTrackButtonVisibility() {
        if (audioTrackBtn == null) {
            return;
        }

        boolean visible = player != null && getAvailableAudioOptions().size() > 1;
        audioTrackBtn.setVisibility(visible ? VISIBLE : GONE);
    }

    private List<AudioOption> getAvailableAudioOptions() {
        ArrayList<AudioOption> options = new ArrayList<>();
        if (player == null) {
            return options;
        }

        int displayIndex = 1;
        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_AUDIO || !group.isSupported()) {
                continue;
            }

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (!group.isTrackSupported(trackIndex)) {
                    continue;
                }

                Format format = group.getTrackFormat(trackIndex);
                String label = getAudioTrackLabel(format, displayIndex);
                options.add(new AudioOption(
                        label,
                        group,
                        trackIndex,
                        group.isTrackSelected(trackIndex),
                        isPreferredMizoAudio(format)
                ));
                displayIndex++;
            }
        }

        return options;
    }

    private String getAudioOptionLabel(AudioOption option) {
        return option.selected ? "✓ " + option.label : option.label;
    }

    private String getAudioTrackLabel(Format format, int index) {
        String label = safeTrim(format.label);
        if (label == null) {
            label = getLanguageDisplayName(format.language);
        }
        if (label == null) {
            label = "Audio " + index;
        }

        String details = getAudioTrackDetails(format);
        if (details != null) {
            label += " • " + details;
        }

        return label;
    }

    private String getLanguageDisplayName(@Nullable String language) {
        String code = safeTrim(language);
        if (code == null || "und".equalsIgnoreCase(code)) {
            return null;
        }

        String lower = code.toLowerCase(Locale.US);
        if ("lus".equals(lower) || "miz".equals(lower)) {
            return "Mizo";
        }

        Locale locale = Locale.forLanguageTag(code);
        String display = locale.getDisplayLanguage(Locale.ENGLISH);
        if (display != null && !display.trim().isEmpty() && !display.equalsIgnoreCase(code)) {
            return display;
        }

        return code.toUpperCase(Locale.US);
    }

    private String getAudioTrackDetails(Format format) {
        if (format.channelCount > 0) {
            if (format.channelCount == 1) {
                return "Mono";
            }
            if (format.channelCount == 2) {
                return "Stereo";
            }
            return format.channelCount + "ch";
        }

        String mime = safeTrim(format.sampleMimeType);
        if (mime == null) {
            return null;
        }

        int slash = mime.indexOf('/');
        return slash >= 0 && slash + 1 < mime.length()
                ? mime.substring(slash + 1).toUpperCase(Locale.US)
                : mime.toUpperCase(Locale.US);
    }

    @Nullable
    private String safeTrim(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isPreferredMizoAudio(Format format) {
        String language = safeTrim(format.language);
        if (language != null) {
            String lowerLanguage = language.toLowerCase(Locale.US);
            if ("lus".equals(lowerLanguage) || "miz".equals(lowerLanguage)) {
                return true;
            }
        }

        String label = safeTrim(format.label);
        if (label == null) {
            return false;
        }

        String lowerLabel = label.toLowerCase(Locale.US);
        return lowerLabel.contains("mizo")
                || lowerLabel.contains("lus")
                || lowerLabel.contains("miz");
    }

    private void applyPreferredAudioTrackIfNeeded() {
        if (preferredAudioApplied || audioSelectionTouched || player == null || trackSelector == null) {
            return;
        }

        List<AudioOption> options = getAvailableAudioOptions();
        if (options.isEmpty()) {
            return;
        }

        preferredAudioApplied = true;
        for (AudioOption option : options) {
            if (option.preferredMizo && !option.selected) {
                setAudioTrackOverride(option, false);
                return;
            }
        }
    }

    private void setFixedAudioTrack(AudioOption option) {
        audioSelectionTouched = true;
        preferredAudioApplied = true;
        setAudioTrackOverride(option, true);
    }

    private void setAudioTrackOverride(AudioOption option, boolean showButton) {
        if (trackSelector == null) {
            return;
        }

        trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                        .setOverrideForType(
                                new TrackSelectionOverride(
                                        option.group.getMediaTrackGroup(),
                                        option.trackIndex
                                )
                        )
        );

        if (showButton) {
            updateAudioTrackButtonVisibility();
        }
    }

    private void showSpeedDialog() {
        if (player == null) {
            Toast.makeText(this, "Player is not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        final float[] speeds = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
        ArrayList<DialogOption> options = new ArrayList<>();

        for (float speed : speeds) {
            options.add(new DialogOption(
                    getSpeedLabel(speed),
                    true,
                    Float.compare(speed, selectedPlaybackSpeed) == 0,
                    () -> {
                        selectedPlaybackSpeed = speed;
                        player.setPlaybackSpeed(selectedPlaybackSpeed);
                        Toast.makeText(this, "Speed: " + formatSpeed(selectedPlaybackSpeed), Toast.LENGTH_SHORT).show();
                    }
            ));
        }

        showOptionDialog("Playback Speed", options);
    }

    private String getSpeedLabel(float speed) {
        String label = formatSpeed(speed);
        if (Float.compare(speed, selectedPlaybackSpeed) == 0) {
            return "✓ " + label;
        }
        return label;
    }

    private String formatSpeed(float speed) {
        if (Float.compare(speed, 1f) == 0) {
            return "Normal";
        }

        if (speed == Math.round(speed)) {
            return String.format(Locale.US, "%.0fx", speed);
        }

        return String.format(Locale.US, "%.2fx", speed);
    }

    private String getAutoQualityLabel() {
        if (selectedQualityHeight <= 0) {
            return "✓ " + getAutoQualityText();
        }
        return "Auto";
    }

    private String getAutoQualityText() {
        int currentHeight = getCurrentVideoHeight();
        if (currentHeight > 0) {
            return "Auto (" + currentHeight + "p)";
        }
        return "Auto";
    }

    private String getQualityLabel(QualityOption option) {
        String label = option.label;
        if (option.height == selectedQualityHeight) {
            label = "✓ " + label;
        }
        if (option.restricted) {
            label += " (Require plan upgrade)";
        }
        return label;
    }

    private List<QualityOption> getAvailableQualityOptions() {
        Map<Integer, QualityOption> qualityMap = new HashMap<>();

        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO || !group.isSupported()) {
                continue;
            }

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (!group.isTrackSupported(trackIndex)) {
                    continue;
                }

                Format format = group.getTrackFormat(trackIndex);
                int height = format.height;
                if (height <= 0) {
                    continue;
                }

                QualityOption existing = qualityMap.get(height);
                if (existing == null || format.bitrate > existing.bitrate) {
                    qualityMap.put(
                            height,
                            new QualityOption(
                                    height + "p",
                                    group,
                                    trackIndex,
                                    format.bitrate,
                                    isQualityRestricted(height)
                            )
                    );
                }
            }
        }

        ArrayList<QualityOption> options = new ArrayList<>(qualityMap.values());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            options.sort(Comparator.comparingInt((QualityOption option) -> option.height).reversed());
        }
        return options;
    }

    private int getCurrentVideoHeight() {
        if (player == null) {
            return 0;
        }

        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) {
                continue;
            }

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (group.isTrackSelected(trackIndex)) {
                    int height = group.getTrackFormat(trackIndex).height;
                    if (height > 0) {
                        return height;
                    }
                }
            }
        }
        return 0;
    }

    private String getCurrentTrackInfo() {
        if (player == null) return "no player";

        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) {
                continue;
            }

            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (group.isTrackSelected(trackIndex)) {
                    Format format = group.getTrackFormat(trackIndex);
                    int height = format.height;
                    int bitrate = format.bitrate;
                    String mime = format.sampleMimeType != null ? format.sampleMimeType : "unknown";
                    return String.format(Locale.US, "selected=%dp bitrate=%dk mime=%s", height, bitrate / 1000, mime);
                }
            }
        }

        return "no video selected";
    }

    private void setAutoQuality() {
        selectedQualityHeight = 0;
        DefaultTrackSelector.Parameters.Builder builder = trackSelector.buildUponParameters()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .clearVideoSizeConstraints()
                .setForceHighestSupportedBitrate(false)
                .setForceLowestBitrate(false);
        applyMaxQualityConstraint(builder);
        trackSelector.setParameters(builder);
    }

    private void setFixedQuality(QualityOption option) {
        if (option.restricted) {
            Toast.makeText(this, option.label + " is not available for your plan", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedQualityHeight = option.height;
        trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .clearVideoSizeConstraints()
                        .setOverrideForType(
                                new TrackSelectionOverride(
                                        option.group.getMediaTrackGroup(),
                                        option.trackIndex
                                )
                        )
        );
    }

    private void applyMaxQualityConstraint(DefaultTrackSelector.Parameters.Builder builder) {
        int maxAllowed = parseMaxQualityHeight();
        if (maxAllowed > 0) {
            builder.setMaxVideoSize(Integer.MAX_VALUE, maxAllowed);
        }
    }

    private boolean isQualityRestricted(int height) {
        int maxAllowed = parseMaxQualityHeight();
        if (maxAllowed <= 0) return false;
        return height > maxAllowed;
    }

    /**
     * Parse the server-provided `maxQuality` string into a maximum height (pixels).
     * Supports tokens like "SD", "HD", "FHD", numeric values ("1080" or "1080p"),
     * and common 4K/UHD tokens. Returns -1 when not set / unlimited.
     */
    private int parseMaxQualityHeight() {
        if (maxQuality == null) return -1;

        String v = maxQuality.trim().toUpperCase();
        if (v.isEmpty()) return -1;

        // Common named presets
        switch (v) {
            case "SD":
                return 360;
            case "HD":
                return 720;
            case "FHD":
            case "FULLHD":
            case "1080P":
            case "1080":
                return 1080;
            case "4K":
            case "UHD":
            case "2160P":
            case "2160":
                return 2160;
            default:
                break;
        }

        // Try to extract digits like "720p" or "720"
        try {
            String digits = v.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                return Integer.parseInt(digits);
            }
        } catch (Exception ignored) {
        }

        return -1;
    }

    private static class QualityOption {
        final String label;
        final Tracks.Group group;
        final int trackIndex;
        final int height;
        final int bitrate;
        final boolean restricted;

        QualityOption(String label, Tracks.Group group, int trackIndex, int bitrate, boolean restricted) {
            this.label = label;
            this.group = group;
            this.trackIndex = trackIndex;
            this.height = group.getTrackFormat(trackIndex).height;
            this.bitrate = bitrate;
            this.restricted = restricted;
        }
    }

    private static class AudioOption {
        final String label;
        final Tracks.Group group;
        final int trackIndex;
        final boolean selected;
        final boolean preferredMizo;

        AudioOption(String label, Tracks.Group group, int trackIndex, boolean selected, boolean preferredMizo) {
            this.label = label;
            this.group = group;
            this.trackIndex = trackIndex;
            this.selected = selected;
            this.preferredMizo = preferredMizo;
        }
    }

    private static class DialogOption {
        final String label;
        final boolean enabled;
        final boolean selected;
        final Runnable action;

        DialogOption(String label, boolean enabled, boolean selected, Runnable action) {
            this.label = label;
            this.enabled = enabled;
            this.selected = selected;
            this.action = action;
        }
    }

    private void stopStreamApi(long watchPosition) {
        String accessToken = SessionManager.getAccessToken(this);
        String deviceToken = SessionManager.getUserDeviceId(this);

        JsonObject body = new JsonObject();

        body.addProperty("stream_token", streamToken);
        body.addProperty("movie_id", movieId);
        body.addProperty("watch_position", watchPosition);
        body.addProperty("user_id", SessionManager.getUserId(PlayerActivity.this));
        body.addProperty("duration", playbackDuration);
        body.addProperty("content_type", isEpisode ? "episode" : "movie");

        Api.getApi().stopStream(
                AuthHeader.bearer(accessToken),
                deviceToken,
                body
        ).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                Log.e("PING", "✅ Stream stopped");
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e("PING", "❌ Stop error: " + t.getMessage());
            }
        });
    }

    private void loadAlsoLikeData(String accessToken, String userId, String contentId, String str) {
        boolean isAgeRestrict = SessionManager.getAgeRestriction(PlayerActivity.this);

        Call<List<Movie>> call = Api.getApi().getAlsoLike(AuthHeader.bearer(accessToken), contentId, userId, str,isAgeRestrict);
        Objects.requireNonNull(call).enqueue(new Callback<List<Movie>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<List<Movie>> call, @NonNull Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    movies.clear();
                    movies.addAll(response.body());
                    setPlaylistArrowVisible(false);

                    PlaylistAdapter adapter = new PlaylistAdapter(
                            movies,
                            item -> {
                                Movie selectedMovie = (Movie) item;

                                startStream(null, selectedMovie, "movie");
                            },
                            PlayerActivity.this::hidePlaylistAndFocusControls
                    );

                    seasonPlaylistScroll.setVisibility(GONE);
                    seasonPlaylistContainer.removeAllViews();
                    playlistRecyclerView.setVisibility(VISIBLE);
                    playlistRecyclerView.setOnKeyListener((v, keyCode, event) -> {
                        if (event.getAction() == KeyEvent.ACTION_DOWN
                                && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            return hidePlaylistAndFocusControls();
                        }
                        return false;
                    });
                    playlistRecyclerView.setAdapter(adapter);

                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Movie>> call, @NonNull Throwable t) {
                setPlaylistArrowVisible(false);

            }
        });
    }

    private void startStream(@Nullable Episode episode, @Nullable Movie movie, String type) {
        loader.setVisibility(VISIBLE);

        if (player != null) {
            player.pause();
        }

        String accessToken = SessionManager.getAccessToken(this);
        String userId = SessionManager.getUserId(this);
        String deviceId = SessionManager.getUserDeviceId(this);

        String mId = movie != null ? movie.id : Objects.requireNonNull(episode).id;

        String seasonId = episode != null ? episode.seasonId : "";

        StreamRepository.startStream(accessToken, deviceId, userId, subscriptionId, mId, seasonId, type, new StreamRepository.StreamCallback() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(StreamResult result) {
                loader.setVisibility(GONE);
                movieId = type.equals("movie") ? Objects.requireNonNull(movie).id : Objects.requireNonNull(episode).id;
                streamUrl =  result.streamUrl;
                streamToken = result.streamToken;
                maxQuality = result.maxQuality;
                watchPosition = result.watch_position != null ? result.watch_position : 0L;

                if (type.equals("movie")) {
                    isEpisode = false;
                    setPlaylistArrowVisible(false);
                    episodeTitle.setVisibility(GONE);
                    titleView.setText(movie.title);

                } else {
                    isEpisode = true;
                    currentEpisodeNumber = episode.episodeNumber;
                    titleView.setText(title);
                    episodeTitle.setVisibility(VISIBLE);
                    episodeTitle.setText("Episode " + currentEpisodeNumber);
                }

                initPlayer(watchPosition);

                if (movie != null && movie.isSeason) {
                    isEpisode = true;

                    movies.clear();

                    loadSeasonData(movie.num);
                } else if (movie != null){
                    isEpisode = false;

                    loadAlsoLikeData(accessToken, userId, Objects.requireNonNull(movie).id, movie.title);
                }

                currentEpisodeIndex = findCurrentEpisodeIndex();
                updateEpisodeNavButtons();
            }

            @Override
            public void onError(String title, String message) {
                onError("", title, message);
            }

            @Override
            public void onError(String code, String title, String message) {
                loader.setVisibility(GONE);
                if (isDeviceRevokedError(code, title, message)) {
                    forceLogoutForDeviceRevoked(message);
                    return;
                }
                showStartStreamErrorDialog(title, message, episode, movie, type);
            }

            @Override
            public void onFailure(Throwable throwable) {
                loader.setVisibility(GONE);
                AppDialog.show(
                        PlayerActivity.this,
                        R.drawable.error,
                        "Error",
                        throwable.getMessage() != null ? throwable.getMessage() : "Request failed",
                        false,
                        false,
                        null,
                        null
                );
            }
        });
    }

    private void showStartStreamErrorDialog(
            String title,
            String message,
            @Nullable Episode requestedEpisode,
            @Nullable Movie requestedMovie,
            String requestedType
    ) {
        String lowerTitle = title == null ? "" : title.toLowerCase();
        String lowerMessage = message == null ? "" : message.toLowerCase();
        boolean isPpv = lowerMessage.contains("rent");
        boolean isSubscription = lowerTitle.contains("subscription") || lowerMessage.contains("subscri");

        if (isPpv || isSubscription) {
            if (!SessionManager.getIsDeviceOwner(this)) {
                show(
                        PlayerActivity.this,
                        R.drawable.warning,
                        "Access Restricted",
                        "Only the account owner can stream PPV content, rent, or subscribe.",
                        true,
                        false,
                        null,
                        this::resumePreviousStream
                );
                return;
            }
            show(
                    PlayerActivity.this,
                    R.drawable.warning,
                    title,
                    message,
                    true,
                    false,
                    () -> startPayment(isPpv, requestedEpisode, requestedMovie, requestedType),
                    this::resumePreviousStream
            );
        } else {
            show(
                    PlayerActivity.this,
                    R.drawable.warning,
                    title,
                    message,
                    true,
                    false,
                    null,
                    this::resumePreviousStream
            );
        }
    }

    private boolean isDeviceRevokedError(String code, String title, String message) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase();
        String normalizedMessage = message == null ? "" : message.trim().toLowerCase();

        return "DEVICE_REVOKED".equals(normalizedCode)
                || normalizedTitle.contains("device access changed")
                || normalizedMessage.contains("device was removed")
                || normalizedMessage.contains("no longer linked")
                || normalizedMessage.contains("sign in again on this device");
    }

    private void forceLogoutForDeviceRevoked(String message) {
        if (player != null) {
            player.release();
        }
        SessionManager.logoutWithReason(
                this,
                message != null && !message.trim().isEmpty()
                        ? message
                        : "Your device was removed from this plan after renewal. Please sign in again to continue."
        );
    }

    private void resumePreviousStream() {
        loader.setVisibility(GONE);

        if (player != null) {
            player.play();
        }

    }

    private void generateQrPayment(
            boolean isPpv,
            @Nullable Episode requestedEpisode,
            @Nullable Movie requestedMovie,
            String requestedType
    ) {
        if (!SessionManager.getIsDeviceOwner(this)) {
            show(
                    PlayerActivity.this,
                    R.drawable.warning,
                    "Access Restricted",
                    "Only the account owner can subscribe or rent content.",
                    true,
                    false,
                    null,
                    this::resumePreviousStream
            );
            return;
        }
        pendingQrIsPpv = isPpv;
        pendingQrEpisode = requestedEpisode;
        pendingQrMovie = requestedMovie;
        pendingQrType = requestedType;

        String contentId = requestedEpisode != null
                ? requestedEpisode.id
                : requestedMovie != null ? requestedMovie.id : movieId;
        String contentType = requestedType == null ? type : requestedType;

        QrPaymentRequest request = new QrPaymentRequest(
                SessionManager.getUserDeviceId(this),
                isPpv ? contentId : null,
                SessionManager.getUserDeviceName(this),
                "tv",
                null,
                isPpv ? "INR" : null,
                null,
                isPpv ? "ppv" : "subscription",
                null,
                null,
                null,
                null,
                "payment",
                "initialized",
                SessionManager.getUserId(this),
                null,
                isPpv ? contentType : null,
                subscriptionId
        );

        showQrPaymentLoadingDialog();

        Api.getApi().createPaymentQr(
                AuthHeader.bearer(SessionManager.getAccessToken(this)),
                SessionManager.getUserDeviceId(this),
                request
        ).enqueue(new Callback<QrLoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<QrLoginResponse> call, @NonNull Response<QrLoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showQrPaymentCode(
                            response.body().getToken(),
                            response.body().getExpiresIn(),
                            requestedEpisode,
                            requestedMovie,
                            requestedType
                    );
                } else {
                    showRecreateQrPaymentState("Failed");
                }
            }

            @Override
            public void onFailure(@NonNull Call<QrLoginResponse> call, @NonNull Throwable t) {
                showRecreateQrPaymentState("Failed");
            }
        });
    }

    private void startPayment(
            boolean isPpv,
            @Nullable Episode requestedEpisode,
            @Nullable Movie requestedMovie,
            String requestedType
    ) {
        if (isPpv || !PaymentFeatureConfig.isAmazonIapEnabled()) {
            generateQrPayment(isPpv, requestedEpisode, requestedMovie, requestedType);
            return;
        }

        pendingQrIsPpv = isPpv;
        pendingQrEpisode = requestedEpisode;
        pendingQrMovie = requestedMovie;
        pendingQrType = requestedType;

        Intent intent = new Intent(this, AmazonIapActivity.class);
        startActivityForResult(intent, AMAZON_IAP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != AMAZON_IAP_REQUEST_CODE) return;
        if (resultCode == RESULT_OK) {
            startStream(
                    pendingQrEpisode,
                    pendingQrMovie,
                    pendingQrType == null ? type : pendingQrType
            );
        } else {
            resumePreviousStream();
        }
    }

    private void showQrPaymentLoadingDialog() {
        closeQrPaymentDialog();

        qrPaymentDialog = createQrPaymentDialog();
        qrPaymentDialog.setContentView(R.layout.dialog_qr_payment);
        setQrPaymentLoadingState();
        qrPaymentDialog.show();
        applyQrPaymentDialogWindow();
    }

    private void showQrPaymentCode(
            String token,
            int expiresIn,
            @Nullable Episode requestedEpisode,
            @Nullable Movie requestedMovie,
            String requestedType
    ) {
        if (qrPaymentDialog == null) {
            qrPaymentDialog = createQrPaymentDialog();
            qrPaymentDialog.setContentView(R.layout.dialog_qr_payment);
        }

        ImageView qrImage = qrPaymentDialog.findViewById(R.id.imgQR);
        TextView showQr = qrPaymentDialog.findViewById(R.id.showQR);
        TextView qrStatus = qrPaymentDialog.findViewById(R.id.qrCount);

        qrImage.setVisibility(VISIBLE);
        showQr.setVisibility(GONE);
        showQr.setOnClickListener(null);
        startQrPaymentCountdown(qrStatus, expiresIn);
        QRUtils.generateQR(qrImage, token);

        if (!qrPaymentDialog.isShowing()) {
            qrPaymentDialog.show();
        }
        applyQrPaymentDialogWindow();
        listenQrPayment(token, requestedEpisode, requestedMovie, requestedType);
    }

    private void setQrPaymentLoadingState() {
        ImageView qrImage = qrPaymentDialog.findViewById(R.id.imgQR);
        TextView showQr = qrPaymentDialog.findViewById(R.id.showQR);
        TextView qrStatus = qrPaymentDialog.findViewById(R.id.qrCount);

        qrImage.setVisibility(VISIBLE);
        showQr.setVisibility(VISIBLE);
        showQr.setText("Generating...");
        showQr.setEnabled(false);
        showQr.setFocusable(false);
        showQr.setFocusableInTouchMode(false);
        showQr.setClickable(false);
        showQr.setOnClickListener(null);
        qrStatus.setText("");
    }

    private void startQrPaymentCountdown(@NonNull TextView qrStatus, int expiresIn) {
        stopQrPaymentCountdown();

        final int[] remainingSeconds = {Math.max(expiresIn, 0)};
        qrPaymentCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                int minutes = remainingSeconds[0] / 60;
                int seconds = remainingSeconds[0] % 60;
                qrStatus.setText(String.format(Locale.US, "Expires in %02d:%02d", minutes, seconds));

                if (remainingSeconds[0] <= 0) {
                    showRecreateQrPaymentState("Expired");
                    return;
                }

                remainingSeconds[0]--;
                qrPaymentHandler.postDelayed(this, 1000);
            }
        };
        qrPaymentCountdownRunnable.run();
    }

    private void showRecreateQrPaymentState(@NonNull String statusText) {
        stopQrPaymentCountdown();
        clearQrPaymentListener();

        if (qrPaymentDialog == null) {
            qrPaymentDialog = createQrPaymentDialog();
            qrPaymentDialog.setContentView(R.layout.dialog_qr_payment);
        }

        ImageView qrImage = qrPaymentDialog.findViewById(R.id.imgQR);
        TextView showQr = qrPaymentDialog.findViewById(R.id.showQR);
        TextView qrStatus = qrPaymentDialog.findViewById(R.id.qrCount);

        qrImage.setVisibility(VISIBLE);
        qrStatus.setText(statusText);
        showQr.setVisibility(VISIBLE);
        showQr.setEnabled(true);
        showQr.setFocusable(true);
        showQr.setFocusableInTouchMode(true);
        showQr.setClickable(true);
        showQr.setText("Recreate QR");
        showQr.setOnClickListener(v -> generateQrPayment(
                pendingQrIsPpv,
                pendingQrEpisode,
                pendingQrMovie,
                pendingQrType
        ));

        if (!qrPaymentDialog.isShowing()) {
            qrPaymentDialog.show();
            applyQrPaymentDialogWindow();
        }

        showQr.requestFocus();
    }

    private void stopQrPaymentCountdown() {
        if (qrPaymentCountdownRunnable != null) {
            qrPaymentHandler.removeCallbacks(qrPaymentCountdownRunnable);
            qrPaymentCountdownRunnable = null;
        }
    }

    private Dialog createQrPaymentDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(d -> {
            clearQrPaymentListener();
            resumePreviousStream();
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        return dialog;
    }

    private void applyQrPaymentDialogWindow() {
        if (qrPaymentDialog == null) return;

        Window window = qrPaymentDialog.getWindow();
        if (window == null) return;

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );
    }

    private void listenQrPayment(
            String token,
            @Nullable Episode requestedEpisode,
            @Nullable Movie requestedMovie,
            String requestedType
    ) {
        clearQrPaymentListener();

        qrPaymentRef = FirebaseDatabase.getInstance()
                .getReference("qr_sessions")
                .child(token);

        qrPaymentListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = firebaseString(snapshot.child("status"));
                if (status == null) {
                    return;
                }

                String normalizedStatus = status.toLowerCase();
                if (normalizedStatus.equals("pending")) {
                    stopQrPaymentCountdown();
                    setQrPaymentStatusText("QR scanned...");
                } else if (normalizedStatus.equals("payment_started")) {
                    stopQrPaymentCountdown();
                    setQrPaymentStatusText("Processing...");
                } else if (normalizedStatus.equals("payment_completed")
                        || normalizedStatus.equals("success")
                        || normalizedStatus.equals("completed")
                        || normalizedStatus.equals("paid")) {
                    stopQrPaymentCountdown();
                    setQrPaymentStatusText("Success");
                    closeQrPaymentDialog();
                    startStream(
                            requestedEpisode,
                            requestedMovie,
                            requestedType == null ? type : requestedType
                    );
                } else if (normalizedStatus.equals("failed")
                        || normalizedStatus.equals("expired")
                        || normalizedStatus.equals("cancelled")) {
                    showRecreateQrPaymentState("Failed");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PlayerActivity", "QR payment listener cancelled: " + error.getMessage());
                showRecreateQrPaymentState("Error");
            }
        };

        qrPaymentRef.addValueEventListener(qrPaymentListener);
    }

    private void setQrPaymentStatusText(@NonNull String text) {
        if (qrPaymentDialog == null) return;

        TextView qrStatus = qrPaymentDialog.findViewById(R.id.qrCount);
        if (qrStatus != null) {
            qrStatus.setText(text);
        }
    }

    private void closeQrPaymentDialog() {
        stopQrPaymentCountdown();
        clearQrPaymentListener();

        if (qrPaymentDialog != null && qrPaymentDialog.isShowing()) {
            qrPaymentDialog.setOnDismissListener(null);
            qrPaymentDialog.dismiss();
        }

        qrPaymentDialog = null;
    }

    private void clearQrPaymentListener() {
        if (qrPaymentRef != null && qrPaymentListener != null) {
            qrPaymentRef.removeEventListener(qrPaymentListener);
        }

        qrPaymentRef = null;
        qrPaymentListener = null;
    }

    private static String firebaseString(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        if (value instanceof String) return (String) value;
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        setPlayerScreenAwake(false);
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        long watchPosition = 0L;

        if (player != null) {
            watchPosition = player.getCurrentPosition();
        }

        stopStreamApi(watchPosition);

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        setPlayerScreenAwake(false);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPlayerScreenAwake(true);
    }
}
