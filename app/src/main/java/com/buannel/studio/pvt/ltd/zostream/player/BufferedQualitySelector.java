package com.buannel.studio.pvt.ltd.zostream.player;

import android.content.Context;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.chunk.Chunk;
import androidx.media3.exoplayer.source.chunk.MediaChunk;
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.ForwardingTrackSelection;
import androidx.media3.exoplayer.upstream.BandwidthMeter;
import java.util.List;

/**
 * Downgrades change the next chunk without replacing the HLS selection object.
 * Upgrades release buffered media and reprepare at the current position.
 */
@UnstableApi
public final class BufferedQualitySelector extends DefaultTrackSelector {
    private final Factory factory;

    public BufferedQualitySelector(Context context) {
        this(context, new Factory());
    }

    private BufferedQualitySelector(Context context, Factory factory) {
        super(context, factory);
        this.factory = factory;
    }

    /** False means the target needs a different codec/group and a normal override. */
    public boolean selectFixed(ExoPlayer player, TrackGroup group, int trackIndex) {
        Selection active = factory.active;
        if (active == null || !active.getTrackGroup().equals(group)
                || active.indexOf(trackIndex) < 0) return false;
        factory.request = new Request(group, trackIndex);
        restartForUpgrade(player, group.getFormat(trackIndex).height);
        return true;
    }

    public static void restartForUpgrade(ExoPlayer player, int targetHeight) {
        Format displayed = player.getVideoFormat();
        if (displayed != null && displayed.height > 0 && targetHeight > displayed.height) {
            // stop() releases buffered media while retaining the media item,
            // position and playWhenReady. Reprepare directly at the fixed quality.
            player.stop();
            player.prepare();
        }
    }

    public void selectAuto() { factory.request = null; }

    static final class Request {
        final TrackGroup group;
        final int trackIndex;
        Request(TrackGroup group, int trackIndex) {
            this.group = group;
            this.trackIndex = trackIndex;
        }
    }

    static final class Factory implements ExoTrackSelection.Factory {
        private final AdaptiveTrackSelection.Factory adaptive = new AdaptiveTrackSelection.Factory();
        volatile Request request;
        volatile Selection active;

        @Override
        public ExoTrackSelection[] createTrackSelections(ExoTrackSelection.Definition[] definitions,
                BandwidthMeter meter, MediaPeriodId period, Timeline timeline) {
            ExoTrackSelection[] selections = adaptive.createTrackSelections(definitions, meter, period, timeline);
            for (int i = 0; i < selections.length; i++) {
                ExoTrackSelection selection = selections[i];
                if (selection != null && MimeTypes.getTrackType(selection.getFormat(0).sampleMimeType)
                        == C.TRACK_TYPE_VIDEO) {
                    selections[i] = new Selection(selection, this);
                }
            }
            return selections;
        }
    }

    static final class Selection extends ForwardingTrackSelection {
        private final BufferedQualitySelector.Factory factory;
        private int manualIndex = C.INDEX_UNSET;

        Selection(ExoTrackSelection adaptive, BufferedQualitySelector.Factory factory) {
            super(adaptive);
            this.factory = factory;
            Request request = factory.request;
            manualIndex = request != null && getTrackGroup().equals(request.group)
                    ? indexOf(request.trackIndex) : C.INDEX_UNSET;
        }

        @Override public void enable() { super.enable(); factory.active = this; }
        @Override public void disable() {
            if (factory.active == this) factory.active = null;
            super.disable();
        }

        @Override public void updateSelectedTrack(long positionUs, long bufferedUs, long availableUs,
                List<? extends MediaChunk> queue, MediaChunkIterator[] iterators) {
            super.updateSelectedTrack(positionUs, bufferedUs, availableUs, queue, iterators);
            Request request = factory.request;
            manualIndex = request != null && getTrackGroup().equals(request.group)
                    ? indexOf(request.trackIndex) : C.INDEX_UNSET;
        }

        @Override public int getSelectedIndex() {
            return manualIndex != C.INDEX_UNSET ? manualIndex : super.getSelectedIndex();
        }
        @Override public int getSelectedIndexInTrackGroup() {
            return getIndexInTrackGroup(getSelectedIndex());
        }
        @Override public Format getSelectedFormat() { return getFormat(getSelectedIndex()); }
        @Override public int getSelectionReason() {
            return manualIndex != C.INDEX_UNSET ? C.SELECTION_REASON_MANUAL : super.getSelectionReason();
        }
        @Override public Object getSelectionData() {
            return manualIndex != C.INDEX_UNSET ? null : super.getSelectionData();
        }
        @Override public int evaluateQueueSize(long positionUs, List<? extends MediaChunk> queue) {
            return factory.request != null ? queue.size() : super.evaluateQueueSize(positionUs, queue);
        }
        @Override public boolean shouldCancelChunkLoad(long positionUs, Chunk chunk,
                List<? extends MediaChunk> queue) {
            return factory.request == null && super.shouldCancelChunkLoad(positionUs, chunk, queue);
        }
        @Override public boolean excludeTrack(int index, long durationMs) {
            // A manual selection must retry/fail rather than silently change quality.
            return manualIndex == C.INDEX_UNSET && super.excludeTrack(index, durationMs);
        }
    }
}

