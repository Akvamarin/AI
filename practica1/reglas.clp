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

; BASE DE REGLAS
; Establecemos la estrategia a RANDOM.
(defrule set-game
  (declare (salience 1000))
    (not (strategy))
  =>
    (set-strategy random)
    (dribble-on salida-prueba-X.txt)
    (assert (strategy))
    (printout t "Estrategia cambiada a random." crlf)
    (printout t "Fase: PRINCIPIO DE RONDA." crlf)
    (printout t "Periodo: 1 Ronda: 1."crlf))

;La última ronda solo contará de la fase CALCULO-PUNTUACION.
(defrule ultima-ronda
  (declare (salience 500))
    ?f <- (object (is-a FASE) (valor PRINCIPIO-DE-RONDA))
    (object (is-a RONDA) (numero 15))
  =>
    (printout t "Fase: CÁLCULO DE LA PUNTUACIÓN." crlf)
    (modify-instance ?f (valor CALCULO-PUNTUACION)))

; Reglas PRINCIPIO-DE-RONDA
; Otorgamos el turno al jugador inicial si no lo tiene al princio de ronda.
(defrule cambiar-turno-jugador-inicial
  (declare (salience 100))
    (object (is-a FASE) (valor PRINCIPIO-DE-RONDA))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador1) (jugador-inicial TRUE))
    ?j2 <- (object (is-a JUGADOR) (id ?jugador2) (jugador-inicial FALSE))
    (not (cambiado-turno-jugador-inicial ?numero))
  =>
    (modify-instance ?j1 (turno TRUE))
    (modify-instance ?j2 (turno FALSE))
    (assert (cambiado-turno-jugador-inicial ?numero))
    (printout t "El jugador " ?jugador1 " comienza la ronda por ser el jugador inicial." crlf))

; Obtenemos una nueva carta de ronda jugable.
(defrule obtener-carta-ronda
  (declare (salience 100))
    (object (is-a FASE) (valor PRINCIPIO-DE-RONDA))
    (object (is-a RONDA) (periodo ?periodo) (numero ?numero))
    ?c <- (object (is-a CARTA-DE-RONDA) (nombre ?nombre) (periodo ?periodo) (jugador 0))
    (not (carta-de-ronda-jugada ?numero))
  =>
    (modify-instance ?c (jugador 3))
    (assert (carta-de-ronda-jugada ?numero))
    (printout t "Nueva carta de ronda disponible: " ?nombre "." crlf))

; Ejecutamos las acciones de inicio de ronda (pozo).
(defrule accion-inicio-ronda
  (declare (salience 100))
    (object (is-a FASE) (valor PRINCIPIO-DE-RONDA))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a INICIO-DE-RONDA) (nombre ?nombre) (contador ?contador) (recogido FALSE))
    (object (is-a CARTA) (nombre ?nombre) (jugador ?jugador))
    ?r <- (object (is-a COMIDA) (cantidad ?cantidad) (jugador ?jugador))
    (test (> ?contador 0))
 =>
    (modify-instance ?r (cantidad (+ ?cantidad 1)))
    (modify-instance ?a (contador (- ?contador 1)) (recogido TRUE))
    (printout t "El jugador " ?jugador " obtiene 1 unidad de comida usando la carta " ?nombre ". Comida total: " (+ ?cantidad 1) "." crlf))

; Convertimos a todos los bebés en adultos después de alimentarlos.
; Debemos hacerlo aquí para no interferir en la fase de alimentación (si la hubiera).
(defrule resetear-persona-recien-nacido
  (declare (salience 2))
    (object (is-a FASE) (valor PRINCIPIO-DE-RONDA))
    ?p <- (object (is-a PERSONA) (recien-nacido TRUE))
  =>
    (modify-instance ?p (recien-nacido FALSE)))

; Cambiamos de fase.
(defrule cambiar-fase-inicio-ronda
  (declare (salience 1))
    ?f <- (object (is-a FASE) (valor PRINCIPIO-DE-RONDA))
=>
    (printout t "Fase: REPOSICION." crlf)
    (modify-instance ?f (valor REPOSICION)))

; Reglas REPOSICION
; Reponemos las casillas del tablero donde se almacenen recursos.
(defrule reponer-recursos
  (declare (salience 100))
    (object (is-a FASE) (valor REPOSICION))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (total ?total) (ronda ?ronda) (repuesto FALSE))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
 =>
    (modify-instance ?a (repuesto TRUE) (total (+ ?total ?ronda)))
    (printout t "Repuesto el número de unidades del recurso de la carta " ?nombre ". Cantidad total: " (+ ?total ?ronda) "." crlf))

; Cambiamos de fase.
(defrule cambiar-fase-reposicion
  (declare (salience 1))
    ?f <- (object (is-a FASE) (valor REPOSICION))
=>
    (printout t "Fase: JORNADA LABORAL." crlf)
    (modify-instance ?f (valor JORNADA-LABORAL)))

; Reglas JORNADA-LABORAL

; Reglas OBTENER-RECURSO
; Caso 1: Obtener recurso si no es un animal (caso más sencillo).
(defrule obtener-recurso-no-animal
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (id ?id) (ocupada FALSE) (recurso ?recurso) (total ?total))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad) (tipo ~ANIMAL))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (+ ?cantidad ?total)))
    (modify-instance ?a (ocupada TRUE) (total 0))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (printout t "El jugador " ?jugador " obtiene " ?total " " ?recurso " usando la carta " ?nombre ". Cantidad " ?recurso ": " (+ ?cantidad ?total) "." crlf))

; Caso 2: Obtener recurso si es un animal.
; Guardamos al animal en una habitación dispobible.
(defrule obtener-recurso-animal-colocar-en-habitacion
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (id ?id) (ocupada FALSE) (recurso ?recurso) (total ?total))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad) (tipo ANIMAL))
    ?t <- (object (is-a HABITACION) (jugador ?jugador) (id ?id-terreno) (animal NULL))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (+ ?cantidad 1)))
    (modify-instance ?t (animal ?recurso))
    (modify-instance ?a (ocupada TRUE) (total (- ?total 1)))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (colocando-animales-en-granja ?jugador ?nombre ?numero))
    (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre ". El animal queda colocado en la habitación del terreno " ?id-terreno ". Cantidad " ?recurso ": " (+ ?cantidad 1) "." crlf))

; Guardamos el animal en un pasto vacío (los pastos vacíos no tienen tipo de animal asignado)
(defrule obtener-recurso-animal-colocar-en-pasto-vacio
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (id ?id) (ocupada FALSE) (recurso ?recurso) (total ?total))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad1) (tipo ANIMAL))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (n-celdas ?celdas) (vallas ?vallas) (establos ?establos) (animal NULL) (cantidad ?cantidad2))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion ?accion) (accion-id 0))
    (test (= ?vallas (+ 2 (* 2 ?celdas))))
    (test (< ?cantidad2 (+ (* 2 ?celdas) (* 2 ?establos))))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (+ ?cantidad1 1)))
    (modify-instance ?t (cantidad (+ ?cantidad2 1)) (animal ?recurso))
    (modify-instance ?a (ocupada TRUE) (total (- ?total 1)))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (colocando-animales-en-granja ?jugador ?nombre ?numero))
    (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre ". El animal queda colocado en el pasto vacío del terreno " ?id-terreno ". Cantidad " ?recurso ": " (+ ?cantidad1 1) "." crlf))

; Guardamos el animal en un pasto no vacío, que contenga algún animal de su mismo tipo
(defrule obtener-recurso-animal-colocar-en-pasto-no-vacio
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (id ?id) (ocupada FALSE) (recurso ?recurso) (total ?total))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad1) (tipo ANIMAL))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (n-celdas ?celdas) (vallas ?vallas) (establos ?establos) (animal ?recurso) (cantidad ?cantidad2))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (test (= ?vallas (+ 2 (* 2 ?celdas))))
    (test (< ?cantidad2 (+ (* 2 ?celdas) (* 2 ?establos))))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (+ ?cantidad1 1)))
    (modify-instance ?t (cantidad (+ ?cantidad2 1)))
    (modify-instance ?a (ocupada TRUE) (total (- ?total 1)))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (colocando-animales-en-granja ?jugador ?nombre ?numero))
    (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre ". El animal queda colocado en el pasto no vacío del terreno " ?id-terreno "." crlf)
    (printout t "Animales en el pasto: " (+ ?cantidad2 1) ". Cantidad " ?recurso ": " (+ ?cantidad1 1) "." crlf))

; ESTRATEGIA: Sólo intercambiamos el animal obtenido por comida cuando no disponemos de habitaciones ni pastos
(defrule obtener-recurso-animal-cambiar-por-comida
  (declare (salience 90))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre1) (id ?id) (ocupada FALSE) (recurso ?recurso) (total ?total))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    (object (is-a ?recurso) (tipo ANIMAL))
    (object (is-a INTERCAMBIAR-RECURSOS) (nombre ?nombre2) (recurso ?recurso) (comida ?comida))
    (object (is-a CARTA) (nombre ?nombre2) (jugador ?jugador))
    ?r <- (object (is-a COMIDA) (cantidad ?cantidad))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion ?accion) (accion-id 0))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (+ ?cantidad ?comida)))
    (modify-instance ?a (ocupada TRUE) (total (- ?total 1)))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (colocando-animales-en-granja ?jugador ?nombre ?numero))
    (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre ", pero no puede guardarlo ni en una habitación ni en un pasto." crlf)
    (printout t "Se intercambia el animal por " ?comida " unidades de comida, usando la carta" ?nombre2 ". Cantidad " ?recurso ": " (+ ?cantidad 1) "." crlf))

; Reglas recursivas.
; Cada regla guarda el animal en un terreno o lo convierte en comida, por lo que debemos ejecutar
; estas reglas recursivamente hasta que no queden más animales.
; Se aplica la misma estrategia que antes: primero intentamos colocar al animal en algún terreno y
; sólo si no se puede lo convertimos en comida.
(defrule obtener-recurso-animal-colocar-en-habitacion-rec
    (declare (salience 200))
      (object (is-a FASE) (valor JORNADA-LABORAL))
      (object (is-a RONDA) (numero ?numero))
      (object (is-a JUGADOR) (id ?jugador))
      ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (recurso ?recurso) (total ?total))
      ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad))
      ?h <- (object (is-a HABITACION) (jugador ?jugador) (id ?id-terreno) (animal NULL))
      (colocando-animales-en-granja ?jugador ?nombre ?numero)
      (test (> ?total 0))
    =>
      (modify-instance ?r (cantidad (+ ?cantidad 1)))
      (modify-instance ?h (animal ?recurso))
      (modify-instance ?a (total (- ?total 1)))
      (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre ". El animal queda colocado en la habitación del terreno " ?id-terreno ". Cantidad " ?recurso ": " (+ ?cantidad 1) "." crlf))

