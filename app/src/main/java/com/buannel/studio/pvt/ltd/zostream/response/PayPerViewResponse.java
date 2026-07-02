package com.buannel.studio.pvt.ltd.zostream.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class PayPerViewResponse implements Serializable {

    @SerializedName("status")
    public String status;

    @SerializedName("message")
    public String message;

    @SerializedName("errors")
    public Object errors;

    @SerializedName("data")
    public Data data;

    public static class Data implements Serializable {

        @SerializedName("type")
        public String type; // movie / episode

        @SerializedName("payment_movie_id")
        public String paymentMovieId;

        @SerializedName("payment_options")
        public PaymentOptions paymentOptions;

        @SerializedName("movie_id")
        public String movieId;

        @SerializedName("movie_num")
        public Integer movieNum;

        @SerializedName("episode_id")
        public String episodeId;

        @SerializedName("title")
        public String title;

        @SerializedName("poster")
        public String poster;

        @SerializedName("isPayPerView")
        public Boolean isPayPerView;

        @SerializedName("ppv_amount")
        public Double ppvAmount;

        @SerializedName("discount_percent")
        public Double discountPercent;

        @SerializedName("discount_amount")
        public Double discountAmount;

        @SerializedName("final_ppv_price")
        public Double finalPpvPrice;

        @SerializedName("season")
        public Season season;

        @SerializedName("content")
        public Content content;
    }

    public static class PaymentOptions implements Serializable {

        @SerializedName("movie")
        public PaymentOption movie;

        @SerializedName("episode")
        public PaymentOption episode;

        @SerializedName("season")
        public SeasonPaymentOption season;
    }

    public static class PaymentOption implements Serializable {

        @SerializedName("payment_movie_id")
        public String paymentMovieId;

        @SerializedName("ppv_amount")
        public Double ppvAmount;

        @SerializedName("discount_percent")
        public Double discountPercent;

        @SerializedName("discount_amount")
        public Double discountAmount;

        @SerializedName("final_ppv_price")
        public Double finalPpvPrice;
    }

    public static class SeasonPaymentOption extends PaymentOption implements Serializable {

        @SerializedName("ppv_episode_count")
        public Integer ppvEpisodeCount;

        @SerializedName("ppv_episodes_total_amount")
        public Double ppvEpisodesTotalAmount;

        @SerializedName("ppv_episodes_discount_amount")
        public Double ppvEpisodesDiscountAmount;

        @SerializedName("ppv_episodes_final_price")
        public Double ppvEpisodesFinalPrice;

        @SerializedName("season_benefit_amount")
        public Double seasonBenefitAmount;

        @SerializedName("season_benefit_message")
        public String seasonBenefitMessage;
    }

    public static class Season implements Serializable {

        @SerializedName("id")
        public String id;

        @SerializedName("movie_id")
        public String movieId;

        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("poster")
        public String poster;

        @SerializedName("amount")
        public Double amount;

        @SerializedName("movie")
        public Movie movie;

        @SerializedName("episodes")
        public List<Episode> episodes;
    }

    public static class Movie implements Serializable {

        @SerializedName("id")
        public String id;

        @SerializedName("num")
        public Integer num;

        @SerializedName("title")
        public String title;

        @SerializedName("poster")
        public String poster;

        @SerializedName("thumbnail")
        public String thumbnail;

        @SerializedName("ppv_amount")
        public Double ppvAmount;

        @SerializedName("isPayPerView")
        public Boolean isPayPerView;
    }

    public static class Episode implements Serializable {

        @SerializedName("id")
        public String id;

        @SerializedName("season_id")
        public String seasonId;

        @SerializedName("movie_id")
        public String movieId;

        @SerializedName("episode_number")
        public Integer episodeNumber;

        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("thumbnail")
        public String thumbnail;

        @SerializedName("amount")
        public Double amount;

        @SerializedName("isPayPerView")
        public Boolean isPayPerView;
    }

    /**
     * Flexible because:
     * movie => content = movie object
     * episode => content = episode object (with nested season sometimes)
     */
    public static class Content implements Serializable {

        @SerializedName("id")
        public String id;

        @SerializedName("num")
        public Integer num;

        @SerializedName("movie_id")
        public String movieId;

        @SerializedName("season_id")
        public String seasonId;

        @SerializedName("episode_number")
        public Integer episodeNumber;

        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("poster")
        public String poster;

        @SerializedName("thumbnail")
        public String thumbnail;

        @SerializedName("ppv_amount")
        public Double ppvAmount;

        @SerializedName("amount")
        public Double amount;

        @SerializedName("isPayPerView")
        public Boolean isPayPerView;

        @SerializedName("season")
        public Season season;
    }
}
