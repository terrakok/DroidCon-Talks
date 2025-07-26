package com.github.terrakok.dctalks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.github.terrakok.dctalks.theme.AppTheme
import com.github.terrakok.dctalks.ui.ArticleListScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

internal lateinit var ImageLoader: ImageLoader
    private set

@Preview
@Composable
internal fun App() {
    //configuration
    val ctx = LocalPlatformContext.current
    LaunchedEffect(Unit) {
        ImageLoader = ImageLoader(ctx)
        SingletonImageLoader.setSafe { ImageLoader }
    }

    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ArticleListScreen()
        }
    }
}
