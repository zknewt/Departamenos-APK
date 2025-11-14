<?php
/**
 * API: Login de usuario
 * Método: POST
 * Parámetros: email, contrasena
 */

require_once 'db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros
validarParametros(['email', 'contrasena']);

$email = sanitizar($_POST['email']);
$contrasena = $_POST['contrasena'];

// Validar formato de email
if (!validarEmail($email)) {
    respuestaError('Formato de email inválido');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Buscar usuario por email
$sql = "SELECT 
            u.id_usuario,
            u.nombre,
            u.apellido,
            u.email,
            u.password_hash,
            u.rol,
            u.estado,
            u.id_departamento,
            d.numero AS depto_numero,
            d.torre,
            d.condominio
        FROM usuarios u
        INNER JOIN departamentos d ON u.id_departamento = d.id_departamento
        WHERE u.email = ?";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

// Verificar si existe el usuario
if ($result->num_rows === 0) {
    $stmt->close();
    $conn->close();
    respuestaError('Credenciales incorrectas', 401);
}

$usuario = $result->fetch_assoc();
$stmt->close();

// Verificar contraseña
if (!verificarPassword($contrasena, $usuario['password_hash'])) {
    $conn->close();
    respuestaError('Credenciales incorrectas', 401);
}

// Verificar estado del usuario
if ($usuario['estado'] !== 'ACTIVO') {
    $conn->close();
    respuestaError('Usuario ' . strtolower($usuario['estado']) . '. Contacta al administrador', 403);
}

// Actualizar último acceso
$sql_update = "UPDATE usuarios SET ultimo_acceso = NOW() WHERE id_usuario = ?";
$stmt_update = $conn->prepare($sql_update);
$stmt_update->bind_param("i", $usuario['id_usuario']);
$stmt_update->execute();
$stmt_update->close();

$conn->close();

// Respuesta exitosa con datos del usuario
respuestaExito([
    'id_usuario' => $usuario['id_usuario'],
    'nombre' => $usuario['nombre'],
    'apellido' => $usuario['apellido'],
    'email' => $usuario['email'],
    'rol' => $usuario['rol'],
    'id_departamento' => $usuario['id_departamento'],
    'departamento' => $usuario['depto_numero'] . ($usuario['torre'] ? ' - Torre ' . $usuario['torre'] : ''),
    'condominio' => $usuario['condominio'],
    'es_admin' => ($usuario['rol'] === 'administrador')
], 'Login exitoso');
?>