package com.buannel.studio.pvt.ltd.zostream.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CheckPpvRentalResponse implements Serializable {

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

        @SerializedName("movie_id")
        public String movieId; // for episode, this is content_id you sent

        @SerializedName("season_id")
        public String seasonId;

        @SerializedName("user_id")
        public String userId;

        @SerializedName("device_type")
        public String deviceType;

        @SerializedName("isPayPerView")
        public Boolean isPayPerView;

        @SerializedName("isRented")
        public Boolean isRented;

        @SerializedName("rented_by")
        public String rentedBy; // movie / episode / season

        @SerializedName("rentalPurchased")
        public String rentalPurchased;

        @SerializedName("rentalExpiry")
        public String rentalExpiry;

        @SerializedName("rental")
        public Rental rental;
    }

    public static class Rental implements Serializable {

        @SerializedName("id")
        public Integer id;

        @SerializedName("payment_movie_id")
        public String paymentMovieId;

        @SerializedName("transaction_id")
        public String transactionId;

        @SerializedName("amount")
        public Double amount;

        @SerializedName("currency")
        public String currency;

        @SerializedName("status")
        public String status;

        @SerializedName("expiry_date")
        public String expiryDate;
    }
}