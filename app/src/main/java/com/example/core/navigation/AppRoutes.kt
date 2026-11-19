package com.example.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Intro : AppRoute

    @Serializable
    data object Auth : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Search : AppRoute

    @Serializable
    data object Library : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data class AlbumDetail(val albumKey: String) : AppRoute

    @Serializable
    data object ArtistDetail : AppRoute
}
