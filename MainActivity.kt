package com.tagreader

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

data class Rec(val label: String, val value: String)
data class TagData(val id: String, val tech: String, val recs: List<Rec>, val time: Long)

class MainActivity : Activity(), NfcAdapter.ReaderCallback {

    private var nfc: NfcAdapter? = null
    private lateinit var status: TextView
    private lateinit var settingsBtn: Button
    private lateinit var resultBox: LinearLayout
    private lateinit var historyTitle: TextView
    private lateinit var historyBox: LinearLayout
    private val history = mutableListOf<TagData>()

    private val bg = Color.parseColor("#E8EEF0")
    private val ink = Color.parseColor("#10252E")
    private val muted = Color.parseColor("#566C76")
    private val accent = Color.parseColor("#2F5BFF")
    private val line = Color.parseColor("#CBD7DC")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfc = NfcAdapter.getDefaultAdapter(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(40))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            addView(root)
        }
        setContentView(scroll)

        root.addView(text("Tag Reader", 28f, ink, bold = true))
        root.addView(text("Hold an NFC card or tag to the back of your phone to read what\u2019s on it.", 16f, muted).apply {
            setPadding(0, dp(4), 0, dp(20))
        })

        status = text("Ready when you are.", 17f, ink, bold = true).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = card(accent.let { Color.parseColor("#DDE5FF") }, 16)
        }
        root.addView(status)

        settingsBtn = Button(this).apply {
            text = "Open NFC settings"
            visibility = View.GONE
            setOnClickListener { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
        }
        root.addView(settingsBtn)

        resultBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = card(Color.WHITE, 16)
            visibility = View.GONE
        }
        root.addView(resultBox, lp(top = 16))

        historyTitle = text("Recent reads", 18f, ink, bold = true).apply { visibility = View.GONE }
        root.addView(historyTitle, lp(top = 24))
        historyBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(historyBox)
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfc
        when {
            adapter == null -> setStatus("This phone doesn\u2019t have NFC.")
            !adapter.isEnabled -> {
                setStatus("NFC is turned off. Turn it on, then come back.")
                settingsBtn.visibility = View.VISIBLE
            }
            else -> {
                settingsBtn.visibility = View.GONE
                setStatus("Scanning. Hold a tag to the back of your phone.")
                adapter.enableReaderMode(
                    this, this,
                    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
                    null
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try { nfc?.disableReaderMode(this) } catch (_: Exception) {}
    }

    override fun onTagDiscovered(tag: Tag) {
        val id = tag.id.joinToString(":") { "%02x".format(it) }
        val tech = tag.techList.joinToString(", ") { it.substringAfterLast('.') }
        val recs = mutableListOf<Rec>()
        try {
            Ndef.get(tag)?.cachedNdefMessage?.records?.forEach { r ->
                try { recs.add(parse(r)) } catch (_: Exception) { recs.add(Rec("Record", "Couldn\u2019t decode")) }
            }
        } catch (_: Exception) {}
        val data = TagData(id, tech, recs, System.currentTimeMillis())
        runOnUiThread {
            show(data)
            history.add(0, data)
            while (history.size > 10) history.removeAt(history.size - 1)
            renderHistory()
            setStatus("Tag read. Scan another whenever you like.")
        }
    }

    private fun parse(r: NdefRecord): Rec {
        val p = r.payload
        if (r.tnf == NdefRecord.TNF_WELL_KNOWN) {
            if (r.type.contentEquals(NdefRecord.RTD_TEXT) && p.isNotEmpty()) {
                val langLen = p[0].toInt() and 0x3F
                val cs = if ((p[0].toInt() and 0x80) != 0) Charsets.UTF_16 else Charsets.UTF_8
                return Rec("Text", String(p, 1 + langLen, p.size - 1 - langLen, cs))
            }
            if (r.type.contentEquals(NdefRecord.RTD_URI)) {
                val u = r.toUri()
                if (u != null) return Rec("Link", u.toString())
            }
        }
        if (r.tnf == NdefRecord.TNF_ABSOLUTE_URI) return Rec("Link", String(r.type, Charsets.UTF_8))
        if (r.tnf == NdefRecord.TNF_MIME_MEDIA) {
            val mt = String(r.type, Charsets.US_ASCII)
            val v = if (mt.startsWith("text/") || mt.contains("json") || mt.contains("xml"))
                String(p, Charsets.UTF_8) else hex(p)
            return Rec("Data ($mt)", v)
        }
        return Rec("Record", hex(p))
    }

    private fun hex(b: ByteArray) = b.joinToString(" ") { "%02x".format(it) }

    private fun show(t: TagData) {
        resultBox.removeAllViews()
        resultBox.visibility = View.VISIBLE
        resultBox.addView(text("Tag found", 18f, ink, bold = true))
        resultBox.addView(field("Tag ID", t.id))
        resultBox.addView(field("Tag type", t.tech, copyable = false))
        if (t.recs.isEmpty()) {
            resultBox.addView(text("No data stored on this tag. Only the ID could be read.", 15f, muted).apply {
                setPadding(0, dp(12), 0, 0)
            })
        }
        t.recs.forEach { resultBox.addView(field(it.label, it.value.ifEmpty { "(empty)" })) }
        val all = buildString {
            append("Tag ID: ${t.id}")
            t.recs.forEachIndexed { i, r -> append("\n${r.label} ${i + 1}: ${r.value}") }
        }
        resultBox.addView(Button(this).apply {
            text = "Copy everything"
            setOnClickListener { copy(all) }
        }, lp(top = 14))
    }

    private fun field(label: String, value: String, copyable: Boolean = true): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(text(label, 14f, muted))
        col.addView(text(value, 17f, ink).apply { setTextIsSelectable(true) })
        row.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (copyable) {
            row.addView(Button(this).apply {
                text = "Copy"
                isAllCaps = false
                setOnClickListener { copy(value) }
            })
        }
        return row
    }

    private fun renderHistory() {
        historyBox.removeAllViews()
        historyTitle.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
        val fmt = DateFormat.getTimeInstance(DateFormat.SHORT)
        history.forEach { t ->
            val first = t.recs.firstOrNull()?.value?.take(40) ?: "ID only"
            historyBox.addView(text("${t.id}\n$first  \u2022  ${fmt.format(Date(t.time))}", 15f, ink).apply {
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = card(Color.WHITE, 12)
                setOnClickListener { show(t) }
            }, lp(top = 8))
        }
    }

    private fun copy(s: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Tag data", s))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun setStatus(s: String) { status.text = s }

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun card(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), line)
    }

    private fun lp(top: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }
}
