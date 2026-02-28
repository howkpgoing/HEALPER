
package com.example.myapplication
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    private lateinit var firebaseAuth: FirebaseAuth;

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        firebaseAuth = Firebase.auth

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (isValidCredentials(username, password)) {
                // 登錄成功，導航到主畫面
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // 結束當前的登錄活動
            } else {
                // 登錄失敗，顯示錯誤消息或採取相應操作
            }
        }
    }

    private fun isValidCredentials(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank() ) {
            Toast.makeText(this, "請填寫所有字段", Toast.LENGTH_SHORT).show()
        }
//        if (username!= "admin"|| password!="admin" ) {
//            Toast.makeText(this, "輸入錯誤", Toast.LENGTH_SHORT).show()
//        }

        firebaseAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
//                    Log.d(TAG, "signInWithEmail:success")
                    val user = firebaseAuth.currentUser
                    Toast.makeText(
                        baseContext,
                        "登入成功.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
//                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
//                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "登入失敗",
                        Toast.LENGTH_SHORT,
                    ).show()
//                    updateUI(null)
                }
            }
        return username == "admin" && password == "admin"
    }
    fun register(view: View) {
        val intent = Intent(this, Register::class.java)
        startActivity(intent)
    }
}