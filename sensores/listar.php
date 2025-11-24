<?php
/**
 * API: Listar sensores de un departamento
 * Método: GET
 * Parámetros: id_departamento
 */

require_once '../db_config.php';

// Validar método
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    respuestaError('Método no permitido', 405);
}

// Validar parámetros
validarParametros(['id_departamento'], 'GET');

$id_departamento = intval($_GET['id_departamento']);

// Conectar a BD
$conn = conectarDB();
if (!$conn) {
    respuestaError('Error de conexión a la base de datos', 500);
}

// Consultar sensores del departamento
$sql = "SELECT 
            s.id_sensor,
            s.codigo_sensor,
            s.tipo,
            s.estado,
            s.descripcion,
            s.fecha_alta,
            s.ultimo_uso,
            u.nombre AS registrado_por_nombre,
            u.apellido AS registrado_por_apellido
        FROM sensores s
        INNER JOIN usuarios u ON s.id_usuario_registro = u.id_usuario
        WHERE s.id_departamento = ?
        ORDER BY s.fecha_alta DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $id_departamento);
$stmt->execute();
$result = $stmt->get_result();

$sensores = [];
while ($row = $result->fetch_assoc()) {
    $sensores[] = [
        'id_sensor' => $row['id_sensor'],
        'codigo_sensor' => $row['codigo_sensor'],
        'tipo' => $row['tipo'],
        'estado' => $row['estado'],
        'descripcion' => $row['descripcion'],
        'fecha_alta' => $row['fecha_alta'],
        'ultimo_uso' => $row['ultimo_uso'],
        'registrado_por' => $row['registrado_por_nombre'] . ' ' . $row['registrado_por_apellido']
    ];
}

$stmt->close();
$conn->close();

respuestaExito($sensores, count($sensores) . " sensor(es) encontrado(s)");
?>