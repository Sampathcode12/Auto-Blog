package com.example.autoblog.data.publish

import android.content.Context

/**
 * Persists WordPress site URL and [application password](https://make.wordpress.org/core/2020/11/05/application-passwords-integration-guide/)
 * credentials for publishing. Stored in private app preferences (not encrypted).
 */
class WordPressConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var siteUrl: String
        get() = prefs.getString(KEY_SITE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SITE, value).apply()
        }

    var username: String
        get() = prefs.getString(KEY_USER, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_USER, value).apply()
        }

    var applicationPassword: String
        get() = prefs.getString(KEY_APP_PASSWORD, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_APP_PASSWORD, value).apply()
        }

    fun save(siteUrl: String, username: String, applicationPassword: String) {
        prefs.edit()
            .putString(KEY_SITE, siteUrl)
            .putString(KEY_USER, username)
            .putString(KEY_APP_PASSWORD, applicationPassword)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "wordpress_publish_config"
        private const val KEY_SITE = "site_url"
        private const val KEY_USER = "username"
        private const val KEY_APP_PASSWORD = "app_password"
    }
}
