package com.mostafa.callmanager

data class CallEntry(
    val name: String,
    val number: String,
    val type: Int,      // CallLog.Calls.OUTGOING_TYPE / INCOMING_TYPE / MISSED_TYPE
    val date: Long,
    val duration: Long,
    val simSlot: Int    // 0 = line 1, 1 = line 2, -1 = unknown
)

data class Contact(
    val id: Long,
    val name: String,
    val number: String,
    val starred: Boolean
)
