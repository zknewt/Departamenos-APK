<?php
/**
 * API: Listar usuarios de un departamento
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

// Consultar usuarios del departamento
$sql = "SELECT 
            u.id_usuario,
            u.nombre,
            u.apellido,
            u.email,
            u.rol,
            u.estado,
            u.telefono,
            u.fecha_registro,
            u.ultimo_acceso,
            d.numero AS depto_numero,
            d.torre
        FROM usuarios u
        INNER JOIN departamentos d ON u.id_departamento = d.id_departamento
        WHERE u.id_departamento = ?
        ORDER BY 
            CASE WHEN u.rol = 'administrador' THEN 0 ELSE 1 END,
            u.fecha_registro ASC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $id_departamento);
$stmt->execute();
$result = $stmt->get_result();

$usuarios = [];
while ($row = $result->fetch_assoc()) {
    $usuarios[] = [
        'id_usuario' => $row['id_usuario'],
        'nombre_completo' => $row['nombre'] . ' ' . $row['apellido'],
        'nombre' => $row['nombre'],
        'apellido' => $row['apellido'],
        'email' => $row['email'],
        'rol' => $row['rol'],
        'estado' => $row['estado'],
        'telefono' => $row['telefono'],
        'departamento' => $row['depto_numero'] . ($row['torre'] ? ' - Torre ' . $row['torre'] : ''),
        'fecha_registro' => $row['fecha_registro'],
        'ultimo_acceso' => $row['ultimo_acceso']
    ];
}

$stmt->close();
$conn->close();

respuestaExito($usuarios, count($usuarios) . " usuario(s) encontrado(s)");
?>