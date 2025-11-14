package com.example.smartaccesscontrol.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.smartaccesscontrol.utils.SweetDialogs
import com.example.smartaccesscontrol.R
import com.example.smartaccesscontrol.api.ApiClient
import com.example.smartaccesscontrol.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var txtCorreo: EditText
    private lateinit var txtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtCorreo = findViewById(R.id.txtCorreo)
        txtPassword = findViewById(R.id.txtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)

        btnLogin.setOnClickListener {
            val correo = txtCorreo.text.toString()
            val password = txtPassword.text.toString()

            if (correo.isEmpty() || password.isEmpty()) {
                SweetDialogs.error(this, "Debes completar todos los campos")
                return@setOnClickListener
            }

            val api = ApiClient.retrofit.create(ApiService::class.java)
            api.login(correo, password).enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful && response.body()?.get("success") == "1") {
                        SweetDialogs.success(this@LoginActivity, "Bienvenido") {
                            startActivity(Intent(this@LoginActivity, AccessControlActivity::class.java))
                        }
                    } else {
                        SweetDialogs.error(this@LoginActivity, "Credenciales incorrectas")
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    SweetDialogs.error(this@LoginActivity, "Error de conexión: ${t.message}")
                }

                override fun onResponse(
                    call: Call<Map<String, Any>?>,
                    response: Response<Map<String, Any>?>
                ) {
                    TODO("Not yet implemented")
                }

                override fun onFailure(
                    call: Call<Map<String, Any>?>,
                    t: Throwable
                ) {
                    TODO("Not yet implemented")
                }
            })
        }

        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
