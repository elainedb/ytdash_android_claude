package dev.elainedb.ytdash_android_claude

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tvError: TextView

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account.email ?: ""
            val authorizedEmails = ConfigHelper.getAuthorizedEmails(this)

            if (authorizedEmails.contains(email)) {
                Log.d("LoginActivity", "Access granted to $email")
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                tvError.text = "Access denied. Your email is not authorized."
                googleSignInClient.signOut()
            }
        } catch (e: ApiException) {
            tvError.text = "Sign-in failed: ${e.statusCode}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tvError = findViewById(R.id.tvError)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnSignIn.setOnClickListener {
            tvError.text = ""
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }
}
