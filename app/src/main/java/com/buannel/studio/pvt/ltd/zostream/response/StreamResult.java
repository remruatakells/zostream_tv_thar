package com.buannel.studio.pvt.ltd.zostream.response;

import com.buannel.studio.pvt.ltd.zostream.ads.ImageAd;

import java.util.HashMap;

public class StreamResult {
    public final String streamUrl;
    public final String streamToken;
    public final String maxQuality;
    public final Long watch_position;
    public final HashMap<String, ImageAd> ads;

    public StreamResult(String streamUrl, String streamToken, String maxQuality, Long watch_position, HashMap<String, ImageAd> ads) {
        this.streamUrl = streamUrl;
        this.streamToken = streamToken;
        this.maxQuality = maxQuality;
        this.watch_position = watch_position;
        this.ads = ads;
    }
}
