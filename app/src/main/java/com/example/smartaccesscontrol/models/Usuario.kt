package com.example.smartaccesscontrol.models

data class Usuario(
    val id_usuario: Int,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String,
    val departamento_id: Int
)
