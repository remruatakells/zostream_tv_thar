package com.buannel.studio.pvt.ltd.zostream.response;

import com.buannel.studio.pvt.ltd.zostream.model.Movie;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GET /api/v4/recommendations/home response. Production clients may encounter
 * either the V4 envelope or the earlier direct payload during the rollout.
 */
public class HomeRecommendationResponse {

    public boolean success;
    public Data data;
    public String message;
    public ApiError error;

    // Compatibility fields for a direct (non-enveloped) recommendation payload.
    public String user;
    @SerializedName("history_size") public int historySize;
    public Map<String, Section> sections;

    public Section getSection(String key) {
        Map<String, Section> sectionMap = data != null && data.sections != null
                ? data.sections
                : sections;
        return sectionMap == null ? null : sectionMap.get(key);
    }

    public boolean hasSections() {
        return (data != null && data.sections != null) || sections != null;
    }

    public static class Data {
        public String user;

        @SerializedName("history_size")
        public int historySize;

        @SerializedName("content_mode")
        public String contentMode;

        public Map<String, Section> sections;
    }

    public static class ApiError {
        public String code;
        public String message;
    }

    public static class Section {
        public List<Item> items;
        public Pagination pagination;
        public Item anchor;

        public List<Movie> publishedMovies() {
            if (items == null || items.isEmpty()) return Collections.emptyList();

            List<Movie> movies = new ArrayList<>();
            for (Item item : items) {
                Movie movie = item == null ? null : item.toMovie();
                if (movie != null) movies.add(movie);
            }
            return movies;
        }
    }

    public static class Pagination {
        @SerializedName("current_page") public int currentPage;
        @SerializedName("per_page") public int perPage;
        public int returned;
        @SerializedName("has_more") public boolean hasMore;
        @SerializedName("next_page") public Integer nextPage;
    }

    public static class Item {
        public String id;
        public String title;
        public String status;
        public String genre;
        public String poster;
        // The recommendation rollout has used both the API snake_case field
        // and the older Android camelCase field. Always treat either as cover.
        @SerializedName(value = "cover_img", alternate = {"coverImg", "cover"})
        public String coverImg;
        public boolean premium;
        public boolean ppv;
        @SerializedName("parent_id") public String parentId;
        @SerializedName("series_title") public String seriesTitle;
        public String thumbnail;

        public Movie toMovie() {
            if (!"Published".equalsIgnoreCase(value(status))) return null;

            boolean episode = !value(parentId).isEmpty();
            String contentId = episode ? value(parentId) : value(id);
            if (contentId.isEmpty()) return null;

            Movie movie = new Movie();
            movie.id = contentId;
            movie.title = episode ? episodeDisplayTitle() : value(title);
            movie.genre = value(genre);
            // Keep both artwork variants. Other app screens still use poster,
            // while TV landscape cards prefer cover_img.
            movie.poster = firstNonEmpty(poster, thumbnail);
            movie.coverImg = firstNonEmpty(coverImg, movie.poster);
            movie.status = "Published";
            movie.isPremium = premium;
            movie.isPayPerView = ppv;
            movie.isSeason = episode;
            return movie;
        }

        private String episodeDisplayTitle() {
            String series = value(seriesTitle);
            String episodeTitle = value(title);
            if (series.isEmpty()) return episodeTitle;
            if (episodeTitle.isEmpty()) return series;
            return series + " • " + episodeTitle;
        }

        private static String firstNonEmpty(String... values) {
            for (String candidate : values) {
                String normalized = value(candidate);
                if (!normalized.isEmpty()) return normalized;
            }
            return "";
        }

        private static String value(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
