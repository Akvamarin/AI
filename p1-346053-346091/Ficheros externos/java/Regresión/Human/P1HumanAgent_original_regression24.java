/*
 * Copyright (c) 2012-2013, Moisés Martínez
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/* Añadidos:

- Necesitamos java.io* para poder usar el buffer que escribe en ejemplos.txt 
(BufferedWriter) y el que elimina líneas incompletas (RandomAccessFile).
- Necesitamos java java.util.* para poder crear la queue que guarda la información
 de hace 5 ticks.
- Necesitamos ch.idsia.agents.controllers.Element porque nuestra queue está formada
por Element.
*/

import java.io.*;
import java.util.*;

import ch.idsia.agents.controllers.T3Element;

/**
 * Created by PLG Group.
 * User: Moisés Martínez 
 * Date: Jan 24, 2014
 * Package: ch.idsia.controllers.agents.controllers;
 */
 
/*
*	Este fichero inicial incluye todos los atributos del tercer tutoral, a excepción de la
*	acción, ya que sería información redundante para la clase, que cambia de la distancia 
*	recorrida en X a la acción que toma Mario. También eliminamos la información del futuro,
*	ya que no es necesaria para la clasificación.
*
*	Incluye además, un balanceo de instancias, recogiendo todas aquellas que se correspondan
*	a los siguientes casos:
*	> Mario salta porque tiene delante un obstáculo y está en el aire (necesita un salto más largo para sobrepasarlo).
*	> Mario salta porque delante tiene un enemigo y está en el suelo.
*	> Mario salta porque delante tiene un obstáculo y está en el suelo.
*	> Mario no salta porque no tiene un enemigo delante y está en el suelo.
*	> Mario no salta porque no tiene un obstáculo delante y está en el suelo.
*	Por tanto, el número de instancias de cada una de estas situaciones será el mismo. 
*	
*	No hemos considerado las instancias en las que Mario tiene un enemigo delante y salta estando en 
*	el aire, ya que nos interesa el momento en el que decide saltar ni cuando Mario se mueve y no está en el suelo,
*	ya que sería una situación en la que cae del salto, que no resulta interesante.
*/
public final class P1HumanAgent_original_regression24 extends KeyAdapter implements Agent
{
    
    private boolean[] Action    = null;
    private String Name         = "T3HumanAgent";

    int tick;
	
    /* Nuevas variables globales */
	
	BufferedWriter tFile; /* Buffer necesario para escribir en el fichero de entrenamiento. */
	LinkedList<T3Element> list; /* Linked list necesaria para guardar las instancias sin información futura adjudicada. */
	
	int [] marioPos; /* Esta variable nos indica la posición de Mario en la matriz. */
    
    boolean isBlocked; /* Esta variable nos indica si el agente está bloqueado por algún obstáculo justo delante. */
	boolean isEnemy; /* Esta variable nos indica si Mario tiene un enemigo delante con una anticipación de dos casillas. */
	boolean isEnemyBelow;
	boolean isDanger;
	boolean isCoin;
	
	boolean jumped = false; /* Esta variable nos indica si el agente saltó en el tick anterior. Para el tick 1 su valor es 'false', ya que no hay tick anterior. */
	int isOnGround; /* Esta variable nos indica si el agente se encuentra tocando el suelo en el tick actual. */
	
	String[] line; /* Variable para concatenar todos los atributos de una instancia */
	int marioState; /* Estado de Mario en el tick actual (gana, pierde, otro) */
	
	/* Contadores para las cuatro situaciones donde Mario ganará puntos. */
	int above_enemy_counter;
	int front_coin_counter; 
	int no_enemy_counter;
	int no_coin_counter;
	int front_enemy_counter;

