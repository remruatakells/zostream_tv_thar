package com.buannel.studio.pvt.ltd.zostream.ui.screens

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.buannel.studio.pvt.ltd.zostream.R
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.model.Movie
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieSelected: (Movie) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        val cleanQuery = query.trim()
        errorMessage = null

        if (cleanQuery.length < 2) {
            results = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        delay(450)

        Api.getApi().searchMovies(
            SessionManager.getUserId(context),
            cleanQuery,
            SessionManager.getAgeRestriction(context)
        )?.enqueue(object : Callback<List<Movie?>?> {
            override fun onResponse(
                call: Call<List<Movie?>?>,
                response: Response<List<Movie?>?>
            ) {
                isLoading = false
                if (response.isSuccessful) {
                    results = response.body()?.filterNotNull().orEmpty()
                } else {
                    results = emptyList()
                    errorMessage = "Search failed. Please try again."
                }
            }

            override fun onFailure(call: Call<List<Movie?>?>, t: Throwable) {
                isLoading = false
                results = emptyList()
                errorMessage = t.message ?: "Search failed. Please try again."
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060A12))
            .padding(horizontal = 42.dp, vertical = 34.dp)
    ) {
        Text(
            text = "Search",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(18.dp))
        SearchInput(
            query = query,
            onQueryChange = { query = it }
        )
        Spacer(modifier = Modifier.height(42.dp))

        when {
            query.trim().length < 2 -> SearchMessage("Type at least 2 characters to search.")
            isLoading -> SearchMessage("Searching...")
            errorMessage != null -> SearchMessage(errorMessage ?: "Search failed.")
            results.isEmpty() -> SearchMessage("No results found.")
            else -> SearchResults(
                movies = results,
                onMovieSelected = onMovieSelected
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .width(620.dp)
            .height(58.dp)
            .background(Color(0xFF111827), shape)
            .border(
                width = 2.dp,
                color = if (focused) Color(0xFFBFDBFE) else Color(0xFF334155),
                shape = shape
            )
            .padding(horizontal = 18.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                EditText(context).apply {
                    setSingleLine(true)
                    setTextColor(android.graphics.Color.WHITE)
                    setHintTextColor(android.graphics.Color.rgb(100, 116, 139))
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setHint("Search movies and series")
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                    setOnFocusChangeListener { view, hasFocus ->
                        focused = hasFocus
                        if (hasFocus) {
                            view.post {
                                val inputMethodManager = context.getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                                ) as InputMethodManager
                                inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                    }
                    setOnClickListener {
                        requestFocus()
                        val inputMethodManager = context.getSystemService(
                            Context.INPUT_METHOD_SERVICE
                        ) as InputMethodManager
                        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                    setOnEditorActionListener { view, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            val inputMethodManager = context.getSystemService(
                                Context.INPUT_METHOD_SERVICE
                            ) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                            true
                        } else {
                            false
                        }
                    }
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) = Unit

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            val newValue = s?.toString().orEmpty()
                            onQueryChange(newValue)
                        }

                        override fun afterTextChanged(s: android.text.Editable?) = Unit
                    })
                    post {
                        requestFocus()
                        val inputMethodManager = context.getSystemService(
                            Context.INPUT_METHOD_SERVICE
                        ) as InputMethodManager
                        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            },
            update = { editText ->
                if (editText.text.toString() != query) {
                    editText.setText(query)
                    editText.setSelection(query.length)
                }
            }
        )
    }
}

@Composable
private fun SearchResults(
    movies: List<Movie>,
    onMovieSelected: (Movie) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(movies) { movie ->
            Column {
                SearchMovieCard(
                    movie = movie,
                    onClick = onMovieSelected
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.title ?: "Untitled",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.width(260.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchMovieCard(
    movie: Movie,
    onClick: (Movie) -> Unit
) {
    Surface(
        onClick = { onClick(movie) },
        modifier = Modifier
            .padding(8.dp)
            .width(280.dp)
            .aspectRatio(16f / 9f),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                Color(0xFF384FFF),
                16.dp
            )
        ),
        border = com.buannel.studio.pvt.ltd.zostream.ui.components.tvFocusedItemBorder()
    ) {
        AsyncImage(
            model = movie.coverImg ?: movie.poster,
            contentDescription = movie.description ?: movie.title,
            placeholder = painterResource(id = R.drawable.placeholder),
            error = painterResource(id = R.drawable.placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchMessage(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = message,
            color = Color(0xFFCBD5E1),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
