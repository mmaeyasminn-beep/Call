package com.mostafa.callmanager

import android.app.AlertDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private var pendingRingtoneTarget: RingtoneTarget = RingtoneTarget.GENERAL

    private enum class RingtoneTarget { GENERAL, LINE1, LINE2 }

    private val ringtonePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            when (pendingRingtoneTarget) {
                RingtoneTarget.GENERAL -> Prefs.setGeneralRingtone(this, uri?.toString())
                RingtoneTarget.LINE1 -> Prefs.setLineRingtone(this, 0, uri?.toString())
                RingtoneTarget.LINE2 -> Prefs.setLineRingtone(this, 1, uri?.toString())
            }
            refreshValues()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = getString(R.string.settings_title)

        findViewById<android.view.View>(R.id.row_general_ringtone).setOnClickListener {
            pendingRingtoneTarget = RingtoneTarget.GENERAL
            launchRingtonePicker()
        }
        findViewById<android.view.View>(R.id.row_line1_ringtone).setOnClickListener {
            pendingRingtoneTarget = RingtoneTarget.LINE1
            launchRingtonePicker()
        }
        findViewById<android.view.View>(R.id.row_line2_ringtone).setOnClickListener {
            pendingRingtoneTarget = RingtoneTarget.LINE2
            launchRingtonePicker()
        }
        findViewById<android.view.View>(R.id.row_default_line).setOnClickListener {
            showLinePickerDialog()
        }

        val autoRecordSwitch = findViewById<Switch>(R.id.switch_auto_record)
        autoRecordSwitch.isChecked = Prefs.isAutoRecordEnabled(this)
        autoRecordSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAutoRecordEnabled(this, isChecked)
        }

        refreshValues()
    }

    private fun launchRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        }
        ringtonePicker.launch(intent)
    }

    private fun showLinePickerDialog() {
        val lines = DualSimHelper.getActiveLines(this)
        if (lines.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_line_title))
                .setMessage("مفيش خطوط متاحة أو الصلاحية غير ممنوحة.")
                .setPositiveButton("حسنًا", null)
                .show()
            return
        }
        val labels = lines.map { it.displayName?.toString() ?: "خط ${it.simSlotIndex + 1}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_line_title))
            .setItems(labels) { _, which ->
                Prefs.setDefaultSubId(this, lines[which].subscriptionId)
                refreshValues()
            }
            .show()
    }

    private fun refreshValues() {
        findViewById<TextView>(R.id.value_general_ringtone).text = ringtoneLabel(Prefs.getGeneralRingtone(this))
        findViewById<TextView>(R.id.value_line1_ringtone).text = ringtoneLabel(Prefs.getLineRingtone(this, 0))
        findViewById<TextView>(R.id.value_line2_ringtone).text = ringtoneLabel(Prefs.getLineRingtone(this, 1))

        val subId = Prefs.getDefaultSubId(this)
        val lines = DualSimHelper.getActiveLines(this)
        val match = lines.firstOrNull { it.subscriptionId == subId }
        findViewById<TextView>(R.id.value_default_line).text =
            match?.displayName?.toString() ?: "افتراضي النظام"
    }

    private fun ringtoneLabel(uriString: String?): String {
        if (uriString == null) return "افتراضية"
        return try {
            val ringtone = RingtoneManager.getRingtone(this, Uri.parse(uriString))
            ringtone?.getTitle(this) ?: "مخصصة"
        } catch (e: Exception) {
            "مخصصة"
        }
    }
}
