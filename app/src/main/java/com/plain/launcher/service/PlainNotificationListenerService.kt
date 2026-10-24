package com.plain.launcher.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.plain.launcher.util.PriorityAlertManager
import com.plain.launcher.util.PriorityAlertStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppNotificationData(
    val count: Int,
    val recentText: String?
)

class PlainNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _notificationCount = MutableStateFlow(0)
        val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

        private val _priorityNotificationCount = MutableStateFlow(0)
        val priorityNotificationCount: StateFlow<Int> = _priorityNotificationCount.asStateFlow()

        private val _nowPlayingText = MutableStateFlow<String?>(null)
        val nowPlayingText: StateFlow<String?> = _nowPlayingText.asStateFlow()

        private val _appNotifications = MutableStateFlow<Map<String, AppNotificationData>>(emptyMap())
        val appNotifications: StateFlow<Map<String, AppNotificationData>> = _appNotifications.asStateFlow()

        private var instance: PlainNotificationListenerService? = null

        var isConnected: Boolean = false
            private set

        fun refresh() {
            instance?.updateNotifications()
            instance?.updateNowPlaying()
        }

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            val myComponent = ComponentName(context, PlainNotificationListenerService::class.java).flattenToString()
            return enabledListeners.contains(myComponent)
        }
    }

    private var rulesJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        isConnected = true
        updateNotifications()
        updateNowPlaying()
        rulesJob?.cancel()
        rulesJob = serviceScope.launch {
            PriorityAlertStore.rulesFlow.collect {
                updateNotifications()
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) {
            instance = null
        }
        isConnected = false
        rulesJob?.cancel()
        rulesJob = null
        _notificationCount.value = 0
        _priorityNotificationCount.value = 0
        _appNotifications.value = emptyMap()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        rulesJob?.cancel()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotifications()
        updateNowPlaying()
        checkPriorityAlerts(sbn)
    }

    private fun checkPriorityAlerts(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.isOngoing || (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)) return

        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return

        val rules = PriorityAlertStore.getRules(this)
        if (rules.isEmpty()) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: ""
        val combinedText = "$title $text".lowercase()

        val matchedRule = rules.firstOrNull { rule ->
            if (rule.packageName != pkg) return@firstOrNull false
            if (rule.keyword.isBlank()) {
                true
            } else {
                combinedText.contains(rule.keyword.trim().lowercase())
            }
        } ?: return

        if (matchedRule.flashTorch) {
            PriorityAlertManager.triggerTorch(this)
        }
        if (matchedRule.glowEdges) {
            PriorityAlertManager.triggerEdgeGlow(this, color = matchedRule.glowColor)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
        updateNowPlaying()
    }

    private fun updateNotifications() {
        try {
            val active = activeNotifications ?: emptyArray()
            val validSbns = active.filter { sbn ->
                !sbn.isOngoing && (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT == 0)
            }
            _notificationCount.value = validSbns.size

            val rules = PriorityAlertStore.getRules(this)
            var pCount = 0

            val map = mutableMapOf<String, AppNotificationData>()
            val sortedSbns = validSbns.sortedByDescending { it.postTime }
            for (sbn in sortedSbns) {
                val pkg = sbn.packageName ?: continue
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
                val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim()

                if (pkg != packageName && rules.isNotEmpty()) {
                    val combinedText = "${title ?: ""} ${text ?: ""}".lowercase()
                    val matches = rules.any { rule ->
                        if (rule.packageName != pkg) return@any false
                        if (rule.keyword.isBlank()) {
                            true
                        } else {
                            combinedText.contains(rule.keyword.trim().lowercase())
                        }
                    }
                    if (matches) {
                        pCount++
                    }
                }

                val existing = map[pkg]
                if (existing == null) {
                    val formatted = when {
                        !title.isNullOrBlank() && !text.isNullOrBlank() -> "$title: $text"
                        !title.isNullOrBlank() -> title
                        !text.isNullOrBlank() -> text
                        else -> null
                    }
                    map[pkg] = AppNotificationData(count = 1, recentText = formatted)
                } else {
                    map[pkg] = existing.copy(count = existing.count + 1)
                }
            }
            _priorityNotificationCount.value = pCount
            _appNotifications.value = map
        } catch (_: Exception) {
            _notificationCount.value = 0
            _priorityNotificationCount.value = 0
            _appNotifications.value = emptyMap()
        }
    }

    private fun updateNowPlaying() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, PlainNotificationListenerService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(component) ?: emptyList()

            var playingTrack: String? = null
            for (controller in controllers) {
                val state = controller.playbackState
                if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                    val metadata = controller.metadata
                    if (metadata != null) {
                        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        if (!title.isNullOrBlank()) {
                            playingTrack = if (!artist.isNullOrBlank()) {
                                "♫ $title • $artist"
                            } else {
                                "♫ $title"
                            }
                            break
                        }
                    }
                }
            }

            // Fallback: check active media notification extras if session metadata was empty
            if (playingTrack == null) {
                val active = activeNotifications ?: emptyArray()
                for (sbn in active) {
                    val notif = sbn.notification
                    val isMedia = notif.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                    if (isMedia) {
                        val title = notif.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                        val text = notif.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                        if (!title.isNullOrBlank()) {
                            playingTrack = if (!text.isNullOrBlank()) "♫ $title • $text" else "♫ $title"
                            break
                        }
                    }
                }
            }

            _nowPlayingText.value = playingTrack
        } catch (_: Exception) {
            _nowPlayingText.value = null
        }
    }
}
