CREATE TABLE usuario (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    nombre  VARCHAR(100) NOT NULL,
    email   VARCHAR(150) NOT NULL,
    perfil  VARCHAR(20)  NOT NULL,
    activo  BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('TECNICO', 'RESPONSABLE'))
);

CREATE TABLE ubicacion (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_ubicacion_nombre UNIQUE (nombre)
);

CREATE TABLE equipo (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    codigo       VARCHAR(20)  NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    fabricante   VARCHAR(100),
    modelo       VARCHAR(100),
    numero_serie VARCHAR(100),
    ubicacion_id BIGINT       NOT NULL,
    estado       VARCHAR(20)  NOT NULL DEFAULT 'OPERATIVO',
    fecha_alta   DATE         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_equipo_codigo UNIQUE (codigo),
    CONSTRAINT fk_equipo_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicacion (id),
    CONSTRAINT ck_equipo_estado CHECK (estado IN ('OPERATIVO', 'AVERIADO', 'BAJA'))
);

CREATE TABLE incidencia (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    equipo_id        BIGINT       NOT NULL,
    reportada_por_id BIGINT       NOT NULL,
    titulo           VARCHAR(150) NOT NULL,
    descripcion      TEXT         NOT NULL,
    prioridad        VARCHAR(20)  NOT NULL,
    estado           VARCHAR(20)  NOT NULL,
    fecha_apertura   DATETIME(6)  NOT NULL,
    fecha_cierre     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_incidencia_equipo FOREIGN KEY (equipo_id) REFERENCES equipo (id),
    CONSTRAINT fk_incidencia_reportada_por FOREIGN KEY (reportada_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_incidencia_prioridad CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_incidencia_estado CHECK (estado IN ('ABIERTA', 'EN_CURSO', 'CERRADA', 'DESCARTADA'))
);

CREATE TABLE orden_trabajo (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    incidencia_id  BIGINT      NOT NULL,
    creada_por_id  BIGINT      NOT NULL,
    tecnico_id     BIGINT,
    descripcion    TEXT        NOT NULL,
    resolucion     TEXT,
    estado         VARCHAR(20) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_inicio   DATETIME(6),
    fecha_fin      DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_orden_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencia (id),
    CONSTRAINT fk_orden_creada_por FOREIGN KEY (creada_por_id) REFERENCES usuario (id),
    CONSTRAINT fk_orden_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuario (id),
    CONSTRAINT ck_orden_estado CHECK (estado IN ('PENDIENTE', 'ASIGNADA', 'EN_CURSO', 'RESUELTA', 'CERRADA', 'CANCELADA'))
);

CREATE TABLE historial (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tipo_entidad   VARCHAR(20)  NOT NULL,
    entidad_id     BIGINT       NOT NULL,
    accion         VARCHAR(30)  NOT NULL,
    valor_anterior VARCHAR(50),
    valor_nuevo    VARCHAR(50)  NOT NULL,
    origen_id      BIGINT,
    usuario_id     BIGINT       NOT NULL,
    fecha          DATETIME(6)  NOT NULL,
    comentario     TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_origen FOREIGN KEY (origen_id) REFERENCES historial (id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    INDEX ix_historial_entidad (tipo_entidad, entidad_id, fecha)
);
