package com.mostafa.callmanager

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

/**
 * Reads the device's two active SIM lines and places outgoing calls
 * through the chosen line, using the system TelecomManager.
 */
object DualSimHelper {

    @SuppressLint("MissingPermission")
    fun getActiveLines(context: Context): List<SubscriptionInfo> {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        return try {
            sm.activeSubscriptionInfoList ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** Finds the matching PhoneAccountHandle for a given subscriptionId. */
    @SuppressLint("MissingPermission")
    fun phoneAccountForSubscription(context: Context, subId: Int): PhoneAccountHandle? {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val handles = try {
            telecomManager.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            return null
        }
        for (handle in handles) {
            val account = telecomManager.getPhoneAccount(handle) ?: continue
            // The account id commonly embeds the subscription id as a string.
            if (account.id.contains(subId.toString())) return handle
        }
        return handles.firstOrNull()
    }

    /** Places a call to [number] using [subId] (or the device default if subId == -1). */
    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String, subId: Int) {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val extras = android.os.Bundle()
        if (subId != -1) {
            phoneAccountForSubscription(context, subId)?.let {
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
            }
        }
        telecomManager.placeCall(Uri.fromParts("tel", number, null), extras)
    }
}
