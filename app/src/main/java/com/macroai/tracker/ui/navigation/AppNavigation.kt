package com.macroai.tracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.macroai.tracker.ui.camera.UnifiedCameraScreen
import com.macroai.tracker.ui.dashboard.DashboardScreen
import com.macroai.tracker.ui.edit.EditFoodLogScreen
import com.macroai.tracker.ui.recipes.SavedMealsScreen
import com.macroai.tracker.ui.search.FoodSearchScreen
import com.macroai.tracker.ui.settings.TdeeSettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
object CameraCaptureRoute

@Serializable
object TdeeSettingsRoute

@Serializable
data class FoodSearchRoute(
    val initialMealType: String? = null
)

@Serializable
data class EditFoodLogRoute(
    val logId: String
)

@Serializable
object SavedMealsRoute

@Composable
fun AppNavigation(
    initialDestination: String? = null,
    onClearInitialDestination: () -> Unit = {}
) {
    val navController = rememberNavController()

    androidx.compose.runtime.LaunchedEffect(initialDestination) {
        if (initialDestination == "scanner") {
            navController.navigate(CameraCaptureRoute)
            onClearInitialDestination()
        }
    }

    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        enterTransition = {
            fadeIn(animationSpec = tween(260)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(260)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                onOpenCamera = { navController.navigate(CameraCaptureRoute) },
                onOpenSettings = { navController.navigate(TdeeSettingsRoute) },
                onOpenSearch = { mealType ->
                    navController.navigate(FoodSearchRoute(initialMealType = mealType?.name))
                },
                onOpenRecipes = { navController.navigate(SavedMealsRoute) },
                onEditFoodLog = { logId -> navController.navigate(EditFoodLogRoute(logId)) }
            )
        }

        composable<FoodSearchRoute> {
            FoodSearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<CameraCaptureRoute> {
            UnifiedCameraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<TdeeSettingsRoute> {
            TdeeSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<EditFoodLogRoute> {
            EditFoodLogScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRecipes = { navController.navigate(SavedMealsRoute) }
            )
        }

        composable<SavedMealsRoute> {
            SavedMealsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.popBackStack(DashboardRoute, inclusive = false)
                }
            )
        }
    }
}