(defrule obtener-recurso-animal-colocar-en-pasto-vacio-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (recurso ?recurso) (total ?total))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad1))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (n-celdas ?celdas) (vallas ?vallas) (establos ?establos) (animal NULL) (cantidad ?cantidad2))
    (colocando-animales-en-granja ?jugador ?nombre ?numero)
    (test (= ?vallas (+ 2 (* 2 ?celdas)))) ;El pasto debe estar totalmente vallado antes de meter ningún animal
    (test (< ?cantidad2 (+ (* 2 ?celdas) (* 2 ?establos))))
    (test (> ?total 0))
  =>
    (modify-instance ?r (cantidad (+ ?cantidad1 1)))
    (modify-instance ?t (cantidad (+ ?cantidad2 1)) (animal ?recurso))
    (modify-instance ?a (total (- ?total 1)))
    (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre ". El animal queda colocado en el pasto vacío del terreno " ?id-terreno ". Cantidad " ?recurso ": " (+ ?cantidad1 1) "." crlf))

(defrule obtener-recurso-animal-colocar-en-pasto-no-vacio-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre) (recurso ?recurso) (total ?total))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad1))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (n-celdas ?celdas) (vallas ?vallas) (establos ?establos) (animal ?recurso) (cantidad ?cantidad2))
    (colocando-animales-en-granja ?jugador ?nombre ?numero)
    (test (= ?vallas (+ 2 (* 2 ?celdas)))) ;El pasto debe estar totalmente vallado antes de meter ningún animal
    (test (< ?cantidad2 (+ (* 2 ?celdas) (* 2 ?establos))))
    (test (> ?total 0))
  =>
    (modify-instance ?r (cantidad (+ ?cantidad1 1)))
    (modify-instance ?t (cantidad (+ ?cantidad2 1)))
    (modify-instance ?a (total (- ?total 1)))
    (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre ". El animal queda colocado en el pasto no vacío del terreno " ?id-terreno "." crlf)
        (printout t "Animales en el pasto: " (+ ?cantidad2 1) ". Cantidad " ?recurso ": " (+ ?cantidad1 1) "." crlf))

(defrule obtener-recurso-animal-cambiar-por-comida-rec
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a RECOGER-RECURSO) (nombre ?nombre1) (recurso ?recurso) (total ?total))
    (object (is-a INTERCAMBIAR-RECURSOS) (nombre ?nombre2) (recurso ?recurso) (comida ?comida))
    (object (is-a CARTA) (nombre ?nombre2) (jugador ?jugador))
    ?r <- (object (is-a COMIDA) (cantidad ?cantidad))
    (colocando-animales-en-granja ?jugador ?nombre1 ?numero)
    (test (> ?total 0))
 =>
    (modify-instance ?r (cantidad (+ ?cantidad ?comida)))
    (modify-instance ?a (total (- ?total 1)))
    (printout t "El jugador " ?jugador " obtiene 1 " ?recurso " usando la carta " ?nombre1 ", pero no puede guardarlo ni en una habitación ni en un pasto." crlf)
    (printout t "Se intercambia el animal por " ?comida " unidades de comida, usando la carta " ?nombre2 ". Cantidad COMIDA: " (+ ?cantidad ?comida) "." crlf))

; REGLAS HORNEAR-PAN
; Horneamos pan si disponemos de alguna adquisición mayor que lo permita
(defrule hornear-pan
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a HORNEAR-PAN) (nombre ?nombre1) (id ?id) (ocupada FALSE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre1) (jugador 3))
    (object (is-a INTERCAMBIAR-RECURSOS) (nombre ?nombre2) (recurso ?recurso) (cantidad ?cantidad1) (comida ?comida) (hornear-pan ?carta))
    (object (is-a ADQUISICION-MAYOR) (nombre ?nombre2) (jugador ?jugador))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    ?c <- (object (is-a COMIDA) (jugador ?jugador) (cantidad ?cantidad3))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?p (accion ?nombre1) (accion-id ?id))
    (modify-instance ?r (cantidad (- ?cantidad2 ?cantidad1)))
    (modify-instance ?c (cantidad (+ ?cantidad3 ?comida)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (cambiar-carta-entera-ocupada ?nombre1 ?numero))
    (printout t "El jugador " ?jugador " hornea pan a través de la carta " ?nombre1 ". Intercambia " ?cantidad1 " " ?recurso " por " ?comida " unidades de comida, usando la carta " ?nombre2 "." crlf)
    (printout t "Cantidad " ?recurso ": " (- ?cantidad2 ?cantidad1) ". Cantidad COMIDA: " (+ ?cantidad3 ?comida) "." crlf))

; REGLAS ARAR-CAMPO
; Convertimos un terreno vacío en un campo (sin vegetal asignado)
(defrule arar-campo
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a ARAR-CAMPO) (nombre ?nombre) (id ?id) (ocupada FALSE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?c1 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    ?c2 <- (object (is-a CONTADOR-CAMPOS) (jugador ?jugador) (cantidad ?n-campos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (unmake-instance ?t)
    (make-instance of CAMPO (jugador ?jugador) (id ?id-terreno))
    (modify-instance ?c1 (cantidad (- ?n-terrenos 1)))
    (modify-instance ?c2 (cantidad (+ ?n-campos 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " ara un campo del terreno " ?id-terreno " usando la carta " ?nombre ". Total CAMPO: " (+ ?n-campos 1) "." crlf))

; REGLAS SEMBRAR-CAMPO
; Colocamos un vegetal (cereal u hortaliza) en un campo.
; Necesitamos una regla distinta para cada uno porque se colocan distintas unidades del vegetal en el campo.
(defrule sembrar-campo-cereal
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a SEMBRAR) (nombre ?nombre) (id ?id) (ocupada FALSE) (id-terreno ?id-terreno) (vegetal CEREAL))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a CAMPO) (jugador ?jugador) (id ?id-terreno) (vegetal NULL) (cantidad 0))
    ?r <- (object (is-a CEREAL) (jugador ?jugador) (cantidad ?cantidad))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (test (>= ?cantidad 1))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?t (vegetal CEREAL) (cantidad 3))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (sembrando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " siembra un campo del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El campo tiene ahora 3 cereales. Cantidad CEREAL: " (- ?cantidad 1) "." crlf))

(defrule sembrar-campo-hortaliza
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a SEMBRAR) (nombre ?nombre) (id ?id) (ocupada FALSE) (id-terreno ?id-terreno) (vegetal HORTALIZA))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a CAMPO) (jugador ?jugador) (id ?id-terreno) (vegetal NULL) (cantidad 0))
    ?r <- (object (is-a HORTALIZA) (jugador ?jugador) (cantidad ?cantidad))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (test (>= ?cantidad 1))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?t (vegetal HORTALIZA) (cantidad 2))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (sembrando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " siembra un campo del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El campo tiene ahora 2 hortalizas. Cantidad HORTALIZA: " (- ?cantidad 1) "." crlf))

; Reglas recursivas.
; Se puede sembrar más de un campo.
; ESTRATEGIA: Sembramos todos los campos que estén disponibles para el jugador.
(defrule sembrar-campo-cereal-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a SEMBRAR) (nombre ?nombre) (id ?id) (id-terreno ?id-terreno) (vegetal CEREAL))
    ?t <- (object (is-a CAMPO) (jugador ?jugador) (id ?id-terreno) (vegetal NULL) (cantidad 0))
    ?r <- (object (is-a CEREAL) (jugador ?jugador) (cantidad ?cantidad))
    (sembrando ?jugador ?nombre ?numero)
    (test (>= ?cantidad 1))
  =>
    (modify-instance ?t (vegetal CEREAL) (cantidad 3))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (printout t "El jugador " ?jugador " siembra un campo del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El campo tiene ahora 3 cereales. Cantidad CEREAL: " (- ?cantidad 1) "." crlf))

(defrule sembrar-campo-hortaliza-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a SEMBRAR) (nombre ?nombre) (id ?id) (id-terreno ?id-terreno) (vegetal HORTALIZA))
    ?t <- (object (is-a CAMPO) (jugador ?jugador) (id ?id-terreno) (vegetal NULL) (cantidad 0))
    ?r <- (object (is-a HORTALIZA) (jugador ?jugador) (cantidad ?cantidad))
    (sembrando ?jugador ?nombre ?numero)
    (test (>= ?cantidad 1))
  =>
    (modify-instance ?t (vegetal HORTALIZA) (cantidad 2))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (printout t "El jugador " ?jugador " siembra un campo del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El campo tiene ahora 2 hortalizas. Cantidad HORTALIZA: " (- ?cantidad 1) "." crlf))

; A la hora de sembrar, es posible que en la misma ronda se consigan nuevos vegetales o campos
; usando a una persona distinta de la familia (después de haber sembrado). Por tanto, si el estado
; "sembrando" sigue latente, se empezará a sembrar de nuevo. Es por ello que debemos eliminar este estado
; en cuanto la persona asignada haya terminado de realizar la acción (ya sea porque no quedan vegetales
; o porque no quedan campos).

; No quedan más vegetales
(defrule quitar-estado-sembrando-no-vegetal
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a SEMBRAR) (nombre ?nombre) (vegetal ?vegetal))
    (object (is-a ?vegetal) (jugador ?jugador) (cantidad ?cantidad))
    ?a <- (sembrando ?jugador ?nombre ?numero)
    (test (= ?cantidad 0))
  =>
    (retract ?a))

; No quedan más campos
(defrule quitar-estado-sembrando-no-campo
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a SEMBRAR) (nombre ?nombre))
    (not (object (is-a CAMPO) (jugador ?jugador) (vegetal NULL)))
    ?a <- (sembrando ?jugador ?nombre ?numero)
  =>
    (retract ?a))