    public P1HumanAgent_original_regression24(){
        this.reset();
		
		/* Inicializamos a 0 los ticks y contadores de instancias. */
        tick = 0;
		
		above_enemy_counter = 0;
		front_coin_counter = 0; 
		no_enemy_counter = 0;
		no_coin_counter = 0;
		front_enemy_counter = 0;
		
		/* Preconfiguración [partes necesarias al iniciarse la ejecución del programa] */
		
		/* 
		
			Linked List heredada del tutorial 3, que usamos en la práctica, pero sin 
			utilidad real, ya que no guardamos ticks, sino que los escribimos al momento.
		
		*/
		list = new LinkedList<T3Element>();
		
		/* Hacemos uso de try-catch para notificar de posibles IOException. */
		try{
			
			/*
				Hemos decidido usar un BufferedWriter distinto para escribir el buffer que el resto de la
				información porque queremos guardar toda la información del header en una misma variable
				y escribir en el fichero una sola vez. Es por esto que su tamaño es ahora de 30000 bytes
				y no los 8192 por defecto.
				
				Por otro lado, necesitamos también un BufferedReader para saber si se ha escrito ya o
				no en el fichero, ya que el header sólo se debe escribir cuando el fichero está vacío. 
			*/
			
			BufferedWriter tFile_header = new BufferedWriter(new FileWriter("entrenamiento_regression_6ticks_3", true), 30000);
			BufferedReader tFile_reader = new BufferedReader(new FileReader("entrenamiento_regression_6ticks_3"));
			
			/*
				Primero comprobamos si el fichero está vacío, es decir, si la primera línea es null.
			*/
			if(tFile_reader.readLine() == null){
				
				/* Obtenemos el header completo a escribir */
				String header = getHeader();
				
				/* Escribimos el header */
				tFile_header.write(header + "\n");
			}
			
			/* 
				Cerramos ambos ficheros, ya que sólo eran necesarios para escribir el header de entrenamiento_regression_6ticks_3 
			*/
			tFile_reader.close();
			tFile_header.close();
			
			/* 
				Hemos decidido usar un BufferedWriter porque queremos que se guarden los
				datos de entrenamiento no sólo al finalizar la partida (cuando cerramos
				el fichero), sino también durante su ejecución. De este modo, cada 8192 bytes
				que recoge el buffer, estos se mandan a escribir al fichero 'entrenamiento_regression_6ticks_3' 
				y el buffer se vacía. Así no perderemos toda la información en el caso de cierre
				abrupto del programa.
				
				Por otro lado, ya que se requiere que el fichero no se escriba de nuevo
				cada vez que se abre, sino añadiendo nueva información a partir de la última
				línea escrita, usamos el argumento 'true' en FileWriter. Este crea el fichero
				'ejemplos.txt' si no existía y escribe desde el final del fichero existente sin 
				sobreescribir datos en el caso contrario.
			*/
			
			tFile = new BufferedWriter(new FileWriter("entrenamiento_regression_6ticks_3", true));
			/* 
				A continuación, creamos un RandomAccessFile; esta elección se debe a que
				para utilizar el 'filepointer' necesitábamos acceder al fichero como si fuera un objeto 
				del tipo RandomAccessFile. Su única función es eliminar
				todo el contenido posterior al último salto de línea.
				
				Por tanto:
				
				- Si la última línea está completa (no hubo un cierre abrupto de programa),
				no se eliminará nada, ya que el último caracter escrito en esos casos es siempre '\n'.
				- Si la última línea no está completa (hubo un cierre abrupto del programa),
				se eliminará la última línea caracter por caracter.
				Todo esto siempre que el fichero no esté vacío. De este modo evitamos guardar información
				incompleta en nuestro fichero.
				
				**A TENER EN CUENTA**:
				Si fue la última ejecución la que tuvo un cierre del programa abrupto, y hemos
				terminado la sesión de entrenamiento, se deberá eliminar manualmente la última
				línea si no queremos información incompleta.
			*/
			
			/* RandomAccessFile que lee y escribe (rw) para buscar y eliminar. */
			RandomAccessFile f = new RandomAccessFile("entrenamiento_regression_6ticks_3", "rw");
			/* La longitud del fichero es length() - 1 porque length() nos lleva a EOF. */
			long length = f.length() - 1;
			/* Este byte lo usaremos para leer caracter a caracter */
			byte b;
			
			if (length >= 0){ /* Si el fichero no está vacío */
				f.seek(length); /* Colocamos el file pointer al final del fichero. */
				b = f.readByte(); /* Leemos el carácter. */
				
				while(b != 10){ /* Mientras que el carácter que estamos leyendo no sea '\n'. */
					f.setLength(length--); /* Quitamos ese último byte del fichero. */
					f.seek(length); /* Colocamos de nuevo el file pointer al final del fichero. */
					b = f.readByte(); /* Volvemos a leer el carácter final. */
				}
				f.close(); /* Una vez eliminada la línea, cerramos RandomAccessFile. */
			}
		}
			
		catch(IOException ex){
			/* Mostramos por el standard output el mensaje de error */
			System.out.println (ex.toString());
		}
		
	}
	
