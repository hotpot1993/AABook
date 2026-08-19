package com.aa.ledger.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aa.ledger.ui.expense.AddExpenseScreen
import com.aa.ledger.ui.admin.AdminScreen
import com.aa.ledger.ui.home.CreateLedgerScreen
import com.aa.ledger.ui.home.HomeScreen
import com.aa.ledger.ui.ledger.LedgerDetailScreen
import com.aa.ledger.ui.member.MemberManageScreen
import com.aa.ledger.ui.settlement.SettlementScreen
import com.aa.ledger.ui.settings.SettingsScreen
import com.aa.ledger.ui.stats.StatsOverviewScreen
import com.aa.ledger.ui.stats.StatsScreen
import com.aa.ledger.ui.theme.AnimSpec

object Routes {
    const val HOME = "home"
    const val LEDGER_DETAIL = "ledger/{ledgerId}"
    const val ADD_EXPENSE = "expense/add?ledgerId={ledgerId}"
    const val EDIT_EXPENSE = "expense/edit/{expenseId}"
    const val SETTLEMENT = "settlement/{ledgerId}"
    const val MEMBER_MANAGE = "members/{ledgerId}"
    const val STATS = "stats/{ledgerId}"
    const val STATS_OVERVIEW = "stats_overview"
    const val SETTINGS = "settings"
    const val CREATE_LEDGER = "create_ledger"
    const val CLOUD_LOGIN = "cloud_login"
    const val ADMIN = "admin"
    const val QUICK_ADD = "add_expense?ledgerId={ledgerId}"

    fun ledgerDetail(ledgerId: Long) = "ledger/$ledgerId"
    fun addExpense(ledgerId: Long) = "expense/add?ledgerId=$ledgerId"
    fun editExpense(expenseId: Long) = "expense/edit/$expenseId"
    fun settlement(ledgerId: Long) = "settlement/$ledgerId"
    fun memberManage(ledgerId: Long) = "members/$ledgerId"
    fun stats(ledgerId: Long) = "stats/$ledgerId"
}

// Routes where the bottom nav should be visible
private val bottomNavRoutes = setOf(
    "home",
    "ledger",
    "stats_overview",
    "stats",
    "settings",
    "add_expense"
)

private fun String?.matchesBottomNav(): Boolean {
    if (this == null) return false
    return bottomNavRoutes.any { prefix ->
        this.startsWith(prefix)
    }
}

// 底部导航「根页面」对应的标签索引（与 BottomNavBar.isTabActive 对齐，用于判断切换方向）
private fun activeTabIndex(route: String?): Int = when {
    route == null -> -1
    route.startsWith("home") || route.startsWith("ledger") -> 0
    route.startsWith("add_expense") -> 1
    route.startsWith("stats") -> 2
    route.startsWith("settings") -> 3
    else -> -1
}

// 是否为底部导航的 4 个「根页面」（区别于其下的详情页，如 stats/{id}、ledger/{id}）
private fun isRootTabRoute(route: String?): Boolean = when (route) {
    "home", "stats_overview", "settings" -> true
    "add_expense?ledgerId={ledgerId}" -> true
    else -> false
}

// 本次导航新页面是否从右侧滑入：底部导航在根页面间切换时，
// 向右（索引变大）从右滑入、向左（索引变小）从左滑入；其余导航一律从右滑入。
private fun slideFromRight(initialRoute: String?, targetRoute: String?): Boolean {
    val from = activeTabIndex(initialRoute)
    val to = activeTabIndex(targetRoute)
    return if (isRootTabRoute(targetRoute) && from >= 0 && to >= 0) from <= to else true
}