; Reglas CONSTRUIR-VALLA
; Caso 1: No hay ningún pasto en la granja del jugador (acción no conjunta).
; Creamos un pasto simple (una celda) en el lugar donde se coloque la primera valla del jugador.
(defrule construir-valla-no-conjunta-no-pastos-simple
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (not (object (is-a PASTO) (jugador ?jugador)))
    (test (>= ?cantidad 1))
    (test (< ?n-vallas 15))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (unmake-instance ?t)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto simple del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

; Creamos un pasto doble (una celda) en el lugar donde se coloque la primera valla del jugador.
(defrule construir-valla-no-conjunta-no-pastos-doble
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (id-terreno ?id-terreno1))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t1 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno1))
    ?t2 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno2&~?id-terreno1))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (not (object (is-a PASTO) (jugador ?jugador)))
    (test (>= ?cantidad 1))
    (test (< ?n-vallas 15))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (unmake-instance ?t1)
    (unmake-instance ?t2)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno1) (id-extra ?id-terreno2) (n-celdas 2) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 2)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto doble del terreno (" ?id-terreno1 "," ?id-terreno2 ") usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

; Caso 2: Hay al menos un pasto en la granja del jugador (acción no conjunta).
; ESTRATEGIA: El jugador no puede crear un nuevo pasto colocando la primera valla hasta que no haya vallado todos los que tenga anteriormente.
; Vallamos un pasto ya existente.
(defrule construir-valla-no-conjunta-si-pastos-vallar-pasto-existente
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (vallas ?vallas) (n-celdas ?celdas))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (test (< ?vallas (+ 2 (* 2 ?celdas))))
    (test (< ?n-vallas 15))
    (test (>= ?cantidad 1))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?t (vallas (+ ?vallas 1)))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c (cantidad (+ ?n-vallas 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye una valla en el pasto del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora " (+ ?vallas 1) " vallas. Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

; Vallamos un terreno vacío para convertirlo en un pasto simple
(defrule construir-valla-no-conjunta-si-pastos-vallar-nuevo-pasto-simple
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (object (is-a PASTO) (jugador ?jugador))
    (not (object (is-a PASTO) (jugador ?jugador) (id ~?id-terreno) (n-celdas ?celdas) (vallas ~=(+ 2 (* 2 ?celdas)))))
    (test (< ?n-vallas 15))
    (test (> ?cantidad 0))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (unmake-instance ?t)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto simple del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

; Vallamos un terreno vacío para convertirlo en un pasto doble
(defrule construir-valla-no-conjunta-si-pastos-vallar-nuevo-pasto-doble
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (id-terreno ?id-terreno1))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t1 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno1))
    ?t2 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno2&~?id-terreno1))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (recien-nacido FALSE) (accion NULL) (accion-id 0))
    (object (is-a PASTO) (jugador ?jugador))
    (not (object (is-a PASTO) (jugador ?jugador) (id ~?id-terreno1) (n-celdas ?celdas) (vallas ~=(+ 2 (* 2 ?celdas)))))
    ;No queremos que haya algún pasto que no esté totalmente vallado y ponernos a vallar otro
    (not (test (= ?id-terreno1 ?id-terreno2)))
    (test (< ?n-vallas 15))
    (test (> ?cantidad 0))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (unmake-instance ?t1)
    (unmake-instance ?t2)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno1) (id-extra ?id-terreno2) (n-celdas 2) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 2)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto doble del terreno (" ?id-terreno1 "," ?id-terreno2 ") usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

; Las reglas que veremos ahora son homólogas a las vistas anteriormente, con la única diferencia de que
; comprueban que se trata de una acción conjunta. (Se debe realizar obligatoriamente tras de otra).
(defrule construir-valla-conjunta-no-pastos-simple
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (conjunta TRUE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    (not (object (is-a PASTO) (jugador ?jugador)))
    (conjunta ?jugador ?nombre ?numero)
    (test (>= ?cantidad 1))
    (test (< ?n-vallas 15))
  =>
    (unmake-instance ?t)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 1)))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto simple del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

(defrule construir-valla-conjunta-no-pastos-doble
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (conjunta TRUE) (id-terreno ?id-terreno1))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t1 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno1))
    ?t2 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno2&~?id-terreno1))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    (not (object (is-a PASTO) (jugador ?jugador)))
    (conjunta ?jugador ?nombre ?numero)
    (test (>= ?cantidad 1))
    (test (< ?n-vallas 15))
  =>
    (unmake-instance ?t1)
    (unmake-instance ?t2)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno1) (id-extra ?id-terreno2) (n-celdas 2) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 2)))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto doble del terreno (" ?id-terreno1 "," ?id-terreno2 ") usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

(defrule construir-valla-conjunta-si-pastos-vallar-pasto-existente
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (conjunta TRUE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (vallas ?vallas) (n-celdas ?celdas))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    (conjunta ?jugador ?nombre ?numero)
    (test (< ?vallas (+ 2 (* 2 ?celdas))))
    (test (< ?n-vallas 15))
    (test (>= ?cantidad 1))
  =>
    (modify-instance ?t (vallas (+ ?vallas 1)))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c (cantidad (+ ?n-vallas 1)))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye una valla en el pasto del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora " (+ ?vallas 1) " vallas. Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

(defrule construir-valla-conjunta-si-pastos-vallar-nuevo-pasto-simple
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (conjunta TRUE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    (object (is-a PASTO) (jugador ?jugador))
    (not (object (is-a PASTO) (jugador ?jugador) (id ~?id-terreno) (n-celdas ?celdas) (vallas ~=(+ 2 (* 2 ?celdas)))))
    (conjunta ?jugador ?nombre ?numero)
    (test (< ?n-vallas 15))
    (test (>= ?cantidad 1))
  =>
    (unmake-instance ?t)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 1)))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto simple del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

(defrule construir-valla-conjunta-si-pastos-vallar-nuevo-pasto-doble
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (conjunta TRUE) (id-terreno ?id-terreno1))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t1 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno1))
    ?t2 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno2&~?id-terreno1))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    (object (is-a PASTO) (jugador ?jugador))
    (not (object (is-a PASTO) (jugador ?jugador) (id ~?id-terreno1) (n-celdas ?celdas) (vallas ~=(+ 2 (* 2 ?celdas)))))
    (conjunta ?jugador ?nombre ?numero)
    (not (test (= ?id-terreno1 ?id-terreno2)))
    (test (< ?n-vallas 15))
    (test (>= ?cantidad 1))
  =>
    (unmake-instance ?t1)
    (unmake-instance ?t2)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno1) (id-extra ?id-terreno2) (n-celdas 2) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 2)))
    (assert (vallando ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto doble del terreno (" ?id-terreno1 "," ?id-terreno2 ") usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

; Reglas recursivas.
; Se pueden construir tantas vallas como se desee, siempre que quede madera para ello.
; En este caso siempre habrá algún pasto ya construido.
; ESTRATEGIA: El jugador debe construir todas las vallas que pueda hasta que se quede sin madera.
(defrule construir-valla-vallar-pasto-existente-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id-terreno ?id-terreno))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (vallas ?vallas) (n-celdas ?celdas))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    (test (< ?vallas (+ 2 (* 2 ?celdas))))
    (test (< ?n-vallas 15))
    (test (>= ?cantidad 1))
    (vallando ?jugador ?nombre ?numero)
  =>
    (modify-instance ?t (vallas (+ ?vallas 1)))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c (cantidad (+ ?n-vallas 1)))
    (printout t "El jugador " ?jugador " construye una valla en el pasto del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora " (+ ?vallas 1) " vallas. Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

(defrule construir-valla-vallar-nuevo-pasto-simple-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id-terreno ?id-terreno))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    (object (is-a PASTO) (jugador ?jugador))
    (not (object (is-a PASTO) (jugador ?jugador) (id ~?id-terreno) (n-celdas ?celdas) (vallas ~=(+ 2 (* 2 ?celdas)))))
    ;No queremos que haya algún pasto que no esté totalmente vallado y ponernos a vallar otro
    (test (< ?n-vallas 15))
    (test (>= ?cantidad 1))
    (vallando ?jugador ?nombre ?numero)
  =>
    (unmake-instance ?t)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 1)))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto simple del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

(defrule construir-valla-vallar-nuevo-pasto-doble-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-VALLA) (nombre ?nombre) (id ?id) (id-terreno ?id-terreno1))
    ?t1 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno1))
    ?t2 <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno2&~?id-terreno1))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c1 <- (object (is-a CONTADOR-VALLAS) (jugador ?jugador) (cantidad ?n-vallas))
    ?c2 <- (object (is-a CONTADOR-PASTOS) (jugador ?jugador) (cantidad ?n-pastos))
    ?c3 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    (object (is-a PASTO) (jugador ?jugador))
    (not (object (is-a PASTO) (jugador ?jugador) (id ~?id-terreno1) (n-celdas ?celdas) (vallas ~=(+ 2 (* 2 ?celdas)))))
    ;No queremos que haya algún pasto que no esté totalmente vallado y ponernos a vallar otro
    (not (test (= ?id-terreno1 ?id-terreno2)))
    (test (>= ?cantidad 1))
    (test (< ?n-vallas 15))
    (vallando ?jugador ?nombre ?numero)
  =>
    (unmake-instance ?t1)
    (unmake-instance ?t2)
    (make-instance of PASTO (jugador ?jugador) (id ?id-terreno1) (id-extra ?id-terreno2) (n-celdas 2) (vallas 1))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c1 (cantidad (+ ?n-vallas 1)))
    (modify-instance ?c2 (cantidad (+ ?n-pastos 1)))
    (modify-instance ?c3 (cantidad (- ?n-terrenos 2)))
    (printout t "El jugador " ?jugador " construye la primera valla del pasto doble del terreno (" ?id-terreno1 "," ?id-terreno2 ") usando la carta " ?nombre "." crlf)
    (printout t "El pasto tiene ahora 1 valla. Cantidad PASTO: " (+ ?n-pastos 1) ". Cantidad VALLA: " (+ ?n-vallas 1) ". Cantidad MADERA: " (- ?cantidad 1) "." crlf))

; Aquí sucede lo mismo que al sembrar: si conseguimos nueva madera, el jugador volverá a construir
; vallas porque el estado "vallando" se encuentra latente. Es por ello que debemos eliminarlo en cuanto
; se termine de vallar (sólo en el caso en que se quede sin madera, ya que si llega al máximo de vallas no puede hacer nada).

