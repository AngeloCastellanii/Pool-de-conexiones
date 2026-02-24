-- 
--  SEED: pool_simulator_db 
-- 

DROP TABLE IF EXISTS pedidos CASCADE;
DROP TABLE IF EXISTS productos CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS categorias CASCADE;

-- 
--  TABLA: categorias
-- 
CREATE TABLE categorias (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(80) NOT NULL UNIQUE,
    descripcion TEXT
);

INSERT INTO categorias (nombre, descripcion) VALUES
    ('Computacion',    'Equipos de computo y accesorios'),
    ('Monitores',      'Pantallas y monitores de escritorio'),
    ('Perifericos',    'Teclados, ratones y dispositivos de entrada'),
    ('Audio y Video',  'Auriculares, webcams y equipos multimedia'),
    ('Almacenamiento', 'Discos duros, SSDs y memorias'),
    ('Redes',          'Routers, switches y cables de red'),
    ('Energia',        'UPS, regletas y fuentes de poder'),
    ('Moviles',        'Tablets, smartphones y accesorios'),
    ('Impresion',      'Impresoras, tintas y escaneres'),
    ('Software',       'Licencias y suscripciones digitales');

-- 
--  TABLA: productos  (100 registros)
-- 
CREATE TABLE productos (
    id           SERIAL PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL,
    precio       NUMERIC(10,2) NOT NULL,
    stock        INTEGER DEFAULT 0,
    categoria_id INTEGER REFERENCES categorias(id),
    activo       BOOLEAN DEFAULT TRUE,
    creado_en    TIMESTAMP DEFAULT NOW()
);

