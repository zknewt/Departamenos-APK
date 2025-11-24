<?php
/**
 * API: Registrar evento de acceso desde NodeMCU
 * Método: POST
 * Parámetros: codigo_sensor (opcional), tipo_evento, resultado
 * USO: NodeMCU puede registrar eventos adicionales si es necesario
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros mínimos
validarParametros(['tipo_evento', 'resultado']);

$codigo_sensor = isset($_POST['codigo_sensor']) ? sanitizar($_POST['codigo_sensor']) : null;
$tipo_evento = strtoupper(sanitizar($_POST['tipo_evento']));
$resultado = strtoupper(sanitizar($_POST['resultado']));
$detalles = isset($_POST['detalles']) ? sanitizar($_POST['detalles']) : null;

// Validar tipo de evento
$tipos_validos = ['ACCESO_VALIDO', 'ACCESO_RECHAZADO', 'APERTURA_MANUAL', 'CIERRE_MANUAL', 'SENSOR_DESCONOCIDO', 'SENSOR_BLOQUEADO'];
if (!in_array($tipo_evento, $tipos_validos)) {
    respuestaError('Tipo de evento inválido');
}

// Validar resultado
if (!in_array($resultado, ['PERMITIDO', 'DENEGADO'])) {
    respuestaError('Resultado inválido. Debe ser PERMITIDO o DENEGADO');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

$id_sensor = null;
$id_usuario = null;

// Si se proporcionó código de sensor, buscar su ID
if ($codigo_sensor) {
    $sql_sensor = "SELECT id_sensor, id_usuario_registro FROM sensores WHERE codigo_sensor = ?";
    $stmt_sensor = $conn->prepare($sql_sensor);
    $stmt_sensor->bind_param("s", $codigo_sensor);
    $stmt_sensor->execute();
    $result_sensor = $stmt_sensor->get_result();
    
    if ($result_sensor->num_rows > 0) {
        $sensor = $result_sensor->fetch_assoc();
        $id_sensor = $sensor['id_sensor'];
        $id_usuario = $sensor['id_usuario_registro'];
    }
    $stmt_sensor->close();
}

// Registrar evento
registrarEvento($conn, $id_sensor, $id_usuario, $tipo_evento, $resultado, 'RFID', $detalles);

$conn->close();

respuestaExito([
    'id_sensor' => $id_sensor,
    'tipo_evento' => $tipo_evento,
    'resultado' => $resultado,
    'fecha_hora' => date('Y-m-d H:i:s')
], 'Evento registrado correctamente');
?>