; Nos quedamos sin madera.
(defrule quitar-estado-vallando-no-madera
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-VALLA) (nombre ?nombre))
    (object (is-a MADERA) (jugador ?jugador) (cantidad ?madera))
    ?a <- (vallando ?jugador ?nombre ?numero)
    (test (= ?madera 0))
  =>
    (retract ?a))

; Regla CONSTRUIR-ESTABLO
; Siempre construimos en pastos vallados, por lo que comprobaremos que existe un pasto totalmente
; vallado para colocar el establo.
(defrule construir-establo
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a CONSTRUIR-ESTABLO) (nombre ?nombre) (id ?id) (ocupada FALSE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (n-celdas ?n-celdas) (vallas ?vallas) (establos ?establos))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c <- (object (is-a CONTADOR-ESTABLOS) (jugador ?jugador) (cantidad ?n-establos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion-id 0) (recien-nacido FALSE) (accion NULL))
    (test (= ?vallas (+ 2 (* 2 ?n-celdas))))
    (test (< ?establos ?n-celdas))
    (test (< ?n-establos 4))
    (test (>= ?cantidad 2))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?t (establos (+ ?establos 1)))
    (modify-instance ?r (cantidad (- ?cantidad 2)))
    (modify-instance ?c (cantidad (+ ?n-establos 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (construyendo-establos ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye un establo en el terreno " ?id-terreno " usando la carta " ?nombre ". El pasto tiene ahora " (+ ?establos 1) " establos." crlf)
    (printout t "Cantidad ESTABLO: " (+ ?n-establos 1) ". Cantidad MADERA: " (- ?cantidad 2) "." crlf))

; Regla recursiva.
; En cada una de estas reglas sólo podemos construir un establo, pero se pueden construir
; tantos establos como se desee. Por eso es necesaria esta regla recursiva.
(defrule construir-establo-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-ESTABLO) (nombre ?nombre) (id-terreno ?id-terreno))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id-terreno) (n-celdas ?n-celdas) (vallas ?vallas) (establos ?establos))
    ?r <- (object (is-a MADERA) (jugador ?jugador) (cantidad ?cantidad))
    ?c <- (object (is-a CONTADOR-ESTABLOS) (jugador ?jugador) (cantidad ?n-establos))
    (construyendo-establos ?jugador ?nombre ?numero)
    (test (= ?vallas (+ 2 (* 2 ?n-celdas))))
    (test (< ?establos ?n-celdas))
    (test (< ?n-establos 4))
    (test (>= ?cantidad 2))
  =>
    (modify-instance ?t (establos (+ ?establos 1)))
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?c (cantidad (+ ?n-establos 1)))
    (printout t "El jugador " ?jugador " construye un establo en el terreno " ?id-terreno " usando la carta " ?nombre ". El pasto tiene ahora " (+ ?establos 1) " establos." crlf)
    (printout t "Cantidad ESTABLO: " (+ ?n-establos 1) ". Cantidad MADERA: " (- ?cantidad 2) "." crlf))

; En el caso de los establos sucede igual que con los estados "sembrando" y "vallando".
; Si nos quedamos sin madera, pero luego la conseguimos y el estado "construyendo establos"
; queda latente, volveremos a construir establos siempre que haya pastos vallados sin establos.

; No queda madera.
(defrule quitar-estado-construyendo-establos-no-madera
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-ESTABLO) (nombre ?nombre))
    (object (is-a MADERA) (jugador ?jugador) (cantidad ?madera))
    ?a <- (construyendo-establos ?jugador ?nombre ?numero)
    (test (<= ?madera 1))
  =>
    (retract ?a))

; Regla CONSTRUIR-HABITACIÓN
; Construimos una habitación en un terreno vacío de la granja del jugador.
(defrule construir-habitacion
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a CONSTRUIR-HABITACION) (nombre ?nombre) (id ?id) (ocupada FALSE) (id-terreno ?id-terreno))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    (object (is-a HABITACION) (jugador ?jugador) (material ?material))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?r <- (object (is-a ?material) (jugador ?jugador) (cantidad ?cantidad))
    ?j <- (object (is-a JUNCO) (jugador ?jugador) (cantidad ?juncos))
    ?c1 <- (object (is-a CONTADOR-HABITACIONES) (jugador ?jugador) (cantidad ?n-habitaciones))
    ?c2 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    ?p <- (object (is-a PERSONA) (accion ?accion) (accion-id 0) (jugador ?jugador) (recien-nacido FALSE))
    (test (< ?n-habitaciones 5))
    (test (>= ?cantidad 5))
    (test (>= ?juncos 2))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (unmake-instance ?t)
    (make-instance of HABITACION (jugador ?jugador) (id ?id-terreno) (material ?material))
    (modify-instance ?r (cantidad (- ?cantidad 5)))
    (modify-instance ?j (cantidad (- ?juncos 1)))
    (modify-instance ?c1 (cantidad (+ ?n-habitaciones 1)))
    (modify-instance ?c2 (cantidad (- ?n-terrenos 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (construyendo-habitaciones ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " construye una habitación en el terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "Cantidad HABITACION: " (+ ?n-habitaciones 1) ". Cantidad " ?material ": " (- ?cantidad 5) ". Cantidad JUNCO: " (- ?juncos 1) "." crlf))

; Regla recursiva.
; Se pueden construir tantas habitaciones como se desee siempre que se posea de los recursos y
; espacio necesarios. Es por ello que necesitamos esta regla recursiva.
(defrule construir-habitacion-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-HABITACION) (nombre ?nombre) (id-terreno ?id-terreno))
    (object (is-a HABITACION) (jugador ?jugador) (material ?material))
    ?t <- (object (is-a TERRENO-VACIO) (jugador ?jugador) (id ?id-terreno))
    ?r <- (object (is-a ?material) (jugador ?jugador) (cantidad ?cantidad))
    ?j <- (object (is-a JUNCO) (jugador ?jugador) (cantidad ?juncos))
    ?c1 <- (object (is-a CONTADOR-HABITACIONES) (jugador ?jugador) (cantidad ?n-habitaciones))
    ?c2 <- (object (is-a CONTADOR-TERRENOS-VACIOS) (jugador ?jugador) (cantidad ?n-terrenos))
    (construyendo-habitaciones ?jugador ?nombre ?numero)
    (test (< ?n-habitaciones 5)) ;Máximo número de habitaciones = 5
    (test (>= ?cantidad 5))
    (test (>= ?juncos 2))
  =>
    (unmake-instance ?t)
    (make-instance of HABITACION (jugador ?jugador) (id ?id-terreno) (material ?material))
    (modify-instance ?r (cantidad (- ?cantidad 5)))
    (modify-instance ?j (cantidad (- ?juncos 1)))
    (modify-instance ?c1 (cantidad (+ ?n-habitaciones 1)))
    (modify-instance ?c2 (cantidad (- ?n-terrenos 1)))
    (printout t "El jugador " ?jugador " construye una habitación en el terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "Cantidad HABITACION: " (+ ?n-habitaciones 1) ". Cantidad " ?material ": " (- ?cantidad 5) ". Cantidad JUNCO: " (- ?juncos 1) "." crlf))

; Volvemos a prevenir que se construyan nuevas habitaciones cuando ya hemos terminado
; de construirlas por haber obtenido nuevos recursos.

; No queda madera/adobe/piedra suficiente.
(defrule quitar-estado-construyendo-habitaciones-no-madera-adobe-piedra
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-HABITACION) (nombre ?nombre))
    (object (is-a HABITACION) (jugador ?jugador) (material ?material))
    (object (is-a ?material) (jugador ?jugador) (cantidad ?cantidad))
    ?a <- (construyendo-habitaciones ?jugador ?nombre ?numero)
    (test (<= ?cantidad 4))
  =>
    (retract ?a))

; No quedan juncos suficientes.
(defrule quitar-estado-construyendo-habitaciones-no-juncos
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a CONSTRUIR-HABITACION) (nombre ?nombre))
    (object (is-a JUNCO) (jugador ?jugador) (cantidad ?cantidad))
    ?a <- (construyendo-habitaciones ?jugador ?nombre ?numero)
    (test (<= ?cantidad 1))
  =>
    (retract ?a))

; Reglas REFORMAR-HABITACION
; Reformamos una habitación (el material del que está hecha).
; Necesitamos dos reglas: transición de madera a adobe y de adobe a piedra.
; Estas acciones son siempre conjuntas, pero con ID 1, por lo que deben dejar el hecho
; correspondiente para que se ejecute la acción con ID 2.
(defrule reformar-habitacion-madera-a-adobe
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a REFORMAR) (nombre ?nombre) (id ?id) (ocupada FALSE))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    (object (is-a CONTADOR-HABITACIONES) (jugador ?jugador) (cantidad ?n-habitaciones))
    ?t <- (object (is-a HABITACION) (jugador ?jugador) (id ?id-terreno) (material MADERA))
    ?r <- (object (is-a ADOBE) (jugador ?jugador) (cantidad ?adobe))
    ?j <- (object (is-a JUNCO) (jugador ?jugador) (cantidad ?juncos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
    (test (>= ?juncos ?n-habitaciones))
    (test (>= ?adobe ?n-habitaciones))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (- ?adobe 1)))
    (modify-instance ?j (cantidad (- ?juncos 1)))
    (modify-instance ?t (material ADOBE))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (reformando ?jugador ?nombre ADOBE ?numero))
    (assert (conjunta ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " reforma la habitación del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "La habitación pasa a estar construida de adobe. Cantidad ADOBE: " (- ?adobe 1) ". Cantidad JUNCO: " (- ?juncos 1) "." crlf))

(defrule reformar-habitacion-adobe-a-piedra
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a REFORMAR) (nombre ?nombre) (id ?id) (ocupada FALSE))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    (object (is-a CONTADOR-HABITACIONES) (jugador ?jugador) (cantidad ?n-habitaciones))
    ?t <- (object (is-a HABITACION) (jugador ?jugador) (id ?id-terreno) (material ADOBE))
    ?r <- (object (is-a PIEDRA) (jugador ?jugador) (cantidad ?piedra))
    ?j <- (object (is-a JUNCO) (jugador ?jugador) (cantidad ?juncos))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
    (test (>= ?juncos ?n-habitaciones))
    (test (>= ?piedra ?n-habitaciones))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (- ?piedra 1)))
    (modify-instance ?j (cantidad (- ?juncos 1)))
    (modify-instance ?t (material PIEDRA))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (reformando ?jugador ?nombre PIEDRA ?numero))
    (assert (conjunta ?jugador ?nombre ?numero))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " reforma la habitación del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "La habitación pasa a estar construida de piedra. Cantidad PIEDRA: " (- ?piedra 1) ". Cantidad JUNCO: " (- ?juncos 1) "." crlf))