// 当前是否已在该标签的「根页面」：重复点击已选中标签时不触发转场
private fun isOnTabRoot(currentRoute: String?, tab: BottomTab): Boolean = when (tab) {
    BottomTab.HOME -> currentRoute == Routes.HOME
    BottomTab.ADD_EXPENSE -> currentRoute == Routes.QUICK_ADD
    BottomTab.STATS -> currentRoute == Routes.STATS_OVERVIEW
    BottomTab.SETTINGS -> currentRoute == Routes.SETTINGS
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    initialNavigateTo: String? = null
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var lastLedgerId by remember { mutableLongStateOf(-1L) }

    // Determine start destination from widget intent
    val startDestination = if (initialNavigateTo == "add_expense") Routes.QUICK_ADD else Routes.HOME

    val showBottomNav = currentRoute.matchesBottomNav()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                val fromRight = slideFromRight(initialState.destination.route, targetState.destination.route)
                slideInHorizontally(
                    animationSpec = tween(durationMillis = AnimSpec.TransitionDuration, easing = AnimSpec.IosEaseOut),
                    initialOffsetX = { fullWidth -> if (fromRight) fullWidth else -fullWidth }
                ) + fadeIn(animationSpec = AnimSpec.FadeTween)
            },
            exitTransition = {
                val fromRight = slideFromRight(initialState.destination.route, targetState.destination.route)
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = AnimSpec.TransitionDuration, easing = AnimSpec.IosEaseOut),
                    targetOffsetX = { fullWidth -> if (fromRight) -fullWidth / 3 else fullWidth / 3 }
                ) + fadeOut(animationSpec = AnimSpec.FadeTween)
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(durationMillis = AnimSpec.TransitionDuration, easing = AnimSpec.IosEaseOut),
                    initialOffsetX = { fullWidth -> -fullWidth / 3 }
                ) + fadeIn(animationSpec = AnimSpec.FadeTween)
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = AnimSpec.TransitionDuration, easing = AnimSpec.IosEaseOut),
                    targetOffsetX = { fullWidth -> fullWidth }
                ) + fadeOut(animationSpec = AnimSpec.FadeTween)
            }
        ) {
            // ─── Home ───
            composable(Routes.HOME) {
                HomeScreen(
                    onLedgerClick = { ledgerId, _ ->
                        lastLedgerId = ledgerId
                        navController.navigate(Routes.ledgerDetail(ledgerId))
                    },
                    onCreateLedger = {
                        navController.navigate(Routes.CREATE_LEDGER)
                    },
                    onSettingsClick = {
                        navController.navigate(Routes.SETTINGS)
                    },
                    onDefaultLedgerChanged = { id ->
                        if (id > 0) lastLedgerId = id
                    }
                )
            }

            // ─── Create Ledger ───
            composable(Routes.CREATE_LEDGER) {
                CreateLedgerScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ─── Quick Add (from bottom nav) ───
            composable(
                route = Routes.QUICK_ADD,
                arguments = listOf(navArgument("ledgerId") { type = NavType.LongType; defaultValue = -1L })
            ) {
                AddExpenseScreen(
                    ledgerId = if (lastLedgerId > 0) lastLedgerId else null,
                    onBack = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                    onSaved = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
                )
            }

            // ─── Stats Overview (from bottom nav, picker page) ───
            composable(Routes.STATS_OVERVIEW) {
                // Show overall stats — navigate to specific ledger stats from here
                StatsOverviewScreen(
                    onLedgerStatsClick = { ledgerId ->
                        navController.navigate(Routes.stats(ledgerId))
                    }
                )
            }

            // ─── Ledger Detail ───
            composable(
                route = Routes.LEDGER_DETAIL,
                arguments = listOf(navArgument("ledgerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val ledgerId = backStackEntry.arguments?.getLong("ledgerId") ?: return@composable
                lastLedgerId = ledgerId
                LedgerDetailScreen(
                    ledgerId = ledgerId,
                    onAddExpense = { navController.navigate(Routes.addExpense(ledgerId)) },
                    onEditExpense = { expenseId -> navController.navigate(Routes.editExpense(expenseId)) },
                    onSettlement = { navController.navigate(Routes.settlement(ledgerId)) },
                    onManageMembers = { navController.navigate(Routes.memberManage(ledgerId)) },
                    onStats = { navController.navigate(Routes.stats(ledgerId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            // ─── Add Expense ───
            composable(
                route = Routes.ADD_EXPENSE,
                arguments = listOf(navArgument("ledgerId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val ledgerId = backStackEntry.arguments?.getLong("ledgerId") ?: -1L
                AddExpenseScreen(
                    ledgerId = if (ledgerId == -1L) null else ledgerId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            // ─── Edit Expense ───
            composable(
                route = Routes.EDIT_EXPENSE,
                arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: return@composable
                AddExpenseScreen(
                    expenseId = expenseId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            // ─── Settlement ───
            composable(
                route = Routes.SETTLEMENT,
                arguments = listOf(navArgument("ledgerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val ledgerId = backStackEntry.arguments?.getLong("ledgerId") ?: return@composable
                SettlementScreen(
                    ledgerId = ledgerId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ─── Member Manage ───
            composable(
                route = Routes.MEMBER_MANAGE,
                arguments = listOf(navArgument("ledgerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val ledgerId = backStackEntry.arguments?.getLong("ledgerId") ?: return@composable
                MemberManageScreen(
                    ledgerId = ledgerId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ─── Stats (per ledger) ───
            composable(
                route = Routes.STATS,
                arguments = listOf(navArgument("ledgerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val ledgerId = backStackEntry.arguments?.getLong("ledgerId") ?: return@composable
                StatsScreen(
                    ledgerId = ledgerId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ─── Settings ───
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onCloudLogin = { navController.navigate(Routes.CLOUD_LOGIN) },
                    onAdmin = { navController.navigate(Routes.ADMIN) }
                )
            }

            // ─── Admin ───
            composable(Routes.ADMIN) {
                AdminScreen(onBack = { navController.popBackStack() })
            }

            // ─── Cloud Login ───
            composable(Routes.CLOUD_LOGIN) {
                com.aa.ledger.ui.auth.LoginScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Bottom Navigation Bar — pinned at bottom
        if (showBottomNav) {
            BottomNavBar(
                currentRoute = currentRoute,
                onTabClick = { tab ->
                    if (!isOnTabRoot(currentRoute, tab)) {
                        when (tab) {
                            BottomTab.HOME -> {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                            BottomTab.ADD_EXPENSE -> {
                                navController.navigate("add_expense?ledgerId=$lastLedgerId") {
                                    popUpTo(Routes.HOME)
                                }
                            }
                            BottomTab.STATS -> {
                                navController.navigate(Routes.STATS_OVERVIEW) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                            BottomTab.SETTINGS -> {
                                navController.navigate(Routes.SETTINGS) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }
    }
}
