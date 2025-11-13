package com.example.smartaccesscontrol.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.smartaccesscontrol.R
import com.example.smartaccesscontrol.api.ApiClient
import com.example.smartaccesscontrol.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var txtNombre: EditText
    private lateinit var txtApellido: EditText
    private lateinit var txtCorreo: EditText
    private lateinit var txtPassword: EditText
    private lateinit var btnRegistrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        txtNombre = findViewById(R.id.txtNombre)
        txtApellido = findViewById(R.id.txtApellido)
        txtCorreo = findViewById(R.id.txtCorreo)
        txtPassword = findViewById(R.id.txtPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val api = ApiClient.retrofit.create(ApiService::class.java)
            api.register(
                txtNombre.text.toString(),
                txtApellido.text.toString(),
                txtCorreo.text.toString(),
                txtPassword.text.toString()
            ).enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(
                    call: Call<Map<String, String>>,
                    response: Response<Map<String, String>>
                ) {
                    if (response.isSuccessful && response.body()?.get("success") == "1") {
                        SweetDialogs.success(this@RegisterActivity, "Usuario registrado") {
                            finish()
                        }
                    } else {
                        SweetDialogs.error(this@RegisterActivity, "Error al registrar usuario")
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    SweetDialogs.error(this@RegisterActivity, "Error de conexión: ${t.message}")
                }
            })
        }
    }
}
