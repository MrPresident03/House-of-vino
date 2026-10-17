package com.houseofvino.app.ui.photos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** Shows a photo from app-private storage. */
@Composable
fun PhotoImage(path: String, description: String?, modifier: Modifier = Modifier) {
    AsyncImage(model = path, contentDescription = description, modifier = modifier, contentScale = ContentScale.Crop)
}
