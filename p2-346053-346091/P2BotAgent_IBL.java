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
import ch.idsia.agents.controllers.instance;
import ch.idsia.agents.controllers.instance_similitud;


public class P2BotAgent_IBL extends BasicMarioAIAgent implements Agent {

    int tick;
	private Random R = null;
	
	/* Nuevas variables globales */
	
	int [] marioPos; /* Esta variable nos indica la posición de Mario en la matriz. */
    
	/* ATRIBUTOS PARA COMPARACIÓN */


	/* INTERMEDIATE REWARD */
	String isEnemy; /* Esta variable nos indica si Mario tiene un enemigo 'cerca' (hasta 2 celdas de distancia). */
	String isCoinMushroom; /* Esta variable nos indica si Mario tiene una moneda o champiñón 'cerca' (hasta 2 celdas de distancia). */
	
	/* AVANZAR */
	boolean jumped = false; /* Esta variable nos indica si el agente saltó en el tick anterior. Para el tick 1 su valor es 'false', ya que no hay tick anterior. */
	boolean isOnGround; /* Esta variable nos indica si el agente se encuentra tocando el suelo en el tick actual. */
	boolean isBlocked; /* Esta variable nos indica si Mario se encuentra bloqueado en la casilla enfrente de él. */
	
	/* No hace falta declarar atributos para evaluación, ya que se obtienen de integrateObservation */
	
	/* RESULTADOS FUNC. PERTENENCIA Y EVALUACIÓN */
	int evaluation; /* Evaluación de la instancia con respecto a sus datos futuros. */
	int situation; /* Situación a la que pertenece la instancia. */
	
	/* OTRAS */
	boolean isEnemyFront; /* Variable auxiliar para saber si hay enemigos, ya que nuestro Mario original sólo miraba si éstos se encontraban frente a él. */

	/* 
		Arrays para guardar cada todas las instancias de la base de conocimiento. En cada uno de ellos
		se guardan las instancias correspondientes a cada tipo de situación.
		
		** CUIDADO **
		Si se cambia la base de conocimiento, se debe cambiar el tamaño de estos arrays por el número de 
		elementos que tenga cada situación.
	*/
	instance[] coin_mushroom = new instance[182]; //Coin Mushroom cerca
	instance[] enemy = new instance[96]; //Enemigo cerca
	instance[] blocked = new instance[101]; //Está bloqueado
	instance[] no_blocked = new instance[177]; //No está bloqueado
	
	
	String[] line = new String[5]; /* Variable para concatenar todos los atributos de la instancia a analizar*/
	int marioState; /* Estado de Mario en el tick actual (gana, pierde, otro) */
	
	/* CONTADORES */
	
	/* 
		Estos contaodores los utilizamos para saber cuál es la siguiente posición
		del array que tenemos que inicializar para cada uno de los cuatro que componen
		la base de conocimiento.
	*/

	int read_coin_mushroomInstance; 
	int read_enemyInstance;
	int read_blockedInstance;
	int read_no_blockedInstance;

