; INGENIERÍA DEL CONOCIMIENTO - PRÁCTICA 1: AGRICOLA
; Autoras: Alba María García García & Irene Martínez Castillo, Grupo 83

; RESTRICCIONES PROPIAS (NO INDICADAS EN EL ENUNCIADO):
;- No hay ortogonalidad. Nos da igual dónde se encuentren los elementos de la granja.
;- En la ejecución de acciones conjuntas, no comprobamos si se puede realizar la segunda.
;- Las acciones con una cláusula y/o siempre se tratarán como acciones independientes, por
; lo que nunca se harán de manera conjunta.
;- Las cartas de adquisición mayor sólo se pueden jugar cuando se obtengan animales y no se puedan guardar en la granja,
; tras ejecutar la acción de "hornear pan" y en la fase de alimentación en caso de no tener la comida necesaria.
;- Si un jugador no puede intercambiar un animal por comida al quedarse sin opciones para colocarlo en su granja,
; ese animal se queda en el tablero.

; BASE DE HECHOS
(defclass JUGADOR (is-a USER)
  (slot id
    (type INTEGER)
    (default 0))
  (slot turno
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE))
  (slot jugador-inicial
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE))
  (slot puntuacion
      (type INTEGER)
      (default 0)))

(defclass TERRENO (is-a USER)
  (slot jugador
    (type INTEGER)
    (default 1))
  (slot id
    (type INTEGER)
    (default 0))
  (slot n-celdas
    (type INTEGER)
    (default 1)))

; Subclases de TERRENO
(defclass TERRENO-VACIO (is-a TERRENO))

(defclass HABITACION (is-a TERRENO)
  (slot material
    (type SYMBOL)
    (allowed-values MADERA ADOBE PIEDRA)
    (default MADERA))
  (slot animal
    (type SYMBOL)
    (allowed-values NULL OVEJA VACA JABALI)
    (default NULL)))

