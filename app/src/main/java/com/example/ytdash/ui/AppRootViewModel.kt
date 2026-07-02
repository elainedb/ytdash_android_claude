package com.example.ytdash.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.domain.repo.AuthRepository
import com.example.ytdash.testmode.TestConfigProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppRootViewModel @Inject constructor(
    authRepository: AuthRepository,
    val testConfigProvider: TestConfigProvider,
) : ViewModel() {
    val sessionEmail: StateFlow<String?> = authRepository.sessionEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
