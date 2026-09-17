package com.mostafa.callmanager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(
    private var items: MutableList<Contact>,
    private val onCall: (Contact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.VH>() {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.avatar)
        val name: TextView = view.findViewById(R.id.name)
        val number: TextView = view.findViewById(R.id.number)
        val star: ImageButton = view.findViewById(R.id.btn_star)
        val callBtn: ImageButton = view.findViewById(R.id.btn_call)
    }

    fun updateItems(newItems: List<Contact>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val contact = items[position]
        holder.name.text = contact.name
        holder.number.text = contact.number
        holder.avatar.text = contact.name.trim().take(1).ifEmpty { "؟" }
        holder.star.setImageResource(
            if (contact.starred) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )

        holder.star.setOnClickListener {
            val newState = !contact.starred
            toggleStarred(holder.itemView.context, contact.id, newState)
            items[position] = contact.copy(starred = newState)
            notifyItemChanged(position)
        }

        holder.callBtn.setOnClickListener { onCall(contact) }
        holder.itemView.setOnClickListener { onCall(contact) }
    }

    override fun getItemCount(): Int = items.size

    private fun toggleStarred(context: Context, contactId: Long, starred: Boolean) {
        try {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }
            val uri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                contactId.toString()
            )
            context.contentResolver.update(uri, values, null, null)
        } catch (e: SecurityException) {
            // Missing WRITE_CONTACTS permission - silently ignore, UI already reverts on reload.
        }
    }
}
