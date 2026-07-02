package com.example.ytdash.testmode

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the [TestConfig] parsed once from `MainActivity`'s launch intent. Set before any screen
 * is shown, read by DI-provided network/auth/link-launching code wherever a runtime override is
 * needed. A holder (rather than threading the intent through every layer) keeps `data`/`domain`
 * free of Android `Intent`/`Activity` imports.
 */
@Singleton
class TestConfigProvider @Inject constructor() {
    @Volatile
    var current: TestConfig = TestConfig()
        private set

    fun update(config: TestConfig) {
        current = config
    }
}