; Reglas recursivas.
; Esta acción requiere de reglas recursivas, ya que se deben reformar TODAS las habitaciones
; que posea el jugador.
(defrule reformar-habitacion-madera-a-adobe-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a REFORMAR) (nombre ?nombre))
    ?t <- (object (is-a HABITACION) (jugador ?jugador) (id ?id-terreno) (material MADERA))
    ?r <- (object (is-a ADOBE) (jugador ?jugador) (cantidad ?adobe))
    ?j <- (object (is-a JUNCO) (jugador ?jugador) (cantidad ?juncos))
    (reformando ?jugador ?nombre ADOBE ?numero)
    (test (>= ?juncos 1))
    (test (>= ?adobe 1))
  =>
    (modify-instance ?t (material ADOBE))
    (modify-instance ?r (cantidad (- ?adobe 1)))
    (modify-instance ?j (cantidad (- ?juncos 1)))
    (printout t "El jugador " ?jugador " reforma la habitación del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "La habitación pasa a estar construida de adobe. Cantidad ADOBE: " (- ?adobe 1) ". Cantidad JUNCO: " (- ?juncos 1) "." crlf))

(defrule reformar-habitacion-adobe-a-piedra-rec
  (declare (salience 200))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a REFORMAR) (nombre ?nombre))
    ?t <- (object (is-a HABITACION) (jugador ?jugador) (id ?id-terreno) (material ADOBE))
    ?r <- (object (is-a PIEDRA) (jugador ?jugador) (cantidad ?piedra))
    ?j <- (object (is-a JUNCO) (jugador ?jugador) (cantidad ?juncos))
    (reformando ?jugador ?nombre PIEDRA ?numero)
    (test (>= ?juncos 1))
    (test (>= ?piedra 1))
  =>
    (modify-instance ?t (material PIEDRA))
    (modify-instance ?r (cantidad (- ?piedra 1)))
    (modify-instance ?j (cantidad (- ?juncos 1)))
    (printout t "El jugador " ?jugador " reforma la habitación del terreno " ?id-terreno " usando la carta " ?nombre "." crlf)
    (printout t "La habitación pasa a estar construida de piedra. Cantidad PIEDRA: " (- ?piedra 1) ". Cantidad JUNCO: " (- ?juncos 1) "." crlf))

; Regla AMPLIAR-FAMILIA
; Creamos una nueva persona en la familia del jugador teniendo al menos una habitación extra.
(defrule ampliar-familia-con-habitacion
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a AMPLIAR-FAMILIA) (nombre ?nombre) (id ?id) (ocupada FALSE) (habitacion TRUE))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    (object (is-a CONTADOR-HABITACIONES) (jugador ?jugador) (cantidad ?n-habitaciones))
    ?c <- (object (is-a CONTADOR-PERSONAS) (jugador ?jugador) (cantidad ?n-personas))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
    (test (< ?n-personas 5))
    (test (> ?n-habitaciones ?n-personas))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (make-instance of PERSONA (jugador ?jugador) (id (+ ?n-personas 1)) (recien-nacido TRUE))
    (modify-instance ?c (cantidad (+ ?n-personas 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (printout t "El jugador " ?jugador " amplia su familia usando la carta " ?nombre ". Cantidad PERSONA: " (+ ?n-personas 1) "." crlf))

; Creamos una nueva persona en la familia del jugador sin tener una habitación extra.
(defrule ampliar-familia-sin-habitacion
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a AMPLIAR-FAMILIA) (nombre ?nombre) (id ?id) (ocupada FALSE) (habitacion FALSE))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a CONTADOR-PERSONAS) (jugador ?jugador) (cantidad ?n-personas))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
    (test (< ?n-personas 5))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (make-instance of PERSONA (jugador ?jugador) (recien-nacido TRUE))
    (modify-instance ?c (cantidad (+ ?n-personas 1)))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (printout t "El jugador " ?jugador " amplia su familia usando la carta " ?nombre ". Cantidad PERSONA: " (+ ?n-personas 1) "." crlf))

; Reglas OBTENER-ADQUISICION-MAYOR
; Caso en que la adquisición mayor sólo necesita un recurso para ser comprada y además no viene de una acción conjunta.
(defrule obtener-adquisicion-mayor-no-conjunta-1-recurso
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-1-RECURSO) (nombre ?carta) (jugador 0) (recurso ?recurso) (cantidad ?cantidad1))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (- ?cantidad2 ?cantidad1)))
    (modify-instance ?c (jugador ?jugador))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre ". Cantidad " ?recurso ": " (- ?cantidad2 ?cantidad1) "." crlf))

; Caso en que la adquisición mayor sólo necesita dos recursos (ninguno de ellos es devolver la carta de hogar)
; para ser comprada y además no viene de una acción conjunta.
(defrule obtener-adquisicion-mayor-no-conjunta-2-recursos-no-hogar
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-2-RECURSOS) (nombre ?carta) (jugador 0) (recurso1 ?recurso1) (cantidad1 ?cantidad1) (recurso2 ?recurso2) (cantidad2 ?cantidad2))
    ?r1 <- (object (is-a ?recurso1) (jugador ?jugador) (cantidad ?cantidad3))
    ?r2 <- (object (is-a ?recurso2) (jugador ?jugador) (cantidad ?cantidad4))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
    (test (>= ?cantidad3 ?cantidad1))
    (test (>= ?cantidad4 ?cantidad2))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r1 (cantidad (- ?cantidad3 ?cantidad1)))
    (modify-instance ?r2 (cantidad (- ?cantidad4 ?cantidad2)))
    (modify-instance ?c (jugador ?jugador))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre "." crlf)
    (printout t "Cantidad " ?recurso1 ": " (- ?cantidad3 ?cantidad1) ". Cantidad " ?recurso2 ": " (- ?cantidad4 ?cantidad2) "." crlf))

; Caso en que la adquisición mayor sólo necesita dos recursos (alguno de ellos es devolver la carta de hogar)
; para ser comprada y además no viene de una acción conjunta. Aquí se decide usar los recursos.
; ESTRATEGIA: Siempre buscaremos comprar la carta con recursos que devolviendo un hogar.
(defrule obtener-adquisicion-mayor-no-conjunta-hogar-y-recurso-gastar-recurso
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-2-RECURSOS) (nombre ?carta) (jugador 0) (recurso1 HOGAR) (recurso2 ?recurso) (cantidad2 ?cantidad1))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (cantidad (- ?cantidad2 ?cantidad1)))
    (modify-instance ?c (jugador ?jugador))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre ". Cantidad " ?recurso ": " (- ?cantidad2 ?cantidad1) "." crlf))

; Caso en que la adquisición mayor sólo necesita dos recursos (alguno de ellos es devolver la carta de hogar)
; para ser comprada y además no viene de una acción conjunta. Aquí se decide usar el hogar.
(defrule obtener-adquisicion-mayor-no-conjunta-hogar-y-recurso-entregar-hogar
  (declare (salience 90))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE))
    ?a <- (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (id ?id) (ocupada FALSE) (conjunta FALSE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-2-RECURSOS) (nombre ?carta) (jugador 0) (recurso1 HOGAR))
    ?r <- (object (is-a ADQUISICION-MAYOR) (nombre ?hogar) (hogar TRUE) (jugador ?jugador))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?r (jugador 0))
    (modify-instance ?c (jugador ?jugador))
    (modify-instance ?a (ocupada TRUE))
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE))
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre ". Pierde la adquisición mayor " ?hogar " a cambio." crlf))

; Reglas homólogas a las anteriores, pero teniendo en cuenta que vienen de una acción conjunta
; y deben ser ejecutadas obligatoriamente (tienen ID 2).
(defrule obtener-adquisicion-mayor-conjunta-1-recurso
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (conjunta TRUE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-1-RECURSO) (nombre ?carta) (jugador 0) (recurso ?recurso) (cantidad ?cantidad1))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    (test (>= ?cantidad2 ?cantidad1))
    ?x <- (conjunta ?jugador ?nombre ?numero)
  =>
    (modify-instance ?r (cantidad (- ?cantidad2 ?cantidad1)))
    (modify-instance ?c (jugador ?jugador))
    (retract ?x)
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre ". Cantidad " ?recurso ": " (- ?cantidad2 ?cantidad1) "." crlf))

(defrule obtener-adquisicion-mayor-conjunta-2-recursos-no-hogar
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (conjunta TRUE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-2-RECURSOS) (nombre ?carta) (jugador 0) (recurso1 ?recurso1) (cantidad1 ?cantidad1) (recurso2 ?recurso2) (cantidad2 ?cantidad2))
    ?r1 <- (object (is-a ?recurso1) (jugador ?jugador) (cantidad ?cantidad3))
    ?r2 <- (object (is-a ?recurso2) (jugador ?jugador) (cantidad ?cantidad4))
    (test (>= ?cantidad3 ?cantidad1))
    (test (>= ?cantidad4 ?cantidad2))
    ?x <- (conjunta ?jugador ?nombre ?numero)
  =>
    (modify-instance ?r1 (cantidad (- ?cantidad3 ?cantidad1)))
    (modify-instance ?r2 (cantidad (- ?cantidad4 ?cantidad2)))
    (modify-instance ?c (jugador ?jugador))
    (retract ?x)
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre "." crlf)
    (printout t "Cantidad " ?recurso1 ": " (- ?cantidad3 ?cantidad1) ". Cantidad " ?recurso2 ": " (- ?cantidad4 ?cantidad2) "." crlf))

(defrule obtener-adquisicion-mayor-conjunta-hogar-y-recurso-gastar-recurso
  (declare (salience 150))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (conjunta TRUE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-2-RECURSOS) (nombre ?carta) (jugador 0) (recurso1 HOGAR) (recurso2 ?recurso) (cantidad2 ?cantidad1))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    (test (>= ?cantidad2 ?cantidad1))
    ?x <- (conjunta ?jugador ?nombre ?numero)
  =>
    (modify-instance ?r (cantidad (- ?cantidad2 ?cantidad1)))
    (modify-instance ?c (jugador ?jugador))
    (retract ?x)
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre ". Cantidad " ?recurso ": " (- ?cantidad2 ?cantidad1) "." crlf))

