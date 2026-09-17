package com.mostafa.callmanager

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactsFragment : Fragment(R.layout.fragment_contacts) {

    private lateinit var recyclerView: RecyclerView
    private var allContacts: List<Contact> = emptyList()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) loadContacts()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val searchBox = view.findViewById<android.widget.EditText>(R.id.search_box)
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (PermissionsHelper.hasAll(requireContext(), PermissionsHelper.CONTACTS_PERMISSIONS)) {
            loadContacts()
        } else {
            permissionLauncher.launch(PermissionsHelper.CONTACTS_PERMISSIONS)
        }
    }

    private fun filter(query: String) {
        val filtered = if (query.isBlank()) allContacts else allContacts.filter {
            it.name.contains(query, ignoreCase = true) || it.number.contains(query)
        }
        (recyclerView.adapter as? ContactsAdapter)?.updateItems(filtered)
    }

    private fun loadContacts() {
        val list = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )
        try {
            requireContext().contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starredIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val seen = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "بدون اسم"
                    val number = cursor.getString(numberIdx) ?: ""
                    val starred = cursor.getInt(starredIdx) == 1
                    val key = "$name-$number"
                    if (seen.add(key)) {
                        list.add(Contact(id, name, number, starred))
                    }
                }
            }
        } catch (e: SecurityException) {
            return
        }

        allContacts = list
        recyclerView.adapter = ContactsAdapter(list.toMutableList()) { contact ->
            val subId = Prefs.getDefaultSubId(requireContext())
            DualSimHelper.placeCall(requireContext(), contact.number, subId)
        }
    }
}
