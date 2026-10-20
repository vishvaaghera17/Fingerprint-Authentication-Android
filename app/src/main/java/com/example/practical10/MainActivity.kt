package com.example.practical10

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.text.DateFormat
import java.util.Date

class MainActivity : FragmentActivity() {

    private lateinit var output: TextView

    private val callLogRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        output = findViewById(R.id.output)

        val biometricButton =
            findViewById<Button>(R.id.btnBiometric)

        val callLogButton =
            findViewById<Button>(R.id.btnCallLog)

        biometricButton.setOnClickListener {
            authenticateFingerprint()
        }

        callLogButton.setOnClickListener {
            checkCallLogPermission()
        }
    }

    // PART A1: Biometric authentication
    private fun authenticateFingerprint() {

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG

        val manager = BiometricManager.from(this)

        when (manager.canAuthenticate(authenticators)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {

                val executor =
                    ContextCompat.getMainExecutor(this)

                val prompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)

                            output.text =
                                "Fingerprint authentication successful."

                            Toast.makeText(
                                this@MainActivity,
                                "Authentication successful",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()

                            Toast.makeText(
                                this@MainActivity,
                                "Fingerprint not recognized. Try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(
                                errorCode,
                                errString
                            )

                            Toast.makeText(
                                this@MainActivity,
                                errString,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                val promptInfo =
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Fingerprint Authentication")
                        .setSubtitle(
                            "Verify your identity using your fingerprint"
                        )
                        .setAllowedAuthenticators(authenticators)
                        .setNegativeButtonText("Cancel")
                        .build()

                prompt.authenticate(promptInfo)
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                output.text =
                    "Please enroll a fingerprint in phone Settings."
            }

            else -> {
                output.text =
                    "Fingerprint authentication is unavailable."
            }
        }
    }

    // PART A2: Check call-log permission
    private fun checkCallLogPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadCallHistory()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALL_LOG),
                callLogRequestCode
            )
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == callLogRequestCode) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                loadCallHistory()
            } else {
                output.text =
                    "Permission denied. Cannot read call history."
            }
        }
    }

    // PART A3: Read and display call history
    private fun loadCallHistory() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            output.text = "Call log permission is required."
            return
        }

        val records = mutableListOf<String>()

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )

        try {

            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->

                val numberIndex =
                    cursor.getColumnIndexOrThrow(
                        CallLog.Calls.NUMBER
                    )

                val dateIndex =
                    cursor.getColumnIndexOrThrow(
                        CallLog.Calls.DATE
                    )

                val durationIndex =
                    cursor.getColumnIndexOrThrow(
                        CallLog.Calls.DURATION
                    )

                val typeIndex =
                    cursor.getColumnIndexOrThrow(
                        CallLog.Calls.TYPE
                    )

                while (cursor.moveToNext() && records.size < 30) {

                    val number =
                        cursor.getString(numberIndex) ?: "Unknown"

                    val date = DateFormat
                        .getDateTimeInstance()
                        .format(
                            Date(cursor.getLong(dateIndex))
                        )

                    val duration =
                        cursor.getLong(durationIndex)

                    val callType =
                        when (cursor.getInt(typeIndex)) {

                            CallLog.Calls.INCOMING_TYPE ->
                                "Incoming"

                            CallLog.Calls.OUTGOING_TYPE ->
                                "Outgoing"

                            CallLog.Calls.MISSED_TYPE ->
                                "Missed"

                            CallLog.Calls.REJECTED_TYPE ->
                                "Rejected"

                            else -> "Other"
                        }

                    records.add(
                        "$callType Call\n" +
                                "Number: $number\n" +
                                "Date and Time: $date\n" +
                                "Duration: $duration seconds"
                    )
                }
            }

            output.text =
                if (records.isEmpty()) {
                    "No call records found."
                } else {
                    records.joinToString(
                        separator = "\n\n----------------\n\n"
                    )
                }

        } catch (e: SecurityException) {

            output.text =
                "Android denied access to call history."

        } catch (e: Exception) {

            output.text =
                "Unable to load call history: ${e.message}"
        }
    }
}