(defrule obtener-adquisicion-mayor-conjunta-hogar-y-recurso-entregar-hogar
  (declare (salience 125))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a OBTENER-ADQUISICION-MAYOR) (nombre ?nombre) (conjunta TRUE) (carta ?carta))
    (object (is-a CARTA) (nombre ?nombre) (jugador 3))
    ?c <- (object (is-a ADQ-MAYOR-2-RECURSOS) (nombre ?carta) (jugador 0) (recurso1 HOGAR) (recurso2 ?recurso) (cantidad2 ?cantidad1))
    ?r <- (object (is-a ADQUISICION-MAYOR) (nombre ?hogar) (hogar TRUE) (jugador ?jugador))
    ?x <- (conjunta ?jugador ?nombre ?numero)
  =>
    (modify-instance ?r (jugador 0))
    (modify-instance ?c (jugador ?jugador))
    (retract ?x)
    (assert (cambiar-carta-entera-ocupada ?nombre ?numero))
    (printout t "El jugador " ?jugador " compra la adquisición mayor " ?carta " usando la carta " ?nombre ". Pierde la adquisición mayor " ?hogar " a cambio." crlf))

; Regla CAMBIAR-JUGADOR-INICIAL
; Cambiamos (o no, ya que el jugador inicial puede usar esta acción para asegurar que no lo pierde) el jugador inicial.
(defrule cambiar-jugador-inicial
  (declare (salience 100))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador) (turno TRUE) (jugador-inicial ?j-inicial1))
    ?j2 <- (object (is-a JUGADOR) (turno FALSE) (jugador-inicial ?j-inicial2))
    ?a <- (object (is-a CAMBIAR-JUGADOR-INICIAL) (nombre ?nombre) (id ?id) (ocupada FALSE))
    (object (is-a CARTA-DE-TABLERO) (nombre ?nombre) (jugador 3))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (accion NULL) (accion-id 0) (recien-nacido FALSE))
  =>
    (modify-instance ?p (accion ?nombre) (accion-id ?id))
    (modify-instance ?j1 (jugador-inicial TRUE) (turno FALSE))
    (modify-instance ?j2 (jugador-inicial FALSE) (turno TRUE))
    (modify-instance ?a (ocupada TRUE))
    (printout t "El jugador " ?jugador " pasa a ser el nuevo jugador inicial usando la carta " ?nombre "." crlf))

; Hay cartas que tienen siempre más de una acción, por lo que hay que poner como ocupadas todas las que compartan el mismo nombre
; (regla recursiva).
(defrule cambiar-ocupada
  (declare (salience 300))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    (object (is-a RONDA) (numero ?numero))
    ?a <- (object (is-a ACCION) (nombre ?nombre) (ocupada FALSE))
    (cambiar-carta-entera-ocupada ?nombre ?numero)
  =>
    (modify-instance ?a (ocupada TRUE)))

; Esta regla sólo se ejecutará si al jugador que tiene el turno no le quedan personas para asignar acciones,
; pero el otro jugador todavía tiene personas de sobra.
(defrule cambiar-turno-familias-desbalanceadas
  (declare (salience 50))
    (object (is-a FASE) (valor JORNADA-LABORAL))
    ?j1 <- (object (is-a JUGADOR) (id ?jugador1) (turno TRUE))
    ?j2 <- (object (is-a JUGADOR) (id ?jugador2) (turno FALSE))
    (object (is-a CONTADOR-PERSONAS) (jugador ?jugador1) (cantidad ?n-personas1))
    (object (is-a CONTADOR-PERSONAS) (jugador ?jugador2) (cantidad ?n-personas2))
    (test (< ?n-personas1 ?n-personas2))
  =>
    (modify-instance ?j1 (turno FALSE))
    (modify-instance ?j2 (turno TRUE)))

; Cambiamos de fase
(defrule cambiar-fase-jornada-laboral
  (declare (salience 1))
  ?f <- (object (is-a FASE) (valor JORNADA-LABORAL))
=>
  (printout t "Fase: REGRESO AL HOGAR." crlf)
  (modify-instance ?f (valor REGRESO-AL-HOGAR)))

; Reglas REGRESO-AL-HOGAR
; En esta fase lo único que hacemos es resetear los hechos ordenados creados durante la fase de
; JORNADA-LABORAL, así como los estados temporales de personas y acciones. (Ejemplo: una persona
; es bebé sólo durante una ronda).

(defrule resetear-recolectado
  (declare (salience 100))
    (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a CAMPO) (recolectado TRUE))
  =>
    (modify-instance ?a (recolectado FALSE)))

