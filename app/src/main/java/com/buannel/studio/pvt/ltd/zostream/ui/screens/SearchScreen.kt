package com.buannel.studio.pvt.ltd.zostream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var showKeyboard by remember { mutableStateOf(false) }

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
            onClick = { showKeyboard = true }
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

    if (showKeyboard) {
        SearchKeyboardDialog(
            query = query,
            onCharacter = { query += it },
            onSpace = {
                if (query.isNotEmpty() && !query.endsWith(' ')) {
                    query += " "
                }
            },
            onDelete = { query = query.dropLast(1) },
            onClear = { query = "" },
            onDone = { showKeyboard = false },
            onDismiss = { showKeyboard = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchInput(
    query: String,
    onClick: () -> Unit,
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
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = query.ifBlank { "Search movies and series" },
            color = if (query.isBlank()) Color(0xFF64748B) else Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SearchKeyboardDialog(
    query: String,
    onCharacter: (String) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstKeyFocusRequester = remember { FocusRequester() }
    val characterRows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M"),
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 850.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(18.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = query.ifBlank { "Search movies and series" },
                    color = if (query.isBlank()) Color(0xFF94A3B8) else Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                )

                characterRows.forEachIndexed { rowIndex, keys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            7.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        keys.forEachIndexed { columnIndex, label ->
                            SearchKeyboardKey(
                                label = label,
                                onClick = { onCharacter(label.lowercase()) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .padding(vertical = 4.dp)
                                    .then(
                                        if (rowIndex == 0 && columnIndex == 0) {
                                            Modifier.focusRequester(firstKeyFocusRequester)
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SearchKeyboardKey("Space", onSpace, Modifier.weight(2f).height(52.dp))
                    SearchKeyboardKey("Delete", onDelete, Modifier.weight(1f).height(52.dp))
                    SearchKeyboardKey("Clear", onClear, Modifier.weight(1f).height(52.dp))
                    SearchKeyboardKey("Done", onDone, Modifier.weight(1f).height(52.dp))
                }
            }
        }

        LaunchedEffect(Unit) {
            firstKeyFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun SearchKeyboardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(
                color = if (focused) Color(0xFFDBEAFE) else Color(0xFF1E293B),
                shape = RoundedCornerShape(9.dp)
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Color(0xFF3B82F6) else Color.Transparent,
                shape = RoundedCornerShape(9.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .tvDpadClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (focused) Color.Black else Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
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
