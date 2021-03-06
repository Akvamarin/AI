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

- Necesitamos java.io* para poder usar el buffer que escribe en entrenamiento_original_1.txt 
(BufferedWriter) y el que elimina líneas incompletas (RandomAccessFile).
- Necesitamos java java.util.* para poder crear la queue que guarda la información
 de hace 5 ticks.
- Necesitamos ch.idsia.agents.controllers.T3Element porque nuestra cola está formada
por T3Element.
*/
import java.io.*; 
import java.util.*; 
import java.lang.Math;
import java.text.*;

import ch.idsia.agents.controllers.T3Element;

/*
*	Este fichero los atributos consideramos como más informados de
*	los que poseíamos en el Tutorial 3, es decir, algunas de las celdas más cercanas above
*	Mario en MergedObs más los nuevos creados por nosotros: distanceEnemy, isEnemy, isBlocked,
*	isOnGround y jumped; basándonos en las variables que el bot de Mario utiliza para moverse.
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

public class P1BotAgent_newAttributes extends BasicMarioAIAgent implements Agent {

    int tick;
	private Random R = null;
	
	/* Nuevas variables globales */
	
	BufferedWriter tFile; /* Buffer necesario para escribir en el fichero de entrenamiento. */
	LinkedList<T3Element> list; /* Linked list necesaria para guardar las instancias sin información futura adjudicada. */
	
	int [] marioPos; /* Esta variable nos indica la posición de Mario en la matriz. */
    
    boolean isBlocked; /* Esta variable nos indica si el agente está bloqueado por algún obstáculo justo delante. */
	boolean isEnemy; /* Esta variable nos indica si Mario tiene un enemigo delante con una anticipación de dos casillas. */
	
	boolean jumped = false; /* Esta variable nos indica si el agente saltó en el tick anterior. Para el tick 1 su valor es 'false', ya que no hay tick anterior. */
	int isOnGround; /* Esta variable nos indica si el agente se encuentra tocando el suelo en el tick actual. */
	
	String[] line; /* Variable para concatenar todos los atributos de una instancia */
	int marioState; /* Estado de Mario en el tick actual (gana, pierde, otro) */
	
	/* Contadores para las cuatro acciones minoritarias que puede ejecutar Mario. */
	int air_jump_counter; /* Mario salta porque delante tiene un obstáculo, estando en el aire */
	int enemy_jump_counter; /* Mario salta porque delante tiene un enemigo, estando en el suelo */
	int block_jump_counter; /* Mario salta porque delante tiene un obstáculo, estando en el suelo */
	
	/* Contador del resto de instancias. */
	int no_enemy_jump_counter; /* Mario no salta porque no tiene delante un enemigo, estando en el suelo. */
	int no_block_jump_counter; /* Mario no salta porque no está bloqueado, estando en el suelo. */

    public P1BotAgent_newAttributes(){
        super("BaselineAgent");
        reset();
		
		/* Inicializamos a 0 los ticks y contadores de instancias. */
        tick = 0;
		
		air_jump_counter = 0;
		enemy_jump_counter = 0;
		block_jump_counter = 0; 
		no_enemy_jump_counter = 0;
		no_block_jump_counter = 0; 
		
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
			
			BufferedWriter tFile_header = new BufferedWriter(new FileWriter("entrenamiento_new_attributes_1.arff", true), 30000);
			BufferedReader tFile_reader = new BufferedReader(new FileReader("entrenamiento_new_attributes_1.arff"));
			
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
				Cerramos ambos ficheros, ya que sólo eran necesarios para escribir el header de entrenamiento_new_attributes_1.arff 
			*/
			tFile_reader.close();
			tFile_header.close();
			
			/* 
				Hemos decidido usar un BufferedWriter porque queremos que se guarden los
				datos de entrenamiento no sólo al finalizar la partida (cuando cerramos
				el fichero), sino también durante su ejecución. De este modo, cada 8192 bytes
				que recoge el buffer, estos se mandan a escribir al fichero 'entrenamiento_new_attributes_1.arff' 
				y el buffer se vacía. Así no perderemos toda la información en el caso de cierre
				abrupto del programa.
				
				Por otro lado, ya que se requiere que el fichero no se escriba de nuevo
				cada vez que se abre, sino añadiendo nueva información a partir de la última
				línea escrita, usamos el argumento 'true' en FileWriter. Este crea el fichero
				'ejemplos.txt' si no existía y escribe desde el final del fichero existente sin 
				sobreescribir datos en el caso contrario.
			*/
			
			tFile = new BufferedWriter(new FileWriter("entrenamiento_new_attributes_1.arff", true));
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
			RandomAccessFile f = new RandomAccessFile("entrenamiento_new_attributes_1.arff", "rw");
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
	*	archivo 'entrenamiento_new_attributes_1.arff' para poder procesar los atributos de las instancias en Weka.
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

		header += "@ATTRIBUTE isBlocked {Blocked, Free}\n"; /* Mario está o no bloqueado por algún obstáculo delante. */
		header += "@ATTRIBUTE isEnemy {Enemy, NoEnemy}\n"; /* Mario tiene o no algún enemigo delante o a dos casillas. */
		header += "@ATTRIBUTE isOnGround {Ground, Air}\n"; /* Mario se encuentra pisando o no el suelo. */
		header += "@ATTRIBUTE jumped {Jumped, NoJumped}\n"; /* Mario saltó o no en el tick anterior. */
		
		/* CLASE */
		
		header += "@ATTRIBUTE action {move_jump, move, jump, still}\n\n"; /* Acción ejecutada por Mario. */
		header += "@data"; /* A partir de aquí informamos que se encuentran las instancias */
		
		return header; /* Devolvemos nuestro header */
	}
	
	/**
	*	getTrainingFile(): Este método se encarga de guardar la información 
	*	requerida de cada tick en una línea del fichero 'entrenamiento_new_attributes_1.arff'. Decidimos pasar la información
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
			008: isBlocked
			009: isEnemy
			010: isOnGround
			011: jumped
			>CLASE
			012: action
		*/
		line = new String[13];
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
		int minCol = mergedObservationZZ.length; /* Las columnas van de 0 a 18, por lo que nunca serán mayores de 19. */
		int minRow = mergedObservationZZ.length; /* Las filas van de 0 a 18, por lo que nunca serán mayores de 19. */
		
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
					if (minCol>j){ /* Si la columna actual está más cerca que el mínimo hasta la fecha. */
						minCol = j;
						if(minRow>i) /* Si la fila actual está más cerca que el mínimo hasta la fecha. */
							minRow=i;
						
					}
				}
			}
		}
		
		minCol -= marioPos[0]; /* Restamos la posición de Mario a la columna mínima obtenida. */ 
		/* 
			Hacemos lo mismo con las filas, pero haciendo uso del valor absoluto, ya que el enemigo puede estar
			encima o debajo de Mario, no como en el caso anterior, que sólo podía estar a la derecha.
		*/
		minRow = Math.abs(minRow - 9); 

		/* 
			Calculamos la distancia euclídea multiplicando por 100 su valor y quitando los decimales,
			ya que salían demasiados.
		*/
		line[l] = String.valueOf((int)(100*getEuclideanDistance(minCol, minRow))); 		
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
		
		/* Debemos llamar al método getTrainingFile() por cada tick del juego para guardar su información correspondiente. */
        getTrainingFile(environment.getMarioEgoPos(), environment.getMarioState(), environment.getMarioFloatPos(), environment.getIntermediateReward(),
						environment.getMergedObservationZZ(1,1), environment.getEvaluationInfoAsInts(), environment.getLevelSceneObservationZ(1),
						environment.getEnemiesObservationZ(1));
		
		/* A continuación, comprobamos los valores en el tick actual para las condiciones necesarias para calcular*
		la siguiente acción del agente. */
		checkConditions(environment.getMergedObservationZZ(1, 1), environment.getMarioState());
		
        
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
		
		/* Añadimos el atributo isBlocked en función de la variable isBlocked. */
		aux.line [aux.line.length - 5] = isBlocked? "Blocked" : "Free";
        
		/* Comprobamos para la celda justo en frente de Mario y la posterior si hay algún enemigo. */
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 80){
        	isEnemy = true;
        	
		}
		/* Añadimos el atributo isEnemy en función de la variable isEnemy. */
		aux.line [aux.line.length - 4] = isEnemy? "Enemy" : "NoEnemy";
		
		/* Comprobamos si Mario se encuentra pisando el suelo. */
		isOnGround = marioState[2];
		
		/* Añadimos el atributo isOnGround en función de la variable isOnGround. */
		aux.line [aux.line.length - 3] = isOnGround==1? "Ground" : "Air";
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
				if ((i == Mario.KEY_JUMP && (isBlocked || isEnemy))){
					/*
						Si Mario saltó en el tick anterior y sigue en el suelo el botón de saltar no surtirá efecto
						aunque se siga pulsando por lo que se libera durante este tick para poder saltar el obstáculo
						en el tick siguiente.
					*/
					if (jumped && isOnGround == 1)
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
		
		T3Element aux = new T3Element(tick, line);
		
		/* Aquí asignamos una clase según el movimiento que ha ejecutado Mario en este tick. */
		if(action[Mario.KEY_RIGHT]){
			if(action[Mario.KEY_JUMP]){
				/* Salta mientras se mueve a la derecha. */
				aux.line [aux.line.length - 1]= "move_jump";
			}
			else 
				/* Se mueve a la derecha. */
				aux.line [aux.line.length - 1]= "move";
		}
		else{
			if(action[Mario.KEY_JUMP]){
				/* Sólo salta. */
				aux.line [aux.line.length - 1]= "jump";
			}
			else 
				/* Se queda quieto. */
				aux.line [aux.line.length - 1]= "still";
		}

		/* Guardamos el atributo jumped. */
		aux.line [aux.line.length - 2] = jumped? "Jumped" : "NoJumped";
		/* Se registra si Mario saltó o no en este tick para usarlo en el tick posterior. */
		jumped = action[3];
		
			
		/* Hacemos uso de try-catch para notificar de posibles IOException. */
		try{
			
			/* El primer tick siempre es Mario cayendo del cielo, por lo que no
			es interesante guardarlo. Sin embargo, incrementamos en uno other_instances_counter
			para poder guardar instancias de salto.*/
			
			if (tick == 0)
				no_enemy_jump_counter++; /* Actualizar contador. */
			
			/* Caso 1: Mario tiene un enemigo delante, está en el suelo y decide saltar. */
			if(isEnemy && isOnGround == 1 && action[Mario.KEY_JUMP]){
				
				/* Sólo insertamos esta instancia si todavía no hemos
				igualado a other_instances_counter para este caso. */
				if (enemy_jump_counter < no_enemy_jump_counter){
					
					enemy_jump_counter++; /* Actualizar contador. */
					
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
			
			/* Caso 2: Mario tiene un obstáculo delante, está en el suelo y decide saltar. */
			else if (isBlocked && isOnGround == 1 && action[Mario.KEY_JUMP]){
				
				/* Sólo insertamos esta instancia si todavía no hemos
				igualado a other_instances_counter para este caso. */
				if (block_jump_counter < no_enemy_jump_counter){
						
					block_jump_counter++; /* Actualizar contador. */
					
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
			
			/* Caso 3: Mario tiene un obstáculo delante, está en el aire y sigue saltando. */
			else if (isBlocked && isOnGround == 0 && action[Mario.KEY_JUMP]){
				
				/* Sólo insertamos esta instancia si todavía no hemos
				igualado a other_instances_counter para este caso. */
				if (air_jump_counter < no_enemy_jump_counter){
						
					air_jump_counter++; /* Actualizar contador. */
						
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
			
			/* Caso 4: Mario está en el suelo y no salta porque no está bloquedo. */
			else if (isOnGround == 1 && !action[Mario.KEY_JUMP] && !isBlocked){
				
				/* Sólo escribimos esta instancia (que es mucho más común) cuando tenemos el mismo número
				de cada una de las otras tres instancias minorarias que esta.*/
				if (no_block_jump_counter < no_enemy_jump_counter){
					
					no_block_jump_counter++; /* Actualizar contador. */
					
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
			
			/* Caso 5: Mario está en el suelo y no salta porque no tiene un enemigo cerca. */
			else if (isOnGround == 1 && !action[Mario.KEY_JUMP] && !isEnemy){
				
				/* Sólo escribimos esta instancia (que es mucho más común) cuando tenemos el mismo número
				de cada una de las otras tres instancias minorarias que esta.*/
				if (enemy_jump_counter == no_enemy_jump_counter && block_jump_counter == no_enemy_jump_counter
				&& air_jump_counter == no_enemy_jump_counter && no_block_jump_counter == no_enemy_jump_counter){
					
					no_enemy_jump_counter++; /* Actualizar contador. */
					
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
			
			/* Se resetean todas las condiciones para elegir la acción en el siguiente tick. */
			isBlocked = false;
			isEnemy = false;
				
			/* En el caso de que Mario haya muerto (marioState[0] == 0) o que haya
			ganado la partida (marioState[0] == 1, cerramos el archivo para que se
			escriba la información que quedaba por mandar en el buffer. */
			
			if(marioState == 0 || marioState == 1)
				tFile.close(); /* Cerramos el fichero. */
			}
		
		catch(IOException ex){
			/* Mostramos por el standard output el mensaje de error. */
			System.out.println (ex.toString());
		}
		
		/* Actualizamos el tick actual */
		tick++;
		
        return action;
    }
	
}