    public P2BotAgent_IBL(){
        super("BaselineAgent");
        reset();
		
		/* Inicializamos a 0 los ticks y contadores de instancias. */
        tick = 0;

		read_coin_mushroomInstance = 0; 
		read_enemyInstance = 0;
		read_blockedInstance = 0;
		read_no_blockedInstance = 0;
		
		
		/* Preconfiguración [partes necesarias al iniciarse la ejecución del programa] */
		
		/* Hacemos uso de try-catch para notificar de posibles IOException. */
		try{

			/* Creamos un buffer para leer la base de conocimiento */
			BufferedReader bufRead = new BufferedReader(new FileReader("base_conocimiento_human_fixedBlocked_more.arff"));
		

			for(int i=0; i<17; ++i) bufRead.readLine(); //El header de Weka son 27 líneas

			String myLine = null;
			/* Mientras que haya líneas que leer */
			while ((myLine = bufRead.readLine()) != null){  

				/* data_line guarda un array con la información de la instancia leída */
				String[] data_line = myLine.split(","); 

				init_IBL_database(data_line);

			}
			
			bufRead.close(); /* Una vez eliminada la línea, cerramos RandomAccessFile. */	

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
	*	init_IBL_database(): Este método se encarga inicializar los cuatro arrays
	*	que guardan la información de la base de conocimiento.
	*
	*	@String[] data_line: instancia recogida directamente del fichero.
	*	
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public void init_IBL_database(String[] data_line){
		
		/* Obtenemos a qué situación pertenece la instancia para saber en qué array la tenemos que guardar */
		switch(data_line[12]) {

		   case "coin_mushroom" : //Situación 1
			/* Inicializamos la instancia sólo con los atributos que necesitamos del fichero */
		      coin_mushroom[read_coin_mushroomInstance] =
		      	new instance(data_line[0], data_line[1], data_line[2], data_line[3], data_line[4], data_line[10], Double.parseDouble(data_line[11]));

		      	++read_coin_mushroomInstance; //Actualizamos el contador
		      break;
		   
		   case "enemy" : //Situación 2
			/* Inicializamos la instancia sólo con los atributos que necesitamos del fichero */
		      enemy[read_enemyInstance]= 
		      	new instance(data_line[0], data_line[1], data_line[2], data_line[3], data_line[4], data_line[10], Double.parseDouble(data_line[11]));

		      	++read_enemyInstance; //Actualizamos el contador
		      break; 

		   case "blocked" : //Situación 3
			/* Inicializamos la instancia sólo con los atributos que necesitamos del fichero */
		      blocked[read_blockedInstance]= 
		      	new instance(data_line[0], data_line[1], data_line[2], data_line[3], data_line[4], data_line[10], Double.parseDouble(data_line[11]));

		      	++read_blockedInstance; //Actualizamos el contador
		      break; 

		   case "no_blocked" : //Situación 4
			/* Inicializamos la instancia sólo con los atributos que necesitamos del fichero */
		      no_blocked[read_no_blockedInstance] = 
		      	new instance(data_line[0], data_line[1], data_line[2], data_line[3], data_line[4], data_line[10], Double.parseDouble(data_line[11]));

		      	++read_no_blockedInstance; //Actualizamos el contador
		      break; 
		  
		}

	}
	
	/**
	*	func_pertenencia(): Este método se encarga indicar a qué situación
	*	pertenece la instancia que hemos obtenido en el tick actual.
	*	
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public String func_pertenencia(){

		String pertenencia = null;
		/* Caso 1: Mario tiene un enemigo 'cerca' (hasta 2 celdas adyacentes). */
		if (!line[0].equals("None")) pertenencia = "enemy";

		/* Caso 2: Mario está bloqueado justo enfrente. */
		else if (line[2].equals("true")) pertenencia = "blocked";

		/* Caso 3: Mario tiene una moneda o champiñón 'cerca' (hasta 2 celdas adyacentes). */
		else if(!line[1].equals("None")) pertenencia = "coin_mushroom";

		/* Caso 4: Mario no está bloqueado justo enfrente. */
		else if(line[2].equals("false")) pertenencia = "no_blocked";

		return pertenencia;
	}
	
	/**
	*	func_similitud(): Este método se encarga de obtener las 10 instancias
	*	más similares a la instancia obtenida en el tick actual siempre que
	*	pertenezcan a la misma situación.
	*
	*	@pertenencia: situación a la que pertenece la instancia del tick actual
	*	
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public instance_similitud[] func_similitud(String pertenencia){

		/* Inicializamos esas 10 instancias a sus valores iniciales */
		instance_similitud pos0 = new instance_similitud(0,0);
		instance_similitud pos1 = new instance_similitud(0,0);
		instance_similitud pos2 = new instance_similitud(0,0);
		instance_similitud pos3 = new instance_similitud(0,0);
		instance_similitud pos4 = new instance_similitud(0,0);
		instance_similitud pos5 = new instance_similitud(0,0);
		instance_similitud pos6 = new instance_similitud(0,0);
		instance_similitud pos7 = new instance_similitud(0,0);
		instance_similitud pos8 = new instance_similitud(0,0);
		instance_similitud pos9 = new instance_similitud(0,0);
		
		instance_similitud[] most_similar = {pos0, pos1, pos2, pos3, pos4, pos5, pos6, pos7, pos8, pos9};
		int puntuacion;
		
		int min_punt = 0; //Puntuación mínima dentro de las 10 elegidas por ahora
		int min_punt_pos = 0; //Posición en la que se encuentra esa instancia
		
		int ground, jump, enemy_, blocked_, coin; //Variables necesarias para la función de similitud

		/* Caso 1: Mario tiene un enemigo 'cerca' (hasta 2 celdas adyacentes). */
		if (pertenencia.equals("enemy")){
			
			for(int i = 0; i < enemy.length; ++i){ //Recorremos el array enemy
				
				/* 
					Vemos si las variables de ambas instancias (tick actual, base de conocimiento) son iguales
					o no, para darle el valor que que usará en la función de similitud (1 si son iguales, 0 si son
					distintas).
				*/
				ground = isOnGround == Boolean.parseBoolean(enemy[i].isOnGround)? 1 : 0;
				
				jump = jumped == Boolean.parseBoolean(enemy[i].jumped)? 1 : 0;
				
				enemy_ = isEnemy.equals(enemy[i].isEnemy)? 1 : 0;
				
				blocked_ = isBlocked == Boolean.parseBoolean(enemy[i].isBlocked)? 1 : 0;
				
				coin = isCoinMushroom.equals(enemy[i].isCoinMushroom)? 1 : 0;
				
				/* Función de similitud propiamente dicha */
				puntuacion = ground + jump + enemy_ + blocked_ + coin;
				
				/* Recorremos el array con las 10 instancias más similares hasta la fecha */
				for(int j = 0; j < most_similar.length; ++j){
					/* 
						Comprobamos si instancia la que estamos mirando actualmente es más pequeña que
						la más pequeña encontrada hasta la fecha.
					*/
					if(min_punt <= most_similar[j].similitud){
					
						min_punt = most_similar[j].similitud; //Actualizamos el mínimo
						min_punt_pos = j; //Actualizamos la posición del mínimo
					}
				}
				
				/* Si la puntuación es mayor que el mínimo, podemos incluir esta instancia en similars */
				if (puntuacion > min_punt){ 
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
					
				/* Si empatan, lo decidimos aleatoriamente */
				else if (puntuacion == min_punt){
					if(Math.random() > 0.5) {
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
						
				}
			}
			
		}
		/* Caso 2: Mario está bloqueado justo enfrente. */
		else if (pertenencia.equals("blocked")){
			for(int i = 0; i < blocked.length; ++i){
				
				/* 
					Vemos si las variables de ambas instancias (tick actual, base de conocimiento) son iguales
					o no, para darle el valor que que usará en la función de similitud (1 si son iguales, 0 si son
					distintas).
				*/
				ground = isOnGround == Boolean.parseBoolean(blocked[i].isOnGround)? 1 : 0;
				
				jump = jumped == Boolean.parseBoolean(blocked[i].jumped)? 1 : 0;
				
				enemy_ = isEnemy.equals(blocked[i].isEnemy)? 1 : 0;
				
				blocked_ = isBlocked == Boolean.parseBoolean(blocked[i].isBlocked)? 1 : 0;
				
				coin = isCoinMushroom.equals(blocked[i].isCoinMushroom)? 1 : 0;
				
				/* Función de similitud propiamente dicha */
				puntuacion = ground + jump + enemy_ + blocked_ + coin;
				
				/* Recorremos el array con las 10 instancias más similares hasta la fecha */
				for(int j = 0; j < most_similar.length; ++j){
					if(min_punt <= most_similar[j].similitud){ 
					
						min_punt = most_similar[j].similitud;
						min_punt_pos = j;
					}
				}
				
				/* Si la puntuación es mayor que el mínimo, podemos incluir esta instancia en similars */
				if (puntuacion > min_punt){
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
				
				/* Si empatan, lo decidimos aleatoriamente */
				else if (puntuacion == min_punt){
					if(Math.random() > 0.5) {
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
						
				}
				
			}
			
		}
		/* Caso 3: Mario tiene una moneda o champiñón 'cerca' (hasta 2 celdas adyacentes). */
		else if(pertenencia.equals("coin_mushroom")){
			for(int i = 0; i < coin_mushroom.length; ++i){
				
				/* 
					Vemos si las variables de ambas instancias (tick actual, base de conocimiento) son iguales
					o no, para darle el valor que que usará en la función de similitud (1 si son iguales, 0 si son
					distintas).
				*/
				ground = isOnGround == Boolean.parseBoolean(coin_mushroom[i].isOnGround)? 1 : 0;
				
				jump = jumped == Boolean.parseBoolean(coin_mushroom[i].jumped)? 1 : 0;
				
				enemy_ = isEnemy.equals(coin_mushroom[i].isEnemy)? 1 : 0;
				
				blocked_ = isBlocked == Boolean.parseBoolean(coin_mushroom[i].isBlocked)? 1 : 0;
				
				coin = isCoinMushroom.equals(coin_mushroom[i].isCoinMushroom)? 1 : 0;
				
				/* Función de similitud propiamente dicha */
				puntuacion = ground + jump + enemy_ + blocked_ + coin;
				
				/* Recorremos el array con las 10 instancias más similares hasta la fecha */
				for(int j = 0; j < most_similar.length; ++j){
					if(min_punt <= most_similar[j].similitud){ 
					
						min_punt = most_similar[j].similitud;
						min_punt_pos = j;
					}
				}
				
				/* Si la puntuación es mayor que el mínimo, podemos incluir esta instancia en similars */
				if (puntuacion > min_punt){
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
				
				/* Si empatan, lo decidimos aleatoriamente */
				else if (puntuacion == min_punt){
					if(Math.random() > 0.5) {
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
						
				}
				
			}
			
		}
		/* Caso 4: Mario no está bloqueado justo enfrente. */
		else if(pertenencia.equals("no_blocked")){
			
			for(int i = 0; i < no_blocked.length; ++i){
				
				/* 
					Vemos si las variables de ambas instancias (tick actual, base de conocimiento) son iguales
					o no, para darle el valor que que usará en la función de similitud (1 si son iguales, 0 si son
					distintas).
				*/
				ground = isOnGround == Boolean.parseBoolean(no_blocked[i].isOnGround)? 1 : 0;
				
				jump = jumped == Boolean.parseBoolean(no_blocked[i].jumped)? 1 : 0;
				
				enemy_ = isEnemy.equals(no_blocked[i].isEnemy)? 1 : 0;
				
				blocked_ = isBlocked == Boolean.parseBoolean(no_blocked[i].isBlocked)? 1 : 0;
				
				coin = isCoinMushroom.equals(no_blocked[i].isCoinMushroom)? 1 : 0;
				
				/* Función de similitud propiamente dicha */
				puntuacion = ground + jump + enemy_ + blocked_ + coin;
				
				/* Recorremos el array con las 10 instancias más similares hasta la fecha */
				for(int j = 0; j < most_similar.length; ++j){
					if(min_punt <= most_similar[j].similitud){ 
					
						min_punt = most_similar[j].similitud;
						min_punt_pos = j;
					}
				}
				
				/* Si la puntuación es mayor que el mínimo, podemos incluir esta instancia en similars */
				if (puntuacion > min_punt){
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
				
				/* Si empatan, lo decidimos aleatoriamente */
				else if (puntuacion == min_punt){
					if(Math.random() > 0.5) {
						most_similar[min_punt_pos] = new instance_similitud(i, puntuacion);
					}
						
				}
			}
			
		}
		return most_similar;
	}
	
	/**
	*	func_evaluation(): Este método se encarga de obtener la acción de la instancia
	*	con más evaluación dentro de las que eligió la función de similitud, para que sea
	*	la que luego ejecute Mario.
	*
	*	@instance_similitud[]: Instancias más similares obtenidas por la función de evaluación
	*	@String pertenencia: situación a la que pertenece la instancia del tick actual
	*	
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public String func_evaluation(instance_similitud[] similars, String pertenencia) {
   
		double max_eval = -100; //El mínimo es un número negativo porque puede haber evaluaciones negativas
		String do_action = null; //La acción se inicializa como null
		
		/* Caso 1: Mario tiene un enemigo 'cerca' (hasta 2 celdas adyacentes). */
		if (pertenencia.equals("enemy")){
			for(int i = 0 ; i < similars.length ; ++i){ //Recorremos el array de instancias similares
				//Comprobamos si es máximo actual es mayor que la evaluación de la instancia similar actual
				if(enemy[similars[i].id].evaluation > max_eval){ 
					max_eval = enemy[similars[i].id].evaluation; //Actualizamos el máximo
					//Actualizamos la acción de esta nueva instancia con la máxima evaluación
					do_action = enemy[similars[i].id].action;
				}
			}
		}
		/* Caso 2: Mario está bloqueado justo enfrente. */
		else if (pertenencia.equals("blocked")){
			for(int i = 0 ; i < similars.length ; ++i){ //Recorremos el array de instancias similares
				//Comprobamos si es máximo actual es mayor que la evaluación de la instancia similar actual
				if(blocked[similars[i].id].evaluation > max_eval){
					max_eval = blocked[similars[i].id].evaluation; //Actualizamos el máximo
					//Actualizamos la acción de esta nueva instancia con la máxima evaluación
					do_action = blocked[similars[i].id].action;
				}
			}
		}
		/* Caso 3: Mario tiene una moneda o champiñón 'cerca' (hasta 2 celdas adyacentes). */
		else if (pertenencia.equals("coin_mushroom")){ //Recorremos el array de instancias similares
		//Comprobamos si es máximo actual es mayor que la evaluación de la instancia similar actual
			for(int i = 0 ; i < similars.length ; ++i){
				if(coin_mushroom[similars[i].id].evaluation > max_eval){
				max_eval = coin_mushroom[similars[i].id].evaluation; //Actualizamos el máximo
				//Actualizamos la acción de esta nueva instancia con la máxima evaluación
				do_action = coin_mushroom[similars[i].id].action;
				}
			}
		}
		/* Caso 4: Mario no está bloqueado justo enfrente. */
		else if (pertenencia.equals("no_blocked")){ //Recorremos el array de instancias similares
			//Comprobamos si es máximo actual es mayor que la evaluación de la instancia similar actual
			for(int i = 0 ; i < similars.length ; ++i){
				if(no_blocked[similars[i].id].evaluation > max_eval){
					max_eval = no_blocked[similars[i].id].evaluation; //Actualizamos el máximo
					//Actualizamos la acción de esta nueva instancia con la máxima evaluación
					do_action = no_blocked[similars[i].id].action;
				}
			}
		}
		
     	return do_action;
    }

	/**
	*	init_currentInstance(): Este método se encarga de obtener los atributos que necesitamos
	*	de la instancia del tick actual. Obtenemos sus atributos mediante los valores que se encuentran
	*	dentro de las variables globales que lo determinan, por lo que no necesita argumentos.
	*
	*	
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public void init_currentInstance (){

		line[0]= isEnemy;
		line[1]= isCoinMushroom;
		line[2]= Boolean.toString(isBlocked);
		line[3]= Boolean.toString(isOnGround);
		line[4]= Boolean.toString(jumped);

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
		
		/* Obtenemos primero los atributos del tick actual. */
		checkConditions(environment.getMergedObservationZZ(1, 1), environment.getMarioState(), environment.getMarioEgoPos());
		
		/* Debemos llamar al método init_currentInstance para inicializar la instancia del tick actual. */
        init_currentInstance();
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

    	/* Ejecutamos de manera continuada las funciones de pertenencia, similitud y evaluación */
		String execute = func_evaluation(func_similitud(func_pertenencia()), func_pertenencia());
       
      	/* Hay acciones que no tenemos nunca en cuenta*/
		action[Mario.KEY_SPEED] = false;
      	action[Mario.KEY_DOWN] = false;
      	action[Mario.KEY_UP] = false;
		
		/* Obtenemos la acción a través de func_evaluation() */

			if(execute.equals("move_right")){ //Se mueve a la derecha
				action[Mario.KEY_LEFT] = false;
        		action[Mario.KEY_RIGHT] = true;
        		action[Mario.KEY_JUMP] = false;

			}
			else if(execute.equals("move_left")){ //Se mueve a la izquierda
				action[Mario.KEY_LEFT] = true;
        		action[Mario.KEY_RIGHT] = false;
        		action[Mario.KEY_JUMP] = false;

			}
			else if(execute.equals("jump")){ //Salta
				action[Mario.KEY_LEFT] = false;
        		action[Mario.KEY_RIGHT] = false;
        		action[Mario.KEY_JUMP] = true;

			}
			else if(execute.equals("jump_right")){ //Salta y se mueve a la derecha
				action[Mario.KEY_LEFT] = false;
        		action[Mario.KEY_RIGHT] = true;
        		action[Mario.KEY_JUMP] = true;

			}
			else if(execute.equals("jump_left")){ //Salta y se mueve a la izquierda
				action[Mario.KEY_LEFT] = true;
        		action[Mario.KEY_RIGHT] = false;
        		action[Mario.KEY_JUMP] = true;

			}
			else if(execute.equals("still")){ //Se queda quieto
				action[Mario.KEY_LEFT] = false;
        		action[Mario.KEY_RIGHT] = false;
        		action[Mario.KEY_JUMP] = false;

			}


		/* Se resetean todas las condiciones para elegir la acción en el siguiente tick. */
		isBlocked = false;
		isEnemyFront = false;
		isEnemy = "None";
		isCoinMushroom = "None";		

		/* Se registra si Mario saltó o no en este tick para usarlo en el tick posterior. */
		jumped = action[3];	
			
		/* Actualizamos el tick actual */
		tick++;

        return action;
    }
	
}