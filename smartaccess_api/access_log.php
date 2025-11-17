<?php
/**
 * API: Registrar evento de acceso manualmente desde app
 * Método: POST
 * Parámetros: id_usuario, metodo, tipo_evento (opcional), detalles (opcional)
 */

require_once 'db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros
validarParametros(['id_usuario', 'metodo']);

$id_usuario = intval($_POST['id_usuario']);
$metodo = strtoupper(sanitizar($_POST['metodo']));
$tipo_evento = isset($_POST['tipo_evento']) ? strtoupper(sanitizar($_POST['tipo_evento'])) : 'ACCESO_VALIDO';
$detalles = isset($_POST['detalles']) ? sanitizar($_POST['detalles']) : 'Acceso desde aplicación móvil';

// Validar método
$metodos_validos = ['RFID', 'APP', 'MANUAL', 'DESCONOCIDO'];
if (!in_array($metodo, $metodos_validos)) {
    respuestaError('Método inválido');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Verificar que el usuario existe
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
$stmt_user->close();

// Determinar resultado según estado del usuario
$resultado = ($usuario['estado'] === 'ACTIVO') ? 'PERMITIDO' : 'DENEGADO';

// Registrar evento
registrarEvento($conn, null, $id_usuario, $tipo_evento, $resultado, $metodo, $detalles);

$conn->close();

respuestaExito([
    'id_usuario' => $id_usuario,
    'usuario' => $usuario['nombre'] . ' ' . $usuario['apellido'],
    'tipo_evento' => $tipo_evento,
    'resultado' => $resultado,
    'metodo' => $metodo,
    'fecha_hora' => date('Y-m-d H:i:s')
], 'Evento de acceso registrado');
?>