; INGENIERÍA DEL CONOCIMIENTO - PRÁCTICA 2: AGRICOLA (PDDL)
; Autoras: Alba María García García & Irene Martínez Castillo, Grupo 83

; RESTRICCIONES PROPIAS (NO INDICADAS EN EL ENUNCIADO):
; - Los jugadores no se intercambian el turno, es decir, todas las personas que pertenecen
;   a un mismo jugador realizan sus acciones conjuntamente.
; - Sólo vamos a simular la fase de jornada laboral, por lo que todas las acciones estarán
;   disponibles para todos los jugadores desde el principio.
; - Cuando un jugador recoge algún recurso no animal del tablero, siempre lo hace en una cantidad
;   constante (5).
; - Los recursos animales se obtienen de 2 en 2, de modo que cada vez que se recojan se coloquen
;   en un pasto vallado y este quede completo (ya que no se permiten establos). Por tanto, Los
;   animales ya no se pueden colocar en habitaciones.
; - Los pastos constan siempre de una única celda (no una o dos, como en CLIPS).
; - En la acción 'vallar', se valla un pasto por completo (4 vallas) siempre que se disponga
;   de la madera necesaria (no vallamos de uno en uno). Se mantiene el límite de vallas por jugador.
; - Si un jugador amplía la familia, no tiene que esperar al siguiente turno para tener una
;   acción más disponible.
; - No existen acciones conjuntas ni recursivas.
; - Una vez que una carta es usada por un jugador, ya no se puede utilizar en el resto de la partida.

