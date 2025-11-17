package com.example.smartaccesscontrol.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.smartaccesscontrol.R
import com.example.smartaccesscontrol.api.ApiClient
import com.example.smartaccesscontrol.api.ApiService
import com.example.smartaccesscontrol.utils.SweetDialogs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var txtNombre: EditText
    private lateinit var txtApellido: EditText
    private lateinit var txtCorreo: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtTelefono: EditText
    private lateinit var spinnerDepartamento: Spinner
    private lateinit var btnRegistrar: Button

    private val departamentos = mutableListOf<Pair<Int, String>>() // id, nombre

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        txtNombre = findViewById(R.id.txtNombre)
        txtApellido = findViewById(R.id.txtApellido)
        txtCorreo = findViewById(R.id.txtCorreo)
        txtPassword = findViewById(R.id.txtPassword)
        txtTelefono = findViewById(R.id.txtTelefono)
        spinnerDepartamento = findViewById(R.id.spinnerDepartamento)
        btnRegistrar = findViewById(R.id.btnRegistrar)

        cargarDepartamentos()

        btnRegistrar.setOnClickListener {
            realizarRegistro()
        }
    }

    private fun cargarDepartamentos() {
        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.listarDepartamentos().enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        val data = body["data"] as? List<Map<String, Any>>
                        data?.forEach { depto ->
                            val id = (depto["id_departamento"] as? Double)?.toInt() ?: 0
                            val nombre = depto["nombre_completo"] as? String ?: ""
                            departamentos.add(Pair(id, nombre))
                        }

                        val adapter = ArrayAdapter(
                            this@RegisterActivity,
                            android.R.layout.simple_spinner_item,
                            departamentos.map { it.second }
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerDepartamento.adapter = adapter
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                SweetDialogs.error(this@RegisterActivity, "Error al cargar departamentos")
            }
        })
    }

    private fun realizarRegistro() {
        val nombre = txtNombre.text.toString().trim()
        val apellido = txtApellido.text.toString().trim()
        val correo = txtCorreo.text.toString().trim()
        val password = txtPassword.text.toString()
        val telefono = txtTelefono.text.toString().trim()

        if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            SweetDialogs.error(this, "Debes completar todos los campos obligatorios")
            return
        }

        if (password.length < 6) {
            SweetDialogs.error(this, "La contraseña debe tener al menos 6 caracteres")
            return
        }

        if (departamentos.isEmpty()) {
            SweetDialogs.error(this, "No hay departamentos disponibles")
            return
        }

        val posicionSeleccionada = spinnerDepartamento.selectedItemPosition
        val idDepartamento = departamentos[posicionSeleccionada].first

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Registrando usuario..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.register(
            nombre,
            apellido,
            correo,
            password,
            idDepartamento,
            telefono.ifEmpty { null }
        ).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                progressDialog.dismiss()

                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        val mensaje = body["mensaje"] as? String ?: "Usuario registrado exitosamente"
                        SweetDialogs.success(this@RegisterActivity, mensaje) {
                            finish()
                        }
                    } else {
                        val mensaje = body?.get("mensaje") as? String ?: "Error al registrar usuario"
                        SweetDialogs.error(this@RegisterActivity, mensaje)
                    }
                } else {
                    SweetDialogs.error(this@RegisterActivity, "Error en el servidor")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progressDialog.dismiss()
                SweetDialogs.error(this@RegisterActivity, "Error de conexión: ${t.message}")
            }
        })
    }
}