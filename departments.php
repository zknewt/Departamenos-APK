<?php
/**
 * API: Gestión de departamentos
 * Métodos: GET (listar), POST (crear)
 */

require_once 'db_config.php';

// ============================================
// LISTAR DEPARTAMENTOS
// ============================================
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    
    // Conectar a BD
    $conn = conectarDB();
    if (!$conn) {
        respuestaError('Error de conexión a la base de datos', 500);
    }
    
    // Obtener todos los departamentos con información adicional
    $sql = "SELECT 
                d.id_departamento,
                d.numero,
                d.torre,
                d.condominio,
                d.piso,
                d.fecha_creacion,
                COUNT(DISTINCT u.id_usuario) as total_usuarios,
                COUNT(DISTINCT s.id_sensor) as total_sensores,
                SUM(CASE WHEN u.rol = 'administrador' THEN 1 ELSE 0 END) as tiene_admin
            FROM departamentos d
            LEFT JOIN usuarios u ON d.id_departamento = u.id_departamento
            LEFT JOIN sensores s ON d.id_departamento = s.id_departamento
            GROUP BY d.id_departamento
            ORDER BY d.torre ASC, d.numero ASC";
    
    $result = $conn->query($sql);
    
    $departamentos = [];
    while ($row = $result->fetch_assoc()) {
        $departamentos[] = [
            'id_departamento' => $row['id_departamento'],
            'numero' => $row['numero'],
            'torre' => $row['torre'],
            'condominio' => $row['condominio'],
            'piso' => $row['piso'],
            'nombre_completo' => $row['numero'] . ($row['torre'] ? ' - Torre ' . $row['torre'] : ''),
            'fecha_creacion' => $row['fecha_creacion'],
            'total_usuarios' => (int)$row['total_usuarios'],
            'total_sensores' => (int)$row['total_sensores'],
            'tiene_administrador' => (int)$row['tiene_admin'] > 0
        ];
    }
    
    $conn->close();
    
    respuestaExito($departamentos, count($departamentos) . " departamento(s) encontrado(s)");
}

// ============================================
// CREAR NUEVO DEPARTAMENTO
// ============================================
elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
    
    // Validar parámetros
    validarParametros(['numero']);
    
    $numero = sanitizar($_POST['numero']);
    $torre = isset($_POST['torre']) ? sanitizar($_POST['torre']) : null;
    $condominio = isset($_POST['condominio']) ? sanitizar($_POST['condominio']) : 'Principal';
    $piso = isset($_POST['piso']) ? intval($_POST['piso']) : null;
    
    // Conectar a BD
    $conn = conectarDB();
    if (!$conn) {
        respuestaError('Error de conexión a la base de datos', 500);
    }
    
    // Verificar si ya existe el departamento
    $sql_check = "SELECT id_departamento FROM departamentos 
                  WHERE numero = ? AND torre = ? AND condominio = ?";
    $stmt_check = $conn->prepare($sql_check);
    $stmt_check->bind_param("sss", $numero, $torre, $condominio);
    $stmt_check->execute();
    $result_check = $stmt_check->get_result();
    
    if ($result_check->num_rows > 0) {
        $stmt_check->close();
        $conn->close();
        respuestaError('El departamento ya existe');
    }
    $stmt_check->close();
    
    // Insertar nuevo departamento
    $sql = "INSERT INTO departamentos (numero, torre, condominio, piso) VALUES (?, ?, ?, ?)";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("sssi", $numero, $torre, $condominio, $piso);
    
    if ($stmt->execute()) {
        $id_departamento = $conn->insert_id;
        $stmt->close();
        $conn->close();
        
        respuestaExito([
            'id_departamento' => $id_departamento,
            'numero' => $numero,
            'torre' => $torre,
            'condominio' => $condominio,
            'piso' => $piso
        ], 'Departamento creado exitosamente');
    } else {
        $stmt->close();
        $conn->close();
        respuestaError('Error al crear el departamento', 500);
    }
}

else {
    respuestaError('Método no permitido', 405);
}
?>