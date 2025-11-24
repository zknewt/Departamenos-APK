<?php
/**
 * API: Eliminar un sensor
 * Método: POST
 * Parámetros: id_sensor, id_usuario
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros
validarParametros(['id_sensor', 'id_usuario']);

$id_sensor = intval($_POST['id_sensor']);
$id_usuario = intval($_POST['id_usuario']);

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Verificar que el sensor existe y obtener su departamento
$sql_sensor = "SELECT id_departamento, codigo_sensor FROM sensores WHERE id_sensor = ?";
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
$codigo_sensor = $sensor['codigo_sensor'];
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
    respuestaError('Solo administradores pueden eliminar sensores', 403);
}
$stmt_user->close();

// Eliminar sensor
$sql_delete = "DELETE FROM sensores WHERE id_sensor = ?";
$stmt_delete = $conn->prepare($sql_delete);
$stmt_delete->bind_param("i", $id_sensor);

if ($stmt_delete->execute()) {
    $stmt_delete->close();
    $conn->close();
    
    respuestaExito([
        'id_sensor' => $id_sensor,
        'codigo_sensor' => $codigo_sensor
    ], 'Sensor eliminado exitosamente');
} else {
    $stmt_delete->close();
    $conn->close();
    respuestaError('Error al eliminar el sensor', 500);
}
?>