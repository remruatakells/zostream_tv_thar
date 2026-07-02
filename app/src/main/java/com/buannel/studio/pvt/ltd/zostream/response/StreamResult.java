package com.buannel.studio.pvt.ltd.zostream.response;

public class StreamResult {
    public final String streamUrl;
    public final String streamToken;
    public final String maxQuality;
    public final Long watch_position;

    public StreamResult(String streamUrl, String streamToken, String maxQuality, Long watch_position) {
        this.streamUrl = streamUrl;
        this.streamToken = streamToken;
        this.maxQuality = maxQuality;
        this.watch_position = watch_position;
    }
}
