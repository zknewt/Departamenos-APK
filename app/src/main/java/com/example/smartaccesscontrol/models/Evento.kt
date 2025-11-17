package com.example.smartaccesscontrol.models

data class Evento(
    val id_evento: Int,
    val fecha_hora: String,
    val tipo_evento: String,
    val resultado: String, // PERMITIDO o DENEGADO
    val metodo: String, // RFID, APP, MANUAL
    val codigo_sensor: String?,
    val tipo_sensor: String?,
    val usuario: String?,
    val departamento: String?,
    val detalles: String?
)