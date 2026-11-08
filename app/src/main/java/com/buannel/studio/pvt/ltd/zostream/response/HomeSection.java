package com.buannel.studio.pvt.ltd.zostream.response;

import com.buannel.studio.pvt.ltd.zostream.model.Movie;

import java.util.List;

public class HomeSection {

    public String title;
    public List<Movie> movies;

    public HomeSection(String title, List<Movie> movies) {
        this.title = title;
        this.movies = movies;
    }
}
