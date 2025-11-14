<?php
/**
 * API: Consultar estado actual de la barrera
 * Método: GET
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    respuestaError('Método no permitido', 405);
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Consultar estado de la barrera
$sql = "SELECT 
            eb.estado,
            eb.ultima_actualizacion,
            eb.tipo_accion,
            u.nombre AS usuario_nombre,
            u.apellido AS usuario_apellido
        FROM estado_barrera eb
        LEFT JOIN usuarios u ON eb.id_usuario_accion = u.id_usuario
        WHERE eb.id = 1";

$result = $conn->query($sql);

if ($result->num_rows === 0) {
    // Si no hay registro, insertar uno por defecto
    $sql_insert = "INSERT INTO estado_barrera (estado, tipo_accion) VALUES ('CERRADA', 'CIERRE_AUTO')";
    $conn->query($sql_insert);
    
    $conn->close();
    respuestaExito([
        'estado' => 'CERRADA',
        'ultima_actualizacion' => date('Y-m-d H:i:s'),
        'tipo_accion' => 'CIERRE_AUTO',
        'usuario' => null
    ], 'Estado de barrera (recién inicializada)');
}

$barrera = $result->fetch_assoc();
$conn->close();

$usuario_accion = null;
if ($barrera['usuario_nombre']) {
    $usuario_accion = $barrera['usuario_nombre'] . ' ' . $barrera['usuario_apellido'];
}

respuestaExito([
    'estado' => $barrera['estado'],
    'ultima_actualizacion' => $barrera['ultima_actualizacion'],
    'tipo_accion' => $barrera['tipo_accion'],
    'usuario' => $usuario_accion
], 'Estado actual de la barrera');
?>