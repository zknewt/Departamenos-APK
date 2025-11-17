package com.example.smartaccesscontrol.models

data class Usuario(
    val id_usuario: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
    val rol: String, // administrador u operador
    val estado: String, // ACTIVO, INACTIVO, BLOQUEADO
    val id_departamento: Int,
    val departamento: String?,
    val condominio: String?,
    val telefono: String?,
    val es_admin: Boolean = false
) {
    val nombreCompleto: String
        get() = "$nombre $apellido"
}