; INGENIERÍA DEL CONOCIMIENTO - PRÁCTICA 2: AGRICOLA (PDDL)
; Autoras: Alba María García García & Irene Martínez Castillo, Grupo 83

; Problema 5: 2 personas y 3 turnos. Empieza con 10 recursos de cada material de construcción.
(define (problem problema-1)
(:domain agricola)
(:objects
  j1 j2 - jugador
  p1-1 p1-2 p1-3 p2-1 p2-2 p2-3 - persona
  vacia-1 vacia-2 - vacia
  habitacion-1 habitacion-2 - habitacion
  campo-1 campo-2 - campo
  pasto-1 pasto-2 - pasto
  comida-1 comida-2 - comida
  madera-1  madera-2 - madera
  adobe-1 adobe-2 - adobe
  piedra-1 piedra-2 - piedra
  junco-1 junco-2 - junco
  cereal-1 cereal-2 - cereal
  hortaliza-1 hortaliza-2 - hortaliza
  alimento-1 alimento-2 - alimento
  oveja-1 oveja-2 - oveja
  jabali-1 jabali-2 - jabali
  vaca-1 vaca-2 - vaca
  una-oveja un-jabali una-vaca - obtener_animal
  coger-un-cereal jornalero tres-maderas un-adobe un-junco pescar una-piedra-1 coger-una-hortaliza una-piedra-2 - obtener_no_animal
  arar-un-campo arar-un-campo-y-o-sembrar - carta_arar
  sembrar-y-o-hornear-pan arar-un-campo-y-o-sembrar - carta_sembrar
  construir-vallas reformar-y-despues-construir-vallas - carta_vallar
  construir-habitaciones-y-o-establos - carta_const_hab
  reformar-y-despues-una-adquisicion-mayor-o-menor reformar-y-despues-construir-vallas - carta_ref_hab
  ampliar-la-familia-sin-tener-habitaciones-libres - amp_fam_sin_hab
  ampliar-la-familia-y-despues-una-adquisicion-menor - amp_fam_con_hab
  jugador-inicial-y-o-adquisicion-menor - carta_jug_inicial
  una-adquisicion-mayor-o-menor reformar-y-despues-una-adquisicion-mayor-o-menor - carta_obt_adq_mayor
  sembrar-y-o-hornear-pan - carta_hornear_pan
  cocina-1 cocina-2 horno-adobe horno-piedra - adq_mayor)

(:init (jugador-actual j1)
       (pertenece p1-1 j1) (pertenece p1-2 j1) (pertenece p1-3 j1)
       (pertenece p2-1 j2) (pertenece p2-2 j2) (pertenece p1-3 j2)
       (recurso-carta un-jabali jabali-1) (recurso-carta un-jabali jabali-2) (recurso-carta una-vaca vaca-1)
       (recurso-carta una-vaca vaca-2) (recurso-carta una-oveja oveja-1) (recurso-carta una-oveja oveja-2)
       (recurso-carta coger-un-cereal cereal-1) (recurso-carta coger-un-cereal cereal-2)
       (recurso-carta jornalero comida-1) (recurso-carta jornalero comida-2)
       (recurso-carta tres-maderas madera-1) (recurso-carta tres-maderas madera-2) (recurso-carta un-adobe adobe-1) (recurso-carta un-adobe adobe-2)
       (recurso-carta un-junco junco-1) (recurso-carta un-junco junco-2)
       (recurso-carta pescar comida-1) (recurso-carta pescar comida-2)
       (recurso-carta una-piedra-1 piedra-1) (recurso-carta una-piedra-1 piedra-2)
       (recurso-carta coger-una-hortaliza hortaliza-1) (recurso-carta coger-una-hortaliza hortaliza-2)
       (recurso-carta una-piedra-2 piedra-1) (recurso-carta una-piedra-2 piedra-2)
       (is-animal oveja-1) (is-animal oveja-2) (is-animal jabali-1) (is-animal jabali-2)
       (is-animal vaca-1) (is-animal vaca-2)
       (material-habitacion j1 madera-1) (material-habitacion j2 madera-2)
       (transicion-reforma j1 madera-1 adobe-1) (transicion-reforma j2 madera-2 adobe-2)
       (transicion-reforma j1 adobe-1 piedra-1) (transicion-reforma j2 adobe-2 piedra-2)
       (jugador-inicial j1)
       (is-adq-mayor-doble horno-adobe) (is-adq-mayor-doble horno-piedra)

       (= (turno-actual) 1)
       (= (ultimo-turno) 3)
       (= (acciones-realizadas j1) 0) (= (acciones-realizadas j2) 0)
       (= (n-acciones j1) 2) (= (n-acciones j2) 2)
       (= (n-recurso j1 madera-1) 10) (= (n-recurso j2 madera-2) 10) (= (n-recurso j1 adobe-1) 10) (= (n-recurso j2 adobe-2) 10)
       (= (n-recurso j1 junco-1) 10) (= (n-recurso j2 junco-2) 10)
       (= (n-recurso j1 piedra-1) 10) (= (n-recurso j2 piedra-2) 10)
       (= (n-recurso j1 cereal-1) 0) (= (n-recurso j2 cereal-2) 0)
       (= (n-recurso j1 hortaliza-1) 0) (= (n-recurso j2 hortaliza-2) 0) (= (n-recurso j1 oveja-1) 0)
       (= (n-recurso j2 oveja-2) 0) (= (n-recurso j1 jabali-1) 0) (= (n-recurso j2 jabali-2) 0)
       (= (n-recurso j1 vaca-1) 0) (= (n-recurso j2 vaca-2) 0)
       (= (n-celdas j1 vacia-1) 13) (= (n-celdas j2 vacia-2) 13) (= (n-celdas j1 pasto-1) 0)
       (= (n-celdas j2 pasto-2) 0) (= (n-celdas j1 habitacion-1) 2) (= (n-celdas j2 habitacion-2) 2)
       (= (n-celdas j1 campo-1) 0) (= (n-celdas j2 campo-2) 0)
       (= (n-campos-sembrados j1) 0) (= (n-campos-sembrados j2) 0)
       (= (n-pastos-completos j1) 0) (= (n-pastos-completos j2) 0)
       (= (n-vallas j1) 0) (= (n-vallas j2) 0)
       (= (jugado p1-1 j1) 0) (= (jugado p1-2 j1) 0) (= (jugado p1-3 j1) 0) (= (jugado p1-4 j1) 0)
       (= (jugado p2-1 j2) 0) (= (jugado p2-2 j2) 0) (= (jugado p2-3 j2) 0) (= (jugado p2-4 j2) 0)
       (= (coste-adq-mayor cocina-1 adobe-1) 4) (= (coste-adq-mayor cocina-1 adobe-2) 4)
       (= (coste-adq-mayor cocina-2 adobe-1) 4) (= (coste-adq-mayor cocina-2 adobe-2) 4)
       (= (coste-adq-mayor horno-adobe adobe-1) 3) (= (coste-adq-mayor horno-adobe adobe-2) 3)
       (= (coste-adq-mayor horno-adobe piedra-1) 1) (= (coste-adq-mayor horno-adobe piedra-2) 1)
       (= (coste-adq-mayor horno-piedra adobe-1) 1) (= (coste-adq-mayor horno-piedra adobe-2) 1)
       (= (coste-adq-mayor horno-piedra piedra-1) 3) (= (coste-adq-mayor horno-piedra piedra-2) 3)
       (= (hornear-pan-1 cocina-1) 3) (= (hornear-pan-1 cocina-2) 3) (= (hornear-pan-1 horno-adobe) 5)
       (= (hornear-pan-1 horno-piedra) 4) (= (hornear-pan-2 horno-piedra) 8)
       (= (penalty j1) 0) (= (penalty j2) 0)
       (= (total-cost) 0))

(:goal (and (juego-terminado)))

(:metric minimize (total-cost)))
