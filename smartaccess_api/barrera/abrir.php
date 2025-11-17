<?php
/**
 * API: Abrir barrera manualmente desde la app
 * Método: POST
 * Parámetros: id_usuario
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros
validarParametros(['id_usuario']);

$id_usuario = intval($_POST['id_usuario']);

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Verificar que el usuario existe y está activo
$sql_user = "SELECT nombre, apellido, estado FROM usuarios WHERE id_usuario = ?";
$stmt_user = $conn->prepare($sql_user);
$stmt_user->bind_param("i", $id_usuario);
$stmt_user->execute();
$result_user = $stmt_user->get_result();

if ($result_user->num_rows === 0) {
    $stmt_user->close();
    $conn->close();
    respuestaError('Usuario no encontrado', 404);
}

$usuario = $result_user->fetch_assoc();
if ($usuario['estado'] !== 'ACTIVO') {
    $stmt_user->close();
    $conn->close();
    respuestaError('Usuario no está activo', 403);
}
$stmt_user->close();

// Actualizar estado de la barrera
$sql_update = "UPDATE estado_barrera SET 
                estado = 'ABIERTA', 
                id_usuario_accion = ?, 
                tipo_accion = 'APERTURA_MANUAL',
                ultima_actualizacion = NOW()
                WHERE id = 1";
$stmt_update = $conn->prepare($sql_update);
$stmt_update->bind_param("i", $id_usuario);

if ($stmt_update->execute()) {
    $stmt_update->close();
    
    // Registrar evento de apertura manual
    registrarEvento($conn, null, $id_usuario, 'APERTURA_MANUAL', 'PERMITIDO', 'APP', 'Apertura manual desde aplicación móvil');
    
    $conn->close();
    
    respuestaExito([
        'estado_barrera' => 'ABIERTA',
        'usuario' => $usuario['nombre'] . ' ' . $usuario['apellido'],
        'fecha_hora' => date('Y-m-d H:i:s')
    ], 'Barrera abierta exitosamente');
} else {
    $stmt_update->close();
    $conn->close();
    respuestaError('Error al abrir la barrera', 500);
}
?>