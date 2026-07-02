package com.buannel.studio.pvt.ltd.zostream.response;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

public class BannerResponse {

    @SerializedName("status")
    private boolean status;

    @SerializedName("data")
    private List<Banner> data;

    public BannerResponse(List<Banner> data) {
        this.data = data;
    }

    public boolean isStatus() {
        return status;
    }

    public List<Banner> getData() {
        return data;
    }

    // ================== INNER MODEL ==================
    public static class Banner {

        @SerializedName("id")
        private int id;

        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        @SerializedName("type")
        private String type;

        @SerializedName("media_type")
        private String mediaType;

        @SerializedName("media_url")
        private String mediaUrl;

        @SerializedName("thumbnail_url")
        private String thumbnailUrl;

        @SerializedName("target_type")
        private String targetType;

        @SerializedName("target_id")
        private String targetId;

        @SerializedName("target_url")
        private String targetUrl;

        @SerializedName("priority")
        private int priority;

        @SerializedName("is_active")
        private boolean isActive;

        @SerializedName("age_restriction_enabled")
        private boolean ageRestrictionEnabled;

        @SerializedName("min_age")
        private Integer minAge;

        @SerializedName("max_age")
        private Integer maxAge;

        @SerializedName("age_rating")
        private String ageRating;

        @SerializedName("requires_parental_pin")
        private boolean requiresParentalPin;

        @SerializedName("start_date")
        private Date startDate;

        @SerializedName("end_date")
        private Date endDate;

        @SerializedName("button_text")
        private String buttonText;

        private String batch;

        // ================== GETTERS ==================

        public int getId() { return id; }

        public String getTitle() { return title; }

        public String getDescription() { return description; }

        public String getType() { return type; }

        public String getMediaType() { return mediaType; }

        public String getMediaUrl() { return mediaUrl; }

        public String getThumbnailUrl() { return thumbnailUrl; }

        public String getTargetType() { return targetType; }

        public String getTargetId() { return targetId; }

        public String getTargetUrl() { return targetUrl; }

        public int getPriority() { return priority; }

        public boolean isActive() { return isActive; }

        public boolean isAgeRestrictionEnabled() { return ageRestrictionEnabled; }

        public Integer getMinAge() { return minAge; }

        public Integer getMaxAge() { return maxAge; }

        public String getAgeRating() { return ageRating; }

        public boolean isRequiresParentalPin() { return requiresParentalPin; }

        public Date getStartDate() { return startDate; }

        public Date getEndDate() { return endDate; }

        public String getButtonText() { return buttonText; }

        public String getBatch() {
            return batch;
        }

        public void setBatch(String batch) {
            this.batch = batch;
        }
    }
}