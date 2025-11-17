package com.example.smartaccesscontrol.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.smartaccesscontrol.R
import com.example.smartaccesscontrol.api.ApiClient
import com.example.smartaccesscontrol.api.ApiService
import com.example.smartaccesscontrol.utils.SessionManager
import com.example.smartaccesscontrol.utils.SweetDialogs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccessControlActivity : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var tvRol: TextView
    private lateinit var tvDepartamento: TextView
    private lateinit var tvEstadoBarrera: TextView
    private lateinit var btnAbrirBarrera: Button
    private lateinit var btnCerrarBarrera: Button
    private lateinit var btnGestionSensores: Button
    private lateinit var btnGestionUsuarios: Button
    private lateinit var btnHistorial: Button
    private lateinit var btnAccesoApp: Button
    private lateinit var btnCerrarSesion: Button

    private lateinit var sessionManager: SessionManager
    private val handler = Handler(Looper.getMainLooper())
    private var actualizandoEstado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_control)

        sessionManager = SessionManager(this)

        // Verificar sesión
        if (!sessionManager.estaLogueado()) {
            irALogin()
            return
        }

        inicializarVistas()
        configurarBotones()
        cargarDatosUsuario()
        iniciarActualizacionEstado()
    }

    private fun inicializarVistas() {
        tvBienvenida = findViewById(R.id.tvBienvenida)
        tvRol = findViewById(R.id.tvRol)
        tvDepartamento = findViewById(R.id.tvDepartamento)
        tvEstadoBarrera = findViewById(R.id.tvEstadoBarrera)
        btnAbrirBarrera = findViewById(R.id.btnAbrirBarrera)
        btnCerrarBarrera = findViewById(R.id.btnCerrarBarrera)
        btnGestionSensores = findViewById(R.id.btnGestionSensores)
        btnGestionUsuarios = findViewById(R.id.btnGestionUsuarios)
        btnHistorial = findViewById(R.id.btnHistorial)
        btnAccesoApp = findViewById(R.id.btnAccesoApp)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
    }

    private fun configurarBotones() {
        val usuario = sessionManager.obtenerUsuario()
        val esAdmin = sessionManager.esAdministrador()

        // Botones de barrera (todos los usuarios)
        btnAbrirBarrera.setOnClickListener { abrirBarrera() }
        btnCerrarBarrera.setOnClickListener { cerrarBarrera() }

        // Botones solo para administradores
        if (esAdmin) {
            btnGestionSensores.setOnClickListener {
                startActivity(Intent(this, SensorManagementActivity::class.java))
            }
            btnGestionUsuarios.setOnClickListener {
                startActivity(Intent(this, UserManagementActivity::class.java))
            }
        } else {
            btnGestionSensores.isEnabled = false
            btnGestionSensores.alpha = 0.5f
            btnGestionUsuarios.isEnabled = false
            btnGestionUsuarios.alpha = 0.5f
        }

        // Acceso desde app (todos los usuarios)
        btnAccesoApp.setOnClickListener { registrarAccesoApp() }

        // Historial (todos los usuarios)
        btnHistorial.setOnClickListener {
            startActivity(Intent(this, AccessHistoryActivity::class.java))
        }

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener { cerrarSesion() }
    }

    private fun cargarDatosUsuario() {
        val usuario = sessionManager.obtenerUsuario() ?: return

        tvBienvenida.text = "Bienvenido, ${usuario.nombreCompleto}"
        tvRol.text = "Rol: ${usuario.rol.capitalize()}"
        tvDepartamento.text = "Departamento: ${usuario.departamento ?: "N/A"}"

        actualizarEstadoBarrera()
    }

    private fun actualizarEstadoBarrera() {
        if (actualizandoEstado) return
        actualizandoEstado = true

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.obtenerEstadoBarrera().enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                actualizandoEstado = false

                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        val data = body["data"] as? Map<String, Any>
                        val estado = data?.get("estado") as? String ?: "CERRADA"

                        tvEstadoBarrera.text = "Estado Barrera: $estado"
                        tvEstadoBarrera.setTextColor(
                            if (estado == "ABIERTA")
                                getColor(android.R.color.holo_green_dark)
                            else
                                getColor(android.R.color.holo_red_dark)
                        )
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                actualizandoEstado = false
            }
        })
    }

    private fun iniciarActualizacionEstado() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                actualizarEstadoBarrera()
                handler.postDelayed(this, 3000) // Actualizar cada 3 segundos
            }
        }, 3000)
    }

    private fun abrirBarrera() {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Abriendo barrera..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.abrirBarrera(usuario.id_usuario).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                progressDialog.dismiss()

                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        SweetDialogs.success(this@AccessControlActivity, "Barrera abierta exitosamente")
                        actualizarEstadoBarrera()
                    } else {
                        val mensaje = body?.get("mensaje") as? String ?: "Error al abrir barrera"
                        SweetDialogs.error(this@AccessControlActivity, mensaje)
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progressDialog.dismiss()
                SweetDialogs.error(this@AccessControlActivity, "Error de conexión: ${t.message}")
            }
        })
    }

    private fun cerrarBarrera() {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Cerrando barrera..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.cerrarBarrera(usuario.id_usuario).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                progressDialog.dismiss()

                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        SweetDialogs.success(this@AccessControlActivity, "Barrera cerrada exitosamente")
                        actualizarEstadoBarrera()
                    } else {
                        val mensaje = body?.get("mensaje") as? String ?: "Error al cerrar barrera"
                        SweetDialogs.error(this@AccessControlActivity, mensaje)
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progressDialog.dismiss()
                SweetDialogs.error(this@AccessControlActivity, "Error de conexión: ${t.message}")
            }
        })
    }

    private fun registrarAccesoApp() {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.registrarAcceso(usuario.id_usuario, "APP", "ACCESO_VALIDO", "Acceso desde aplicación móvil")
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val success = body?.get("success") as? Boolean ?: false

                        if (success) {
                            SweetDialogs.success(this@AccessControlActivity, "Acceso registrado") {
                                abrirBarrera()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    SweetDialogs.error(this@AccessControlActivity, "Error al registrar acceso")
                }
            })
    }

    private fun cerrarSesion() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("¿Cerrar sesión?")
            .setContentText("¿Estás seguro de que deseas cerrar sesión?")
            .setConfirmText("Sí, cerrar")
            .setCancelText("Cancelar")
            .setConfirmClickListener { dialog ->
                dialog.dismiss()
                sessionManager.cerrarSesion()
                irALogin()
            }
            .show()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}