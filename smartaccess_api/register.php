<?php
/**
 * API: Registro de nuevo usuario
 * Método: POST
 * Parámetros: nombre, apellido, email, contrasena, id_departamento, telefono (opcional)
 */

require_once 'db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros requeridos
validarParametros(['nombre', 'apellido', 'email', 'contrasena', 'id_departamento']);

// Obtener y sanitizar datos
$nombre = sanitizar($_POST['nombre']);
$apellido = sanitizar($_POST['apellido']);
$email = sanitizar($_POST['email']);
$contrasena = $_POST['contrasena'];
$id_departamento = intval($_POST['id_departamento']);
$telefono = isset($_POST['telefono']) ? sanitizar($_POST['telefono']) : null;

// Validar formato de email
if (!validarEmail($email)) {
    respuestaError('Formato de email inválido');
}

// Validar longitud de contraseña
if (strlen($contrasena) < 6) {
    respuestaError('La contraseña debe tener al menos 6 caracteres');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Verificar que el departamento existe
$sql_depto = "SELECT id_departamento FROM departamentos WHERE id_departamento = ?";
$stmt_depto = $conn->prepare($sql_depto);
$stmt_depto->bind_param("i", $id_departamento);
$stmt_depto->execute();
$result_depto = $stmt_depto->get_result();

if ($result_depto->num_rows === 0)
    {
    $stmt_depto->close();
    $conn->close();
    respuestaError('Departamento no encontrado', 404);
}
$stmt_depto->close();

// Verificar si el email ya existe
$sql_check = "SELECT id_usuario FROM usuarios WHERE email = ?";
$stmt_check = $conn->prepare($sql_check);
$stmt_check->bind_param("s", $email);
$stmt_check->execute();
$result_check = $stmt_check->get_result();

if ($result_check->num_rows > 0) {
    $stmt_check->close();
    $conn->close();
    respuestaError('El email ya está registrado');
}
$stmt_check->close();

// Verificar si ya hay un administrador en el departamento
$sql_admin = "SELECT COUNT(*) as total FROM usuarios 
              WHERE id_departamento = ? AND rol = 'administrador'";
$stmt_admin = $conn->prepare($sql_admin);
$stmt_admin->bind_param("i", $id_departamento);
$stmt_admin->execute();
$result_admin = $stmt_admin->get_result();
$admin_count = $result_admin->fetch_assoc();
$stmt_admin->close();

// Si no hay administrador, este será el primero (administrador)
// Si ya hay administrador, será operador
$rol = ($admin_count['total'] == 0) ? 'administrador' : 'operador';

// Hashear contraseña
$password_hash = hashearPassword($contrasena);

// Insertar nuevo usuario
$sql = "INSERT INTO usuarios (nombre, apellido, email, password_hash, id_departamento, rol, telefono) 
        VALUES (?, ?, ?, ?, ?, ?, ?)";

$stmt = $conn->prepare($sql);
$stmt->bind_param("ssssiss", $nombre, $apellido, $email, $password_hash, $id_departamento, $rol, $telefono);

if ($stmt->execute()) {
    $id_usuario = $conn->insert_id;
    $stmt->close();
    $conn->close();
    
    respuestaExito([
        'id_usuario' => $id_usuario,
        'nombre' => $nombre,
        'apellido' => $apellido,
        'email' => $email,
        'rol' => $rol,
        'id_departamento' => $id_departamento
    ], 'Usuario registrado exitosamente como ' . $rol);
} else {
    $stmt->close();
    $conn->close();
    respuestaError('Error al registrar el usuario', 500);
}
?>