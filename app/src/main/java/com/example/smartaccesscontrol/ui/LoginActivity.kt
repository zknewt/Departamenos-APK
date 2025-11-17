package com.example.smartaccesscontrol.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.smartaccesscontrol.R
import com.example.smartaccesscontrol.api.ApiClient
import com.example.smartaccesscontrol.api.ApiService
import com.example.smartaccesscontrol.models.Usuario
import com.example.smartaccesscontrol.utils.SessionManager
import com.example.smartaccesscontrol.utils.SweetDialogs
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var txtCorreo: EditText
    private lateinit var txtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya está logueado
        sessionManager = SessionManager(this)
        if (sessionManager.estaLogueado()) {
            irAAccessControl()
            return
        }

        setContentView(R.layout.activity_login)

        txtCorreo = findViewById(R.id.txtCorreo)
        txtPassword = findViewById(R.id.txtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)

        btnLogin.setOnClickListener {
            realizarLogin()
        }

        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun realizarLogin() {
        val correo = txtCorreo.text.toString().trim()
        val password = txtPassword.text.toString()

        if (correo.isEmpty() || password.isEmpty()) {
            SweetDialogs.error(this, "Debes completar todos los campos")
            return
        }

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Iniciando sesión..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.login(correo, password).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                progressDialog.dismiss()

                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        // Obtener datos del usuario
                        val data = body["data"] as? Map<String, Any>
                        if (data != null) {
                            val usuario = Usuario(
                                id_usuario = (data["id_usuario"] as? Double)?.toInt() ?: 0,
                                nombre = data["nombre"] as? String ?: "",
                                apellido = data["apellido"] as? String ?: "",
                                email = data["email"] as? String ?: "",
                                rol = data["rol"] as? String ?: "operador",
                                estado = "ACTIVO",
                                id_departamento = (data["id_departamento"] as? Double)?.toInt() ?: 0,
                                departamento = data["departamento"] as? String,
                                condominio = data["condominio"] as? String,
                                telefono = null,
                                es_admin = data["es_admin"] as? Boolean ?: false
                            )

                            // Guardar sesión
                            sessionManager.guardarSesion(usuario)

                            SweetDialogs.success(this@LoginActivity, "¡Bienvenido ${usuario.nombre}!") {
                                irAAccessControl()
                            }
                        } else {
                            SweetDialogs.error(this@LoginActivity, "Error al obtener datos del usuario")
                        }
                    } else {
                        val mensaje = body?.get("mensaje") as? String ?: "Credenciales incorrectas"
                        SweetDialogs.error(this@LoginActivity, mensaje)
                    }
                } else {
                    SweetDialogs.error(this@LoginActivity, "Error en el servidor")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progressDialog.dismiss()
                SweetDialogs.error(this@LoginActivity, "Error de conexión: ${t.message}")
            }
        })
    }

    private fun irAAccessControl() {
        val intent = Intent(this, AccessControlActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}