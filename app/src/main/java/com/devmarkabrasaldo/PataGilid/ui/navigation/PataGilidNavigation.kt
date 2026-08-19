package com.devmarkabrasaldo.PataGilid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devmarkabrasaldo.PataGilid.di.AppContainer
import com.devmarkabrasaldo.PataGilid.ui.screens.auth.LoginScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.auth.OnboardingScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.climbs.FullScreenPhotoGalleryScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.climbs.HikeLogCreationScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.climbs.SummitLogDetailScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.mountains.AddCustomMountainScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.mountains.MainScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.mountains.MountainDetailScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.profile.AdminModerationQueueScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.profile.DonationQRScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.lists.MountainListDetailScreen
import com.devmarkabrasaldo.PataGilid.ui.screens.lists.MountainListsViewModel
import com.devmarkabrasaldo.PataGilid.ui.screens.climbs.ClimbsListViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

object Screen {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val MOUNTAIN_DETAIL = "mountain_detail/{mountainId}"
    const val ADD_CUSTOM_MOUNTAIN = "add_custom_mountain?mountainId={mountainId}"
    const val HIKE_LOG_CREATION = "hike_log_creation/{mountainId}"
    const val SUMMIT_LOG_DETAIL = "summit_log_detail/{logId}"
    const val PHOTO_GALLERY = "photo_gallery/{startIndex}?urls={urls}"
    const val DONATION_QR = "donation_qr"
    const val ADMIN_MODERATION = "admin_moderation"
    const val USER_CONTRIBUTIONS = "user_contributions"
    const val SPONSORS = "sponsors"

    const val MOUNTAIN_LIST_DETAIL = "mountain_list_detail/{listId}"

    fun mountainDetail(mountainId: String) = "mountain_detail/$mountainId"
    fun hikeLogCreation(mountainId: String) = "hike_log_creation/$mountainId"
    fun summitLogDetail(logId: String) = "summit_log_detail/$logId"
    fun mountainListDetail(listId: String) = "mountain_list_detail/$listId"
    fun photoGallery(startIndex: Int, urls: List<String>): String {
        val encodedUrls = java.net.URLEncoder.encode(urls.joinToString("||"), "UTF-8")
        return "photo_gallery/$startIndex?urls=$encodedUrls"
    }
    fun addCustomMountain(mountainId: String? = null): String {
        return if (mountainId != null) "add_custom_mountain?mountainId=$mountainId" else "add_custom_mountain"
    }
}