	/**
	*	getHeader(): Este método se encarga de obtener en forma de String todo el texto que necesita el
	*	archivo 'entrenamiento_regression_6ticks_3' para poder procesar los atributos de las instancias en Weka.
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public String getHeader(){
		
		String header = ""; /* Inicializamos la variable a devolver */
		
		header += "@RELATION P1Mario-data-training-T3Original\n\n"; /* Nombre del fichero */
		
		/* 
			Como sólo tenemos que escribir en el header del fichero 7 atributos de MergedObs,
			hemos decidido evitar el for loop que teníamos y escribirlos directamente.
		*/		
		
		header += "@ATTRIBUTE MergedObs[7][9] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
		header += "@ATTRIBUTE MergedObs[8][9] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
		header += "@ATTRIBUTE MergedObs[8][10] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
		header += "@ATTRIBUTE MergedObs[9][10] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
		header += "@ATTRIBUTE MergedObs[9][11] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
		header += "@ATTRIBUTE MergedObs[10][9] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
		header += "@ATTRIBUTE MergedObs[10][10] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
		
		/* NUEVOS ATRIBUTOS */
		
		header += "@ATTRIBUTE distanceEnemy NUMERIC\n"; /* Distancia euclídea al enemigo más cercano. */
		header += "@ATTRIBUTE distanceCoin NUMERIC\n"; /* Distancia euclídea al enemigo más cercano. */	
		
		header += "@ATTRIBUTE isOnGround {Ground, Air}\n"; /* Mario se encuentra pisando o no el suelo. */

		header += "@ATTRIBUTE isDanger {Enemy, NoEnemy}\n";
		header += "@ATTRIBUTE isEnemyBelow {Enemy, NoEnemy}\n";
		header += "@ATTRIBUTE isCoin {Coin, NoCoin}\n";
		header += "@ATTRIBUTE Action {move, jump, move_jump, still}\n";
		header += "@ATTRIBUTE reward NUMERIC\n";
		/* CLASE */
		
		header += "@ATTRIBUTE futureReward NUMERIC\n\n"; /* Acción ejecutada por Mario. */
		header += "@data"; /* A partir de aquí informamos que se encuentran las instancias */
		
