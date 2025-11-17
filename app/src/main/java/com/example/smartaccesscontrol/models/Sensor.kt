package com.example.smartaccesscontrol.models

data class Sensor(
    val id_sensor: Int,
    val codigo_sensor: String,
    val tipo: String, // LLAVERO o TARJETA
    val estado: String, // ACTIVO, INACTIVO, PERDIDO, BLOQUEADO
    val descripcion: String?,
    val fecha_alta: String,
    val ultimo_uso: String?,
    val registrado_por: String
)