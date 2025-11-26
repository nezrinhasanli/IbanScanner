package com.example.ibanscanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.scanbot.sdk.textpattern.ContentValidationCallback
import io.scanbot.sdk.textpattern.CustomContentValidator
import io.scanbot.sdk.ui_v2.common.ScanbotColor
import io.scanbot.sdk.ui_v2.common.StyledText
import io.scanbot.sdk.ui_v2.textpattern.TextPatternScannerActivity
import io.scanbot.sdk.ui_v2.textpattern.configuration.TextPatternScannerScreenConfiguration
import java.math.BigInteger

class MainActivity : ComponentActivity() {
    private lateinit var resultLauncher: ActivityResultLauncher<TextPatternScannerScreenConfiguration>
    private var isScannerOpen = false
    private var lastScannedRaw: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        resultLauncher =
            registerForActivityResult(TextPatternScannerActivity.ResultContract()) { resultEntity ->
                isScannerOpen = false

                val raw = resultEntity.result?.rawText?.replace(" ", "") ?: ""

                lastScannedRaw = raw

                if (resultEntity.resultOk) {
                    Toast.makeText(this, "Valid IBAN: $raw", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Wrong IBAN", Toast.LENGTH_SHORT).show()
                }
            }

        setContent {
            SerialScannerScreen(
                onScanClick = { launchScanner() },
                enabled = !isScannerOpen
            )
        }
    }

    private fun launchScanner() {
        if (isScannerOpen) return

        val configuration = TextPatternScannerScreenConfiguration().apply {
            scannerConfiguration.validator = CustomContentValidator().apply {
                callback = object : ContentValidationCallback {
                    override fun clean(rawText: String): String {
                        return rawText.replace(" ", "")
                    }

                    override fun validate(text: String): Boolean {
                        return isValidIban(text.trim())
                    }
                }
            }

            topBar.title = StyledText(
                visible = true,
                text = "IBAN Scanner",
                color = ScanbotColor(Color.White)
            )
            topBar.backgroundColor = ScanbotColor(Color.Black)
        }

        isScannerOpen = true
        resultLauncher.launch(configuration)
    }

    companion object {
        fun isValidIban(receiverIban: String): Boolean {
            return receiverIban.length > 7 &&
                    receiverIban.startsWith("AZ") &&
                    !receiverIban.drop(4).startsWith("AZEN") &&
                    checkMod97(receiverIban)
        }

        fun checkMod97(receiverIban: String): Boolean {
            val reformatted = receiverIban.removeRange(0..3) + receiverIban.take(4)
            val mod97Format = reformatted.map {
                if (it.isLetter()) (it.code - 55).toString() else it.toString()
            }.joinToString("")

            val mod97 = mod97Format.toBigInteger().mod(BigInteger.valueOf(97L))
            return mod97 == BigInteger.ONE
        }
    }
}

@Composable
fun SerialScannerScreen(onScanClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onScanClick, enabled = enabled) {
            Text("Scan")
        }
    }
}
