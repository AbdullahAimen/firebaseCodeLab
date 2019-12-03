package com.authentication.otp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialAutoCompleteTextView
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import kotlinx.coroutines.*
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    val TIME_OUT = 60

    lateinit var currentUserPhone: TextView
    lateinit var otpTV: TextView
    lateinit var otpET: TextInputEditText
    lateinit var sendOTPbtn: Button
    lateinit var verifyOTPbtn: Button
    lateinit var logout: Button
    lateinit var autoCompleteET: MaterialAutoCompleteTextView

    lateinit var auth: FirebaseAuth
    var job: Deferred<Unit>? = null

    var mCallback: OnVerificationStateChangedCallbacks? = null
    var verificationCode: String = ""

    private fun Activity.hideKeyboard() = hideKeyboard(currentFocus ?: View(this))

    private fun Context.hideKeyboard(view: View) =
        (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                ).hideSoftInputFromWindow(view.windowToken, 0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        defineUI()

        val adapter = ArrayAdapter(
            this, R.layout.item_drop_down,
            resources.getStringArray(R.array.phone_number_content)
        )
        autoCompleteET.setAdapter(adapter)

        startFirebaseLogin()

        setPhoneNumber()

        sendOTPbtn.setOnClickListener {
            //TODO send OTP to the selected phone number
            if (autoCompleteET.text != null && autoCompleteET.text.isNotEmpty())
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    autoCompleteET.text.toString(),                     // Phone number to verify
                    TIME_OUT.toLong(),                           // Timeout duration
                    TimeUnit.SECONDS,                // Unit of timeout
                    this@MainActivity,        // Activity (for callback binding)
                    mCallback!!
                )
            else {
                hideKeyboard()
                Toast
                    .makeText(
                        this@MainActivity,
                        "Please type phone number or pick from list",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }

        }
        verifyOTPbtn.setOnClickListener {
            hideKeyboard()
            if (otpET.text!!.isNotEmpty()) {
                val credential =
                    PhoneAuthProvider.getCredential(verificationCode, otpET.text.toString())
                SigninWithPhone(credential)
            } else {
                Toast
                    .makeText(
                        this@MainActivity,
                        "Please type OTP number",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }

        logout.setOnClickListener {
            if (job != null && job!!.isActive)
                job!!.cancel()
            FirebaseAuth.getInstance().signOut()
            setPhoneNumber()
        }
    }

    private fun defineUI() {
        sendOTPbtn = findViewById(R.id.button)
        verifyOTPbtn = findViewById(R.id.button2)
        otpTV = findViewById(R.id.textView)
        otpET = findViewById(R.id.editText)
        currentUserPhone = findViewById(R.id.textView4)
        logout = findViewById(R.id.logout)
        autoCompleteET = findViewById(R.id.outlined_exposed_dropdown_editable)


    }

    private fun countDown() = GlobalScope.async(Dispatchers.IO) {
        hideKeyboard()

        repeat(TIME_OUT+1) {
            val res = DecimalFormat("00").format(TIME_OUT - it)
            println("Kotlin Coroutines World! $res")
            withContext(Dispatchers.Main) {
                otpTV.text = "00:$res"
            }
            delay(1000)
        }
        println("finished")

    }

    private fun SigninWithPhone(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Correct OTP", Toast.LENGTH_SHORT)
                        .show()
                    if (job!!.isActive)
                        job!!.cancel()
                    setPhoneNumber()
                } else {
                    Toast.makeText(this@MainActivity, "Incorrect OTP", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun setPhoneNumber() {
        val user = auth.currentUser
        try {
            currentUserPhone.text = user?.phoneNumber
        } catch (e: Exception) {
            Toast.makeText(this, "Phone number not found", Toast.LENGTH_SHORT).show()
            currentUserPhone.text = "---"
        }
    }

    private fun startFirebaseLogin() {
        auth = FirebaseAuth.getInstance()
        mCallback = object : OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                Toast.makeText(this@MainActivity, "verification completed", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@MainActivity, "verification failed", Toast.LENGTH_SHORT).show()
                Log.d("FirebaseException", e.toString())
            }

            override fun onCodeSent(
                s: String,
                forceResendingToken: ForceResendingToken
            ) {
                super.onCodeSent(s, forceResendingToken)
                verificationCode = s
                Log.d("verificationCode", verificationCode)
                Toast.makeText(this@MainActivity, "Code Sent", Toast.LENGTH_SHORT).show()
                job = if (job == null || job!!.isCancelled)
                    countDown()
                else {
                    job!!.cancel()
                    countDown()
                }

            }
        }
    }
}
