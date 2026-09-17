package com.mostafa.callmanager

import android.provider.CallLog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLogAdapter(
    private val items: List<CallEntry>,
    private val onCall: (CallEntry) -> Unit
) : RecyclerView.Adapter<CallLogAdapter.VH>() {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.avatar)
        val name: TextView = view.findViewById(R.id.name)
        val meta: TextView = view.findViewById(R.id.meta)
        val callBtn: ImageButton = view.findViewById(R.id.btn_call)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call_log, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.avatar.text = item.name.trim().take(1).ifEmpty { "؟" }

        val typeLabel = when (item.type) {
            CallLog.Calls.OUTGOING_TYPE -> "صادر"
            CallLog.Calls.INCOMING_TYPE -> "وارد"
            CallLog.Calls.MISSED_TYPE -> "لم يرد عليه"
            else -> "مكالمة"
        }
        val dateStr = SimpleDateFormat("dd/MM  hh:mm a", Locale("ar")).format(Date(item.date))
        holder.meta.text = "$typeLabel · $dateStr"

        holder.callBtn.setOnClickListener { onCall(item) }
        holder.itemView.setOnClickListener { onCall(item) }
    }

    override fun getItemCount(): Int = items.size
}
