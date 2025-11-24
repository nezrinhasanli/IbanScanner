package com.example.ibanscanner

import android.os.Bundle
import android.provider.Settings.Global.getString
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ibanscanner.ui.theme.IbanScannerTheme
import io.scanbot.sdk.textpattern.ContentValidationCallback
import io.scanbot.sdk.textpattern.CustomContentValidator
import io.scanbot.sdk.ui_v2.textpattern.TextPatternScannerActivity
import io.scanbot.sdk.ui_v2.textpattern.configuration.TextPatternScannerScreenConfiguration
import java.math.BigInteger
import java.util.regex.Pattern


class MainActivity : ComponentActivity() {

    private lateinit var resultLauncher: ActivityResultLauncher<TextPatternScannerScreenConfiguration>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        resultLauncher =
            registerForActivityResult(TextPatternScannerActivity.ResultContract()) { resultEntity ->
                if (resultEntity.resultOk) {
                    val scannedText = resultEntity.result?.rawText?.replace(" ", "") ?: ""
                    Toast.makeText(this, "Valid IBAN: $scannedText", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Wrong IBAN", Toast.LENGTH_SHORT).show()
                    resultEntity.result?.rawText?.replace(" ", "")
                }
            }

        setContent {
            SerialScannerScreen(onScanClick = { launchScanner() })
        }
    }

    private fun launchScanner() {
        val configuration = TextPatternScannerScreenConfiguration()

        configuration.scannerConfiguration.validator = CustomContentValidator().apply {
            callback = IbanValidatorCallback
        }

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

object IbanValidatorCallback : ContentValidationCallback {
    override fun clean(rawText: String): String {
        return rawText.replace(" ", "")
    }

    override fun validate(text: String): Boolean {
        return MainActivity.isValidIban(text.trim())
    }
}

@Composable
fun SerialScannerScreen(onScanClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onScanClick) {
            Text("Scan")
        }
    }
}