(define (domain agricola)
  (:requirements :strips :typing :fluents :action-costs)
  ; Pequeña jerarquía de tipos
  (:types jugador
          persona
          celda
            vacia - celda
            habitacion - celda
            campo - celda
            pasto - celda
          recurso
            comida - recurso
            construccion - recurso
              madera - construccion
              adobe - construccion
              piedra - construccion
              junco - construccion
            vegetal - recurso
              cereal - vegetal
              hortaliza - vegetal
            animal - recurso
              oveja - animal
              vaca - animal
              jabali - animal
          carta
            carta_accion - carta
              carta_obtener_recurso - carta_accion
                obtener_animal - carta_obtener_recurso
                obtener_no_animal - carta_obtener_recurso
              carta_arar - carta_accion
              carta_sembrar - carta_accion
              carta_vallar - carta_accion
              carta_const_hab - carta_accion
              carta_ref_hab - carta_accion
              carta_amp_fam - carta_accion
                amp_fam_sin_hab - carta_amp_fam
                amp_fam_con_hab - carta_amp_fam
              carta_jug_inicial - carta_accion
              carta_obt_adq_mayor - carta_accion
              carta_hornear_pan - carta_accion
            adq_mayor - carta
          )

  ; Predicados
  (:predicates (jugador-actual ?j - jugador) ; Indica el jugador que está jugando actualmente
               (pertenece ?p - persona ?j - jugador) ; Indica si la persona ?p pertenece al jugador ?j
               (recurso-carta ?c - carta_obtener_recurso ?r - recurso) ; Indica el recurso ?r que se puede obtener con la carta ?c
               (is-animal ?r - recurso) ; Indica si el recurso ?r es de tipo animal
               (material-habitacion ?j - jugador ?r - construccion) ; Indica el material ?c del que están hechas las habitaciones del jugador ?j
               (transicion-reforma ?j - jugador ?r1 ?r2 - construccion) ; Indica la transición de materiales permitidos en las reformas
               (jugador-inicial ?j - jugador) ; Indica qué jugador ?j es el jugador inicial
               (adq-mayor ?j - jugador ?a - adq_mayor) ; Indica que la adquisición mayor ?a pertenece al jugador ?j
               (is-adq-mayor-doble ?a - adq_mayor) ; Indica si una adquisición mayor ?a necesita dos recursos para ser comprada
               (usada ?c - carta_accion) ; Indica si la carta ?c ya ha sido usada en el juego
               (juego-terminado)) ; Indica que el juego ya ha terminado

   ; Funciones
   (:functions (turno-actual) ; Indica el turno actual
               (ultimo-turno) ; Indica cuál es el último turno
               (acciones-realizadas ?j - jugador) ; Indica el número de acciones realizadas por el jugador ?j en el turno actual
               (n-acciones ?j - jugador) ; Indica el número de acciones disponibles del jugador ?j en el turno actual
               (n-recurso ?j - jugador ?r - recurso) ; Indica el número de unidades del recurso ?r que dispone el jugador ?j
               (n-celdas  ?j - jugador ?c - celda) ; Indica el número de celdas de tipo ?c que tiene el jugador ?j
               (n-campos-sembrados ?j - jugador) ; Indica el número de campos sembrados para el jugador ?j
               (n-pastos-completos ?j - jugador) ; Indica el número de pastos completos (llenos) para el jugador ?j
               (n-vallas ?j - jugador) ; Indica el número de vallas que ha construido el jugador ?j
               (jugado ?p - persona ?j - jugador) ; Indica el último turno en que la persona ?p del jugador ?j ha jugado
               (coste-adq-mayor ?a - adq_mayor ?r - construccion) ; Indica las unidades del material ?c que se necesitan para comprar la adq. mayor ?a
               (hornear-pan-1 ?a - adq_mayor) ; Unidades de comida obtenidas utilizando una unidad de cereal con la adquisición mayor ?a
               (hornear-pan-2 ?a - adq_mayor) ; Unidades de comida obtenidas utilizando dos unidades de cereal con la adquisición mayor ?a
               (penalty ?j - jugador) ; Métrica específica para cada jugador
               (total-cost)) ; Suma de las métricas de ambos jugadores

;  ACCIÓN BÁSICA 'JUGAR' CREADA EN LA VERSIÓN 0
;  (:action JUGAR
;    :parameters (?j - jugador ?p - persona)
;    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
;                  (not (= (acciones-realizadas ?j) (n-acciones ?j))))
;    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)))

  ; Con esta acción, el jugador puede obtener 5 unidades del recurso no animal ?r utilizando
  ; la carta ?ca, siempre que esta no haya sido usada anteriormente.
  (:action OBTENER-RECURSO-NO-ANIMAL
    :parameters (?j - jugador ?p - persona ?r - recurso ?ca - obtener_no_animal)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (recurso-carta ?ca ?r) (not (is-animal ?r)) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (increase (n-recurso ?j ?r) 5) (increase (penalty ?j) 4) (increase (total-cost) 4) (usada ?ca)))

  ; Con esta acción, el jugador puede obtener 2 unidades del recurso animal ?r utilizando
  ; la carta ?ca, siempre que esta no haya sido usada anteriormente y que tenga algún pasto
  ; vallado que no esté completo, es decir, lleno de animales.
  (:action OBTENER-RECURSO-ANIMAL
    :parameters (?j - jugador ?p - persona ?r - animal ?ca - obtener_animal  ?pa - pasto)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (recurso-carta ?ca ?r) (> (n-celdas ?j ?pa) (n-pastos-completos ?j))
                  (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (increase (n-recurso ?j ?r) 2) (increase (n-pastos-completos ?j) 1)
            (increase (penalty ?j) 3) (increase (total-cost) 3) (usada ?ca)))

  ; Con esta acción, el jugador puede arar un campo utilizando la carta ?ca,
  ; siempre que esta no haya sido usada anteriormente y exista alguna celda vacía.
  (:action ARAR-CAMPO
    :parameters (?j - jugador ?p - persona ?ca - carta_arar ?c - campo ?v - vacia)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (> (n-celdas ?j ?v) 0) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (decrease (n-celdas ?j ?v) 1) (increase (n-celdas ?j ?c) 1)
            (increase (penalty ?j) 5) (increase (total-cost) 5) (usada ?ca)))

  ; Con esta acción, el jugador puede sembrar un campo utilizando la carta ?ca,
  ; siempre que esta no haya sido usada anteriormente, exista algún campo sin sembrar y
  ; disponga de al menos un cereal u hortaliza.

  ; No necesitamos dos acciones de 'sembrar campo' porque luego no vamos a recolectarlos,
  ; por lo que no nos importa el número de vegetales que haya sobre ellos (El número de
  ; unidades depositadas de hortaliza y cereal difiere).
  (:action SEMBRAR-CAMPO
    :parameters (?j - jugador ?p - persona ?ca - carta_sembrar ?c - campo ?ce - vegetal)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (> (n-celdas ?j ?c) (n-campos-sembrados ?j)) (> (n-recurso ?j ?ce) 0)
                  (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (decrease (n-recurso ?j ?ce) 1) (increase (n-campos-sembrados ?j) 1)
            (increase (penalty ?j) 4)(increase (total-cost) 4)(usada ?ca)))

  ; Con esta acción, el jugador puede vallar por completo (4 vallas) un pasto utilizando la carta ?ca,
  ; siempre que esta no haya sido usada anteriormente, disponga de las unidades de madera suficientes y
  ; de alguna celda vacía y además no exceda el número máximo de vallas por jugador (15).
  (:action VALLAR
    :parameters (?j - jugador ?p - persona ?m - madera ?ca - carta_vallar ?c - vacia ?pa - pasto)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (>= (n-recurso ?j ?m) 4) (< (n-vallas ?j) 15) (> (n-celdas ?j ?c) 0)
                  (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (decrease (n-recurso ?j ?m) 4) (increase (n-vallas ?j) 4) (decrease (n-celdas ?j ?c) 1)
            (increase (n-celdas ?j ?pa) 1) (increase (penalty ?j) 6) (increase (total-cost) 6) (usada ?ca)))

  ; Con esta acción, el jugador puede construir una habitación del material que tenga construido el resto de casas
  ; utilizando la carta ?ca, siempre que esta no haya sido usada anteriormente, disponga de las unidades de materiales
  ; de construcción suficientes, no exceda el número máximo de acciones por jugador (5) y haya al menos una celda
  ; vacía libre.
  (:action CONSTRUIR-HABITACION
    :parameters (?j - jugador ?p - persona ?ca - carta_const_hab ?r - construccion ?ju - junco ?v - vacia ?h - habitacion)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (material-habitacion ?j ?r) (>= (n-recurso ?j ?r) 5) (>= (n-recurso ?j ?ju) 2)
                  (< (n-celdas ?j ?h) 5) (> (n-celdas ?j ?v) 0) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
              (decrease (n-recurso ?j ?r) 5) (decrease (n-recurso ?j ?ju) 2) (increase (n-celdas ?j ?h) 1)
              (decrease (n-celdas ?j ?v) 1) (increase (penalty ?j) 5)(increase (total-cost) 5)  (usada ?ca)))

  ; Con esta acción, el jugador puede reformar todas las habitaciones que haya construido, es decir,
  ; que estas estén construidas de un nuevo material indicado por "transicion-reforma", utilizando la carta ?ca,
  ; siempre que esta no haya sido usada anteriormente y disponga de las unidades de materiales
  ; de construcción suficientes.
  (:action REFORMAR-HABITACIONES
    :parameters (?j - jugador ?p - persona ?ca - carta_ref_hab ?r1 ?r2 - construccion ?ju - junco ?h - habitacion)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (material-habitacion ?j ?r1) (transicion-reforma ?j ?r1 ?r2) (>= (n-recurso ?j ?r2)
                  (* 5 (n-celdas ?j ?h))) (>= (n-recurso ?j ?ju) (* 2 (n-celdas ?j ?h)))
                  (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (decrease (n-recurso ?j ?r2) (* 5 (n-celdas ?j ?h))) (decrease (n-recurso ?j ?ju) (* 5 (n-celdas ?j ?h)))
            (not (material-habitacion ?j ?r1)) (material-habitacion ?j ?r2)
            (increase (total-cost) 2) (increase (penalty ?j) 2) (usada ?ca)))

  ; Con esta acción, el jugador puede crear una nueva persona en su familia, utilizando la carta ?ca,
  ; siempre que esta no haya sido usada anteriormente y disponga de las habitaciones suficientes.
  (:action AMPLIAR-FAMILIA-CON-HABITACION
    :parameters (?j - jugador ?p - persona ?ca - carta_amp_fam ?h - habitacion)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (> (n-celdas ?j ?h) (n-acciones ?j)) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (increase (n-acciones ?j) 1) (increase (penalty ?j) 1) (increase (total-cost) 1)
            (usada ?ca)))

  ; Con esta acción, el jugador puede crear una nueva persona en su familia, utilizando la carta ?ca,
  ; siempre que esta no haya sido usada anteriormente. (No es necesario comprobar el número de habitaciones)
  (:action AMPLIAR-FAMILIA-SIN-HABITACION
    :parameters (?j - jugador ?p - persona ?ca - amp_fam_sin_hab)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (pertenece ?p ?j) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (increase (n-acciones ?j) 1) (increase (penalty ?j) 1)  (increase (total-cost) 1) (usada ?ca)))

  ; Con esta acción, el jugador que tenga el turno actual se convierte en el jugador inicial, es decir,
  ; el que comenzará el siguiente turno, utilizando la carta ?ca, siempre que esta no haya sido usada anteriormente.
  ; (No importa si quien ejecuta esta acción es el jugador inicial, ya que así mantiene su condición)
  (:action CAMBIAR-JUGADOR-INICIAL
    :parameters (?j - jugador ?p - persona ?ca - carta_jug_inicial)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (pertenece ?p ?j) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (jugador-inicial ?j) (increase (penalty ?j) 4) (increase (total-cost) 4) (usada ?ca)))

  ; Con esta acción, el jugador puede obtener una adquisición mayor que sólo requiera un tipo de recurso,
  ; utilizando la carta ?ca, siempre que dicha adquisición mayor no haya sido adquirida por ningún otro
  ; jugador con anterioridad. Las adquisiciones mayores permiten hornear pan.
  (:action OBTENER-ADQ-MAYOR-UN-RECURSO
    :parameters (?j1 ?j2 - jugador ?p - persona ?ca - carta_obt_adq_mayor ?a - adq_mayor ?r - construccion)
    :precondition (and (jugador-actual ?j1) (pertenece ?p ?j1) (not (= (jugado ?p ?j1) (turno-actual)))
                  (not (= (acciones-realizadas ?j1) (n-acciones ?j1)))
                  (pertenece ?p ?j1) (not (adq-mayor j1 ?a)) (not (adq-mayor j2 ?a))
                  (> (n-recurso ?j1 ?r) (coste-adq-mayor ?a ?r)) (not (is-adq-mayor-doble ?a))
                  (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j1) (turno-actual)) (increase (acciones-realizadas ?j1) 1)
            (adq-mayor j1 ?a) (decrease (n-recurso ?j1 ?r) (coste-adq-mayor ?a ?r))
            (increase (penalty ?j1) 7) (increase (total-cost) 7) (usada ?ca)))

  ; Con esta acción, el jugador puede obtener una adquisición mayor que requiera dos tipos de recurso,
  ; utilizando la carta ?ca, siempre que dicha adquisición mayor no haya sido adquirida por ningún otro
  ; jugador con anterioridad. Las adquisiciones mayores permiten hornear pan.
  (:action OBTENER-ADQ-MAYOR-DOS-RECURSOS
    :parameters (?j1 ?j2 - jugador ?p - persona ?ca - carta_obt_adq_mayor ?a - adq_mayor ?r1 ?r2 - construccion)
    :precondition (and (jugador-actual ?j1) (pertenece ?p ?j1) (not (= (jugado ?p ?j1) (turno-actual)))
                  (not (= (acciones-realizadas ?j1) (n-acciones ?j1)))
                  (pertenece ?p ?j1) (not (adq-mayor j1 ?a)) (not (adq-mayor j2 ?a))
                  (> (n-recurso ?j1 ?r1) (coste-adq-mayor ?a ?r1)) (> (n-recurso ?j1 ?r2) (coste-adq-mayor ?a ?r2))
                  (not (= ?r1 ?r2)) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j1) (turno-actual)) (increase (acciones-realizadas ?j1) 1)
            (adq-mayor j1 ?a) (decrease (n-recurso ?j1 ?r1) (coste-adq-mayor ?a ?r1))
            (decrease (n-recurso ?j1 ?r2) (coste-adq-mayor ?a ?r2)) (increase (penalty ?j1) 7) (increase (total-cost) 7)
            (usada ?ca)))

  ; Con esta acción, el jugador puede hornear pan para obtener unidades de comida usando sólo un cereal,
  ; utilizando la carta ?ca, siempre que disponga de las unidades de cereal necesarias y de alguna adquisición
  ; mayor.
  (:action HORNEAR-PAN-UN-CEREAL
    :parameters (?j - jugador ?p - persona ?a - adq_mayor ?co - comida ?r - cereal ?ca - carta_hornear_pan)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (adq-mayor ?j ?a) (> (n-recurso ?j ?r) 1) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (decrease (n-recurso ?j ?r) 1) (increase (n-recurso ?j ?co) (hornear-pan-1 ?a))
            (increase (penalty ?j) 4) (increase (total-cost) 4) (usada ?ca)))

  ; Con esta acción, el jugador puede hornear pan para obtener unidades de comida usando dos cereales,
  ; utilizando la carta ?ca, siempre que disponga de las unidades de cereal necesarias y de alguna adquisición
  ; mayor que permita este intercambio (no todas permiten permiten utilizar dos cereales).
  (:action HORNEAR-PAN-DOS-CEREALES
    :parameters (?j - jugador ?p - persona ?a - adq_mayor ?co - comida ?r - cereal ?ca - carta_hornear_pan)
    :precondition (and (jugador-actual ?j) (pertenece ?p ?j) (not (= (jugado ?p ?j) (turno-actual)))
                  (not (= (acciones-realizadas ?j) (n-acciones ?j)))
                  (adq-mayor ?j ?a) (> (n-recurso ?j ?r) 2) (not (usada ?ca)))
    :effect (and (assign (jugado ?p ?j) (turno-actual)) (increase (acciones-realizadas ?j) 1)
            (decrease (n-recurso ?j ?r) 1) (increase (n-recurso ?j ?co) (hornear-pan-2 ?a))
            (increase (penalty ?j) 4) (increase (total-cost) 4) (usada ?ca)))

  ; Acción que permite cambiar de jugador cuando aquel que tiene el turno ha realizado todas sus acciones.
  (:action CAMBIAR-JUGADOR
    :parameters (?j1 ?j2 - jugador)
    :precondition (and (jugador-actual ?j1) (= (acciones-realizadas ?j1) (n-acciones ?j1)))
    :effect (and (not (jugador-actual ?j1)) (jugador-actual ?j2)))

  ; Acción que permite cambiar de jugador cuando todos los jugadores han realizado todas sus acciones.
  ; Resetea el número de acciones realizas a 0 para que puedan volver a jugar el turno siguiente.
  (:action CAMBIAR-TURNO
    :parameters (?j1 ?j2 - jugador)
    :precondition (and (jugador-inicial ?j1) (= (acciones-realizadas ?j1) (n-acciones ?j1))
                  (= (acciones-realizadas ?j2) (n-acciones ?j2)) (not (= ?j1 ?j2))
                  (jugador-inicial ?j1))
    :effect (and (increase (turno-actual) 1) (not (jugador-actual ?j2)) (jugador-actual ?j1)
            (assign (acciones-realizadas ?j1) 0) (assign (acciones-realizadas ?j2) 0)))

  ; Acción que permite terminar el juego cuando todos los jugadores han realizado todas sus acciones
  ; en el último turno marcado por el fichero de problema.
  (:action TERMINAR
    :parameters (?j1 ?j2 - jugador)
    :precondition (and (= (turno-actual) (ultimo-turno)) (not (juego-terminado))
                  (= (acciones-realizadas ?j1) (n-acciones ?j1))
                  (= (acciones-realizadas ?j2) (n-acciones ?j2)) (not (= ?j1 ?j2)))
    :effect (juego-terminado)))
