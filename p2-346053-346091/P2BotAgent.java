/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
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

import java.util.Random;

/* Añadidos:

- Necesitamos java.io* para poder usar el buffer que escribe en entrenamiento_ibl_bot.txt 
(BufferedWriter) y el que elimina líneas incompletas (RandomAccessFile).
- Necesitamos java java.util.* para poder crear la queue que guarda la información
 de hace 6 o 24 ticks.
- Necesitamos ch.idsia.agents.controllers.P2Element porque nuestra cola está formada
por objetos P2Element.
*/
import java.io.*; 
import java.util.*; 
import java.lang.Math;

import ch.idsia.agents.controllers.P2Element;

public class P2BotAgent extends BasicMarioAIAgent implements Agent {

    int tick;
	private Random R = null;
	
	/* Nuevas variables globales */
	
	BufferedWriter tFile; /* Buffer necesario para escribir en el fichero de entrenamiento. */
	LinkedList<P2Element> list; /* Linked list necesaria para guardar las instancias sin información futura adjudicada. */
	
	int [] marioPos; /* Esta variable nos indica la posición de Mario en la matriz. */
    
	/* ATRIBUTOS PARA COMPARACIÓN */
	
	/* INTERMEDIATE REWARD */
	String isEnemy; /* Esta variable nos indica si Mario tiene un enemigo 'cerca' (hasta 2 celdas de distancia). */
	String isCoinMushroom; /* Esta variable nos indica si Mario tiene una moneda o champiñón 'cerca' (hasta 2 celdas de distancia). */
	
	/* AVANZAR */
	boolean jumped = false; /* Esta variable nos indica si el agente saltó en el tick anterior. Para el tick 1 su valor es 'false', ya que no hay tick anterior. */
	boolean isOnGround; /* Esta variable nos indica si el agente se encuentra tocando el suelo en el tick actual. */
	boolean isBlocked; /* Esta variable nos indica si Mario se encuentra bloqueado en la casilla enfrente de él. */
	
	/* No hace falta declarar atributos para evalución, ya que se obtienen de integrateObservation */
	
	/* RESULTADOS FUNC. PERTENENCIA Y EVALUACIÓN */
	int evaluation; /* Evaluación de la instancia con respecto a sus datos futuros. */
	int situation; /* Situación a la que pertenece la instancia. */
	
	/* OTRAS */
	boolean isEnemyFront; /* Variable auxiliar para saber si hay enemigos, ya que nuestro Mario original sólo miraba si éstos se encontraban frente a él. */
	
	String[] line; /* Variable para concatenar todos los atributos de una instancia */
	int marioState; /* Estado de Mario en el tick actual (gana, pierde, otro) */
	
	/* CONTADORES */
	int near_coin_mushroom_counter; /* Moneda/champiñón cerca. */
	int near_enemy_counter; /* Enemigo cerca. */
	int is_blocked_counter; /* Mario está bloqueado. */
	int no_blocked_counter; /* Mario no está bloqueado. */

    public P2BotAgent(){
        super("BaselineAgent");
        reset();
		
		/* Inicializamos a 0 los ticks y contadores de instancias. */
        tick = 0;
		
		near_coin_mushroom_counter = 0;
		near_enemy_counter = 0; 
		is_blocked_counter = 0;
		no_blocked_counter = 0;
		
		
		/* Preconfiguración [partes necesarias al iniciarse la ejecución del programa] */
		
		/* 
		
			Linked List heredada del tutorial 3, que usamos en la práctica, pero sin 
			utilidad real, ya que no guardamos ticks, sino que los escribimos al momento.
		
		*/
		list = new LinkedList<P2Element>();
		
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
			
			BufferedWriter tFile_header = new BufferedWriter(new FileWriter("base_conocimiento_bot.arff", true), 30000);
			BufferedReader tFile_reader = new BufferedReader(new FileReader("base_conocimiento_bot.arff"));
			
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
				Cerramos ambos ficheros, ya que sólo eran necesarios para escribir el header de P1BotAgent_ejemplos.arff 
			*/
			tFile_reader.close();
			tFile_header.close();
			
			/* 
				Hemos decidido usar un BufferedWriter porque queremos que se guarden los
				datos de entrenamiento no sólo al finalizar la partida (cuando cerramos
				el fichero), sino también durante su ejecución. De este modo, cada 8192 bytes
				que recoge el buffer, estos se mandan a escribir al fichero 'P1BotAgent_ejemplos.arff' 
				y el buffer se vacía. Así no perderemos toda la información en el caso de cierre
				abrupto del programa.
				
				Por otro lado, ya que se requiere que el fichero no se escriba de nuevo
				cada vez que se abre, sino añadiendo nueva información a partir de la última
				línea escrita, usamos el argumento 'true' en FileWriter. Este crea el fichero
				'ejemplos.txt' si no existía y escribe desde el final del fichero existente sin 
				sobreescribir datos en el caso contrario.
			*/
			
			tFile = new BufferedWriter(new FileWriter("base_conocimiento_bot.arff", true));
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
			RandomAccessFile f = new RandomAccessFile("base_conocimiento_bot.arff", "rw");
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
			}
			
