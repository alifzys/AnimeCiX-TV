package com.alifzys.an1mecix.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alifzys.an1mecix.AppContainer
import com.alifzys.an1mecix.ui.detail.DetailScreen
import com.alifzys.an1mecix.ui.home.HomeScreen
import com.alifzys.an1mecix.ui.player.PlayerScreen
import com.alifzys.an1mecix.ui.search.SearchScreen
import com.alifzys.an1mecix.ui.categories.CategoriesScreen
import com.alifzys.an1mecix.ui.categories.BrowseScreen
import com.alifzys.an1mecix.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{titleId}"
    fun detail(id: Int) = "detail/$id"
    const val BROWSE = "browse/{slug}/{name}"
    fun browse(slug: String, name: String) = "browse/$slug/$name"
    const val PLAYER = "player/{titleId}/{seasonNumber}/{episodeId}/{sourceId}"
    fun player(titleId: Int, seasonNumber: Int, episodeId: Int, sourceId: Int = -1) =
        "player/$titleId/$seasonNumber/$episodeId/$sourceId"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn(tween(160)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(160)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onOpenDetail = { id -> nav.navigate(Routes.detail(id)) },
                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                onOpenCategories = { nav.navigate(Routes.CATEGORIES) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                container = container,
                onOpenDetail = { id -> nav.navigate(Routes.detail(id)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                container = container,
                onOpenBrowse = { slug, name -> nav.navigate(Routes.browse(slug, name)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.BROWSE,
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
            )
        ) { entry ->
            BrowseScreen(
                container = container,
                slug = entry.arguments?.getString("slug").orEmpty(),
                name = entry.arguments?.getString("name").orEmpty(),
                onOpenDetail = { id -> nav.navigate(Routes.detail(id)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("titleId") { type = NavType.IntType })
        ) { entry ->
            DetailScreen(
                container = container,
                titleId = entry.arguments?.getInt("titleId") ?: 0,
                onPlayEpisode = { titleId, seasonNum, epId, sourceId ->
                    nav.navigate(Routes.player(titleId, seasonNum, epId, sourceId))
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.PLAYER,
            arguments = listOf(
                navArgument("titleId") { type = NavType.IntType },
                navArgument("seasonNumber") { type = NavType.IntType },
                navArgument("episodeId") { type = NavType.IntType },
                navArgument("sourceId") { type = NavType.IntType },
            )
        ) { entry ->
            PlayerScreen(
                container = container,
                titleId = entry.arguments?.getInt("titleId") ?: 0,
                seasonNumber = entry.arguments?.getInt("seasonNumber") ?: 1,
                episodeId = entry.arguments?.getInt("episodeId") ?: 0,
                sourceId = entry.arguments?.getInt("sourceId") ?: -1,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
