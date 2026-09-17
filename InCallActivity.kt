package com.mostafa.callmanager

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * Demo "during call" screen: mute / speaker / record / add call / hold / keypad.
 *
 * IMPORTANT REAL-WORLD LIMITATIONS (read before shipping):
 * 1) To fully replace the system in-call UI (and truly control an ongoing
 *    telephony call: real hold, real merge/conference, ending the actual
 *    call) the app must be registered as an Android InCallService and set
 *    as the default Phone/Dialer app. That is a much bigger integration
 *    than this screen - this activity demonstrates the UI/UX only and
 *    wires up what a normal app *can* legally control (microphone mute,
 *    speakerphone) via AudioManager.
 * 2) Recording call audio is restricted by Google Play policy and blocked
 *    on many OEM builds since Android 10. MediaRecorder.AudioSource.VOICE_CALL
 *    is attempted here but will silently fail on most modern devices -
 *    show the user a clear message rather than pretending it always works.
 */
class InCallActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private var isMuted = false
    private var isSpeakerOn = false
    private var isRecording = false
    private var isHeld = false
    private var recorder: MediaRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_call)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val name = intent.getStringExtra(EXTRA_NAME) ?: "مكالمة"
        val subLabel = intent.getStringExtra(EXTRA_LINE_LABEL) ?: ""
        findViewById<TextView>(R.id.incall_name).text = name
        findViewById<TextView>(R.id.incall_avatar).text = name.trim().take(1).ifEmpty { "؟" }
        findViewById<TextView>(R.id.incall_status).text = subLabel

        findViewById<View>(R.id.btn_mute).setOnClickListener { toggleMute() }
        findViewById<View>(R.id.btn_speaker).setOnClickListener { toggleSpeaker() }
        findViewById<View>(R.id.btn_record).setOnClickListener { toggleRecording() }
        findViewById<View>(R.id.btn_hold).setOnClickListener { toggleHold() }
        findViewById<View>(R.id.btn_add_call).setOnClickListener { openAddCall() }
        findViewById<View>(R.id.btn_keypad).setOnClickListener { openKeypad() }
        findViewById<View>(R.id.btn_end_call).setOnClickListener { finish() }

        if (Prefs.isAutoRecordEnabled(this)) {
            toggleRecording()
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        audioManager.isMicrophoneMute = isMuted
        toast(if (isMuted) "الصوت مكتوم" else "الصوت مفعّل")
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = isSpeakerOn
        toast(if (isSpeakerOn) "مكبر الصوت شغال" else "مكبر الصوت مقفول")
    }

    private fun toggleHold() {
        // Real call hold requires InCallService/Telecom integration - this is a UI placeholder.
        isHeld = !isHeld
        toast(if (isHeld) "المكالمة معلّقة (تجريبي)" else "تم إلغاء التعليق (تجريبي)")
    }

    private fun openAddCall() {
        // Real conference/merge calling requires InCallService - here we just
        // let the user pick another number to dial from contacts.
        startActivity(android.content.Intent(this, MainActivity::class.java))
        toast("افتح جهات الاتصال واختار رقم لإضافته للمكالمة")
    }

    private fun openKeypad() {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.in_call_keypad))
            .setMessage("لوحة الأرقام DTMF هنا (تكامل كامل يحتاج InCallService).")
            .setPositiveButton("حسنًا", null)
            .show()
    }

    private fun toggleRecording() {
        if (!isRecording) {
            if (android.content.pm.PackageManager.PERMISSION_GRANTED !=
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.RECORD_AUDIO
                )
            ) {
                toast("محتاج صلاحية الميكروفون عشان التسجيل")
                return
            }
            try {
                val outFile = File(getExternalFilesDir(null), "call_${System.currentTimeMillis()}.m4a")
                recorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(outFile.absolutePath)
                    prepare()
                    start()
                }
                isRecording = true
                toast("بدأ التسجيل")
            } catch (e: Exception) {
                // VOICE_CALL source is blocked on most Android 10+ devices/OEMs.
                toast("تعذّر بدء التسجيل - النظام بيمنع تسجيل صوت المكالمة على الجهاز ده")
                recorder = null
            }
        } else {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) {
                // ignore - recorder may never have started successfully
            } finally {
                recorder = null
                isRecording = false
                toast("تم إيقاف التسجيل")
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) { }
    }

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_LINE_LABEL = "extra_line_label"
    }
}