(defclass CAMPO (is-a TERRENO)
  (slot vegetal
    (type SYMBOL)
    (allowed-values NULL CEREAL HORTALIZA)
    (default NULL))
  (slot cantidad
    (type INTEGER)
    (default 0))
  (slot recolectado
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

(defclass PASTO (is-a TERRENO)
  (slot id-extra
    (type INTEGER)
    (default 0))
  (slot vallas
    (type INTEGER)
    (default 0))
  (slot establos
    (type INTEGER)
    (default 0))
  (slot animal
    (type SYMBOL)
    (allowed-values NULL OVEJA VACA JABALI)
    (default NULL))
  (slot cantidad
    (type INTEGER)
    (default 0)))

(defclass CONTADOR (is-a USER)
  (slot jugador
    (type INTEGER)
    (default 1))
  (slot recurso
    (type SYMBOL)
    (allowed-values TERRENO-VACIO HABITACION CAMPO PASTO ESTABLO VALLA PERSONA CARTA-DE-MENDICIDAD))
  (slot cantidad
    (type INTEGER)
    (default 0))
  (slot puntuado
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

; Subclases de CONTADOR
(defclass CONTADOR-TERRENOS-VACIOS (is-a CONTADOR))

(defclass CONTADOR-HABITACIONES (is-a CONTADOR))

(defclass CONTADOR-CAMPOS (is-a CONTADOR))

(defclass CONTADOR-PASTOS (is-a CONTADOR))

(defclass CONTADOR-VALLAS (is-a CONTADOR))

(defclass CONTADOR-ESTABLOS (is-a CONTADOR))

(defclass CONTADOR-PERSONAS (is-a CONTADOR))

(defclass CONTADOR-CARTAS-DE-MENDICIDAD (is-a CONTADOR))

(defclass CARTA (is-a USER)
  (slot nombre
    (type SYMBOL))
  (slot jugador ; 0: Ningún jugador / 1: Jugador 1 / 2: Jugador 2 / 3: Ambos
    (type INTEGER)
    (default 0)))

; Subclases de CARTA
(defclass CARTA-DE-TABLERO (is-a CARTA))

(defclass CARTA-DE-RONDA (is-a CARTA)
  (slot periodo
    (type INTEGER)
    (default 0)))

(defclass CARTA-DE-MENDICIDAD (is-a CARTA))

(defclass ADQUISICION-MAYOR (is-a CARTA)
  (slot hornear-pan
    (type INTEGER)
    (default 0))
  (slot hogar
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE))
  (slot puntos
    (type INTEGER)
    (default 0))
  (slot recurso-pts-extra
    (type SYMBOL)
    (allowed-values NULL MADERA ADOBE JUNCO)
    (default NULL))
  (slot cantidad-pts-extra1
    (type INTEGER)
    (default 0))
  (slot cantidad-pts-extra2
    (type INTEGER)
    (default 0))
  (slot cantidad-pts-extra3
    (type INTEGER)
    (default 0))
  (slot puntuada
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

; Subclases de ADQUISICION-MAYOR
(defclass ADQ-MAYOR-1-RECURSO (is-a ADQUISICION-MAYOR)
  (slot recurso
    (type SYMBOL)
    (allowed-values HOGAR MADERA ADOBE MADERA PIEDRA JUNCO))
   (slot cantidad
     (type INTEGER)
     (default 0)))

(defclass ADQ-MAYOR-2-RECURSOS (is-a ADQUISICION-MAYOR)
  (slot recurso1
    (type SYMBOL)
    (allowed-values HOGAR MADERA ADOBE MADERA PIEDRA JUNCO))
  (slot cantidad1
    (type INTEGER)
    (default 0))
  (slot recurso2
    (type SYMBOL)
    (allowed-values HOGAR MADERA ADOBE MADERA PIEDRA JUNCO))
  (slot cantidad2
    (type INTEGER)
    (default 0)))

(defclass ACCION (is-a USER)
  (slot nombre
    (type SYMBOL))
  (slot id
    (type INTEGER)
    (default 1))
  (slot conjunta
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE))
  (slot ocupada
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

; Subclases de ACCION
(defclass INICIO-DE-RONDA (is-a ACCION)
  (slot contador
    (type INTEGER)
    (default 5))
  (slot recogido
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

(defclass RECOGER-RECURSO (is-a ACCION)
  (slot recurso
    (type SYMBOL)
    (allowed-values MADERA ADOBE PIEDRA JUNCO CEREAL HORTALIZA OVEJA VACA JABALI COMIDA))
  (slot total ; Cantidad total
    (type INTEGER)
    (default 0))
  (slot ronda ; Lo que añadimos por ronda
    (type INTEGER)
    (default 0))
  (slot repuesto
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

(defclass INTERCAMBIAR-RECURSOS (is-a ACCION)
  (slot recurso
    (type SYMBOL)
    (allowed-values MADERA ADOBE PIEDRA JUNCO CEREAL HORTALIZA OVEJA VACA JABALI COMIDA))
  (slot cantidad
    (type INTEGER)
    (default 0))
  (slot comida
    (type INTEGER)
    (default 0))
  (slot hornear-pan
    (type INTEGER)
    (default 0)))

(defclass HORNEAR-PAN (is-a ACCION)
  (slot carta
    (type INTEGER)
    (default 0)))

(defclass ARAR-CAMPO (is-a ACCION)
  (slot id-terreno
    (type INTEGER)
    (default 0)))

(defclass SEMBRAR (is-a ACCION)
  (slot vegetal
    (type SYMBOL)
    (allowed-values CEREAL HORTALIZA))
  (slot id-terreno
    (type INTEGER)
    (default 0)))

(defclass CONSTRUIR-VALLA (is-a ACCION)
  (slot id-terreno
    (type INTEGER)
    (default 0)))

(defclass CONSTRUIR-ESTABLO (is-a ACCION)
  (slot id-terreno
    (type INTEGER)
    (default 0)))

(defclass CONSTRUIR-HABITACION (is-a ACCION)
  (slot id-terreno
    (type INTEGER)
    (default 0)))

(defclass REFORMAR (is-a ACCION))

(defclass AMPLIAR-FAMILIA (is-a ACCION)
  (slot habitacion
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

(defclass OBTENER-ADQUISICION-MAYOR (is-a ACCION)
  (slot carta
    (type SYMBOL)))

(defclass CAMBIAR-JUGADOR-INICIAL (is-a ACCION))

(defclass PERSONA (is-a USER)
  (slot jugador
    (type INTEGER)
    (default 1))
  (slot id
    (type INTEGER)
    (default 1))
  (slot recien-nacido
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE))
  (slot accion
    (type SYMBOL)
    (default NULL))
  (slot accion-id
    (type INTEGER)
    (default 0))
  (slot alimentado
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

(defclass RONDA (is-a USER)
  (slot numero
    (type INTEGER)
    (default 1))
  (slot periodo
    (type INTEGER)
    (default 1)))

(defclass FASE (is-a USER)
  (slot valor
    (type SYMBOL)
    (allowed-values PRINCIPIO-DE-RONDA REPOSICION JORNADA-LABORAL REGRESO-AL-HOGAR RECOLECCION ALIMENTACION PROCREACION CALCULO-PUNTUACION)
    (default PRINCIPIO-DE-RONDA)))

(defclass COSECHA (is-a USER)
  (slot ronda
    (type INTEGER)))

(defclass RECURSO (is-a USER)
  (slot jugador
    (type INTEGER))
  (slot cantidad
    (type INTEGER)
    (default 0))
  (slot tipo
    (type SYMBOL)
    (allowed-values ANIMAL VEGETAL MATERIAL COMIDA))
  (slot crias
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE))
  (slot puntuado
    (type SYMBOL)
    (allowed-values TRUE FALSE)
    (default FALSE)))

; Subclases de RECURSO
(defclass MADERA (is-a RECURSO))
(defclass ADOBE (is-a RECURSO))
(defclass PIEDRA (is-a RECURSO))
(defclass JUNCO (is-a RECURSO))
(defclass CEREAL (is-a RECURSO))
(defclass HORTALIZA (is-a RECURSO))
(defclass OVEJA (is-a RECURSO))
(defclass VACA (is-a RECURSO))
(defclass JABALI (is-a RECURSO))
(defclass COMIDA (is-a RECURSO))

(defclass TABLA-PUNTUACION (is-a USER)
  (slot recurso
    (type SYMBOL)
    (allowed-values MADERA ADOBE PIEDRA JUNCO CEREAL HORTALIZA OVEJA VACA JABALI COMIDA CAMPO PASTO TERRENO-VACIO ESTABLO PERSONA CARTA-DE-MENDICIDAD))
  (slot cantidad
    (type INTEGER)
    (default 0))
  (slot puntos
    (type INTEGER)
    (default 0)))
