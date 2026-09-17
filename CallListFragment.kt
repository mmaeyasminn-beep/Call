package com.mostafa.callmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Shows a filtered slice of the device call log.
 * filterType == null -> "All"; otherwise one of CallLog.Calls.*_TYPE.
 */
class CallListFragment : Fragment(R.layout.fragment_call_list) {

    private var filterType: Int? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var permissionView: View

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) loadCalls()
        }

    companion object {
        private const val ARG_FILTER = "arg_filter"
        fun newInstance(filterType: Int?): CallListFragment {
            val f = CallListFragment()
            val args = Bundle()
            filterType?.let { args.putInt(ARG_FILTER, it) }
            f.arguments = args
            return f
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filterType = if (arguments?.containsKey(ARG_FILTER) == true) arguments!!.getInt(ARG_FILTER) else null

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        permissionView = view.findViewById(R.id.permission_view)
        view.findViewById<Button>(R.id.btn_grant).setOnClickListener {
            permissionLauncher.launch(PermissionsHelper.CALL_LOG_PERMISSIONS)
        }

        if (PermissionsHelper.hasAll(requireContext(), PermissionsHelper.CALL_LOG_PERMISSIONS)) {
            loadCalls()
        } else {
            showPermissionPrompt()
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionsHelper.hasAll(requireContext(), PermissionsHelper.CALL_LOG_PERMISSIONS)) {
            loadCalls()
        }
    }

    private fun showPermissionPrompt() {
        recyclerView.visibility = View.GONE
        permissionView.visibility = View.VISIBLE
    }

    private fun loadCalls() {
        recyclerView.visibility = View.VISIBLE
        permissionView.visibility = View.GONE

        val entries = mutableListOf<CallEntry>()
        val projection = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )
        val selection = if (filterType != null) "${CallLog.Calls.TYPE} = ?" else null
        val selectionArgs = if (filterType != null) arrayOf(filterType.toString()) else null

        try {
            requireContext().contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx) else "غير معروف"
                    val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.OUTGOING_TYPE
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    val duration = if (durationIdx >= 0) cursor.getLong(durationIdx) else 0L
                    entries.add(
                        CallEntry(
                            name = name ?: number ?: "غير معروف",
                            number = number ?: "",
                            type = type,
                            date = date,
                            duration = duration,
                            simSlot = -1
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            showPermissionPrompt()
            return
        }

        recyclerView.adapter = CallLogAdapter(entries) { entry ->
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val subId = Prefs.getDefaultSubId(requireContext())
                DualSimHelper.placeCall(requireContext(), entry.number, subId)
                // Shows our custom during-call controls (mute/speaker/record demo)
                // alongside the system dialer UI - see InCallActivity's class doc
                // for what it can and cannot really control.
                val intent = android.content.Intent(requireContext(), InCallActivity::class.java)
                intent.putExtra(InCallActivity.EXTRA_NAME, entry.name)
                intent.putExtra(InCallActivity.EXTRA_LINE_LABEL, if (subId != -1) "خط مختار" else "الخط الافتراضي")
                startActivity(intent)
            } else {
                permissionLauncher.launch(PermissionsHelper.CALLING_PERMISSIONS)
            }
        }
    }
}
