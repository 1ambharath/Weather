package com.example.thailandweather.ui.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationBackStackTest {

    private class TestNavigationManager(initialTab: NavigationTab = NavigationTab.HOME) {
        val backStack = mutableListOf<ScreenDestination>(ScreenDestination.Tab(initialTab))
        var isDrawerOpen = false

        val currentDestination: ScreenDestination
            get() = backStack.lastOrNull() ?: ScreenDestination.Tab(NavigationTab.HOME)

        val currentTab: NavigationTab
            get() = when (val dest = currentDestination) {
                is ScreenDestination.Tab -> dest.tab
                else -> (backStack.findLast { it is ScreenDestination.Tab } as? ScreenDestination.Tab)?.tab ?: NavigationTab.HOME
            }

        fun navigateTo(dest: ScreenDestination) {
            if (backStack.lastOrNull() != dest) {
                backStack.add(dest)
            }
        }

        fun isBackHandlerEnabled(): Boolean {
            return isDrawerOpen || backStack.size > 1
        }

        fun handleBack(): Boolean {
            if (isDrawerOpen) {
                isDrawerOpen = false
                return true
            }
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
                return true
            }
            return false // Let system exit app
        }
    }

    @Test
    fun testInitialStateHasOneEntryAndExitsAppOnBack() {
        val nav = TestNavigationManager(NavigationTab.HOME)
        assertEquals(1, nav.backStack.size)
        assertEquals(ScreenDestination.Tab(NavigationTab.HOME), nav.currentDestination)
        assertEquals(NavigationTab.HOME, nav.currentTab)

        // When user is on first screen, BackHandler must NOT intercept so system exits app
        assertFalse("Back handler should be disabled on first screen so app exits", nav.isBackHandlerEnabled())
        val handled = nav.handleBack()
        assertFalse("Back should not be handled internally when on root screen", handled)
    }

    @Test
    fun testSequentialBackNavigationAcrossTabs() {
        val nav = TestNavigationManager(NavigationTab.HOME)

        // Navigate: Home -> Alerts -> News -> Explore
        nav.navigateTo(ScreenDestination.Tab(NavigationTab.ALERTS))
        nav.navigateTo(ScreenDestination.Tab(NavigationTab.NEWS))
        nav.navigateTo(ScreenDestination.Tab(NavigationTab.EXPLORE))

        assertEquals(4, nav.backStack.size)
        assertEquals(NavigationTab.EXPLORE, nav.currentTab)
        assertTrue(nav.isBackHandlerEnabled())

        // 1st back: Explore -> News
        assertTrue(nav.handleBack())
        assertEquals(NavigationTab.NEWS, nav.currentTab)

        // 2nd back: News -> Alerts
        assertTrue(nav.handleBack())
        assertEquals(NavigationTab.ALERTS, nav.currentTab)

        // 3rd back: Alerts -> Home
        assertTrue(nav.handleBack())
        assertEquals(NavigationTab.HOME, nav.currentTab)
        assertEquals(1, nav.backStack.size)

        // 4th back: At root screen -> Exit app
        assertFalse(nav.isBackHandlerEnabled())
        assertFalse(nav.handleBack())
    }

    @Test
    fun testSubScreensPushAndPop() {
        val nav = TestNavigationManager(NavigationTab.HOME)

        // Open AI Briefing
        nav.navigateTo(ScreenDestination.AiBriefing)
        assertEquals(ScreenDestination.AiBriefing, nav.currentDestination)
        assertEquals(NavigationTab.HOME, nav.currentTab) // tab stays Home in background

        // From AI Briefing open AI Settings
        nav.navigateTo(ScreenDestination.AiSettings)
        assertEquals(ScreenDestination.AiSettings, nav.currentDestination)

        // Back: AI Settings -> AI Briefing
        assertTrue(nav.handleBack())
        assertEquals(ScreenDestination.AiBriefing, nav.currentDestination)

        // Back: AI Briefing -> Home
        assertTrue(nav.handleBack())
        assertEquals(ScreenDestination.Tab(NavigationTab.HOME), nav.currentDestination)

        // Next back exits app
        assertFalse(nav.handleBack())
    }

    @Test
    fun testDrawerClosesOnBackBeforeNavigatingScreens() {
        val nav = TestNavigationManager(NavigationTab.HOME)
        nav.navigateTo(ScreenDestination.Tab(NavigationTab.PLACES))
        assertEquals(2, nav.backStack.size)

        // Open drawer
        nav.isDrawerOpen = true
        assertTrue(nav.isBackHandlerEnabled())

        // Back closes drawer without popping the stack
        assertTrue(nav.handleBack())
        assertFalse(nav.isDrawerOpen)
        assertEquals(ScreenDestination.Tab(NavigationTab.PLACES), nav.currentDestination)
        assertEquals(2, nav.backStack.size)

        // Next back pops Places -> Home
        assertTrue(nav.handleBack())
        assertEquals(ScreenDestination.Tab(NavigationTab.HOME), nav.currentDestination)
    }

    @Test
    fun testDuplicateNavigationIgnored() {
        val nav = TestNavigationManager(NavigationTab.HOME)
        nav.navigateTo(ScreenDestination.Tab(NavigationTab.HOME))
        nav.navigateTo(ScreenDestination.Tab(NavigationTab.HOME))
        assertEquals(1, nav.backStack.size)
    }
}
