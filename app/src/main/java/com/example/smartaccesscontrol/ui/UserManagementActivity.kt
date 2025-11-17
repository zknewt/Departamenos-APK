package com.example.smartaccesscontrol.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.smartaccesscontrol.R
import com.example.smartaccesscontrol.api.ApiClient
import com.example.smartaccesscontrol.api.ApiService
import com.example.smartaccesscontrol.models.Usuario
import com.example.smartaccesscontrol.utils.SessionManager
import com.example.smartaccesscontrol.utils.SweetDialogs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagementActivity : AppCompatActivity() {

    private lateinit var listViewUsuarios: ListView
    private lateinit var sessionManager: SessionManager
    private val usuarios = mutableListOf<Usuario>()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        sessionManager = SessionManager(this)

        // Verificar que sea administrador
        if (!sessionManager.esAdministrador()) {
            SweetDialogs.error(this, "Solo administradores pueden acceder") {
                finish()
            }
            return
        }

        listViewUsuarios = findViewById(R.id.listViewUsuarios)

        adapter = UsuarioAdapter()
        listViewUsuarios.adapter = adapter

        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        val usuarioActual = sessionManager.obtenerUsuario() ?: return

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.listarUsuarios(usuarioActual.id_departamento).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        usuarios.clear()
                        val data = body["data"] as? List<Map<String, Any>>
                        data?.forEach { userData ->
                            val usuario = Usuario(
                                id_usuario = (userData["id_usuario"] as? Double)?.toInt() ?: 0,
                                nombre = userData["nombre"] as? String ?: "",
                                apellido = userData["apellido"] as? String ?: "",
                                email = userData["email"] as? String ?: "",
                                rol = userData["rol"] as? String ?: "",
                                estado = userData["estado"] as? String ?: "",
                                id_departamento = (userData["id_departamento"] as? Double)?.toInt() ?: 0,
                                departamento = userData["departamento"] as? String,
                                condominio = null,
                                telefono = userData["telefono"] as? String,
                                es_admin = (userData["rol"] as? String) == "administrador"
                            )
                            usuarios.add(usuario)
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                SweetDialogs.error(this@UserManagementActivity, "Error al cargar usuarios: ${t.message}")
            }
        })
    }

    private fun cambiarEstadoUsuario(usuario: Usuario) {
        val estados = arrayOf("ACTIVO", "INACTIVO", "BLOQUEADO")
        val estadoActualIndex = estados.indexOf(usuario.estado)

        AlertDialog.Builder(this)
            .setTitle("Cambiar Estado de Usuario")
            .setSingleChoiceItems(estados, estadoActualIndex) { dialog, which ->
                val nuevoEstado = estados[which]
                dialog.dismiss()

                if (nuevoEstado != usuario.estado) {
                    confirmarCambioEstadoUsuario(usuario, nuevoEstado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarCambioEstadoUsuario(usuario: Usuario, nuevoEstado: String) {
        val usuarioActual = sessionManager.obtenerUsuario() ?: return

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Actualizando estado..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.actualizarEstadoUsuario(usuario.id_usuario, nuevoEstado, usuarioActual.id_usuario)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val body = response.body()
                        val success = body?.get("success") as? Boolean ?: false

                        if (success) {
                            SweetDialogs.success(this@UserManagementActivity, "Estado actualizado") {
                                cargarUsuarios()
                            }
                        } else {
                            val mensaje = body?.get("mensaje") as? String ?: "Error al actualizar"
                            SweetDialogs.error(this@UserManagementActivity, mensaje)
                        }
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    progressDialog.dismiss()
                    SweetDialogs.error(this@UserManagementActivity, "Error de conexión")
                }
            })
    }

    private fun cambiarRolUsuario(usuario: Usuario) {
        val roles = arrayOf("operador", "administrador")
        val rolesDisplay = arrayOf("Operador", "Administrador")
        val rolActualIndex = roles.indexOf(usuario.rol)

        AlertDialog.Builder(this)
            .setTitle("Cambiar Rol de Usuario")
            .setSingleChoiceItems(rolesDisplay, rolActualIndex) { dialog, which ->
                val nuevoRol = roles[which]
                dialog.dismiss()

                if (nuevoRol != usuario.rol) {
                    confirmarCambioRol(usuario, nuevoRol)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarCambioRol(usuario: Usuario, nuevoRol: String) {
        val usuarioActual = sessionManager.obtenerUsuario() ?: return

        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("¿Cambiar rol?")
            .setContentText("Se cambiará el rol de ${usuario.nombreCompleto} a ${nuevoRol}")
            .setConfirmText("Cambiar")
            .setCancelText("Cancelar")
            .setConfirmClickListener { dialog ->
                dialog.dismiss()

                val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                progressDialog.titleText = "Cambiando rol..."
                progressDialog.setCancelable(false)
                progressDialog.show()

                val api = ApiClient.retrofit.create(ApiService::class.java)
                api.cambiarRolUsuario(usuario.id_usuario, nuevoRol, usuarioActual.id_usuario)
                    .enqueue(object : Callback<Map<String, Any>> {
                        override fun onResponse(
                            call: Call<Map<String, Any>>,
                            response: Response<Map<String, Any>>
                        ) {
                            progressDialog.dismiss()

                            if (response.isSuccessful) {
                                val body = response.body()
                                val success = body?.get("success") as? Boolean ?: false

                                if (success) {
                                    SweetDialogs.success(this@UserManagementActivity, "Rol actualizado") {
                                        cargarUsuarios()
                                    }
                                } else {
                                    val mensaje = body?.get("mensaje") as? String ?: "Error al cambiar rol"
                                    SweetDialogs.error(this@UserManagementActivity, mensaje)
                                }
                            }
                        }

                        override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                            progressDialog.dismiss()
                            SweetDialogs.error(this@UserManagementActivity, "Error de conexión")
                        }
                    })
            }
            .show()
    }

    inner class UsuarioAdapter : BaseAdapter() {
        override fun getCount(): Int = usuarios.size
        override fun getItem(position: Int): Any = usuarios[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@UserManagementActivity)
                .inflate(R.layout.item_usuario, parent, false)

            val usuario = usuarios[position]
            val usuarioActual = sessionManager.obtenerUsuario()

            val tvNombre = view.findViewById<TextView>(R.id.tvNombreUsuario)
            val tvEmail = view.findViewById<TextView>(R.id.tvEmailUsuario)
            val tvRol = view.findViewById<TextView>(R.id.tvRolUsuario)
            val tvEstado = view.findViewById<TextView>(R.id.tvEstadoUsuario)
            val btnCambiarEstado = view.findViewById<Button>(R.id.btnCambiarEstadoUsuario)
            val btnCambiarRol = view.findViewById<Button>(R.id.btnCambiarRolUsuario)

            tvNombre.text = usuario.nombreCompleto
            tvEmail.text = usuario.email
            tvRol.text = "Rol: ${usuario.rol.capitalize()}"
            tvEstado.text = "Estado: ${usuario.estado}"

            // Color del estado
            val colorEstado = when (usuario.estado) {
                "ACTIVO" -> android.R.color.holo_green_dark
                "INACTIVO" -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            tvEstado.setTextColor(getColor(colorEstado))

            // Color del rol
            if (usuario.es_admin) {
                tvRol.setTextColor(getColor(android.R.color.holo_blue_dark))
                tvRol.setTypeface(null, android.graphics.Typeface.BOLD)
            }

            // No permitir modificar a otros administradores ni a uno mismo
            val puedeModificar = usuario.id_usuario != usuarioActual?.id_usuario

            btnCambiarEstado.isEnabled = puedeModificar
            btnCambiarRol.isEnabled = puedeModificar

            if (!puedeModificar) {
                btnCambiarEstado.alpha = 0.5f
                btnCambiarRol.alpha = 0.5f
            }

            btnCambiarEstado.setOnClickListener {
                if (puedeModificar) {
                    cambiarEstadoUsuario(usuario)
                }
            }

            btnCambiarRol.setOnClickListener {
                if (puedeModificar) {
                    cambiarRolUsuario(usuario)
                }
            }

            return view
        }
    }
}