INSERT INTO productos (nombre, precio, stock, categoria_id) VALUES
-- Computacion (1)
('Laptop Pro 15 Core i7',          1299.99,  45, 1),
('Laptop Slim 13 Core i5',          849.99,  60, 1),
('Desktop Gaming RTX 4070',        2199.99,  20, 1),
('Mini PC Intel N100',              299.99,  80, 1),
('Workstation Xeon W5',            3499.99,   8, 1),
('Laptop 2-en-1 Ryzen 5',          749.99,  35, 1),
('Chromebook 14',                   329.99,  50, 1),
('NUC Intel Core Ultra 7',         699.99,  25, 1),
('Laptop Gamer RTX 4060',         1599.99,  18, 1),
('MacBook Pro M3 14in',           1999.99,  12, 1),
-- Monitores (2)
('Monitor 4K 27in IPS',            549.00,  30, 2),
('Monitor Curvo 32in 165Hz',       429.99,  40, 2),
('Monitor 24in Full HD 144Hz',     249.99,  75, 2),
('Monitor Ultrawide 34in QHD',     699.99,  22, 2),
('Monitor Portatil 15.6in USB-C',  259.99,  55, 2),
('Monitor 27in 4K OLED',           899.99,  15, 2),
('Monitor Gaming 24in 240Hz',      349.99,  35, 2),
('Monitor 32in 4K VA',             479.99,  28, 2),
('Monitor 4K 43in TV y PC',        649.99,  18, 2),
('Monitor 27in 2K IPS 75Hz',       299.99,  60, 2),
-- Perifericos (3)
('Teclado Mecanico RGB Cherry MX',  89.99, 120, 3),
('Mouse Inalambrico Ergonomico',    39.99, 200, 3),
('Mouse Gaming 25600 DPI',          69.99, 150, 3),
('Teclado Compacto Bluetooth',      49.99, 180, 3),
('Mouse Pad XL RGB',                29.99, 300, 3),
('Teclado Gaming TKL',              79.99, 100, 3),
('Mouse Vertical Ergonomico',       44.99, 130, 3),
('Combo Teclado y Mouse Inalambrico',59.99,160, 3),
('Teclado Mecanico Gateron Red',    74.99, 110, 3),
('Trackpad Externo Bluetooth',      69.99,  80, 3),
-- Audio y Video (4)
('Auriculares BT Noise Cancelling',129.99,  75, 4),
('Webcam FHD 1080p 60fps',          69.99,  60, 4),
('Webcam 4K con Microfono',        119.99,  40, 4),
('Auriculares Gaming 7.1 Surround', 89.99,  90, 4),
('Microfono USB Condensador',       79.99,  55, 4),
('Barra de Sonido 2.1 Bluetooth',  149.99,  35, 4),
('Auriculares In-Ear TWS',          49.99, 200, 4),
('Capturadora HDMI 4K',            129.99,  30, 4),
('Luz de Aro LED para Streaming',   39.99, 120, 4),
('Altavoz Bluetooth Portatil',      59.99, 100, 4),
-- Almacenamiento (5)
('SSD NVMe 1TB Gen4',              109.99,  80, 5),
('SSD NVMe 2TB Gen4',              189.99,  50, 5),
('SSD SATA 500GB',                  54.99, 120, 5),
('HDD Externo 4TB USB 3.2',         99.99,  65, 5),
('HDD Externo 1TB USB-C',           59.99,  90, 5),
('USB Flash Drive 256GB',           24.99, 200, 5),
('Tarjeta SD UHS-II 256GB',         44.99, 150, 5),
('SSD NVMe 512GB',                  69.99,  95, 5),
('Enclosure SSD M.2 USB-C',         29.99, 130, 5),
('NAS 2 Bahias Synology',          299.99,  20, 5),
-- Redes (6)
('Router WiFi 6 AX3000',           129.99,  45, 6),
('Router WiFi 6E AX6600',          249.99,  28, 6),
('Switch Giga 8 puertos',           49.99,  70, 6),
('Switch Giga 24 puertos',         199.99,  15, 6),
('Adaptador WiFi USB 6',            29.99, 120, 6),
('Cable Cat6 10m',                   9.99, 500, 6),
('Powerline AV1000 Kit 2 uds',      79.99,  40, 6),
('Access Point WiFi 6',            149.99,  32, 6),
('Mesh WiFi 6 Pack 3 nodos',       299.99,  22, 6),
('NIC PCIe 2.5GbE',                 39.99,  60, 6),
-- Energia (7)
('UPS 1000VA 600W Online',         189.99,  25, 7),
('UPS 650VA 360W',                  89.99,  50, 7),
('Regleta 6 tomas USB',             24.99, 200, 7),
('Fuente de Poder 750W 80+ Gold',  109.99,  40, 7),
('Fuente de Poder 1000W 80+ Plat', 189.99,  20, 7),
('Cargador GaN 100W USB-C',         49.99, 150, 7),
('Cargador Inalambrico 15W Qi',     19.99, 300, 7),
('SAI 2000VA Rack 2U',             399.99,  10, 7),
('Fuente de Poder 550W 80+ Bron',   74.99,  55, 7),
('Hub Cargador USB 10 puertos',     39.99, 100, 7),
-- Moviles (8)
('Tablet 10in Android 13 4GB RAM', 199.99,  40, 8),
('Tablet 12in AMOLED 8GB RAM',     399.99,  25, 8),
('iPad 10ma Gen 64GB',             449.99,  30, 8),
('Soporte Articulado para Tablet',  24.99, 150, 8),
('Funda Teclado para Tablet 10in',  39.99, 100, 8),
('Stylus Activo Universal',         29.99, 200, 8),
('Power Bank 20000mAh 65W',         49.99, 120, 8),
('Holder Magnetico para Auto',      14.99, 300, 8),
('Adaptador USB-C a HDMI 4K',       19.99, 250, 8),
('Cable USB-C a USB-C 2m 100W',      9.99, 400, 8),
-- Impresion (9)
('Impresora Laser Monocromatica',  299.99,  18, 9),
('Impresora Laser Color',          499.99,  12, 9),
('Impresora de Tinta Multifuncion',149.99,  35, 9),
('Escaner Plano A4 1200DPI',       129.99,  22, 9),
('Impresora Fotografica 6in',       99.99,  30, 9),
('Cartucho Negro Pack x4',          29.99, 200, 9),
('Toner Negro Compatible',          49.99, 130, 9),
('Papel Fotografico A4 100 hojas',  14.99, 300, 9),
('Impresora Termica Etiquetas',    179.99,  25, 9),
('Guillotina Papel A4',             34.99,  60, 9),
-- Software (10)
('Licencia Windows 11 Pro',        199.99, 999, 10),
('Licencia Office 365 Personal',    69.99, 999, 10),
('Antivirus Total Security 3 PCs',  39.99, 999, 10),
('VPN Premium Anual',               49.99, 999, 10),
('Adobe CC Plan Mensual',           54.99, 999, 10),
('Licencia AutoCAD 1 anio',        229.99, 999, 10),
('Backup Cloud 2TB Anual',          99.99, 999, 10),
('Password Manager Premium',        35.99, 999, 10),
('Licencia Windows Server 2025',   499.99, 999, 10),
('Suite Ofimatica con Soporte',     29.99, 999, 10);

-- 
--  TABLA: clientes  (50 registros)
-- 
CREATE TABLE clientes (
    id        SERIAL PRIMARY KEY,
    nombre    VARCHAR(80)  NOT NULL,
    email     VARCHAR(120) NOT NULL UNIQUE,
    ciudad    VARCHAR(60),
    activo    BOOLEAN DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT NOW()
);

