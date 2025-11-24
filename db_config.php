<?php
/**
 * Configuración de conexión a base de datos
 * SmartAccess API
 */

// Configuración de base de datos
define('DB_HOST', 'localhost');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_NAME', 'smartaccess_db');

// Configuración de headers para API REST
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Manejo de peticiones OPTIONS (preflight)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

/**
 * Función para conectar a la base de datos
 * @return mysqli|null Conexión a la BD o null si falla
 */
function conectarDB() {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);
    
    if ($conn->connect_error) {
        error_log("Error de conexión: " . $conn->connect_error);
        return null;
    }
    
    $conn->set_charset("utf8mb4");
    return $conn;
}

/**
 * Función para responder JSON con éxito
 * @param mixed $data Datos a devolver
 * @param string $mensaje Mensaje opcional
 */
function respuestaExito($data = null, $mensaje = "Operación exitosa") {
    echo json_encode([
        'success' => true,
        'mensaje' => $mensaje,
        'data' => $data
    ], JSON_UNESCAPED_UNICODE);
    exit();
}

/**
 * Función para responder JSON con error
 * @param string $mensaje Mensaje de error
 * @param int $codigo Código HTTP
 */
function respuestaError($mensaje, $codigo = 400) {
    http_response_code($codigo);
    echo json_encode([
        'success' => false,
        'mensaje' => $mensaje
    ], JSON_UNESCAPED_UNICODE);
    exit();
}

/**
 * Función para validar que vengan los parámetros requeridos
 * @param array $parametros Array con los nombres de parámetros requeridos
 * @param string $metodo GET o POST
 */
function validarParametros($parametros, $metodo = 'POST') {
    $datos = ($metodo === 'POST') ? $_POST : $_GET;
    
    foreach ($parametros as $param) {
        if (!isset($datos[$param]) || empty(trim($datos[$param]))) {
            respuestaError("El parámetro '$param' es requerido");
        }
    }
}

/**
 * Función para sanitizar entrada de datos
 * @param string $data Dato a sanitizar
 * @return string Dato limpio
 */
function sanitizar($data) {
    $data = trim($data);
    $data = stripslashes($data);
    $data = htmlspecialchars($data);
    return $data;
}

/**
 * Función para validar email
 * @param string $email Email a validar
 * @return bool
 */
function validarEmail($email) {
    return filter_var($email, FILTER_VALIDATE_EMAIL);
}

/**
 * Función para hashear contraseña
 * NOTA: Usando MD5 solo para desarrollo. En producción usar password_hash()
 * @param string $password Contraseña plana
 * @return string Hash de la contraseña
 */
function hashearPassword($password) {
    // Para desarrollo
    return md5($password);
    
    // Para producción descomentar:
    // return password_hash($password, PASSWORD_BCRYPT);
}

/**
 * Función para verificar contraseña
 * @param string $password Contraseña plana
 * @param string $hash Hash almacenado
 * @return bool
 */
function verificarPassword($password, $hash) {
    // Para desarrollo
    return md5($password) === $hash;
    
    // Para producción descomentar:
    // return password_verify($password, $hash);
}

/**
 * Función para registrar un evento de acceso
 * @param mysqli $conn Conexión a BD
 * @param int|null $id_sensor ID del sensor
 * @param int|null $id_usuario ID del usuario
 * @param string $tipo_evento Tipo de evento
 * @param string $resultado PERMITIDO o DENEGADO
 * @param string $metodo RFID, APP, MANUAL
 * @param string|null $detalles Detalles adicionales
 */
function registrarEvento($conn, $id_sensor, $id_usuario, $tipo_evento, $resultado, $metodo = 'RFID', $detalles = null) {
    $stmt = $conn->prepare("INSERT INTO eventos_acceso (id_sensor, id_usuario, tipo_evento, resultado, metodo, detalles) VALUES (?, ?, ?, ?, ?, ?)");
    $stmt->bind_param("iissss", $id_sensor, $id_usuario, $tipo_evento, $resultado, $metodo, $detalles);
    $stmt->execute();
    $stmt->close();
}
?>