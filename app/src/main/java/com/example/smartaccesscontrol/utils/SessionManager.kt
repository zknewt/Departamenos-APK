package com.example.smartaccesscontrol.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.smartaccesscontrol.models.Usuario

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("SmartAccessSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID_USUARIO = "id_usuario"
        private const val KEY_NOMBRE = "nombre"
        private const val KEY_APELLIDO = "apellido"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROL = "rol"
        private const val KEY_ESTADO = "estado"
        private const val KEY_ID_DEPARTAMENTO = "id_departamento"
        private const val KEY_DEPARTAMENTO = "departamento"
        private const val KEY_CONDOMINIO = "condominio"
        private const val KEY_ES_ADMIN = "es_admin"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun guardarSesion(usuario: Usuario) {
        prefs.edit().apply {
            putInt(KEY_ID_USUARIO, usuario.id_usuario)
            putString(KEY_NOMBRE, usuario.nombre)
            putString(KEY_APELLIDO, usuario.apellido)
            putString(KEY_EMAIL, usuario.email)
            putString(KEY_ROL, usuario.rol)
            putString(KEY_ESTADO, usuario.estado)
            putInt(KEY_ID_DEPARTAMENTO, usuario.id_departamento)
            putString(KEY_DEPARTAMENTO, usuario.departamento)
            putString(KEY_CONDOMINIO, usuario.condominio)
            putBoolean(KEY_ES_ADMIN, usuario.es_admin)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun obtenerUsuario(): Usuario? {
        if (!estaLogueado()) return null

        return Usuario(
            id_usuario = prefs.getInt(KEY_ID_USUARIO, 0),
            nombre = prefs.getString(KEY_NOMBRE, "") ?: "",
            apellido = prefs.getString(KEY_APELLIDO, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            rol = prefs.getString(KEY_ROL, "operador") ?: "operador",
            estado = prefs.getString(KEY_ESTADO, "ACTIVO") ?: "ACTIVO",
            id_departamento = prefs.getInt(KEY_ID_DEPARTAMENTO, 0),
            departamento = prefs.getString(KEY_DEPARTAMENTO, null),
            condominio = prefs.getString(KEY_CONDOMINIO, null),
            telefono = null,
            es_admin = prefs.getBoolean(KEY_ES_ADMIN, false)
        )
    }

    fun estaLogueado(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun esAdministrador(): Boolean {
        return prefs.getBoolean(KEY_ES_ADMIN, false)
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}