INSERT INTO clientes (nombre, email, ciudad) VALUES
('Ana Garcia',        'ana.garcia@email.com',       'Caracas'),
('Carlos Martinez',   'carlos.martinez@email.com',  'Maracaibo'),
('Luisa Hernandez',   'luisa.hernandez@email.com',  'Valencia'),
('Juan Rodriguez',    'juan.rodriguez@email.com',   'Barquisimeto'),
('Maria Lopez',       'maria.lopez@email.com',      'Merida'),
('Pedro Gonzalez',    'pedro.gonzalez@email.com',   'Maracay'),
('Sofia Torres',      'sofia.torres@email.com',     'Caracas'),
('Diego Ramirez',     'diego.ramirez@email.com',    'San Cristobal'),
('Valentina Flores',  'valentina.flores@email.com', 'Caracas'),
('Andres Castillo',   'andres.castillo@email.com',  'Maracaibo'),
('Isabella Moreno',   'isabella.moreno@email.com',  'Valencia'),
('Sebastian Jimenez', 'sebastian.jimenez@email.com','Barquisimeto'),
('Camila Ruiz',       'camila.ruiz@email.com',      'Merida'),
('Emilio Vargas',     'emilio.vargas@email.com',    'Caracas'),
('Gabriela Medina',   'gabriela.medina@email.com',  'Maracaibo'),
('Rafael Suarez',     'rafael.suarez@email.com',    'Valencia'),
('Natalia Castro',    'natalia.castro@email.com',   'Maracay'),
('Lorenzo Ortega',    'lorenzo.ortega@email.com',   'Caracas'),
('Paola Sanchez',     'paola.sanchez@email.com',    'Barquisimeto'),
('Miguel Pena',       'miguel.pena@email.com',      'Maracaibo'),
('Laura Diaz',        'laura.diaz@email.com',       'Merida'),
('Felipe Aguilar',    'felipe.aguilar@email.com',   'Caracas'),
('Mariana Vega',      'mariana.vega@email.com',     'Valencia'),
('Tomas Reyes',       'tomas.reyes@email.com',      'Maracaibo'),
('Daniela Cruz',      'daniela.cruz@email.com',     'Caracas'),
('Javier Rios',       'javier.rios@email.com',      'San Cristobal'),
('Patricia Mendoza',  'patricia.mendoza@email.com', 'Barquisimeto'),
('Alejandro Nunez',   'alejandro.nunez@email.com',  'Caracas'),
('Claudia Romero',    'claudia.romero@email.com',   'Maracaibo'),
('Ricardo Herrera',   'ricardo.herrera@email.com',  'Valencia'),
('Adriana Molina',    'adriana.molina@email.com',   'Merida'),
('Francisco Silva',   'francisco.silva@email.com',  'Caracas'),
('Elena Guerrero',    'elena.guerrero@email.com',   'Maracay'),
('Santiago Campos',   'santiago.campos@email.com',  'Barquisimeto'),
('Rosa Cortes',       'rosa.cortes@email.com',      'Caracas'),
('Hector Delgado',    'hector.delgado@email.com',   'Maracaibo'),
('Alicia Gutierrez',  'alicia.gutierrez@email.com', 'Valencia'),
('Manuel Serrano',    'manuel.serrano@email.com',   'Caracas'),
('Beatriz Navarro',   'beatriz.navarro@email.com',  'San Cristobal'),
('Nicolas Ibanez',    'nicolas.ibanez@email.com',   'Barquisimeto'),
('Teresa Blanco',     'teresa.blanco@email.com',    'Caracas'),
('Armando Crespo',    'armando.crespo@email.com',   'Maracaibo'),
('Veronica Santana',  'veronica.santana@email.com', 'Valencia'),
('David Espinoza',    'david.espinoza@email.com',   'Merida'),
('Carmen Padilla',    'carmen.padilla@email.com',   'Maracay'),
('Alberto Fuentes',   'alberto.fuentes@email.com',  'Caracas'),
('Rebeca Pedraza',    'rebeca.pedraza@email.com',   'Barquisimeto'),
('Gustavo Lozano',    'gustavo.lozano@email.com',   'Maracaibo'),
('Susana Mora',       'susana.mora@email.com',       'Caracas'),
('Eduardo Parra',     'eduardo.parra@email.com',    'Valencia');

-- 
--  TABLA: pedidos  (300 registros via generate_series)
-- 
CREATE TABLE pedidos (
    id          SERIAL PRIMARY KEY,
    cliente_id  INTEGER REFERENCES clientes(id),
    producto_id INTEGER REFERENCES productos(id),
    cantidad    INTEGER DEFAULT 1,
    total       NUMERIC(10,2),
    estado      VARCHAR(20) DEFAULT 'pendiente',
    creado_en   TIMESTAMP DEFAULT NOW()
);

INSERT INTO pedidos (cliente_id, producto_id, cantidad, total, estado, creado_en)
SELECT
    (floor(random() * 50) + 1)::INTEGER,
    (floor(random() * 100) + 1)::INTEGER,
    (floor(random() * 5) + 1)::INTEGER,
    round((random() * 2000 + 10)::NUMERIC, 2),
    CASE floor(random() * 4)::INTEGER
        WHEN 0 THEN 'pendiente'
        WHEN 1 THEN 'enviado'
        WHEN 2 THEN 'entregado'
        ELSE       'cancelado'
    END,
    NOW() - (floor(random() * 365)::INTEGER * INTERVAL '1 day')
FROM generate_series(1, 300);

-- 
--  VERIFICACION FINAL
-- 
SELECT tabla, registros FROM (
    SELECT 'categorias' AS tabla, COUNT(*) AS registros FROM categorias
    UNION ALL
    SELECT 'productos',  COUNT(*) FROM productos
    UNION ALL
    SELECT 'clientes',   COUNT(*) FROM clientes
    UNION ALL
    SELECT 'pedidos',    COUNT(*) FROM pedidos
) t
ORDER BY tabla;
