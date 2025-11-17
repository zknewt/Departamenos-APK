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
import com.example.smartaccesscontrol.models.Sensor
import com.example.smartaccesscontrol.utils.SessionManager
import com.example.smartaccesscontrol.utils.SweetDialogs
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SensorManagementActivity : AppCompatActivity() {

    private lateinit var btnAgregarSensor: Button
    private lateinit var listViewSensores: ListView
    private lateinit var sessionManager: SessionManager
    private val sensores = mutableListOf<Sensor>()
    private lateinit var adapter: SensorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_management)

        sessionManager = SessionManager(this)

        // Verificar que sea administrador
        if (!sessionManager.esAdministrador()) {
            SweetDialogs.error(this, "Solo administradores pueden acceder") {
                finish()
            }
            return
        }

        btnAgregarSensor = findViewById(R.id.btnAgregarSensor)
        listViewSensores = findViewById(R.id.listViewSensores)

        adapter = SensorAdapter()
        listViewSensores.adapter = adapter

        btnAgregarSensor.setOnClickListener {
            mostrarDialogoAgregarSensor()
        }

        cargarSensores()
    }

    private fun cargarSensores() {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.listarSensores(usuario.id_departamento).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val success = body?.get("success") as? Boolean ?: false

                    if (success) {
                        sensores.clear()
                        val data = body["data"] as? List<Map<String, Any>>
                        data?.forEach { sensorData ->
                            val sensor = Sensor(
                                id_sensor = (sensorData["id_sensor"] as? Double)?.toInt() ?: 0,
                                codigo_sensor = sensorData["codigo_sensor"] as? String ?: "",
                                tipo = sensorData["tipo"] as? String ?: "",
                                estado = sensorData["estado"] as? String ?: "",
                                descripcion = sensorData["descripcion"] as? String,
                                fecha_alta = sensorData["fecha_alta"] as? String ?: "",
                                ultimo_uso = sensorData["ultimo_uso"] as? String,
                                registrado_por = sensorData["registrado_por"] as? String ?: ""
                            )
                            sensores.add(sensor)
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                SweetDialogs.error(this@SensorManagementActivity, "Error al cargar sensores: ${t.message}")
            }
        })
    }

    private fun mostrarDialogoAgregarSensor() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_sensor, null)
        val etCodigoSensor = dialogView.findViewById<EditText>(R.id.etCodigoSensor)
        val spinnerTipo = dialogView.findViewById<Spinner>(R.id.spinnerTipoSensor)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)

        val tipos = arrayOf("TARJETA", "LLAVERO")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterSpinner

        AlertDialog.Builder(this)
            .setTitle("Agregar Sensor")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val codigo = etCodigoSensor.text.toString().trim()
                val tipo = spinnerTipo.selectedItem.toString()
                val descripcion = etDescripcion.text.toString().trim()

                if (codigo.isEmpty()) {
                    SweetDialogs.error(this, "Debes ingresar el código del sensor")
                    return@setPositiveButton
                }

                registrarSensor(codigo, tipo, descripcion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarSensor(codigo: String, tipo: String, descripcion: String?) {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Registrando sensor..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.registrarSensor(
            codigo,
            tipo,
            usuario.id_departamento,
            usuario.id_usuario,
            descripcion
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
                        SweetDialogs.success(this@SensorManagementActivity, "Sensor registrado exitosamente") {
                            cargarSensores()
                        }
                    } else {
                        val mensaje = body?.get("mensaje") as? String ?: "Error al registrar sensor"
                        SweetDialogs.error(this@SensorManagementActivity, mensaje)
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progressDialog.dismiss()
                SweetDialogs.error(this@SensorManagementActivity, "Error de conexión: ${t.message}")
            }
        })
    }

    private fun cambiarEstadoSensor(sensor: Sensor) {
        val estados = arrayOf("ACTIVO", "INACTIVO", "PERDIDO", "BLOQUEADO")
        val estadoActualIndex = estados.indexOf(sensor.estado)

        AlertDialog.Builder(this)
            .setTitle("Cambiar Estado")
            .setSingleChoiceItems(estados, estadoActualIndex) { dialog, which ->
                val nuevoEstado = estados[which]
                dialog.dismiss()

                if (nuevoEstado != sensor.estado) {
                    confirmarCambioEstado(sensor, nuevoEstado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarCambioEstado(sensor: Sensor, nuevoEstado: String) {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Actualizando estado..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.actualizarEstadoSensor(sensor.id_sensor, nuevoEstado, usuario.id_usuario)
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
                            SweetDialogs.success(this@SensorManagementActivity, "Estado actualizado") {
                                cargarSensores()
                            }
                        } else {
                            val mensaje = body?.get("mensaje") as? String ?: "Error al actualizar"
                            SweetDialogs.error(this@SensorManagementActivity, mensaje)
                        }
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    progressDialog.dismiss()
                    SweetDialogs.error(this@SensorManagementActivity, "Error de conexión")
                }
            })
    }

    private fun eliminarSensor(sensor: Sensor) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("¿Eliminar sensor?")
            .setContentText("Esta acción no se puede deshacer")
            .setConfirmText("Eliminar")
            .setCancelText("Cancelar")
            .setConfirmClickListener { dialog ->
                dialog.dismiss()
                confirmarEliminacion(sensor)
            }
            .show()
    }

    private fun confirmarEliminacion(sensor: Sensor) {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Eliminando sensor..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.eliminarSensor(sensor.id_sensor, usuario.id_usuario)
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
                            SweetDialogs.success(this@SensorManagementActivity, "Sensor eliminado") {
                                cargarSensores()
                            }
                        } else {
                            val mensaje = body?.get("mensaje") as? String ?: "Error al eliminar"
                            SweetDialogs.error(this@SensorManagementActivity, mensaje)
                        }
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    progressDialog.dismiss()
                    SweetDialogs.error(this@SensorManagementActivity, "Error de conexión")
                }
            })
    }

    inner class SensorAdapter : BaseAdapter() {
        override fun getCount(): Int = sensores.size
        override fun getItem(position: Int): Any = sensores[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@SensorManagementActivity)
                .inflate(R.layout.item_sensor, parent, false)

            val sensor = sensores[position]

            val tvCodigo = view.findViewById<TextView>(R.id.tvCodigoSensor)
            val tvTipo = view.findViewById<TextView>(R.id.tvTipoSensor)
            val tvEstado = view.findViewById<TextView>(R.id.tvEstadoSensor)
            val tvDescripcion = view.findViewById<TextView>(R.id.tvDescripcionSensor)
            val btnCambiarEstado = view.findViewById<Button>(R.id.btnCambiarEstado)
            val btnEliminar = view.findViewById<Button>(R.id.btnEliminarSensor)

            tvCodigo.text = "Código: ${sensor.codigo_sensor}"
            tvTipo.text = "Tipo: ${sensor.tipo}"
            tvEstado.text = "Estado: ${sensor.estado}"
            tvDescripcion.text = sensor.descripcion ?: "Sin descripción"

            // Color del estado
            val colorEstado = when (sensor.estado) {
                "ACTIVO" -> android.R.color.holo_green_dark
                "INACTIVO" -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            tvEstado.setTextColor(getColor(colorEstado))

            btnCambiarEstado.setOnClickListener {
                cambiarEstadoSensor(sensor)
            }

            btnEliminar.setOnClickListener {
                eliminarSensor(sensor)
            }

            return view
        }
    }
}