; Reseteamos el estado de las personas
(defrule resetear-persona-adulto
  (declare (salience 100))
    (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    ?p <- (object (is-a PERSONA) (accion ?accion&~NULL) (accion-id ?accion-id&~0) (recien-nacido FALSE) (alimentado ?alimentado))
  =>
    (modify-instance ?p (accion NULL) (accion-id 0) (alimentado FALSE)))

; Reseteamos el estado de las acciones
(defrule resetear-accion
  (declare (salience 100))
    (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    ?a <- (object (is-a ACCION) (ocupada TRUE))
  =>
    (modify-instance ?a (ocupada FALSE)))

(defrule resetear-inicio-de-ronda
  (declare (salience 100))
    (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    ?a <- (object (is-a INICIO-DE-RONDA) (recogido TRUE))
  =>
    (modify-instance ?a (recogido FALSE)))

(defrule resetear-recoger-recurso
  (declare (salience 100))
    (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    ?a <- (object (is-a RECOGER-RECURSO) (repuesto TRUE))
  =>
    (modify-instance ?a (repuesto FALSE)))

(defrule resetear-recurso
  (declare (salience 100))
    (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    ?a <- (object (is-a RECURSO) (crias TRUE))
  =>
    (modify-instance ?a (crias FALSE)))

; Cambiamos de fase (hay cosecha)
(defrule cambiar-fase-regreso-al-hogar-cosecha
  (declare (salience 50))
    ?f <- (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    (object (is-a RONDA) (numero ?r))
    (object (is-a COSECHA) (ronda ?r))
  =>
    (modify-instance ?f (valor RECOLECCION))
    (printout t "Reseteando estados y valores temporales..." crlf)
    (printout t "Fase: COSECHA - RECOLECCION." crlf))

; Cambiamos de fase (no hay cosecha)
(defrule cambiar-fase-regreso-al-hogar
  (declare (salience 1))
    ?f <- (object (is-a FASE) (valor REGRESO-AL-HOGAR))
    ?r <- (object (is-a RONDA) (numero ?n) (periodo ?periodo))
  =>
    (modify-instance ?f (valor PRINCIPIO-DE-RONDA))
    (modify-instance ?r (numero (+ 1 ?n)))
    (printout t "Reseteando estados y valores temporales..." crlf)
    (printout t "Fase: PRINCIPIO DE RONDA." crlf)
    (printout t "Periodo: " ?periodo " Número: " (+ 1 ?n) "." crlf))

; Reglas RECOLECCIÓN
; Recogemos una unidad del recurso de cada campo y la almacenamos en la reserva.
(defrule recolectar
  (declare (salience 100))
    (object (is-a FASE) (valor RECOLECCION))
    (object (is-a JUGADOR) (id ?jugador))
    ?t <- (object (is-a CAMPO) (jugador ?jugador) (id ?id-terreno) (recolectado FALSE) (cantidad ?cantidad1) (vegetal ?vegetal))
    ?r <- (object (is-a ?vegetal) (jugador ?jugador) (cantidad ?cantidad2))
    (test (> ?cantidad1 0))
  =>
    (modify-instance ?t (cantidad (- ?cantidad1 1)) (recolectado TRUE))
    (modify-instance ?r (cantidad (+ ?cantidad2 1)))
    (printout t "El jugador " ?jugador " recolecta 1 " ?vegetal " del campo del terreno " ?id-terreno ". El campo tiene ahora " (- ?cantidad1 1) " " ?vegetal ". Cantidad " ?vegetal ": " (+ ?cantidad2 1) "." crlf))

; Si el campo se queda vacío tras recolectarlo, debemos indicar que tiene vegetal NULL,
; para poder sembrarlo de nuevo en la fase de jornada laboral.
(defrule resetear-campo
  (declare (salience 50))
    (object (is-a FASE) (valor RECOLECCION))
    ?t <- (object (is-a CAMPO) (cantidad 0) (vegetal ~NULL))
  =>
    (modify-instance ?t (vegetal NULL)))

; Cambiamos de fase
(defrule cambiar-fase-recoleccion
  (declare (salience 1))
    ?f <- (object (is-a FASE) (valor RECOLECCION))
   =>
    (printout t "Fase: COSECHA - ALIMENTACION." crlf)
    (modify-instance ?f (valor ALIMENTACION)))

; Reglas ALIMENTACION
; Alimentamos a un recién nacido (necesita sólo una unidad de comida)
(defrule alimentar-recien-nacido
  (declare (salience 100))
    (object (is-a FASE) (valor ALIMENTACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?r <- (object (is-a COMIDA) (jugador ?jugador) (cantidad ?cantidad))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (id ?id) (recien-nacido TRUE) (alimentado FALSE))
    (test (>= ?cantidad 1))
  =>
    (modify-instance ?r (cantidad (- ?cantidad 1)))
    (modify-instance ?p (alimentado TRUE))
    (printout t "El jugador " ?jugador " alimenta al recién nacido con ID " ?id ". Cantidad COMIDA: " (- ?cantidad 1) "." crlf))

; Alimentamos a un adulto (necesita dos unidades de comida)
(defrule alimentar-adulto
  (declare (salience 100))
    (object (is-a FASE) (valor ALIMENTACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?r <- (object (is-a COMIDA) (jugador ?jugador) (cantidad ?cantidad))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (id ?id) (recien-nacido FALSE) (alimentado FALSE))
    (test (>= ?cantidad 2))
  =>
    (modify-instance ?r (cantidad (- ?cantidad 2)))
    (modify-instance ?p (alimentado TRUE))
    (printout t "El jugador " ?jugador " alimenta al adulto con ID " ?id ". Cantidad COMIDA: " (- ?cantidad 2) "." crlf)) ;ALBA

; Usamos alguna carta de adquisición mayor para obtener comida.
; ESTRATEGIA: Cuando no se posea la comida necesaria para alimentar a toda la familia,
; se usarán primero las cartas de adquisición mayor, después se intercambiarán vegetales por comida
; y como último recurso se usarán las cartas de mendicidad.
(defrule intercambiar-recurso-no-animal-comida
  (declare (salience 90))
    (object (is-a FASE) (valor ALIMENTACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a INTERCAMBIAR-RECURSOS) (nombre ?nombre) (recurso ?recurso) (cantidad ?cantidad1) (comida ?comida))
    (object (is-a ADQUISICION-MAYOR) (nombre ?nombre) (jugador ?jugador))
    ?r <- (object (is-a ?recurso) (tipo ~ANIMAL) (jugador ?jugador) (cantidad ?cantidad2))
    ?c <- (object (is-a COMIDA) (jugador ?jugador) (cantidad ?cantidad3))
    (object (is-a PERSONA) (jugador ?jugador) (alimentado FALSE))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?r (cantidad (- ?cantidad2 ?cantidad1)))
    (modify-instance ?c (cantidad (+ ?comida ?cantidad3)))
    (printout t "El jugador " ?jugador " obtiene " ?comida " COMIDA por " ?cantidad1 " " ?recurso " usando la carta " ?nombre ". Cantidad " ?recurso ": " (- ?cantidad2 ?cantidad1) ". Cantidad COMIDA: " (+ ?comida ?cantidad3) "." crlf))

(defrule intercambiar-recurso-animal-comida-quitar-de-habitacion
  (declare (salience 90))
    (object (is-a FASE) (valor ALIMENTACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a INTERCAMBIAR-RECURSOS) (nombre ?nombre) (recurso ?recurso) (comida ?comida))
    (object (is-a ADQUISICION-MAYOR) (nombre ?nombre) (jugador ?jugador))
    ?r <- (object (is-a ?recurso) (tipo ANIMAL) (jugador ?jugador) (cantidad ?cantidad1))
    ?t <- (object (is-a HABITACION) (jugador ?jugador) (animal ?recurso) (id ?id-terreno))
    ?c <- (object (is-a COMIDA) (jugador ?jugador) (cantidad ?cantidad2))
    (object (is-a PERSONA) (jugador ?jugador) (alimentado FALSE))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?t (animal NULL))
    (modify-instance ?r (cantidad (- ?cantidad1 1)))
    (modify-instance ?c (cantidad (+ ?comida ?cantidad2)))
    (printout t "El jugador " ?jugador " obtiene " ?comida " COMIDA por 1 " ?recurso " usando la carta " ?nombre ". Se retira el animal de la habitación del terreno " ?id-terreno "." crlf)
    (printout t "Cantidad " ?recurso ": " (- ?cantidad1 1) ". Cantidad COMIDA: " (+ ?comida ?cantidad2) "." crlf))

(defrule intercambiar-recurso-animal-comida-quitar-de-pasto
  (declare (salience 90))
    (object (is-a FASE) (valor ALIMENTACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a INTERCAMBIAR-RECURSOS) (nombre ?nombre) (recurso ?recurso) (comida ?comida))
    (object (is-a ADQUISICION-MAYOR) (nombre ?nombre) (jugador ?jugador))
    ?r <- (object (is-a ?recurso) (tipo ANIMAL) (jugador ?jugador) (cantidad ?cantidad1))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (animal ?recurso) (cantidad ?cantidad2&~0) (id ?id-terreno))
    ?c <- (object (is-a COMIDA) (jugador ?jugador) (cantidad ?cantidad3))
    (object (is-a PERSONA) (jugador ?jugador) (alimentado FALSE))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?t (cantidad (- ?cantidad2 1)))
    (modify-instance ?r (cantidad (- ?cantidad1 1)))
    (modify-instance ?c (cantidad (+ ?comida ?cantidad3)))
    (printout t "El jugador " ?jugador " obtiene " ?comida " COMIDA por 1 " ?recurso " usando la carta " ?nombre ". Se retira el animal del pasto del terreno " ?id-terreno "." crlf)
    (printout t "Quedan " (- ?cantidad2 1) ?recurso " en el pasto. Cantidad " ?recurso ": " (- ?cantidad1 1) ". Cantidad COMIDA: " (+ ?comida ?cantidad3) "." crlf))

; Si el pasto se queda vacío tras quitar un animal, debemos quitarle el tipo de animales que guarda,
; para que puedan entrar otro tipo de animales (la restricción del tipo anterior ya no aplica).
(defrule resetear-pasto
  (declare (salience 60))
    (object (is-a FASE) (valor ALIMENTACION))
    ?t <- (object (is-a PASTO) (cantidad 0) (animal ~NULL))
  =>
    (modify-instance ?t (animal NULL)))

; Cambiamos vegetales (cereales u hortalizas) por comida.
(defrule cambiar-vegetal-comida
  (declare (salience 50))
    (object (is-a FASE) (valor ALIMENTACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad1) (tipo VEGETAL))
    ?c <- (object (is-a COMIDA) (jugador ?jugador) (cantidad ?cantidad2))
    (object (is-a PERSONA) (jugador ?jugador) (alimentado FALSE))
    (test (> ?cantidad1 0))
  =>
    (modify-instance ?r (cantidad (- ?cantidad1 1)))
    (modify-instance ?c (cantidad (+ ?cantidad2 1)))
    (printout t "El jugador " ?jugador " obtiene 1 COMIDA por 1 " ?recurso ". Cantidad " ?recurso ": " (- ?cantidad1 1) ". Cantidad COMIDA: " (+ ?cantidad2 1) "." crlf))

; Usamos una carta de mendicidad porque no hemos podido conseguir alimento de ninguna otra manera.
(defrule mendigar
  (declare (salience 5))
    (object (is-a FASE) (valor ALIMENTACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?p <- (object (is-a PERSONA) (jugador ?jugador) (id ?id) (alimentado FALSE))
    ?m <- (object (is-a CARTA-DE-MENDICIDAD) (jugador 0))
    ?c <- (object (is-a CONTADOR-CARTAS-DE-MENDICIDAD) (jugador ?jugador) (cantidad ?n))
  =>
    (modify-instance ?m (jugador ?jugador))
    (modify-instance ?c (cantidad (+ ?n 1)))
    (modify-instance ?p (alimentado TRUE))
    (printout t "El jugador " ?jugador " usa una carta de mendicidad para alimentar al miembro de su familia con ID " ?id "." crlf))

; Cambiamos de fase.
(defrule cambiar-fase-alimentacion
  (declare (salience 1))
    ?f <- (object (is-a FASE) (valor ALIMENTACION))
  =>
    (modify-instance ?f (valor PROCREACION))
    (printout t "Fase: COSECHA - PROCREACION." crlf))

; Reglas PROCREACION

; Regla para generar una cría.
(defrule crear-cria
  (declare (salience 200))
    (object (is-a FASE) (valor PROCREACION))
    (object (is-a JUGADOR) (id ?jugador))
    ?a <- (object (is-a ?animal) (tipo ANIMAL) (cantidad ?cantidad) (crias FALSE) (jugador ?jugador))
    (test (>= ?cantidad 2))
  =>
    (modify-instance ?a (crias TRUE)))

; Repartimos una cría en una habitación.
(defrule repartir-cria-habitacion
  (declare (salience 90))
    (object (is-a FASE) (valor PROCREACION))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?r <- (object (is-a ?animal) (tipo ANIMAL) (cantidad ?cantidad) (crias TRUE) (jugador ?jugador))
    ?t <- (object (is-a HABITACION) (jugador ?jugador) (id ?id) (animal NULL))
    (not (repartida ?jugador ?animal ?numero))
  =>
    (modify-instance ?t (animal ?animal))
    (modify-instance ?r (cantidad (+ ?cantidad 1)) (crias FALSE))
    (assert (repartida ?jugador ?animal ?numero))
    (printout t "El jugador " ?jugador " reparte una cría de " ?animal " en la habitación del terreno " ?id ". Cantidad " ?animal ": " (+ ?cantidad 1) "." crlf))

; Repartimos una cría en un pasto vacío.
(defrule repartir-cria-pasto-vacio
  (declare (salience 90))
    (object (is-a FASE) (valor PROCREACION))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?r <- (object (is-a ?animal) (tipo ANIMAL) (crias TRUE) (cantidad ?cantidad1) (jugador ?jugador))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id) (animal NULL) (cantidad ?cantidad2) (n-celdas ?celdas) (vallas ?vallas) (establos ?establos))
    (test (= ?vallas (+ 2 (* 2 ?celdas))))
    (test (< ?cantidad2 (+ (* 2 ?celdas) (* 2 ?establos))))
    (not (repartida ?jugador ?animal))
  =>
    (modify-instance ?t (cantidad (+ ?cantidad2 1)) (animal ?animal))
    (modify-instance ?r (cantidad (+ ?cantidad1 1)) (crias FALSE))
    (assert (repartida ?jugador ?animal ?numero))
    (printout t "El jugador " ?jugador " reparte una cría de " ?animal " en el pasto vacío del terreno " ?id "." crlf)
    (printout t "Animales en el pasto: 1. Cantidad " ?animal ": " (+ ?cantidad1 1) "." crlf))

; Repartimos una cría en un pasto no vacío.
(defrule repartir-cria-pasto-no-vacio
  (declare (salience 90))
    (object (is-a FASE) (valor PROCREACION))
    (object (is-a RONDA) (numero ?numero))
    (object (is-a JUGADOR) (id ?jugador))
    ?r <- (object (is-a ?animal) (tipo ANIMAL) (crias TRUE) (cantidad ?cantidad1) (jugador ?jugador))
    ?t <- (object (is-a PASTO) (jugador ?jugador) (id ?id) (animal ?animal) (cantidad ?cantidad2) (n-celdas ?celdas) (vallas ?vallas) (establos ?establos))
    (test (= ?vallas (+ 2 (* 2 ?celdas))))
    (test (< ?cantidad2 (+ (* 2 ?celdas) (* 2 ?establos))))
    (not (repartida ?jugador ?animal ?numero))
  =>
    (modify-instance ?t (cantidad (+ ?cantidad2 1)))
    (modify-instance ?r (cantidad (+ ?cantidad1 1)) (crias FALSE))
    (assert (repartida ?jugador ?animal ?numero))
    (printout t "El jugador " ?jugador " reparte una cría de " ?animal " en el pasto no vacío del terreno " ?id "." crlf)
    (printout t "Animales en el pasto: " (+ ?cantidad2 1) ". Cantidad " ?animal ": " (+ ?cantidad1 1) "." crlf))

; Cambiamos de fase.
(defrule cambiar-fase-procreacion
  (declare (salience 1))
    ?f <- (object (is-a FASE) (valor PROCREACION))
    ?r <- (object (is-a RONDA) (periodo ?periodo) (numero ?numero))
  =>
   (modify-instance ?f (valor PRINCIPIO-DE-RONDA))
   (modify-instance ?r (periodo (+ ?periodo 1)) (numero (+ ?numero 1)))
   (printout t "Fase: PRINCIPIO DE RONDA." crlf)
   (printout t "Periodo: " (+ ?periodo 1) " Ronda: " (+ ?numero 1) "."crlf))

; Reglas CALCULO-PUNTUACIÓN
; Todos los vegetales que se encuentran en los campos deben pasar a la reserva para poder tenerlos en cuenta en la puntuación.
(defrule añadir-vegetales-en-campo-a-reserva
  (declare (salience 200))
  (object (is-a FASE) (valor CALCULO-PUNTUACION))
  (object (is-a JUGADOR) (id ?jugador))
  ?c <-(object (is-a CAMPO) (vegetal ?recurso)(jugador ?jugador)(cantidad ?c-campo))
  ?r <-(object (is-a ?recurso) (cantidad ?c-recurso) (jugador ?jugador))
  (test (neq ?c-campo 0))
=>
  (modify-instance ?r (cantidad (+ ?c-campo ?c-recurso)))
  (modify-instance ?c (cantidad 0))
  (printout t "El jugador " ?jugador " suma " ?c-campo " unidades de " ?recurso " a la reserva personal. TOTAL: " (+ ?c-campo ?c-recurso) "." crlf))

; Sumamos los puntos que den los distintos recursos de acuerdo a TABLA-PUNTUACION.
(defrule sumar-puntos-recurso
  (declare (salience 100))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos1))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (puntuado FALSE) (cantidad ?cantidad))
    (object (is-a TABLA-PUNTUACION) (recurso ?recurso) (cantidad ?cantidad) (puntos ?puntos2))
  =>
    (modify-instance ?j (puntuacion (+ ?puntos1 ?puntos2)))
    (modify-instance ?r (puntuado TRUE))
    (printout t "El jugador " ?jugador " suma " ?puntos2 " puntos por tener " ?cantidad " unidades de " ?recurso ". TOTAL: " (+ ?puntos1 ?puntos2) "." crlf))

; Sumamos los puntos que den los distintos contadores de acuerdo a TABLA-PUNTUACION.
(defrule sumar-puntos-contador
  (declare (salience 100))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos1))
    ?c <- (object (is-a CONTADOR) (jugador ?jugador) (puntuado FALSE) (recurso ?recurso) (cantidad ?cantidad))
    (object (is-a TABLA-PUNTUACION) (recurso ?recurso) (cantidad ?cantidad) (puntos ?puntos2))
  =>
    (modify-instance ?j (puntuacion (+ ?puntos1 ?puntos2)))
    (modify-instance ?c (puntuado TRUE))
    (printout t "El jugador " ?jugador " suma " ?puntos2 " puntos por tener " ?cantidad " unidades de " ?recurso ". TOTAL: " (+ ?puntos1 ?puntos2) "." crlf))

