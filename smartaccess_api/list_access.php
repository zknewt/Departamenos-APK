<?php
/**
 * API: Listar eventos de acceso
 * Método: GET
 * Parámetros opcionales: 
 *   - id_departamento: filtrar por departamento
 *   - limite: cantidad de registros (default 50)
 *   - resultado: PERMITIDO o DENEGADO
 */

require_once 'db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    respuestaError('Método no permitido', 405);
}

// Parámetros opcionales
$id_departamento = isset($_GET['id_departamento']) ? intval($_GET['id_departamento']) : null;
$limite = isset($_GET['limite']) ? intval($_GET['limite']) : 50;
$resultado = isset($_GET['resultado']) ? strtoupper(sanitizar($_GET['resultado'])) : null;

// Validar límite
if ($limite < 1 || $limite > 200) {
    $limite = 50;
}

// Validar resultado
if ($resultado && !in_array($resultado, ['PERMITIDO', 'DENEGADO'])) {
    respuestaError('Resultado inválido. Debe ser PERMITIDO o DENEGADO');
}

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Construir query dinámicamente
$sql = "SELECT 
            ea.id_evento,
            ea.fecha_hora,
            ea.tipo_evento,
            ea.resultado,
            ea.metodo,
            ea.detalles,
            s.codigo_sensor,
            s.tipo AS tipo_sensor,
            u.nombre AS usuario_nombre,
            u.apellido AS usuario_apellido,
            d.numero AS depto_numero,
            d.torre
        FROM eventos_acceso ea
        LEFT JOIN sensores s ON ea.id_sensor = s.id_sensor
        LEFT JOIN usuarios u ON ea.id_usuario = u.id_usuario
        LEFT JOIN departamentos d ON (s.id_departamento = d.id_departamento OR u.id_departamento = d.id_departamento)
        WHERE 1=1";

$params = [];
$types = "";

// Filtro por departamento
if ($id_departamento) {
    $sql .= " AND (s.id_departamento = ? OR u.id_departamento = ?)";
    $params[] = $id_departamento;
    $params[] = $id_departamento;
    $types .= "ii";
}

// Filtro por resultado
if ($resultado) {
    $sql .= " AND ea.resultado = ?";
    $params[] = $resultado;
    $types .= "s";
}

$sql .= " ORDER BY ea.fecha_hora DESC LIMIT ?";
$params[] = $limite;
$types .= "i";

// Preparar y ejecutar
$stmt = $conn->prepare($sql);
if (!empty($params)) {
    $stmt->bind_param($types, ...$params);
}
$stmt->execute();
$result = $stmt->get_result();

$eventos = [];
while ($row = $result->fetch_assoc()) {
    $usuario = null;
    if ($row['usuario_nombre']) {
        $usuario = $row['usuario_nombre'] . ' ' . $row['usuario_apellido'];
    }
    
    $departamento = null;
    if ($row['depto_numero']) {
        $departamento = $row['depto_numero'] . ($row['torre'] ? ' - Torre ' . $row['torre'] : '');
    }
    
    $eventos[] = [
        'id_evento' => $row['id_evento'],
        'fecha_hora' => $row['fecha_hora'],
        'tipo_evento' => $row['tipo_evento'],
        'resultado' => $row['resultado'],
        'metodo' => $row['metodo'],
        'codigo_sensor' => $row['codigo_sensor'],
        'tipo_sensor' => $row['tipo_sensor'],
        'usuario' => $usuario,
        'departamento' => $departamento,
        'detalles' => $row['detalles']
    ];
}

$stmt->close();
$conn->close();

respuestaExito([
    'total' => count($eventos),
    'limite_aplicado' => $limite,
    'eventos' => $eventos
], count($eventos) . " evento(s) encontrado(s)");
?>