		return header; /* Devolvemos nuestro header */
	}
	
	/**
	*	getFutureAttributes(): Este método se encarga de insertar los atributos del futuro
	*	en instancias que contienen información pasada y presente.
	*
	*	@aux: elemento del tick actual, pero del futuro con respecto a instancias guardadas
	*	anteriormente.
	*	@position: diferencia en ticks del tick del pasado al del presente.
	*	@coins: posición del atributo en el que se guardan las monedas de diferencia con el tick de referencia
	*	@enemies: posición del atributo en el que se guardan las enemigos derrotados de diferencia con el tick de referencia
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public void getFutureAttributes(T3Element aux, int position, int intReward){
		
		int id = tick - position; /* Respecto a qué tick debemos escribir la información actual */
			
		int counter = 0; /* Creamos un counter para saber en qué posición se encuentra el tick que buscamos dentro de la linked list */
		boolean stop = false; /* Variable boolena necesaria que indica que ya hemos encontrado dicho tick si es true */
		
		/* Obtenemos el primer elemento de la linked list y lo guardamos en aux2 */
		T3Element aux2;
		aux2 = list.getFirst();
		
		/* Comenzamos el while loop para saber en qué posición se encuentra el tick que buscamos */
		while(!stop){
			if(aux2.id == id){ /* Si el tick coincide */
				
				aux2.line[aux2.line.length - 1] = String.valueOf(intReward); 
				stop = true; /* Una vez lo encontramos, debemos parar de buscar */
			}
			aux2 = list.get(counter++); /* Obtenemos el siguiente elemento en la lista */
		}
	}
	
	public void getTrainingFile (int[] position, int[] marioState, float[] MarioFloatPos, int IntermediateReward, 
								byte[][] mergedObservationZZ, int[] infoEvaluacion, byte[][] levelSceneObservationZ, 
								byte[][] EnemiesObservationZ){
		
		/* 
			A continuación, declaramos e inicializamos las variables que nos indican el número de monedas,
			bloques y enemigos en pantalla, ya que no existe ningún modo de obtenerlas a través de environment.
		*/
		int screenCoins = 0; /* Número de monedas en pantalla */
		int screenBlocks = 0; /* Número de bloques en pantalla */
		int screenEnemies = 0; /* Número de enemigos en pantalla */
		
		this.marioState = marioState[0]; /* Inicializamos el estado de Mario para cada tick */
		
		/* 
			Para comenzar, ya sólo necesitamos la variable que nos guarda las instancias completas de cada tick.
			Se trata de un array de String, ya que todas las instancias tienen el mismo número de atributos, y
			nunca cambian de posición en él. El número total de atributos es 380.
			
			-- ATRIBUTOS --
			>PASADOS
			000: MergedObs[7][9]
			001: MergedObs[8][9]
			002: MergedObs[8][10]
			003: MergedObs[9][10]
			004: MergedObs[9][11]
			005: MergedObs[10][9]
			006: MergedObs[10][10]
			007: distanceEnemy
			008: distanceCoin
			009: isOnGround
			010: isDanger
			011: isEnemyBelow
			012: isCoin
			013: Action
			014: reward
			>CLASE
			015: futureReward
		*/
		line = new String[16];
		int l = 0;
		
		if (tick == 0) /* Como la posición de Mario es constante, sólo necesitamos conocer la posición de Mario en el instante inicial. */
			/* Obtenemos la posición de Mario en la matriz. */
			marioPos = position;
		
		/* 
			A partir de aquí, vamos añadiendo los atributos a su posición del array correspondiente.
			Pasamos a String todas las variables que no lo sean.
		*/
		
		/*
			A continuación, como sólo necesitamos guardar algunas celdas de 
			MergedObs, hemos decidido no incluir estos atributos con un for
			como en los ficheros anteriores, sino uno a uno.
		*/
		
		/* MERGED OBSERVATION */
		
		line[l] = Byte.toString(mergedObservationZZ[7][9]); ++l;
		line[l] = Byte.toString(mergedObservationZZ[8][9]); ++l;
		line[l] = Byte.toString(mergedObservationZZ[8][10]); ++l;
		line[l] = Byte.toString(mergedObservationZZ[9][10]); ++l;
		line[l] = Byte.toString(mergedObservationZZ[9][11]); ++l;
		line[l] = Byte.toString(mergedObservationZZ[10][9]); ++l;
		line[l] = Byte.toString(mergedObservationZZ[10][10]); ++l;

		/* DISTANCIA EUCLÍDEA */
		
		/* 
			Antes de caluclar la distancia euclídea, necesitamos calcular la
			fila y la columna con un enemigo más cercana a Mario.
		*/
		int minColEnemy = mergedObservationZZ.length; /* Las columnas van de 0 a 18, por lo que nunca serán mayores de 19. */
		int minRowEnemy = mergedObservationZZ.length; /* Las filas van de 0 a 18, por lo que nunca serán mayores de 19. */
		int minColCoin = mergedObservationZZ.length; 
		int minRowCoin = mergedObservationZZ.length; 

		for (int i = 0; i < mergedObservationZZ.length; ++i){ /* Traversamos las filas. */
			/* 
				Traversamos las columnas desde la posición de Mario hasta la última a la derecha. 
				(No nos interesan los enemigos que pueda tener detrás, ya que siempre se mueve hacia delante.)
			*/
			for (int j = marioPos[1]; j < mergedObservationZZ.length; ++j){ 
				if(mergedObservationZZ[i][j] == 80){ /* Si en la celda se encuentra un enemigo. */
				/*
					La prioridad a la hora de que un enemigo esté o no más cerca es si la columna
					en la que se encuentra el enemigo es más cercana a Mario, ya que los enemigos
					se mueven horizontalmente.
				*/
					if (minColEnemy>j){ /* Si la columna actual está más cerca que el mínimo hasta la fecha. */
						minColEnemy = j;
						if(minRowEnemy>i) /* Si la fila actual está más cerca que el mínimo hasta la fecha. */
							minRowEnemy=i;
						
					}
				}
				if(mergedObservationZZ[i][j] == 2){ 
					if (minColCoin>j){ 
						minColCoin = j;
						if(minRowCoin>i) 
							minRowCoin=i;	
					}
				}
			}
		}
		
		minColEnemy -= marioPos[0]; /* Restamos la posición de Mario a la columna mínima obtenida. */ 
		/* 
			Hacemos lo mismo con las filas, pero haciendo uso del valor absoluto, ya que el enemigo puede estar
			encima o debajo de Mario, no como en el caso anterior, que sólo podía estar a la derecha.
		*/
		minRowEnemy = Math.abs(minRowEnemy - 9); 

		minColCoin -= marioPos[0];
		minRowCoin = Math.abs(minRowCoin - 9); 

		/* 
			Calculamos la distancia euclídea multiplicando por 100 su valor y quitando los decimales,
			ya que salían demasiados.
		*/
		line[l] = String.valueOf((int)(100*getEuclideanDistance(minColEnemy, minRowEnemy)));++l;
		line[l] = String.valueOf((int)(100*getEuclideanDistance(minColCoin, minRowCoin))); 		

		line[line.length - 2] = String.valueOf(IntermediateReward);

		/* 
			Una vez tenemos todos los elementos del 'pasado', pasamos a crear un elemento
			auxiliar, de manera que lo podamos insertar luego al final de nuestra linked list.
		*/
		

		T3Element aux = new T3Element(tick, line);
		
		list.add(aux);

		/* Si ya hemos pasado 6 ticks... */
		if(tick >= 6){
			
			/* Añadimos la información sobre monedas y enemigos del tick actual al del hace 6 ticks */
			getFutureAttributes(aux, 6, IntermediateReward);
			aux = list.poll();
			boolean coinsChecked = false;
			/*-- ATRIBUTOS --
			>PASADOS
			000: MergedObs[7][9]
			001: MergedObs[8][9]
			002: MergedObs[8][10]
			003: MergedObs[9][10]
			004: MergedObs[9][11]
			005: MergedObs[10][9]
			006: MergedObs[10][10]
			007: distanceEnemy
			008: distanceCoin
			009: isOnGround
			010: isDanger
			011: isEnemyBelow
			012: isCoin
			013: Action
			014: reward
			>CLASE
			015: futureReward*/
			try{
				if (aux.id == 0)
					no_enemy_counter++;
				
				if (aux.line[aux.line.length - 4].equals("Coin")){
					if (front_coin_counter < no_enemy_counter){
						++front_coin_counter; coinsChecked = true;
						for(int i = 0; i<aux.line.length; ++i){
							if(i == aux.line.length - 1)
								tFile.write(aux.line[i] + "\n");
							else
								tFile.write(aux.line[i] + ",");
						}
					}
				}
				
				else if (aux.line[aux.line.length - 4].equals("NoCoin")){
					if (no_coin_counter < no_enemy_counter){
						++no_coin_counter; coinsChecked = true;
						for(int i = 0; i<aux.line.length; ++i){
							if(i == aux.line.length - 1)
								tFile.write(aux.line[i] + "\n");
							else
								tFile.write(aux.line[i] + ",");
						}
					}
				}
				if (!coinsChecked){
					if (aux.line[aux.line.length - 5].equals("Enemy")){
						if (above_enemy_counter < no_enemy_counter){
							++above_enemy_counter;
							for(int i = 0; i<aux.line.length; ++i){
								if(i == aux.line.length - 1)
									tFile.write(aux.line[i] + "\n");
								else
									tFile.write(aux.line[i] + ",");
							}
						}
					}
				
					else if (aux.line[aux.line.length - 6].equals("Enemy")){
						if (front_enemy_counter < no_enemy_counter){
							++front_enemy_counter;
							for(int i = 0; i<aux.line.length; ++i){
								if(i == aux.line.length - 1)
									tFile.write(aux.line[i] + "\n");
								else
									tFile.write(aux.line[i] + ",");
							}
						}
					}

					else if (aux.line[aux.line.length - 6].equals("NoEnemy") && aux.line[aux.line.length - 5].equals("NoEnemy")){
						if (above_enemy_counter == no_enemy_counter && front_coin_counter == no_enemy_counter 
						&& no_coin_counter == no_enemy_counter){
							++no_enemy_counter;
							for(int i = 0; i<aux.line.length; ++i){
								if(i == aux.line.length - 1)
									tFile.write(aux.line[i] + "\n");
							else
								tFile.write(aux.line[i] + ",");
							}
						}
					}
				}
				
				if(marioState[0]== 0 || marioState[0]== 1)
					tFile.close(); /* Cerramos el fichero. */

			}
			catch(IOException ex){
				/* Mostramos por el standard output el mensaje de error. */
				System.out.println (ex.toString());
			}
		}
		/* Si ya hemos pasado 12 ticks... */
/*		if(tick >= 12){
			
			/* Añadimos la información sobre monedas y enemigos del tick actual al del hace 12 ticks */
/*			getFutureAttributes(aux, 12, 388);
			
		}
		
		/* Si ya hemos pasado 24 ticks... */
/*		if(tick >= 24){
			
			/* Añadimos la información sobre monedas y enemigos del tick actual al del hace 24 ticks */
/*			getFutureAttributes(aux, 24, 390);
			
			/* 
				Obtenemos el primer elemento de nuestra lista, que es el acaba de recibir la información
				futura de dentro de 24 ticks, por lo que su instancia ya está completa y se puede escribir en 
				el fichero.
			*/
			
/*			aux = list.poll();
			
			/* Hacemos uso de try-catch para notificar de posibles IOException. */
/*			try{
				/* 
					Escribimos cada atributo de la instancia separado por comas.
					En el caso del último atributo, la clase, escribimos un salto de línea,
					ya que cada instancia se coloca en una línea diferente.
				*/
/*				for(int i = 0; i<aux.line.length; ++i){
					/* Último atributo */
/*					if(i == aux.line.length - 1)
						tFile.write(aux.line[i] + "\n");
					/* El resto */
/*					else
						tFile.write(aux.line[i] + ",");
				}
				
				/* En el caso de que Mario haya muerto (marioState[0] == 0) o que haya
				ganado la partida (marioState[0] == 1, cerramos el archivo para que se
				escriba la información que quedaba por mandar en el buffer. */
			
/*				if(marioState[0]== 0 || marioState[0]== 1)
					tFile.close(); /* Cerramos el fichero. */
/*			}
		
			catch(IOException ex){
				/* Mostramos por el standard output el mensaje de error. */
/*				System.out.println (ex.toString());
			}
		}*/
		
	}
	
	/**
	*	getEuclideanDistance(): Este método se encarga de calcular la distancia euclídea
	*	usando las distancias relativas a y b a un cierto punto del plano.
	*
	*	@int a: distancia en el eje x.
	*	@int b: distancia en el eje y.
	*	
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public double getEuclideanDistance(int a, int b){
		double d;
		/* Fórmula de la distancia euclídea. */
		d = Math.sqrt((a)*(a) + (b)*(b));
		
		return d;
	}
	
    @Override
    public String getName() { return Name; }

    @Override
    public void setName(String name) { Name = name; }
	
	/**
	*	checkConditions(): Este método se encarga de obtener los valores de las variables globales isBlocked,
	*	isEnemy e isOnGround, tomando la información de la variable environment. Estas variables globales
	*	son necesarias para determinar la acción de Mario en el próximo tick.
	*
	*	@byte[][] mergedObservation: información sobre el escenario y los enemigos que rodean a Mario.
	*	@int[] marioState: estado de Mario.
	*   @int[] position: posición de Mario en la matriz.
	*	
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public void checkConditions(byte[][] mergedObservation, int[] marioState){
		
		T3Element aux = new T3Element(tick, line);

		/* 
			Comprobamos para la celda justo en frente de Mario si hay algún tipo de obstáculo que lo pudiera bloquear.
			Consideramos aquí a los pinchones porque no son enemigos a los que se les pueda matar. 
		*/
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == -60 || mergedObservation[marioPos[0]][marioPos[1] + 1] == -85 || 
		   mergedObservation[marioPos[0]][marioPos[1] + 1] == -24 || mergedObservation[marioPos[0]][marioPos[1] + 1] == -62 || 
		   mergedObservation[marioPos[0]][marioPos[1] + 1] == 93){
			isBlocked = true;	
		}

		/* Comprobamos para la celda justo en frente de Mario y la posterior si hay algún enemigo. */
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 2){
        	isCoin = true;
		}
		/* Añadimos el atributo isEnemy en función de la variable isEnemy. */
		aux.line [aux.line.length - 4] = isCoin? "Coin" : "NoCoin";


		if(mergedObservation[marioPos[0] + 1][marioPos[1]] == 80 || mergedObservation[marioPos[0] - 1][marioPos[1] + 1] == 80){
        	isEnemyBelow = true;
		}
		/* Añadimos el atributo isEnemy en función de la variable isEnemy. */
		aux.line [aux.line.length - 5] = isEnemyBelow? "Enemy" : "NoEnemy";
		
		if(mergedObservation[marioPos[0] + 1][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 1] == 80){
        	isDanger = true;
		}
		/* Añadimos el atributo isEnemy en función de la variable isEnemy. */
		aux.line [aux.line.length - 6] = isDanger? "Enemy" : "NoEnemy";

		/* Comprobamos para la celda justo en frente de Mario y la posterior si hay algún enemigo. */
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 80)
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 80){
        	isEnemy = true;
		}

		/* Comprobamos si Mario se encuentra pisando el suelo. */
		isOnGround = marioState[2];
		
		/* Añadimos el atributo isOnGround en función de la variable isOnGround. */
		aux.line [aux.line.length - 7] = isOnGround==1? "Ground" : "Air";
	}

    @Override
    public boolean[] getAction() {
		
    	T3Element aux = new T3Element(tick, line);
			
		/* Se resetean todas las condiciones para elegir la acción en el siguiente tick. */
		isBlocked = false;
		isEnemy = false;
		isEnemyBelow = false;
		isDanger = false;
		isCoin = false;
				
		if(Action[Mario.KEY_RIGHT]){
    		if(Action[Mario.KEY_JUMP]){
    			aux.line [aux.line.length - 3] = "move_jump";
    		}
			else{
    			aux.line [aux.line.length - 3] = "move";
    		}
    	}
		else if (Action[Mario.KEY_JUMP]){
    		aux.line [aux.line.length - 3] = "jump";
    	}
		else{
			aux.line [aux.line.length - 3] = "still";
		}

		/* Se registra si Mario saltó o no en este tick para usarlo en el tick posterior. */
		jumped = Action[3];	
			
		/* Actualizamos el tick actual */
		tick++;
		
        return Action;
    }
	
	@Override
    public void integrateObservation(Environment environment){
		
		/* Debemos llamar al método getTrainingFile() por cada tick del juego para guardar su información correspondiente. */
        getTrainingFile(environment.getMarioEgoPos(), environment.getMarioState(), environment.getMarioFloatPos(), environment.getIntermediateReward(),
						environment.getMergedObservationZZ(1,1), environment.getEvaluationInfoAsInts(), environment.getLevelSceneObservationZ(1),
						environment.getEnemiesObservationZ(2));
		
		/* A continuación, comprobamos los valores en el tick actual para las condiciones necesarias para calcular*
		la siguiente acción del agente. */
		checkConditions(environment.getMergedObservationZZ(1, 1), environment.getMarioState());
    }
	
    @Override
    public void giveIntermediateReward(float intermediateReward){
    }

    @Override
    public void reset()
    {
        Action = new boolean[Environment.numberOfKeys];
    }

    @Override
    public void setObservationDetails(final int rfWidth, final int rfHeight, final int egoRow, final int egoCol)
    {
    }

    public boolean[] getAction(Environment observation)
    {
        return Action;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        toggleKey(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        toggleKey(e.getKeyCode(), false);
    }


    private void toggleKey(int keyCode, boolean isPressed)
    {	
	
		/* 
			Haciendo uso de este método, vamos a determinar qué teclas presiona
			el usuario para así determinar la acción de Mario. Aquellas teclas
			darán lugar a un valor 'true' en el vector de acciones de Mario.
			Por defecto, todas las acciones de todas las instancias son false, 
			así que aquí sobreescribimos algunas con 'true'.
		*/
		
        switch (keyCode)
        {
            case KeyEvent.VK_LEFT:
                Action[Mario.KEY_LEFT] = isPressed;
                break;
				
            case KeyEvent.VK_RIGHT:
                Action[Mario.KEY_RIGHT] = isPressed;
                break;
				
            case KeyEvent.VK_DOWN:
                Action[Mario.KEY_DOWN] = isPressed;
                break;
				
            case KeyEvent.VK_UP:
                Action[Mario.KEY_UP] = isPressed;
                break;

            case KeyEvent.VK_S:
                Action[Mario.KEY_JUMP] = isPressed;
                break;
				
            case KeyEvent.VK_A:
                Action[Mario.KEY_SPEED] = isPressed;
                break;
        }
		
    }

}



