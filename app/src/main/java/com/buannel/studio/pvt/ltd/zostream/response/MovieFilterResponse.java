package com.buannel.studio.pvt.ltd.zostream.response;

import com.buannel.studio.pvt.ltd.zostream.model.Movie;

import java.util.List;

public class MovieFilterResponse {
    public String status;
    public Filters filters;
    public List<Movie> data;
    public Pagination pagination;

    public static class Filters {
        public String category;
        public String genre;
    }

    public static class Pagination {
        public int current_page;
        public int per_page;
        public int total;
        public int last_page;
        public String next_page_url;
        public String prev_page_url;
    }
}