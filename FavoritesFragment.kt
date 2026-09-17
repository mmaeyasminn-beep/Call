package com.mostafa.callmanager

import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Shows only the contacts the user starred as favorites (no settings here anymore). */
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) loadFavorites()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyView = view.findViewById(R.id.empty_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        if (PermissionsHelper.hasAll(requireContext(), PermissionsHelper.CONTACTS_PERMISSIONS)) {
            loadFavorites()
        } else {
            permissionLauncher.launch(PermissionsHelper.CONTACTS_PERMISSIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionsHelper.hasAll(requireContext(), PermissionsHelper.CONTACTS_PERMISSIONS)) {
            loadFavorites()
        }
    }

    private fun loadFavorites() {
        val list = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        try {
            requireContext().contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1",
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "بدون اسم"
                    val number = cursor.getString(numberIdx) ?: ""
                    val key = "$name-$number"
                    if (seen.add(key)) {
                        list.add(Contact(id, name, number, true))
                    }
                }
            }
        } catch (e: SecurityException) {
            return
        }

        emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.adapter = FavoritesAdapter(list) { contact ->
            val subId = Prefs.getDefaultSubId(requireContext())
            DualSimHelper.placeCall(requireContext(), contact.number, subId)
        }
    }
}
