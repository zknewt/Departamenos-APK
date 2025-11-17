package com.example.smartaccesscontrol.api

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ==========================================
    // AUTENTICACIÓN
    // ==========================================

    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("email") email: String,
        @Field("contrasena") contrasena: String
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("register.php")
    fun register(
        @Field("nombre") nombre: String,
        @Field("apellido") apellido: String,
        @Field("email") email: String,
        @Field("contrasena") contrasena: String,
        @Field("id_departamento") idDepartamento: Int,
        @Field("telefono") telefono: String? = null
    ): Call<Map<String, Any>>

    // ==========================================
    // DEPARTAMENTOS
    // ==========================================

    @GET("departments.php")
    fun listarDepartamentos(): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("departments.php")
    fun crearDepartamento(
        @Field("numero") numero: String,
        @Field("torre") torre: String? = null,
        @Field("condominio") condominio: String? = null,
        @Field("piso") piso: Int? = null
    ): Call<Map<String, Any>>

    // ==========================================
    // SENSORES
    // ==========================================

    @GET("sensores/listar.php")
    fun listarSensores(
        @Query("id_departamento") idDepartamento: Int
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("sensores/registrar.php")
    fun registrarSensor(
        @Field("codigo_sensor") codigoSensor: String,
        @Field("tipo") tipo: String, // LLAVERO o TARJETA
        @Field("id_departamento") idDepartamento: Int,
        @Field("id_usuario_registro") idUsuarioRegistro: Int,
        @Field("descripcion") descripcion: String? = null
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("sensores/actualizar_estado.php")
    fun actualizarEstadoSensor(
        @Field("id_sensor") idSensor: Int,
        @Field("nuevo_estado") nuevoEstado: String, // ACTIVO, INACTIVO, PERDIDO, BLOQUEADO
        @Field("id_usuario") idUsuario: Int
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("sensores/eliminar.php")
    fun eliminarSensor(
        @Field("id_sensor") idSensor: Int,
        @Field("id_usuario") idUsuario: Int
    ): Call<Map<String, Any>>

    // ==========================================
    // BARRERA
    // ==========================================

    @FormUrlEncoded
    @POST("barrera/abrir.php")
    fun abrirBarrera(
        @Field("id_usuario") idUsuario: Int
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("barrera/cerrar.php")
    fun cerrarBarrera(
        @Field("id_usuario") idUsuario: Int
    ): Call<Map<String, Any>>

    @GET("barrera/estado.php")
    fun obtenerEstadoBarrera(): Call<Map<String, Any>>

    // ==========================================
    // EVENTOS DE ACCESO
    // ==========================================

    @GET("list_access.php")
    fun listarAccesos(
        @Query("id_departamento") idDepartamento: Int? = null,
        @Query("limite") limite: Int? = 50,
        @Query("resultado") resultado: String? = null // PERMITIDO o DENEGADO
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("access_log.php")
    fun registrarAcceso(
        @Field("id_usuario") idUsuario: Int,
        @Field("metodo") metodo: String, // RFID, APP, MANUAL
        @Field("tipo_evento") tipoEvento: String? = "ACCESO_VALIDO",
        @Field("detalles") detalles: String? = null
    ): Call<Map<String, Any>>

    // ==========================================
    // USUARIOS
    // ==========================================

    @GET("usuarios/listar.php")
    fun listarUsuarios(
        @Query("id_departamento") idDepartamento: Int
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("usuarios/actualizar_estado.php")
    fun actualizarEstadoUsuario(
        @Field("id_usuario_objetivo") idUsuarioObjetivo: Int,
        @Field("nuevo_estado") nuevoEstado: String, // ACTIVO, INACTIVO, BLOQUEADO
        @Field("id_usuario_admin") idUsuarioAdmin: Int
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("usuarios/cambiar_rol.php")
    fun cambiarRolUsuario(
        @Field("id_usuario_objetivo") idUsuarioObjetivo: Int,
        @Field("nuevo_rol") nuevoRol: String, // administrador u operador
        @Field("id_usuario_admin") idUsuarioAdmin: Int
    ): Call<Map<String, Any>>

    // ==========================================
    // RFID (Para NodeMCU - opcional en app)
    // ==========================================

    @GET("rfid/validar.php")
    fun validarSensorRFID(
        @Query("codigo_sensor") codigoSensor: String
    ): Call<Map<String, Any>>
}