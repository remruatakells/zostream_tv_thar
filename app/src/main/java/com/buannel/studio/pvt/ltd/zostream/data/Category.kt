package com.buannel.studio.pvt.ltd.zostream.data

import com.buannel.studio.pvt.ltd.zostream.model.Movie

data class Category(
    val id: String,
    val name: String,
    val movieList: List<Movie>
)
