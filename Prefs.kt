package com.mostafa.callmanager

import android.content.Context
import android.content.SharedPreferences

/**
 * Small wrapper around SharedPreferences for all app settings:
 * general ringtone, per-line ringtones, default calling line and
 * the auto-record-call toggle.
 */
object Prefs {
    private const val FILE = "call_manager_prefs"

    private const val KEY_GENERAL_RINGTONE = "general_ringtone_uri"
    private const val KEY_LINE1_RINGTONE = "line1_ringtone_uri"
    private const val KEY_LINE2_RINGTONE = "line2_ringtone_uri"
    private const val KEY_DEFAULT_SUB_ID = "default_sub_id"
    private const val KEY_AUTO_RECORD = "auto_record"

    private fun sp(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getGeneralRingtone(context: Context): String? =
        sp(context).getString(KEY_GENERAL_RINGTONE, null)

    fun setGeneralRingtone(context: Context, uri: String?) {
        sp(context).edit().putString(KEY_GENERAL_RINGTONE, uri).apply()
    }

    fun getLineRingtone(context: Context, slot: Int): String? =
        sp(context).getString(if (slot == 0) KEY_LINE1_RINGTONE else KEY_LINE2_RINGTONE, null)

    fun setLineRingtone(context: Context, slot: Int, uri: String?) {
        val key = if (slot == 0) KEY_LINE1_RINGTONE else KEY_LINE2_RINGTONE
        sp(context).edit().putString(key, uri).apply()
    }

    fun getDefaultSubId(context: Context): Int =
        sp(context).getInt(KEY_DEFAULT_SUB_ID, -1)

    fun setDefaultSubId(context: Context, subId: Int) {
        sp(context).edit().putInt(KEY_DEFAULT_SUB_ID, subId).apply()
    }

    fun isAutoRecordEnabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_AUTO_RECORD, false)

    fun setAutoRecordEnabled(context: Context, enabled: Boolean) {
        sp(context).edit().putBoolean(KEY_AUTO_RECORD, enabled).apply()
    }
}
