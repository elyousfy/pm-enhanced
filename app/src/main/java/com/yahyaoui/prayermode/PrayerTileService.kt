package com.yahyaoui.prayermode

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.yahyaoui.prayermode.data.SettingsStore

/** Quick Settings tile: toggles PM (Enhanced) on and off. */
class PrayerTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val turningOff = SettingsStore.current(this).settings.masterOn
        SettingsStore.update(this) { it.copy(settings = it.settings.copy(masterOn = !turningOff)) }
        if (turningOff) DndController.disable(this, notify = false)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (SettingsStore.current(this).settings.masterOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