@Composable
fun PataGilidNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val currentUser by container.authRepository.currentUser.collectAsState()

    val listsViewModel: MountainListsViewModel = viewModel(
        factory = MountainListsViewModel.Factory(container.mountainListRepository, container.mountainRepository)
    )
    
    val climbsListViewModel: ClimbsListViewModel = viewModel(
        factory = ClimbsListViewModel.Factory(container.mountainRepository)
    )
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val hasSeenOnboarding = sharedPrefs.getBoolean("hasSeenOnboarding", false)

    val startDestination = if (currentUser == null) {
        if (hasSeenOnboarding) Screen.LOGIN else Screen.ONBOARDING
    } else {
        Screen.MAIN
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null && !hasSeenOnboarding) {
            sharedPrefs.edit().putBoolean("hasSeenOnboarding", true).apply()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.ONBOARDING) {
            OnboardingScreen(onNavigateToLogin = {
                sharedPrefs.edit().putBoolean("hasSeenOnboarding", true).apply()
                navController.navigate(Screen.LOGIN) {
                    popUpTo(Screen.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Screen.LOGIN) {
            LoginScreen(
                authRepository = container.authRepository,
                onLoginSuccess = {
                    navController.navigate(Screen.MAIN) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MAIN) {
            MainScreen(
                container = container,
                listsViewModel = listsViewModel,
                climbsListViewModel = climbsListViewModel,
                onNavigateToDetail = { id -> navController.navigate(Screen.mountainDetail(id)) },
                onNavigateToAddCustom = { navController.navigate(Screen.ADD_CUSTOM_MOUNTAIN) },
                onNavigateToHikeLogDetail = { logId -> navController.navigate(Screen.summitLogDetail(logId)) },
                onNavigateToListDetail = { list -> navController.navigate(Screen.mountainListDetail(list.id)) },
                onNavigateToDonation = { navController.navigate(Screen.DONATION_QR) },
                onNavigateToAdminQueue = { navController.navigate(Screen.ADMIN_MODERATION) },
                onNavigateToContributions = { navController.navigate(Screen.USER_CONTRIBUTIONS) },
                onNavigateToSponsors = { navController.navigate(Screen.SPONSORS) },
                onNavigateToOnboarding = { navController.navigate(Screen.ONBOARDING) },
                onSignOut = {
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(Screen.MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.USER_CONTRIBUTIONS) {
            com.devmarkabrasaldo.PataGilid.ui.screens.profile.UserContributionsScreen(
                mountainRepository = container.mountainRepository,
                authRepository = container.authRepository,
                onNavigateBack = { navController.navigateUp() },
                onEditMountain = { mountainId -> navController.navigate(Screen.addCustomMountain(mountainId)) },
                onViewMountain = { mountainId -> navController.navigate(Screen.mountainDetail(mountainId)) }
            )
        }

        composable(Screen.SPONSORS) {
            com.devmarkabrasaldo.PataGilid.ui.screens.profile.SponsorsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.MOUNTAIN_DETAIL,
            arguments = listOf(navArgument("mountainId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mountainId = backStackEntry.arguments?.getString("mountainId") ?: ""
            MountainDetailScreen(
                mountainId = mountainId,
                container = container,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogClimb = { id -> navController.navigate(Screen.hikeLogCreation(id)) }
            )
        }

        composable(
            route = Screen.ADD_CUSTOM_MOUNTAIN,
            arguments = listOf(navArgument("mountainId") { nullable = true; type = NavType.StringType })
        ) { navBackStackEntry ->
            val mountainId = navBackStackEntry.arguments?.getString("mountainId")
            AddCustomMountainScreen(
                mountainId = mountainId,
                container = container,
                onNavigateBack = { navController.popBackStack() },
                onMountainAdded = { newMountainId, navigateToMountain, openLog ->
                    navController.popBackStack()
                    if (navigateToMountain) {
                        navController.navigate(Screen.mountainDetail(newMountainId))
                        if (openLog) {
                            navController.navigate(Screen.hikeLogCreation(newMountainId))
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.HIKE_LOG_CREATION,
            arguments = listOf(navArgument("mountainId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mountainId = backStackEntry.arguments?.getString("mountainId") ?: ""
            HikeLogCreationScreen(
                mountainId = mountainId,
                container = container,
                onNavigateBack = { navController.popBackStack() },
                onLogSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SUMMIT_LOG_DETAIL,
            arguments = listOf(navArgument("logId") { type = NavType.StringType })
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getString("logId") ?: ""
            SummitLogDetailScreen(
                logId = logId,
                container = container,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGallery = { idx, urls -> navController.navigate(Screen.photoGallery(idx, urls)) }
            )
        }

        composable(
            route = Screen.PHOTO_GALLERY,
            arguments = listOf(
                navArgument("startIndex") { type = NavType.IntType; defaultValue = 0 },
                navArgument("urls") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0
            val encodedUrls = backStackEntry.arguments?.getString("urls") ?: ""
            val urls = java.net.URLDecoder.decode(encodedUrls, "UTF-8").split("||").filter { it.isNotBlank() }
            FullScreenPhotoGalleryScreen(
                initialIndex = startIndex,
                photoUrls = urls,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DONATION_QR) {
            DonationQRScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.ADMIN_MODERATION) {
            AdminModerationQueueScreen(
                repository = container.mountainRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.mountainDetail(id)) }
            )
        }

        composable(
            route = Screen.MOUNTAIN_LIST_DETAIL,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            val lists by listsViewModel.lists.collectAsState()
            val list = lists.firstOrNull { it.id == listId }
            if (list != null) {
                MountainListDetailScreen(
                    list = list,
                    mountainRepository = container.mountainRepository,
                    viewModel = listsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMountainDetail = { mountainId -> navController.navigate(Screen.mountainDetail(mountainId)) },
                    onBrowseMountains = {
                        navController.navigate(Screen.MAIN) {
                            popUpTo(Screen.MAIN) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