			f.close(); /* Una vez eliminada la línea, cerramos RandomAccessFile. */
		}
			
		catch(IOException ex){
			/* Mostramos por el standard output el mensaje de error */
			System.out.println (ex.toString());
		}
		
	}

    public void reset() {
        // Dummy reset, of course, but meet formalities!
        R = new Random();
    }
	
	/**
	*	getHeader(): Este método se encarga de obtener en forma de String todo el texto que necesita el
	*	archivo 'P1BotAgent_ejemplos.arff' para poder procesar los atributos de las instancias en Weka.
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public String getHeader(){
		
		String header = ""; /* Inicializamos la variable a devolver */
		
		header += "@relation P2Mario-data-training-Bot\n\n"; /* Nombre del fichero */
				
		
		/* COMPARACIÓN */
		header += "@attribute isEnemy {Front, Up, Behind, None}\n";
		header += "@attribute isCoinMushroom {Front, Up, Behind, None}\n";
		header += "@attribute isBlocked {true, false}\n";
		header += "@attribute isOnGround {true, false}\n";
		header += "@attribute jumped {true, false}\n";
		
		/* EVALUACIÓN */
		header += "@attribute distancePassedPhys NUMERIC\n";
		header += "@attribute marioStatus NUMERIC\n";
		header += "@attribute coinsGained NUMERIC\n";
		header += "@attribute mushroomsDevoured NUMERIC\n";
		header += "@attribute killsByStomp NUMERIC\n";
		
		/* ACCIÓN */
		header += "@attribute action {move_right, move_left, jump, jump_right, jump_left, still}\n";
		
		/* RESULTADOS EVALUACIÓN / PERTENENCIA */
		header += "@attribute evaluation NUMERIC\n";
		header += "@attribute situation {coin_mushroom, enemy, blocked, no_blocked}\n\n";
		
		header += "@data"; /* A partir de aquí informamos que se encuentran las instancias */
		
		return header; /* Devolvemos nuestro header */
	}



	/**
	*	evaluate(): Este método se encarga de obtener una instancia y, a través de sus atributos
	*	de evaluación, obtener su evaluación correspondiente.
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public String evaluate(String[] instance) {
    /*	
    	//COMPARACIÓN//
		0.header += "@ATTRIBUTE isEnemy {Front, Up, Behid, None}\n";
		1.header += "@ATTRIBUTE isCoinMushroom {Front, Up, Behid, None}\n";
		2.header += "@ATTRIBUTE isBlocked {true, false}\n";
		3.header += "@ATTRIBUTE isOnGround {true, false}\n";
		4.header += "@ATTRIBUTE jumped {true, false}\n";
		
		//EVALUACIÓN//
		5.header += "@ATTRIBUTE distancePassedPhys NUMERIC\n";
		6.header += "@ATTRIBUTE marioStatus NUMERIC\n";
		7.header += "@ATTRIBUTE coinsGained NUMERIC\n";
		8.header += "@ATTRIBUTE mushroomsDevoured NUMERIC\n";
		9.header += "@ATTRIBUTE killsByStomp NUMERIC\n";
	*/
		double eval; /* Aquí guardaremos el resultado de la función de evaluación */
		
		/* La fórmula es diferente dependiendo del valor de marioStatus */
		if(Integer.parseInt(instance[6]) < 0) //If marioStatus < 0
		//50*(distancePassedCells/33) + 2*17*marioStatus + 6*coinsGained + 23*mushroomsDevoured + 4*killsByStomp
			eval = 50*Double.parseDouble(instance[5])/33 + 2*17*Double.parseDouble(instance[6]) + 
				6*Double.parseDouble(instance[7]) + 23*Double.parseDouble(instance[8]) + 4*Double.parseDouble(instance[9]);
		else
			//50*(distancePassedCells/33) + 6*coinsGained + 23*mushroomsDevoured + 4*killsByStomp
			eval = 50*Double.parseDouble(instance[5])/33 + 
				6*Double.parseDouble(instance[7]) + 23*Double.parseDouble(instance[8]) + 4*Double.parseDouble(instance[9]);
		
		/* Devolvemos el valor de la función de evaluación sólo con dos decimales */
		String evaluation = String.valueOf(Math.floor(eval * 100) / 100);
     	
		return evaluation;
    }




	
	/**
	*	getFutureAttributes(): Este método se encarga de insertar los atributos del futuro
	*	en instancias que contienen información pasada y presente.
	*
	*	@aux: elemento del tick actual, pero del futuro con respecto a instancias guardadas
	*	anteriormente.
	*	@position: diferencia en ticks del tick del pasado al del presente.
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public void getFutureAttributes(P2Element aux, int position){
		
		int id = tick - position; /* Respecto a qué tick debemos escribir la información actual */
			
		int counter = 0; /* Creamos un counter para saber en qué posición se encuentra el tick que buscamos dentro de la linked list */
		boolean stop = false; /* Variable boolena necesaria que indica que ya hemos encontrado dicho tick si es true */
		
		/* Obtenemos el primer elemento de la linked list y lo guardamos en aux2 */
		P2Element aux2;
		aux2 = list.getFirst();
		
		/* Comenzamos el while loop para saber en qué posición se encuentra el tick que buscamos */
		while(!stop){
			if(aux2.id == id){ /* Si el tick coincide */
				
				/* Los atributos de evaluación se obtienen con una resta de los valores a tener en cuenta para n y n+6 ticks */
				aux2.line[aux2.line.length - 8] = String.valueOf(Integer.parseInt(line[line.length - 8]) - Integer.parseInt(aux2.line[aux2.line.length - 8]));
				aux2.line[aux2.line.length - 7] = String.valueOf(Integer.parseInt(line[line.length - 7]) - Integer.parseInt(aux2.line[aux2.line.length - 7]));
				aux2.line[aux2.line.length - 6] = String.valueOf(Integer.parseInt(line[line.length - 6]) - Integer.parseInt(aux2.line[aux2.line.length - 6]));
				aux2.line[aux2.line.length - 5] = String.valueOf(Integer.parseInt(line[line.length - 5]) - Integer.parseInt(aux2.line[aux2.line.length - 5]));
				aux2.line[aux2.line.length - 4] = String.valueOf(Integer.parseInt(line[line.length - 4]) - Integer.parseInt(aux2.line[aux2.line.length - 4]));
				stop = true; /* Una vez lo encontramos, debemos parar de buscar */
			}
			aux2 = list.get(counter++); /* Obtenemos el siguiente elemento en la lista */
		}
	}

	/**
	*	getTrainingFile(): Este método se encarga de guardar la información 
	*	requerida de cada tick en una línea del fichero 'P1BotAgent_ejemplos.arff'. Decidimos pasar la información
	*   por parámetros distintos para no tener que ejecutar los métodos de los que provienen otra vez aquí.
	*
	*	@int[] position: posición de Mario relativa a la matriz.
	*	@int[] marioState: estado de Mario.
	*	@float[] MarioFloatPos: posición de Mario en los ejes X e Y.
	*	@int IntermediateReward: recompensa conseguida en el punto actual.
	*	@byte[][] mergedObservationZZ: información sobre el escenario y los enemigos que rodean a Mario.
	*	@int[] infoEvaluacion: información de evaluación de Mario.
	*	@byte[][] levelSceneObservationZ: información sobre el escenario que rodea a Mario.
	*	@byte[][] EnemiesObservationZ: información sobre los enemigos que rodean a Mario.
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public void getTrainingFile (int[] marioState, float[] MarioFloatPos, int IntermediateReward, 
								byte[][] mergedObservationZZ, int[] infoEvaluacion, byte[][] levelSceneObservationZ, 
								byte[][] EnemiesObservationZ){
		
		this.marioState = marioState[0]; /* Inicializamos el estado de Mario para cada tick */
		
		/* 
			Para comenzar, ya sólo necesitamos la variable que nos guarda las instancias completas de cada tick.
			Se trata de un array de String, ya que todas las instancias tienen el mismo número de atributos, y
			nunca cambian de posición en él. El número total de atributos es 13.
			
			-- ATRIBUTOS --
			>COMPARACIÓN
			000: isEnemy
			001: isCoinMushroom
			002: isBlocked
			003: isOnGround
			004: jumped
			>EVALUACIÓN
			005: distancePassedPhys
			006: marioStatus
			007: coinsGained
			008: mushroomsDevoured
			009: killsByStomp
			>ACCIÓN
			010: action
			>RESULTADOS EVALUACIÓN / PERTENENCIA
			011: evaluation
			06: situation
		*/
		line = new String[13]; /* Instancia completa */
		int l = 0; /* Contador para los atributos */ 
		
		/* Obtenemos los atributos de pertenencia / comparación */
		line[l] = isEnemy; ++l;
		line[l] = isCoinMushroom; ++l;
		line[l] = Boolean.toString(isBlocked); ++l;
		line[l] = Boolean.toString(isOnGround); ++l;
		line[l] = Boolean.toString(jumped); ++l;
		
		/* Obtenemos los atributos de evaluación */
		line[l] = Integer.toString(infoEvaluacion[1]); ++l; //distancePassedPhys
		line[l] = Integer.toString(marioState[1]); ++l; //marioMode
		line[l] = Integer.toString(infoEvaluacion[10]); ++l; //coinsGained
		line[l] = Integer.toString(infoEvaluacion[9]); ++l; //mushroomsDevoured
		line[l] = Integer.toString(infoEvaluacion[5]); //killsByStomp

		/* 
			Una vez tenemos todos los elementos del 'pasado', pasamos a crear un elemento
			auxiliar, de manera que lo podamos insertar luego al final de nuestra linked list.
		*/
		P2Element aux = new P2Element(tick, line);
		
		list.add(aux);

		/* Si ya hemos pasado 6 ticks... */
		if(tick >= 6){
			
			/* Añadimos la información sobre intermediateReward del tick actual al del hace 6 ticks */
			getFutureAttributes(aux, 6);
			aux = list.poll();

			try{
			
			/* El primer tick siempre es Mario cayendo del cielo, por lo que no
			es interesante guardarlo. Sin embargo, incrementamos en uno no_blocked_counter
			para poder empezar a guardar instancias. 
			
			Las situaciones no son mutualmente excluyentes, por eso usamos siempre if
			y no else if. Para evitar que una instancia se clasifique en más de un tipo de
			situación, hemos creado la variable 'visited'. Esta pasa a true cuando la
			instancia que estamos analizando ya ha sido escrita.
			*/
			
			boolean visited = false;
			
			if (tick == 6)
				no_blocked_counter++; /* Actualizar contador. */
			
			/* Caso 1: Mario tiene una moneda o champiñón 'cerca' (hasta 2 celdas adyacentes). */
			if(!aux.line[1].equals("None")){
				
				/* Sólo insertamos esta instancia si todavía no hemos
				igualado a no_blocked_counter para este caso. */
				if (near_coin_mushroom_counter < no_blocked_counter){
					
					visited = true;
					
					near_coin_mushroom_counter++; /* Actualizar contador. */
					
					aux.line[aux.line.length - 1] = "coin_mushroom"; /* Adjudicamos la situación */

					aux.line[11] = evaluate(aux.line); /* Añadimos la evaluación */
					
					/* 
						Escribimos cada atributo de la instancia separado por comas.
						En el caso del último atributo, la clase, escribimos un salto de línea,
						ya que cada instancia se coloca en una línea diferente.
					*/
					
					for(int i = 0; i<aux.line.length; ++i){
					/* Último atributo */
					if(i == aux.line.length - 1)
						tFile.write(aux.line[i] + "\n");
					/* El resto */
					else
						tFile.write(aux.line[i] + ",");
					}
				}
			}
			
			/* Caso 2: Mario tiene un enemigo 'cerca' (hasta 2 celdas adyacentes). */
			if (!aux.line[0].equals("None") && !visited){
				
				/* Sólo insertamos esta instancia si todavía no hemos
				igualado a no_blocked_counter para este caso. */
				if (near_enemy_counter < no_blocked_counter){
						
					near_enemy_counter++; /* Actualizar contador. */
					
					aux.line[aux.line.length - 1] = "enemy"; /* Adjudicamos la situación */

					aux.line[11] = evaluate(aux.line); /* Añadimos la evaluación */
					
					/* 
						Escribimos cada atributo de la instancia separado por comas.
						En el caso del último atributo, la clase, escribimos un salto de línea,
						ya que cada instancia se coloca en una línea diferente.
					*/
					
					for(int i = 0; i<aux.line.length; ++i){
					/* Último atributo */
					if(i == aux.line.length - 1)
						tFile.write(aux.line[i] + "\n");
					/* El resto */
					else
						tFile.write(aux.line[i] + ",");
					}
				}
			}
			
			/* Caso 3: Mario está bloqueado justo enfrente. */
			if (aux.line[2].equals("true") && !visited){
				
				/* Sólo insertamos esta instancia si todavía no hemos
				igualado a no_blocked_counter para este caso. */
				if (is_blocked_counter < no_blocked_counter){
						
					is_blocked_counter++; /* Actualizar contador. */
					
					aux.line[aux.line.length - 1] = "blocked"; /* Adjudicamos la situación */

					aux.line[11] = evaluate(aux.line); /* Añadimos la evaluación */
						
					/* 
						Escribimos cada atributo de la instancia separado por comas.
						En el caso del último atributo, la clase, escribimos un salto de línea,
						ya que cada instancia se coloca en una línea diferente.
					*/
						
					for(int i = 0; i<aux.line.length; ++i){
					/* Último atributo */
					if(i == aux.line.length - 1)
						tFile.write(aux.line[i] + "\n");
					/* El resto */
					else
						tFile.write(aux.line[i] + ",");
					}
				}
			}
			
			/* Caso 4: Mario no está bloqueado justo enfrente. */
			if(aux.line[2].equals("false") && !visited) {
				
				/* Sólo escribimos esta instancia (que es mucho más común) cuando tenemos el mismo número
				de cada una de las otras tres instancias minorarias que esta.*/
				if (near_coin_mushroom_counter == no_blocked_counter && near_enemy_counter == no_blocked_counter &&
					is_blocked_counter == no_blocked_counter){
					
					no_blocked_counter++; /* Actualizar contador. */
					
					aux.line[aux.line.length - 1] = "no_blocked"; /* Adjudicamos la situación */

					aux.line[11] = evaluate(aux.line); /* Añadimos la evaluación */
					
					/* 
						Escribimos cada atributo de la instancia separado por comas.
						En el caso del último atributo, la clase, escribimos un salto de línea,
						ya que cada instancia se coloca en una línea diferente.
					*/
					
					for(int i = 0; i<aux.line.length; ++i){
					/* Último atributo */
					if(i == aux.line.length - 1)
						tFile.write(aux.line[i] + "\n");
					/* El resto */
					else
						tFile.write(aux.line[i] + ",");
					}
				}
			}
				
			/* En el caso de que Mario haya muerto (marioState[0] == 0) o que haya
			ganado la partida (marioState[0] == 1, cerramos el archivo para que se
			escriba la información que quedaba por mandar en el buffer. */
			
			if(this.marioState == 0 || this.marioState == 1)
				tFile.close(); /* Cerramos el fichero. */
			}
		
			catch(IOException ex){
				/* Mostramos por el standard output el mensaje de error. */
				System.out.println (ex.toString());
			}
		}
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

    public void integrateObservation(Environment environment){
        // IMPORTANTE: Si se utilizan métodos que tardan mucho como println, cada tick puede tardar en procesarse más de
        // de lo que permite la competición de Mario AI. Si el agente es demasiado lento procesando y el simulador no
        // puede funcionar en tiempo real, se cerrará automáticamente, lor lo que se insta a que el código escrito sea
        // lo más eficiente posible.
        
        
        // INFORMACION DEL ENTORNO
        
        // En la interfaz Environment.java vienen definidos los metodos que se pueden emplear para recuperar informacion
        // del entorno de Mario. Algunos de los mas importantes (y que utilizaremos durante el curso)...
		
		
        //System.out.println("------------------ TICK " + tick + " ------------------");

        
        // Devuelve un array de 19x19 donde Mario ocupa la posicion 9,9 con informacion de los T3Elementos
        // en la escena. La funcion getLevelSceneObservationZ recibe un numero para indicar el nivel de detalle
        // de la informacion devuelta. En uno de los anexos del tutorial 1 se puede encontrar informacion de 
        // los niveles de detalle y el tipo de informacion devuelta.
	/*  System.out.println("\nESCENA");
        byte [][] envesc;
        envesc = environment.getLevelSceneObservationZ(1);
        for (int mx = 0; mx < envesc.length; mx++){
            System.out.print(mx + ": [");
            for (int my = 0; my < envesc[mx].length; my++)
                System.out.print(envesc[mx][my] + " ");

            System.out.println("]");
        }
        
       
       
        // Devuelve un array de 19x19 donde Mario ocupa la posicion 9,9 con informacion de los enemigos
        // en la escena. La funcion getEnemiesObservationZ recibe un numero para indicar el nivel de detalle
        // de la informacion devuelta. En uno de los anexos del tutorial 1 se puede encontrar informacion de 
        // los niveles de detalle y el tipo de informacion devuelta.
        System.out.println("\nENEMIGOS");
        byte [][] envenm;
        envenm = environment.getEnemiesObservationZ(1);
        for (int mx = 0; mx < envenm.length; mx++) {
            System.out.print(mx + ": [");
            for (int my = 0; my < envenm[mx].length; my++)
                System.out.print(envenm[mx][my] + " ");
            
            System.out.println("]");
        }
        
        
        
        // Devuelve un array de 19x19 donde Mario ocupa la posicion 9,9 con la union de los dos arrays
        // anteriores, es decir, devuelve en un mismo array la informacion de los T3Elementos de la
        // escena y los enemigos.
        System.out.println("\nMERGE");
        byte [][] env;
        env = environment.getMergedObservationZZ(1, 1); 
        for (int mx = 0; mx < env.length; mx++) {
            System.out.print(mx + ": [");
            for (int my = 0; my < env[mx].length; my++)
                System.out.print(env[mx][my] + " ");

            System.out.println("]");
        }
    */   

        // Posicion de Mario utilizando las coordenadas del sistema
        //System.out.println("POSICION MARIO");
        float[] posMario;
        posMario = environment.getMarioFloatPos();
        //for (int mx = 0; mx < posMario.length; mx++)
             //System.out.print(posMario[mx] + " ");
        
        // Posicion que ocupa Mario en el array anterior
        //System.out.println("\nPOSICION MARIO MATRIZ");
        int[] posMarioEgo;
        posMarioEgo = environment.getMarioEgoPos();
        //for (int mx = 0; mx < posMarioEgo.length; mx++)
             //System.out.print(posMarioEgo[mx] + " ");
        
        
        // Estado de mario
        // marioStatus, marioMode, isMarioOnGround (1 o 0), isMarioAbleToJump() (1 o 0), isMarioAbleToShoot (1 o 0), 
        // isMarioCarrying (1 o 0), killsTotal, killsByFire,  killsByStomp, killsByShell, timeLeft
        //System.out.println("\nESTADO MARIO");
        int[] marioState;
        marioState = environment.getMarioState();
        //for (int mx = 0; mx < marioState.length; mx++){
            //System.out.print(marioState[mx] + " ");
		//}        
		
        // Mas informacion de evaluacion...
        // distancePassedCells, distancePassedPhys, flowersDevoured, killsByFire, killsByShell, killsByStomp, killsTotal, marioMode,
        // marioStatus, mushroomsDevoured, coinsGained, timeLeft, timeSpent, hiddenBlocksFound
        //System.out.println("\nINFORMACION DE EVALUACION");
        int[] infoEvaluacion;
        infoEvaluacion = environment.getEvaluationInfoAsInts();
        //for (int mx = 0; mx < infoEvaluacion.length; mx++)
             //System.out.print(infoEvaluacion[mx] + " ");

        // Informacion del refuerzo/puntuacion que ha obtenido Mario. Nos puede servir para determinar lo bien o mal que lo esta haciendo.
        // Por defecto este valor engloba: reward for coins, killed creatures, cleared dead-ends, bypassed gaps, hidden blocks found
        //System.out.println("\nREFUERZO"); 
        int reward = environment.getIntermediateReward();
        //System.out.print(reward);

        //System.out.println("\n");
		
		/* Actualizamos los atributos de evaluación. */
		checkConditions(environment.getMergedObservationZZ(1, 1), environment.getMarioState(), environment.getMarioEgoPos());
		
		/* Debemos llamar al método getTrainingFile() por cada tick del juego para guardar su información correspondiente. */
        getTrainingFile(environment.getMarioState(), environment.getMarioFloatPos(), environment.getIntermediateReward(), environment.getMergedObservationZZ(1,1), 
						environment.getEvaluationInfoAsInts(), environment.getLevelSceneObservationZ(1), environment.getEnemiesObservationZ(1));
    }
	
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
	public void checkConditions(byte[][] mergedObservation, int[] marioState, int[] position){
		
		marioPos = position; /* Obtenemos la posición de Mario. */
		P2Element aux = new P2Element(tick, line);

		/* 
			Comprobamos para la celda justo en frente de Mario si hay algún tipo de obstáculo que lo pudiera bloquear.
			Consideramos aquí a los pinchones porque no son enemigos a los que se les pueda matar. 
		*/
		
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == -60 || mergedObservation[marioPos[0]][marioPos[1] + 1] == -85 || 
		   mergedObservation[marioPos[0]][marioPos[1] + 1] == -24 || mergedObservation[marioPos[0]][marioPos[1] + 1] == -62 || 
		   mergedObservation[marioPos[0]][marioPos[1] + 1] == 93){
			isBlocked = true;	
		}
		
		/* Dependiendo del estado de Mario, no es lo mismo si este es pequeño que si es grande en cuanto a las celdas que lo rodean. */
		if(marioState[1] != 0){ /* Mario grande (2 celdas de altura) */
			/* Moneda / champiñón delante (hasta 2 celdas) */
			if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 2 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 2 || 
			mergedObservation[marioPos[0] - 1][marioPos[1] + 1] == 2 || mergedObservation[marioPos[0] - 1][marioPos[1] + 2] == 2){
				isCoinMushroom = "Front";
			}
			
			/* Moneda / champiñón detrás (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0]][marioPos[1] - 1] == 2 || mergedObservation[marioPos[0]][marioPos[1] - 2] == 2 || 
			mergedObservation[marioPos[0] - 1][marioPos[1] - 1] == 2 || mergedObservation[marioPos[0] - 1][marioPos[1] - 2] == 2){
				isCoinMushroom = "Behind";
			}
			
			/* Moneda / champiñón arriba (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0] - 2][marioPos[1]] == 2 || mergedObservation[marioPos[0] - 3][marioPos[1]] == 2){
				isCoinMushroom = "Up";
			}
			
			/* No hay Moneda / champiñón en ninguna de las áreas consideradas */
			else{
				isCoinMushroom = "None";
			}
			
			/* Enemigo delante (hasta 2 celdas) */
			if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 80 || 
			mergedObservation[marioPos[0] - 1][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0] - 1][marioPos[1] + 2] == 80){
				isEnemy = "Front";
			}
			
			/* Enemigo detrás (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0]][marioPos[1] - 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] - 2] == 80 || 
			mergedObservation[marioPos[0] - 1][marioPos[1] - 1] == 80 || mergedObservation[marioPos[0] - 1][marioPos[1] - 2] == 2){
				isEnemy = "Behind";
			}
			
			/* Enemigo arriba (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0] - 2][marioPos[1]] == 80 || mergedObservation[marioPos[0] - 3][marioPos[1]] == 80){
				isEnemy = "Up";
			}
			
			/* No hay enemigo en ninguna de las áreas consideradas */
			else{
				isEnemy = "None";
			}
		}
		
		else{ /* Mario pequeño (1 celda de altura) */
			/* Moneda / champiñón delante (hasta 2 celdas) */
			if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 2 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 2){
				isCoinMushroom = "Front";
			}
			
			/* Moneda / champiñón detrás (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0]][marioPos[1] - 1] == 2 || mergedObservation[marioPos[0]][marioPos[1] - 2] == 2){
				isCoinMushroom = "Behind";
			}
			
			/* Moneda / champiñón arriba (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0] - 1][marioPos[1]] == 2 || mergedObservation[marioPos[0] - 2][marioPos[1]] == 2){
				isCoinMushroom = "Up";
			}
			
			/* No hay Moneda / champiñón en ninguna de las áreas consideradas */
			else{
				isCoinMushroom = "None";
			}
			
			/* Enemigo delante (hasta 2 celdas) */
			if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 80){
				isEnemy = "Front";
			}
			
			/* Enemigo detrás (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0]][marioPos[1] - 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] - 2] == 80){
				isEnemy = "Behind";
			}
			
			/* Enemigo arriba (hasta 2 celdas) */
			else if(mergedObservation[marioPos[0] - 1][marioPos[1]] == 80 || mergedObservation[marioPos[0] - 2][marioPos[1]] == 80){
				isEnemy = "Up";
			}
			
			/* No hay enemigo en ninguna de las áreas consideradas */
			else{
				isCoinMushroom = "None";
			}
		}
		
		/* Comprobamos si Mario tiene un enemigo hasta 2 celdas delante (para el bot procedural) */
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 80){
			isEnemyFront = true;
		}

		/* Comprobamos si Mario se encuentra pisando el suelo. */
		isOnGround = marioState[2]==1? true : false;
	}

    public boolean[] getAction() {
		
        // La accion es un array de booleanos de dimension 6
        // action[Mario.KEY_LEFT] Mueve a Mario a la izquierda
        // action[Mario.KEY_RIGHT] Mueve a Mario a la derecha
        // action[Mario.KEY_DOWN] Mario se agacha si esta en estado grande
        // action[Mario.KEY_JUMP] Mario salta
        // action[Mario.KEY_SPEED] Incrementa la velocidad de Mario y dispara si esta en modo fuego
        // action[Mario.KEY_UP] Arriba
        // Se puede utilizar cualquier combinacion de valores true, false para este array
        // Por ejemplo: (false true false true false false) Mario salta a la derecha
        // IMPORTANTE: Si se ejecuta la accion anterior todo el tiempo, Mario no va a saltar todo el tiempo hacia adelante. 
        // Cuando se ejecuta la primera vez la accion anterior, se pulsa el boton de saltar, y se mantiene pulsado hasta que 
        // no se indique explicitamente action[Mario.KEY_JUMP] = false. Si habeis podido jugar a Mario en la consola de verdad, 
        // os dareis cuenta que si manteneis pulsado todo el tiempo el boton de saltar, Mario no salta todo el tiempo sino una 
        // unica vez en el momento en que se pulsa. Para volver a saltar debeis despulsarlo (action[Mario.KEY_JUMP] = false), 
        // y volverlo a pulsar (action[Mario.KEY_JUMP] = true).


    	
        /* for (int i = 0; i < Environment.numberOfKeys; ++i) {
            boolean toggleParticularAction = R.nextBoolean();
            toggleParticularAction = (i == 0 && toggleParticularAction && R.nextBoolean()) ? R.nextBoolean() : toggleParticularAction;
            toggleParticularAction = (i == 1 || i > 3 && !toggleParticularAction) ? R.nextBoolean() : toggleParticularAction;
            toggleParticularAction = (i > 3 && !toggleParticularAction) ? R.nextBoolean() : toggleParticularAction;
            action[i] = toggleParticularAction;
        }
        if (action[1])
            action[0] = false;
		*/
		
		/* BOT PROCEDURAL */
		
		/*
			Este bucle es la causa de que Mario se desplace hacia la derecha hasta encontrar un enemigo/obstáculo y lo salte para continuar.
			En el caso de los obstáculos hemos considerado observar la celda justo delante de Mario, pero con los enemigos, al ser
			peligrosos y poder matar a Mario, consideramos la celda justo delante y la siguiente.
			Para todas las posibles acciones que puede realizar Mario:
		*/

        for (int i = 0; i < Environment.numberOfKeys; ++i) {
			/* MOVIMIENTO BÁSICO */
			/* Inicialmente, Mario siempre se mueve a la derecha. */
			if(i == Mario.KEY_RIGHT)
            	action[i] = true;
            else{
				/* SALTO */
				/* Sin embargo, si Mario se encuentra bloqueado por un obstáculo o se aproxima a un enemigo
				y estamos considerando el movimiento salto: */
				if ((i == Mario.KEY_JUMP && (isBlocked || isEnemyFront))){
					/*
						Si Mario saltó en el tick anterior y sigue en el suelo el botón de saltar no surtirá efecto
						aunque se siga pulsando por lo que se libera durante este tick para poder saltar el obstáculo
						en el tick siguiente.
					*/
					if (jumped && isOnGround)
						action[i] = false; /* Salto desactivado. */
					/* En cualquier otra ocasión, Mario puede saltar sin problemas. */
					else
						action[i] = true;
				}
				
				else
					/* El resto de acciones que no sean desplazarse a la derecha o saltar son simplemente rechazadas. */
					action[i] = false;
			}
       	}

		/* Se resetean todas las condiciones para elegir la acción en el siguiente tick. */
		isBlocked = false;
		isEnemyFront = false;
		isEnemy = "None";
		isCoinMushroom = "None";
    	
    	P2Element aux = list.getLast(); /* Instancia que obtuvimos en el tick actual */
		
		/* Obtenemos la acción que ejecutó Mario en el tick actual */
    	if(action[Mario.KEY_RIGHT]){
    		if(action[Mario.KEY_JUMP]){
    			aux.line[aux.line.length - 3] = "jump_right"; /* Saltó mientras se movía a la derecha */
    		}
			else{
    			aux.line[aux.line.length - 3] = "move_right"; /* Se movió a la derecha */
    		}
    	}
		else if(action[Mario.KEY_LEFT]){
    		if(action[Mario.KEY_JUMP]){
    			aux.line[aux.line.length - 3] = "jump_left"; /* Saltó mientras se movía a la izquierda */
    		}
			else{
    			aux.line[aux.line.length - 3] = "move_left"; /* Se movió a la izquierda */
    		}
    	}
		else if(action[Mario.KEY_JUMP]){
    		aux.line[aux.line.length - 3] = "jump"; /* Sólo saltó */
    	}
		else{
			aux.line[aux.line.length - 3] = "still"; /* Se quedó quieto */
		}

		/* Se registra si Mario saltó o no en este tick para usarlo en el tick posterior. */
		jumped = action[3];	
			
		/* Actualizamos el tick actual */
		tick++;
		
        return action;
    }
	
}