package com.powerguard.app

import android.app.Application
import android.content.Context
import com.powerguard.app.data.db.AppDatabase
import com.powerguard.app.data.repository.EventLogRepository
import com.powerguard.app.data.repository.SettingsRepository
import com.powerguard.app.service.controller.FeatureController
import com.powerguard.app.util.wrapWithSavedLocale

class PowerGuardApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val eventLogRepository: EventLogRepository by lazy { EventLogRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val featureController: FeatureController by lazy { FeatureController(this) }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.wrapWithSavedLocale())
    }
}
