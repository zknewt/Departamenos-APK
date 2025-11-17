package com.example.smartaccesscontrol.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartaccesscontrol.R
import com.example.smartaccesscontrol.api.ApiClient
import com.example.smartaccesscontrol.api.ApiService
import com.example.smartaccesscontrol.models.Evento
import com.example.smartaccesscontrol.utils.SessionManager
import com.example.smartaccesscontrol.utils.SweetDialogs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AccessHistoryActivity : AppCompatActivity() {

    private lateinit var spinnerFiltro: Spinner
    private lateinit var listViewHistorial: ListView
    private lateinit var tvTotalEventos: TextView
    private lateinit var btnActualizar: Button
    private lateinit var sessionManager: SessionManager
    private val eventos = mutableListOf<Evento>()
    private lateinit var adapter: EventoAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var actualizandoAutomaticamente = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_history)

        sessionManager = SessionManager(this)

        spinnerFiltro = findViewById(R.id.spinnerFiltroHistorial)
        listViewHistorial = findViewById(R.id.listViewHistorial)
        tvTotalEventos = findViewById(R.id.tvTotalEventos)
        btnActualizar = findViewById(R.id.btnActualizar)

        configurarSpinner()

        adapter = EventoAdapter()
        listViewHistorial.adapter = adapter

        btnActualizar.setOnClickListener {
            cargarHistorial()
        }

        cargarHistorial()
        iniciarActualizacionAutomatica()
    }

    private fun configurarSpinner() {
        val filtros = arrayOf("Todos", "Permitidos", "Denegados")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, filtros)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiltro.adapter = adapterSpinner

        spinnerFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                cargarHistorial()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun cargarHistorial() {
        val usuario = sessionManager.obtenerUsuario() ?: return

        val filtroSeleccionado = spinnerFiltro.selectedItemPosition
        val resultado = when (filtroSeleccionado) {
            1 -> "PERMITIDO"
            2 -> "DENEGADO"
            else -> null
        }

        val api = ApiClient.retrofit.create(ApiService::class.java)
        api.listarAccesos(usuario.id_departamento, 100, resultado)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val success = body?.get("success") as? Boolean ?: false

                        if (success) {
                            eventos.clear()
                            val data = body["data"] as? Map<String, Any>
                            val eventosData = data?.get("eventos") as? List<Map<String, Any>>

                            eventosData?.forEach { eventoData ->
                                val evento = Evento(
                                    id_evento = (eventoData["id_evento"] as? Double)?.toInt() ?: 0,
                                    fecha_hora = eventoData["fecha_hora"] as? String ?: "",
                                    tipo_evento = eventoData["tipo_evento"] as? String ?: "",
                                    resultado = eventoData["resultado"] as? String ?: "",
                                    metodo = eventoData["metodo"] as? String ?: "",
                                    codigo_sensor = eventoData["codigo_sensor"] as? String,
                                    tipo_sensor = eventoData["tipo_sensor"] as? String,
                                    usuario = eventoData["usuario"] as? String,
                                    departamento = eventoData["departamento"] as? String,
                                    detalles = eventoData["detalles"] as? String
                                )
                                eventos.add(evento)
                            }

                            tvTotalEventos.text = "Total de eventos: ${eventos.size}"
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    // Error silencioso para no molestar con popups en actualización automática
                }
            })
    }

    private fun iniciarActualizacionAutomatica() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (actualizandoAutomaticamente) {
                    cargarHistorial()
                    handler.postDelayed(this, 5000) // Actualizar cada 5 segundos
                }
            }
        }, 5000)
    }

    override fun onResume() {
        super.onResume()
        actualizandoAutomaticamente = true
        cargarHistorial()
    }

    override fun onPause() {
        super.onPause()
        actualizandoAutomaticamente = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun formatearFecha(fechaHora: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val fecha = formatoEntrada.parse(fechaHora)
            if (fecha != null) formatoSalida.format(fecha) else fechaHora
        } catch (e: Exception) {
            fechaHora
        }
    }

    inner class EventoAdapter : BaseAdapter() {
        override fun getCount(): Int = eventos.size
        override fun getItem(position: Int): Any = eventos[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@AccessHistoryActivity)
                .inflate(R.layout.item_evento, parent, false)

            val evento = eventos[position]

            val tvFechaHora = view.findViewById<TextView>(R.id.tvFechaHoraEvento)
            val tvTipoEvento = view.findViewById<TextView>(R.id.tvTipoEvento)
            val tvResultado = view.findViewById<TextView>(R.id.tvResultadoEvento)
            val tvMetodo = view.findViewById<TextView>(R.id.tvMetodoEvento)
            val tvUsuario = view.findViewById<TextView>(R.id.tvUsuarioEvento)
            val tvSensor = view.findViewById<TextView>(R.id.tvSensorEvento)
            val tvDetalles = view.findViewById<TextView>(R.id.tvDetallesEvento)
            val indicadorColor = view.findViewById<View>(R.id.indicadorColorEvento)

            tvFechaHora.text = formatearFecha(evento.fecha_hora)
            tvTipoEvento.text = evento.tipo_evento.replace("_", " ")
            tvResultado.text = evento.resultado
            tvMetodo.text = "Método: ${evento.metodo}"

            tvUsuario.text = "Usuario: ${evento.usuario ?: "N/A"}"

            val sensorInfo = if (evento.codigo_sensor != null) {
                "${evento.tipo_sensor ?: "Sensor"}: ${evento.codigo_sensor}"
            } else {
                "Sin sensor asociado"
            }
            tvSensor.text = sensorInfo

            tvDetalles.text = evento.detalles ?: ""
            tvDetalles.visibility = if (evento.detalles.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Color según resultado
            val color = if (evento.resultado == "PERMITIDO") {
                getColor(android.R.color.holo_green_dark)
            } else {
                getColor(android.R.color.holo_red_dark)
            }
            tvResultado.setTextColor(color)
            indicadorColor.setBackgroundColor(color)

            return view
        }
    }
}