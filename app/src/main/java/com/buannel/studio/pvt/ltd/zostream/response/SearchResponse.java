package com.buannel.studio.pvt.ltd.zostream.response;

import com.buannel.studio.pvt.ltd.zostream.model.SearchMovie;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchResponse {

    @SerializedName("type")
    public String type;

    @SerializedName("query")
    public String query;

    @SerializedName("results")
    public SearchResult results;

    public static class SearchResult {

        @SerializedName("current_page")
        public int currentPage;

        @SerializedName("data")
        public List<SearchMovie> data;

        @SerializedName("first_page_url")
        public String firstPageUrl;

        @SerializedName("from")
        public int from;

        @SerializedName("last_page")
        public int lastPage;

        @SerializedName("last_page_url")
        public String lastPageUrl;

        @SerializedName("links")
        public List<PageLink> links;

        @SerializedName("next_page_url")
        public String nextPageUrl;

        @SerializedName("path")
        public String path;

        @SerializedName("per_page")
        public int perPage;

        @SerializedName("prev_page_url")
        public String prevPageUrl;

        @SerializedName("to")
        public int to;

        @SerializedName("total")
        public int total;
    }

    public static class PageLink {

        @SerializedName("url")
        public String url;

        @SerializedName("label")
        public String label;

        @SerializedName("active")
        public boolean active;
    }
}