; Sumamos los puntos que dan las habitaciones de adobe (contador no puede obtener el material).
(defrule sumar-puntos-habitaciones-adobe
  (declare (salience 100))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos))
    ?c <- (object (is-a CONTADOR-HABITACIONES) (jugador ?jugador) (puntuado FALSE) (cantidad ?cantidad))
    (object (is-a HABITACION) (jugador ?jugador) (material ADOBE))
  =>
    (modify-instance ?j (puntuacion (+ ?puntos ?cantidad)))
    (modify-instance ?c (puntuado TRUE))
    (printout t "El jugador " ?jugador " suma " ?cantidad " puntos por tener " ?cantidad " habitaciones de adobe . TOTAL: " (+ ?puntos ?cantidad) "." crlf))

; Sumamos los puntos que dan las habitaciones de piedra (contador no puede obtener el material).
(defrule sumar-puntos-habitaciones-piedra
  (declare (salience 100))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos))
    ?c <- (object (is-a CONTADOR-HABITACIONES) (jugador ?jugador) (puntuado FALSE) (cantidad ?cantidad))
    (object (is-a HABITACION) (jugador ?jugador) (material PIEDRA))
  =>
    (modify-instance ?j (puntuacion (+ ?puntos (* 2 ?cantidad))))
    (modify-instance ?c (puntuado TRUE))
    (printout t "El jugador " ?jugador " suma " (* 2 ?cantidad) " puntos por tener " ?cantidad " habitaciones de piedra . TOTAL: " (+ ?puntos ?cantidad) "." crlf))

; Sumamos los puntos de una carta de adquisición con puntos extra y que cumpla sus requisitos.
; Se empieza siempre por los puntos más altos (3) hasta llegar al caso en que no se añade ningún punto.
; Añadiendo 3 puntos.
(defrule sumar-puntos-cartas-adquisicion-mayor-extra3
  (declare (salience 100))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos1))
    ?c <- (object (is-a ADQUISICION-MAYOR) (nombre ?nombre) (jugador ?jugador) (puntos ?puntos2) (recurso-pts-extra ?recurso) (cantidad-pts-extra3 ?cantidad1) (puntuada FALSE))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?j (puntuacion (+ (+ ?puntos1 ?puntos2) 3)))
    (modify-instance ?c (puntuada TRUE))
    (printout t "El jugador " ?jugador " suma " ?puntos2 " por haber comprado la carta " ?nombre ". " crlf)
    (printout t "Consigue 3 puntos extra por tener " ?cantidad2 " unidades de " ?recurso ". TOTAL: " (+ (+ ?puntos1 ?puntos2) 3) "." crlf))

; Añadiendo 2 puntos.
(defrule sumar-puntos-cartas-adquisicion-mayor-extra2
  (declare (salience 90))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos1))
    ?c <- (object (is-a ADQUISICION-MAYOR) (nombre ?nombre) (jugador ?jugador) (puntos ?puntos2) (recurso-pts-extra ?recurso) (cantidad-pts-extra2 ?cantidad1) (puntuada FALSE))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?j (puntuacion (+ (+ ?puntos1 ?puntos2) 2)))
    (modify-instance ?c (puntuada TRUE))
    (printout t "El jugador " ?jugador " suma " ?puntos2 " por haber comprado la carta " ?nombre ". " crlf)
    (printout t "Consigue 2 puntos extra por tener " ?cantidad2 " unidades de " ?recurso ". TOTAL: " (+ (+ ?puntos1 ?puntos2) 2) "." crlf))

; Añadiendo un punto.
(defrule sumar-puntos-cartas-adquisicion-mayor-extra1
  (declare (salience 80))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos1))
    ?c <- (object (is-a ADQUISICION-MAYOR) (nombre ?nombre) (jugador ?jugador) (puntos ?puntos2) (recurso-pts-extra ?recurso) (cantidad-pts-extra1 ?cantidad1) (puntuada FALSE))
    ?r <- (object (is-a ?recurso) (jugador ?jugador) (cantidad ?cantidad2))
    (test (>= ?cantidad2 ?cantidad1))
  =>
    (modify-instance ?j (puntuacion (+ (+ ?puntos1 ?puntos2) 1)))
    (modify-instance ?c (puntuada TRUE))
    (printout t "El jugador " ?jugador " suma " ?puntos2 " por haber comprado la carta " ?nombre ". " crlf)
    (printout t "Consigue 1 punto extra por tener " ?cantidad2 " unidades de " ?recurso ". TOTAL: " (+ (+ ?puntos1 ?puntos2) 1) "." crlf))

; Sumamos los puntos de una carta de adquisición mayor sin puntos extra o con ellos pero sin
; poder obtener ninguno.
(defrule sumar-puntos-cartas-adquisicion-mayor-no-extra
  (declare (salience 100))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    ?j <- (object (is-a JUGADOR) (id ?jugador) (puntuacion ?puntos1))
    ?c <- (object (is-a ADQUISICION-MAYOR) (nombre ?nombre) (jugador ?jugador) (puntos ?puntos2) (puntuada FALSE))
  =>
    (modify-instance ?j (puntuacion (+ ?puntos1 ?puntos2)))
    (modify-instance ?c (puntuada TRUE))
    (printout t "El jugador " ?jugador " suma " ?puntos2 " puntos por haber comprado la carta " ?nombre ". TOTAL: " (+ ?puntos1 ?puntos2) "." crlf))

; Terminamos de escribir el fichero. Caso algún jugador gana.
(defrule terminar-ejecucion-ganador
  (declare (salience 10))
    (object (is-a JUGADOR) (id ?j1) (puntuacion ?p1))
    (object (is-a JUGADOR) (id ?j2) (puntuacion ?p2))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    (strategy)
    (test (> ?p1 ?p2))
  =>
    (printout t "PUNTOS TOTALES JUGADOR " ?j1 ": " ?p1 "." crlf)
    (printout t "PUNTOS TOTALES JUGADOR " ?j2 ": " ?p2 "." crlf)
    (printout t "El jugador " ?j1 " ha ganado la partida con " (- ?p1 ?p2) " puntos de diferencia." crlf)
    (dribble-off)
    (halt))

; Terminamos de escribir el fichero. Caso los jugadores empatan.
(defrule terminar-ejecucion-empate
  (declare (salience 10))
    (object (is-a JUGADOR) (id ?j1) (puntuacion ?p1))
    (object (is-a JUGADOR) (id ?j2) (puntuacion ?p2))
    (object (is-a FASE) (valor CALCULO-PUNTUACION))
    (strategy)
    (test (neq ?j1 ?j2))
    (test (eq ?p1 ?p2))
  =>
    (printout t "PUNTOS TOTALES AMBOS JUGADORES " ?p1 "." crlf)
    (printout t "La partida ha acabado en empate." crlf)
    (dribble-off)
    (halt))
