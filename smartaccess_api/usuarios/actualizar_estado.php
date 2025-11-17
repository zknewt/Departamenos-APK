<?php
/**
 * API: Actualizar estado de un usuario
 * Método: POST
 * Parámetros: id_usuario_objetivo, nuevo_estado, id_usuario_admin
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros
validarParametros(['id_usuario_objetivo', 'nuevo_estado', 'id_usuario_admin']);

$id_usuario_objetivo = intval($_POST['id_usuario_objetivo']);
$nuevo_estado = strtoupper(sanitizar($_POST['nuevo_estado']));
$id_usuario_admin = intval($_POST['id_usuario_admin']);

// Validar estado
$estados_validos = ['ACTIVO', 'INACTIVO', 'BLOQUEADO'];
if (!in_array($nuevo_estado, $estados_validos)) {
    respuestaError('Estado inválido. Debe ser: ACTIVO, INACTIVO o BLOQUEADO');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Verificar que el usuario objetivo existe
$sql_objetivo = "SELECT id_departamento, nombre, apellido, estado, rol FROM usuarios WHERE id_usuario = ?";
$stmt_objetivo = $conn->prepare($sql_objetivo);
$stmt_objetivo->bind_param("i", $id_usuario_objetivo);
$stmt_objetivo->execute();
$result_objetivo = $stmt_objetivo->get_result();

if ($result_objetivo->num_rows === 0) {
    $stmt_objetivo->close();
    $conn->close();
    respuestaError('Usuario objetivo no encontrado', 404);
}

$usuario_objetivo = $result_objetivo->fetch_assoc();
$id_departamento = $usuario_objetivo['id_departamento'];
$estado_anterior = $usuario_objetivo['estado'];
$stmt_objetivo->close();

// Verificar que el usuario admin es del mismo departamento y es administrador
$sql_admin = "SELECT rol FROM usuarios WHERE id_usuario = ? AND id_departamento = ?";
$stmt_admin = $conn->prepare($sql_admin);
$stmt_admin->bind_param("ii", $id_usuario_admin, $id_departamento);
$stmt_admin->execute();
$result_admin = $stmt_admin->get_result();

if ($result_admin->num_rows === 0) {
    $stmt_admin->close();
    $conn->close();
    respuestaError('Administrador no pertenece al mismo departamento', 403);
}

$admin = $result_admin->fetch_assoc();
if ($admin['rol'] !== 'administrador') {
    $stmt_admin->close();
    $conn->close();
    respuestaError('Solo administradores pueden cambiar el estado de usuarios', 403);
}
$stmt_admin->close();

// No permitir cambiar el estado de otro administrador
if ($usuario_objetivo['rol'] === 'administrador' && $id_usuario_objetivo !== $id_usuario_admin) {
    $conn->close();
    respuestaError('No se puede cambiar el estado de otro administrador', 403);
}

// Actualizar estado
$sql_update = "UPDATE usuarios SET estado = ? WHERE id_usuario = ?";
$stmt_update = $conn->prepare($sql_update);
$stmt_update->bind_param("si", $nuevo_estado, $id_usuario_objetivo);

if ($stmt_update->execute()) {
    $stmt_update->close();
    $conn->close();
    
    respuestaExito([
        'id_usuario' => $id_usuario_objetivo,
        'nombre' => $usuario_objetivo['nombre'] . ' ' . $usuario_objetivo['apellido'],
        'estado_anterior' => $estado_anterior,
        'estado_nuevo' => $nuevo_estado
    ], "Estado actualizado de $estado_anterior a $nuevo_estado");
} else {
    $stmt_update->close();
    $conn->close();
    respuestaError('Error al actualizar el estado', 500);
}
?>