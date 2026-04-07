package dev.elainedb.ytdash_android_claude

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.domain.usecases.SignInWithGoogle
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var signInWithGoogle: SignInWithGoogle

    private lateinit var errorText: TextView

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            when (val signInResult = signInWithGoogle(result.data)) {
                is Result.Success -> {
                    Log.d("LoginActivity", "Access granted to ${signInResult.data.email}")
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                is Result.Error -> {
                    errorText.text = signInResult.failure.message
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        errorText = findViewById(R.id.errorText)
        val signInButton: Button = findViewById(R.id.signInButton)

        signInButton.setOnClickListener {
            errorText.text = ""
            signInLauncher.launch(signInWithGoogle.getSignInIntent())
        }
    }
}
