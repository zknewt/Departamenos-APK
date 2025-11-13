package com.example.smartaccesscontrol.api

import com.example.smartaccesscontrol.models.Usuario
import com.example.smartaccesscontrol.models.Acceso
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

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
        @Field("tipo_usuario") tipo: String = "residente",
        @Field("tarjeta_rfid") tarjeta: String? = null
    ): Call<Map<String, Any>>

    @FormUrlEncoded
    @POST("access_log.php")
    fun registrarAcceso(
        @Field("usuario_id") usuarioId: Int,
        @Field("metodo") metodo: String,
        @Field("estado") estado: String = "permitido"
    ): Call<Map<String, Any>>

    @GET("list_access.php")
    fun listarAccesos(): Call<Map<String, Any>>
}
