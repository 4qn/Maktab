package com.edu.maktab.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edu.maktab.R
import com.edu.maktab.databinding.FragmentLoginBinding
import com.edu.maktab.utils.isValidEmail
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

    private var _binding: FragmentLoginBinding? = null
    private lateinit var auth: FirebaseAuth
    private var isSignUp = true
    private lateinit var span: ClickableSpan

    private lateinit var googleSignInClient: GoogleSignInClient
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.signInButton.setSize(SignInButton.SIZE_WIDE)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        var ss = SpannableString(getString(R.string.account_register_text))
        binding.textView.text = getString(R.string.account_register_text)
        span = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val directions = LoginFragmentDirections.actionRegisterFragment()
                findNavController().navigate(directions)
                /* binding.btnLogin.text = if (isSignUp) {
                     ss = SpannableString(getString(R.string.account_login_text))
                     ss.setSpan(span, 25, 30, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                     binding.textLogin.text = ss
                     getString(R.string.register)
                 } else {
                     ss = SpannableString(getString(R.string.account_register_text))

                     ss.setSpan(span, 20, 28, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                     binding.textLogin.text = ss
                     getString(R.string.login)
                 }*/
                isSignUp = !isSignUp
                Log.d(TAG, "onClick: ")
            }
        }
        binding.btnLogin.text = getString(R.string.login)
        val foregroundColorSpan = ForegroundColorSpan(Color.RED)
//        ss.setSpan(foregroundColorSpan, 25, 30, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
        ss.setSpan(span, 20, 28, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        binding.textLogin.text = ss
        binding.textLogin.movementMethod = LinkMovementMethod.getInstance()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        auth = Firebase.auth
        val directions = LoginFragmentDirections.actionBookListFragment()

        binding.signInButton.setOnClickListener {
//            findNavController().navigate(directions)
            signIn()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etUserName.text.toString()
            val password = binding.etPassword.text.toString()
            if (!email.isValidEmail()) {
                Snackbar.make(it, "Enter Correct Email Address", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (password.trim().length < 6) {
                Snackbar.make(it, "Password Should be more than 6 Letter", Snackbar.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
//            if (isSignUp) {
//                signUp(email, password)
//            } else {
            signIn(email, password)
//            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }

    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        Log.d(TAG, "updateUI: ${user?.uid}")
        if (user != null) {
            val directions = LoginFragmentDirections.actionBookListFragment()
            findNavController().navigate(directions)
        }


    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signUp(email: String, password: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        requireContext(), "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }

            }
        /* val actionCodeSettings = ActionCodeSettings.newBuilder()
             // URL you want to redirect back to. The domain (www.example.com) for this
             // URL must be whitelisted in the Firebase Console.
             .setUrl("https://maktab-lib.firebaseapp.com")
             // This must be true
             .setHandleCodeInApp(true)

             .setAndroidPackageName(
                 "com.edu.maktab",
                 true, null
             ).build()*/

//        sendSignInLink("furqan@insuide.com", actionCodeSettings)
        // [END auth_build_action_code_settings]
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        requireContext(), "Login failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun sendSignInLink(email: String, actionCodeSettings: ActionCodeSettings) {
        // [START auth_send_sign_in_link]
        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("TAG", "Email sent.")
                } else {
                    Log.d("TAG", "Email sent.--${task.exception}")
                }
            }
        // [END auth_send_sign_in_link]
    }
}