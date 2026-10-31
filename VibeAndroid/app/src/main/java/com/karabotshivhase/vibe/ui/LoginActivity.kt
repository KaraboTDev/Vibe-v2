package com.karabotshivhase.vibe.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.karabotshivhase.vibe.VibeApp
import com.karabotshivhase.vibe.databinding.ActivityLoginBinding
import com.karabotshivhase.vibe.ui.auth.LoginViewModel
import com.karabotshivhase.vibe.ui.auth.UiEvent
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory((application as VibeApp).authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as VibeApp
        if (app.tokenStore.isLoggedIn()) {
            openHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootScrollView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom),
            )
            insets
        }

        binding.emailEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearEmailError()
        }

        binding.passwordEditText.doOnTextChanged { _, _, _, _ ->
            viewModel.clearPasswordError()
        }

        binding.loginButton.setOnClickListener {
            hideKeyboard()
            val email = binding.emailEditText.text?.toString().orEmpty()
            val password = binding.passwordEditText.text?.toString().orEmpty()
            viewModel.login(email, password)
        }

        binding.goToRegisterTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.emailInputLayout.error = state.emailError?.let { getString(it) }
                        binding.passwordInputLayout.error = state.passwordError?.let { getString(it) }

                        binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                        binding.loginButton.isEnabled = !state.isLoading
                        binding.emailEditText.isEnabled = !state.isLoading
                        binding.passwordEditText.isEnabled = !state.isLoading

                        state.messageRes?.let { msgRes ->
                            Snackbar.make(binding.root, msgRes, Snackbar.LENGTH_LONG).show()
                            viewModel.messageShown()
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is UiEvent.NavigateHome -> openHome()
                        }
                    }
                }
            }
        }
    }

    private fun openHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}
