package dev.elainedb.ytdash_android_claude

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dev.elainedb.ytdash_android_claude.auth.domain.usecase.SignInWithGoogle
import dev.elainedb.ytdash_android_claude.core.error.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var googleSignInClient: GoogleSignInClient

    @Inject
    lateinit var signInWithGoogle: SignInWithGoogle

    private lateinit var tvError: TextView

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            val signInResult = signInWithGoogle(result.data)
            when (signInResult) {
                is Result.Success -> {
                    Log.d("LoginActivity", "Access granted to ${signInResult.data.email}")
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                is Result.Error -> {
                    tvError.text = signInResult.failure.message
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tvError = findViewById(R.id.tvError)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)

        btnSignIn.setOnClickListener {
            tvError.text = ""
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }
}
