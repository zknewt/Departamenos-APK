package com.example.smartaccesscontrol.models

data class Acceso(
    val id_acceso: Int,
    val usuario_id: Int,
    val fecha_hora: String,
    val tipo_acceso: String, // Entrada / Salida
    val resultado: String    // Permitido / Denegado
)

