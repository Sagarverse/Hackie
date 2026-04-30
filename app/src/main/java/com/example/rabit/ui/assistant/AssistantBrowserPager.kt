package com.example.rabit.ui.assistant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.browser.BrowserScreen
import com.example.rabit.ui.browser.BrowserViewModel

@Composable
fun AssistantBrowserPager(
    assistantViewModel: AssistantViewModel,
    browserViewModel: BrowserViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true
    ) { page ->
        when (page) {
            0 -> AssistantScreen(
                viewModel = assistantViewModel,
                mainViewModel = mainViewModel,
                onBack = onBack,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToKeyboard = onNavigateToKeyboard,
                onNavigate = onNavigate
            )
            1 -> BrowserScreen(
                viewModel = browserViewModel,
                onBack = onBack
            )
        }
    }
}
