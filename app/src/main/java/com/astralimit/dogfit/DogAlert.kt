package com.astralimit.dogfit

data class DogAlert(
    val id: Long = System.currentTimeMillis(),
    val type: AlertType = AlertType.HEALTH_TIP,
    val message: String = "",
    val severity: Int = 1,
    val recommendedAction: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    fun getIconRes(): Int {
        return when (type) {
            AlertType.BATTERY_LOW -> android.R.drawable.ic_lock_idle_low_battery
            AlertType.HEALTH_TIP -> android.R.drawable.ic_menu_info_details
            AlertType.VET_REMINDER -> android.R.drawable.ic_menu_today
            AlertType.LOW_ACTIVITY -> android.R.drawable.ic_menu_directions
            AlertType.GOAL_ACHIEVED -> android.R.drawable.star_big_on
            AlertType.OVEREXERTION -> android.R.drawable.ic_dialog_alert
            AlertType.VACCINATION -> android.R.drawable.ic_dialog_info
        }
    }
}

enum class AlertType {
    BATTERY_LOW,
    HEALTH_TIP,
    VET_REMINDER,
    LOW_ACTIVITY,
    GOAL_ACHIEVED,
    OVEREXERTION,
    VACCINATION
}