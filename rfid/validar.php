<?php
/**
 * API: Validar acceso por código de sensor RFID
 * Método: GET o POST
 * Parámetros: codigo_sensor
 * USO: NodeMCU enviará el UID leído para validar acceso
 */

require_once '../db_config.php';

// Aceptar GET o POST
$codigo_sensor = null;

if ($_SERVER['REQUEST_METHOD'] === 'GET' && isset($_GET['codigo_sensor'])) {
    $codigo_sensor = sanitizar($_GET['codigo_sensor']);
} elseif ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['codigo_sensor'])) {
    $codigo_sensor = sanitizar($_POST['codigo_sensor']);
} else {
    respuestaError('Parámetro codigo_sensor es requerido');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Buscar el sensor y validar su estado
$sql = "SELECT 
            s.id_sensor,
            s.codigo_sensor,
            s.tipo,
            s.estado,
            s.id_departamento,
            d.numero AS depto_numero,
            d.torre,
            s.id_usuario_registro
        FROM sensores s
        INNER JOIN departamentos d ON s.id_departamento = d.id_departamento
        WHERE s.codigo_sensor = ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $codigo_sensor);
$stmt->execute();
$result = $stmt->get_result();

// Si el sensor no existe
if ($result->num_rows === 0) {
    $stmt->close();
    
    // Registrar evento de sensor desconocido
    registrarEvento($conn, null, null, 'SENSOR_DESCONOCIDO', 'DENEGADO', 'RFID', "Código sensor desconocido: $codigo_sensor");
    
    $conn->close();
    
    respuestaExito([
        'acceso_permitido' => false,
        'motivo' => 'SENSOR_DESCONOCIDO',
        'codigo_sensor' => $codigo_sensor,
        'accion_led' => 'ROJO',
        'accion_barrera' => 'MANTENER_CERRADA'
    ], 'Sensor no registrado');
}

$sensor = $result->fetch_assoc();
$stmt->close();

// Verificar estado del sensor
$acceso_permitido = false;
$motivo = '';
$accion_led = 'ROJO';
$accion_barrera = 'MANTENER_CERRADA';
$tipo_evento = 'ACCESO_RECHAZADO';
$resultado = 'DENEGADO';

if ($sensor['estado'] === 'ACTIVO') {
    // Acceso permitido
    $acceso_permitido = true;
    $motivo = 'SENSOR_ACTIVO';
    $accion_led = 'VERDE';
    $accion_barrera = 'ABRIR_10_SEGUNDOS';
    $tipo_evento = 'ACCESO_VALIDO';
    $resultado = 'PERMITIDO';
    
    // Actualizar estado de barrera
    $sql_barrera = "UPDATE estado_barrera SET 
                    estado = 'ABIERTA',
                    tipo_accion = 'APERTURA_AUTO',
                    ultima_actualizacion = NOW()
                    WHERE id = 1";
    $conn->query($sql_barrera);
} else {
    // Acceso denegado según estado
    switch ($sensor['estado']) {
        case 'INACTIVO':
            $motivo = 'SENSOR_INACTIVO';
            $tipo_evento = 'ACCESO_RECHAZADO';
            break;
        case 'PERDIDO':
            $motivo = 'SENSOR_PERDIDO';
            $tipo_evento = 'ACCESO_RECHAZADO';
            break;
        case 'BLOQUEADO':
            $motivo = 'SENSOR_BLOQUEADO';
            $tipo_evento = 'SENSOR_BLOQUEADO';
            break;
    }
}

// Registrar evento de acceso
registrarEvento(
    $conn, 
    $sensor['id_sensor'], 
    $sensor['id_usuario_registro'], 
    $tipo_evento, 
    $resultado, 
    'RFID', 
    "$motivo - Depto: {$sensor['depto_numero']}{$sensor['torre']}"
);

$conn->close();

// Responder con instrucciones para el NodeMCU
respuestaExito([
    'acceso_permitido' => $acceso_permitido,
    'motivo' => $motivo,
    'codigo_sensor' => $codigo_sensor,
    'tipo_sensor' => $sensor['tipo'],
    'estado_sensor' => $sensor['estado'],
    'departamento' => $sensor['depto_numero'] . $sensor['torre'],
    'accion_led' => $accion_led,
    'accion_barrera' => $accion_barrera,
    'tiempo_apertura' => 10 // segundos
], $acceso_permitido ? 'Acceso permitido' : 'Acceso denegado');
?>