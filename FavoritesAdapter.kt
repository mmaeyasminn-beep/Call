package com.mostafa.callmanager

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    private val items: List<Contact>,
    private val onCall: (Contact) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.VH>() {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.avatar)
        val name: TextView = view.findViewById(R.id.name)
        val number: TextView = view.findViewById(R.id.number)
        val callBtn: ImageButton = view.findViewById(R.id.btn_call)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val contact = items[position]
        holder.name.text = contact.name
        holder.number.text = contact.number
        holder.avatar.text = contact.name.trim().take(1).ifEmpty { "؟" }
        holder.callBtn.setOnClickListener { onCall(contact) }
        holder.itemView.setOnClickListener { onCall(contact) }
    }

    override fun getItemCount(): Int = items.size
}
