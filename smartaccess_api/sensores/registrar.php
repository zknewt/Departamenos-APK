<?php
/**
 * API: Registrar nuevo sensor RFID
 * Método: POST
 * Parámetros: codigo_sensor, tipo, id_departamento, id_usuario_registro, descripcion (opcional)
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros requeridos
validarParametros(['codigo_sensor', 'tipo', 'id_departamento', 'id_usuario_registro']);

// Obtener y sanitizar datos
$codigo_sensor = sanitizar($_POST['codigo_sensor']);
$tipo = strtoupper(sanitizar($_POST['tipo']));
$id_departamento = intval($_POST['id_departamento']);
$id_usuario_registro = intval($_POST['id_usuario_registro']);
$descripcion = isset($_POST['descripcion']) ? sanitizar($_POST['descripcion']) : null;

// Validar tipo de sensor
if (!in_array($tipo, ['LLAVERO', 'TARJETA'])) {
    respuestaError('Tipo de sensor inválido. Debe ser LLAVERO o TARJETA');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Verificar que el usuario es administrador del departamento
$sql_check = "SELECT rol FROM usuarios WHERE id_usuario = ? AND id_departamento = ?";
$stmt_check = $conn->prepare($sql_check);
$stmt_check->bind_param("ii", $id_usuario_registro, $id_departamento);
$stmt_check->execute();
$result_check = $stmt_check->get_result();

if ($result_check->num_rows === 0) {
    $stmt_check->close();
    $conn->close();
    respuestaError('Usuario no pertenece al departamento', 403);
}

$usuario = $result_check->fetch_assoc();
if ($usuario['rol'] !== 'administrador') {
    $stmt_check->close();
    $conn->close();
    respuestaError('Solo administradores pueden registrar sensores', 403);
}
$stmt_check->close();

// Verificar que el código del sensor no exista
$sql_existe = "SELECT id_sensor FROM sensores WHERE codigo_sensor = ?";
$stmt_existe = $conn->prepare($sql_existe);
$stmt_existe->bind_param("s", $codigo_sensor);
$stmt_existe->execute();
$result_existe = $stmt_existe->get_result();

if ($result_existe->num_rows > 0) {
    $stmt_existe->close();
    $conn->close();
    respuestaError('El código del sensor ya está registrado');
}
$stmt_existe->close();

// Insertar nuevo sensor
$sql = "INSERT INTO sensores (codigo_sensor, tipo, estado, id_departamento, id_usuario_registro, descripcion) 
        VALUES (?, ?, 'ACTIVO', ?, ?, ?)";

$stmt = $conn->prepare($sql);
$stmt->bind_param("ssiis", $codigo_sensor, $tipo, $id_departamento, $id_usuario_registro, $descripcion);

if ($stmt->execute()) {
    $id_sensor = $conn->insert_id;
    
    // Registrar evento
    registrarEvento($conn, $id_sensor, $id_usuario_registro, 'ACCESO_VALIDO', 'PERMITIDO', 'APP', 'Sensor registrado desde la aplicación');
    
    $stmt->close();
    $conn->close();
    
    respuestaExito([
        'id_sensor' => $id_sensor,
        'codigo_sensor' => $codigo_sensor,
        'tipo' => $tipo,
        'estado' => 'ACTIVO'
    ], 'Sensor registrado exitosamente');
} else {
    $stmt->close();
    $conn->close();
    respuestaError('Error al registrar el sensor', 500);
}
?>