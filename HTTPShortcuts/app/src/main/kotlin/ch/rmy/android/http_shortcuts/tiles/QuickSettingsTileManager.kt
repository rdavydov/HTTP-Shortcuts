package ch.rmy.android.http_shortcuts.tiles

import android.os.Build

object QuickSettingsTileManager {

    fun supportsQuickSettingsTiles(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
}
