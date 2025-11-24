<?php
/**
 * API: Actualizar estado de un sensor
 * Método: POST
 * Parámetros: id_sensor, nuevo_estado, id_usuario
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros
validarParametros(['id_sensor', 'nuevo_estado', 'id_usuario']);

$id_sensor = intval($_POST['id_sensor']);
$nuevo_estado = strtoupper(sanitizar($_POST['nuevo_estado']));
$id_usuario = intval($_POST['id_usuario']);

// Validar estado
$estados_validos = ['ACTIVO', 'INACTIVO', 'PERDIDO', 'BLOQUEADO'];
if (!in_array($nuevo_estado, $estados_validos)) {
    respuestaError('Estado inválido. Debe ser: ACTIVO, INACTIVO, PERDIDO o BLOQUEADO');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Verificar que el sensor existe y obtener su departamento
$sql_sensor = "SELECT id_departamento, estado FROM sensores WHERE id_sensor = ?";
$stmt_sensor = $conn->prepare($sql_sensor);
$stmt_sensor->bind_param("i", $id_sensor);
$stmt_sensor->execute();
$result_sensor = $stmt_sensor->get_result();

if ($result_sensor->num_rows === 0) {
    $stmt_sensor->close();
    $conn->close();
    respuestaError('Sensor no encontrado', 404);
}

$sensor = $result_sensor->fetch_assoc();
$id_departamento = $sensor['id_departamento'];
$estado_anterior = $sensor['estado'];
$stmt_sensor->close();

// Verificar que el usuario es administrador del departamento
$sql_user = "SELECT rol FROM usuarios WHERE id_usuario = ? AND id_departamento = ?";
$stmt_user = $conn->prepare($sql_user);
$stmt_user->bind_param("ii", $id_usuario, $id_departamento);
$stmt_user->execute();
$result_user = $stmt_user->get_result();

if ($result_user->num_rows === 0) {
    $stmt_user->close();
    $conn->close();
    respuestaError('Usuario no pertenece al departamento', 403);
}

$usuario = $result_user->fetch_assoc();
if ($usuario['rol'] !== 'administrador') {
    $stmt_user->close();
    $conn->close();
    respuestaError('Solo administradores pueden cambiar el estado de sensores', 403);
}
$stmt_user->close();

// Actualizar estado del sensor
$sql_update = "UPDATE sensores SET estado = ? WHERE id_sensor = ?";
$stmt_update = $conn->prepare($sql_update);
$stmt_update->bind_param("si", $nuevo_estado, $id_sensor);

if ($stmt_update->execute()) {
    // Si se marca como PERDIDO o BLOQUEADO, registrar fecha de baja
    if (in_array($nuevo_estado, ['PERDIDO', 'BLOQUEADO'])) {
        $sql_baja = "UPDATE sensores SET fecha_baja = NOW() WHERE id_sensor = ?";
        $stmt_baja = $conn->prepare($sql_baja);
        $stmt_baja->bind_param("i", $id_sensor);
        $stmt_baja->execute();
        $stmt_baja->close();
    }
    
    $stmt_update->close();
    $conn->close();
    
    respuestaExito([
        'id_sensor' => $id_sensor,
        'estado_anterior' => $estado_anterior,
        'estado_nuevo' => $nuevo_estado
    ], "Estado del sensor actualizado de $estado_anterior a $nuevo_estado");
} else {
    $stmt_update->close();
    $conn->close();
    respuestaError('Error al actualizar el estado', 500);
}
?>