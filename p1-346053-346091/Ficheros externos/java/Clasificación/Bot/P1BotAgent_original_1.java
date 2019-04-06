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

import ch.idsia.agents.controllers.T3Element;

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

public class P1BotAgent_original_1 extends BasicMarioAIAgent implements Agent {

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

    public P1BotAgent_original_1(){
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
			
			BufferedWriter tFile_header = new BufferedWriter(new FileWriter("entrenamiento_original_1.arff", true), 30000);
			BufferedReader tFile_reader = new BufferedReader(new FileReader("entrenamiento_original_1.arff"));
			
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
				Cerramos ambos ficheros, ya que sólo eran necesarios para escribir el header de entrenamiento_original_1.arff 
			*/
			tFile_reader.close();
			tFile_header.close();
			
			/* 
				Hemos decidido usar un BufferedWriter porque queremos que se guarden los
				datos de entrenamiento no sólo al finalizar la partida (cuando cerramos
				el fichero), sino también durante su ejecución. De este modo, cada 8192 bytes
				que recoge el buffer, estos se mandan a escribir al fichero 'entrenamiento_original_1.arff' 
				y el buffer se vacía. Así no perderemos toda la información en el caso de cierre
				abrupto del programa.
				
				Por otro lado, ya que se requiere que el fichero no se escriba de nuevo
				cada vez que se abre, sino añadiendo nueva información a partir de la última
				línea escrita, usamos el argumento 'true' en FileWriter. Este crea el fichero
				'ejemplos.txt' si no existía y escribe desde el final del fichero existente sin 
				sobreescribir datos en el caso contrario.
			*/
			
			tFile = new BufferedWriter(new FileWriter("entrenamiento_original_1.arff", true));
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
			RandomAccessFile f = new RandomAccessFile("entrenamiento_original_1.arff", "rw");
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
	*	archivo 'entrenamiento_original_1.arff' para poder procesar los atributos de las instancias en Weka.
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public String getHeader(){
		
		String header = ""; /* Inicializamos la variable a devolver */
		
		header += "@RELATION P1Mario-data-training-T3Original\n\n"; /* Nombre del fichero */
		
		/* ATRIBUTOS PASADOS */
		
		header += "@ATTRIBUTE MarioPosX NUMERIC\n"; /* Posición de Mario en el eje X */
		header += "@ATTRIBUTE MarioPosY NUMERIC\n"; /* Posición de Mario en el eje Y*/
		header += "@ATTRIBUTE Reward NUMERIC\n"; /* Reward obtenido hasta la fecha */
		
		/* 
			Para escribir los atributos de MergedObservationZZ hemos decidido usar un
			for loop doble, para poder nombrar correctamente cada atributo según su
			posición en la matriz. Esta variable es nominal. Sus valores son los 
			correspondientes a los del nivel de detalle 1, ya que es el valor que
			cogemos para escribir los atributos.
		*/		
		for(int i = 0; i < 19; ++i){
			for(int j = 0; j < 19; ++j){
				header += "@ATTRIBUTE MergedObs["+i+"]["+j+"] {-85, -62, -60, -24, 0, 1, 2, 3, 5, 25, 80, 93}\n";
			}
		}
		
		header += "@ATTRIBUTE distancePassedCells NUMERIC\n"; /* Número de celdas recorridas */
		header += "@ATTRIBUTE distancePassedPhys NUMERIC\n"; /* Posición de Mario en el eje X */
		header += "@ATTRIBUTE flowersDevoured NUMERIC\n"; /* Flores de fuego obtenidas */
		header += "@ATTRIBUTE killsByFire NUMERIC\n"; /* Muertes por flor de fuego */
		header += "@ATTRIBUTE killsByShell NUMERIC\n"; /* Muertes por caparazón de koopa */
		header += "@ATTRIBUTE killsByStomp NUMERIC\n"; /* Muertes por pisotón */
		header += "@ATTRIBUTE killsTotal NUMERIC\n"; /* Muertes totales */
		
		header += "@ATTRIBUTE marioMode {0, 1, 2}\n"; /* Modo de Mario [pequeño, grande, fuego]. Nominal */
		header += "@ATTRIBUTE marioStatus {0, 1, 2}\n"; /* Estado de Mario [gana, pierde, otro] */
		
		header += "@ATTRIBUTE mushroomsDevoured NUMERIC\n"; /* Champiñones obtenidos */
		header += "@ATTRIBUTE coinsGained NUMERIC\n"; /* Monedas obtenidas */
		header += "@ATTRIBUTE timeLeft NUMERIC\n"; /* Tiempo restante */
		header += "@ATTRIBUTE timeSpent NUMERIC\n"; /* Tiempo gastado */
		header += "@ATTRIBUTE hiddenBlocksFound NUMERIC\n"; /* Bloques secretos encontrados */
		
		header += "@ATTRIBUTE screenCoins NUMERIC\n"; /* Monedas en pantalla */
		header += "@ATTRIBUTE screenBlocks NUMERIC\n"; /* Bloques en pantalla */
		header += "@ATTRIBUTE screenEnemies NUMERIC\n"; /* Enemigos en pantalla */
		
		/* CLASE */
		
		header += "@ATTRIBUTE action {move_jump, move, jump, still}\n\n"; /* Acción que ejecuta Mario. */
		header += "@data"; /* A partir de aquí informamos que se encuentran las instancias */
		
		return header; /* Devolvemos nuestro header */
	}
	
	/**
	*	getTrainingFile(): Este método se encarga de guardar la información 
	*	requerida de cada tick en una línea del fichero 'entrenamiento_original_1.arff'. Decidimos pasar la información
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
			000: MarioPosX
			001: MarioPosY
			002: Reward
			003: MergedObs[0][0]
			...
			363: MergedObs[18][18]
			364: distancePassedCells
			365: distancePassedPhys
			366: flowersDevoured
			367: killsByFire
			368: killsByShell
			369: killsByStomp
			370: killsTotal
			371: marioMode
			372: marioStatus
			373: mushroomsDevoured
			374: coinsGained
			375: timeLeft
			376: timeSpent
			377: hiddenBlocksFound
			378: screenCoins
			379: screenBlocks
			380: screenEnemies
			>CLASE
			381: action
		*/
		line = new String[382];
		int l = 0;
		
		if (tick == 0) /* Como la posición de Mario es constante, sólo necesitamos conocer la posición de Mario en el instante inicial. */
			/* Obtenemos la posición de Mario en la matriz. */
			marioPos = position;
		
		/* 
			A partir de aquí, vamos añadiendo los atributos a su posición del array correspondiente.
			Pasamos a String todas las variables que no lo sean.
		*/
		
		/* POSICIÓN MARIO */
		
		for(int i = 0; i<MarioFloatPos.length; ++i){
			line[l] = Float.toString(MarioFloatPos[i]); ++l;
		}
		
		/* REWARD */
		
		line[l]= Integer.toString(IntermediateReward); ++l;
		
		/*
			A continuación, necesitamos guardar toda la información que devuelve la matriz de
			getMergedObservationZZ(), pasada como argumento con mergedObservation.
			Hemos decidido el nivel de detalle 1 ya que se trata del nivel intermedio.
		*/
		
		/* MERGED OBSERVATION */
		
		for (int i = 0; i < mergedObservationZZ.length; ++i){
			for (int j = 0; j < mergedObservationZZ.length; ++j){
				line[l] = Byte.toString(mergedObservationZZ[i][j]); ++l;
			}
		}
		
		/* INFO EVALUACIÓN */
		
		for (int i = 0; i < infoEvaluacion.length; ++i){
			
			line[l] = Integer.toString(infoEvaluacion[i]); ++l;
		}
		
		/* 
			El nivel de detalle 1 es suficiente para determinar con un único
			código si la celda de la matriz se trata de un bloque o no, sin
			diferenciar entre tipos de bloques.
		*/
		
		/* NÚMERO DE MONEDAS Y BLOQUES EN PANTALLA */
		
		for (int i = 0; i < levelSceneObservationZ.length; ++i){
			for (int j = 0; j < levelSceneObservationZ.length; ++j){
				/* Las monedas tienen el código 2 */
				if (levelSceneObservationZ[i][j] == 2){
					screenCoins++;
				}
				/* Los bloques tienen el código -24 */
				else if (levelSceneObservationZ[i][j] == -24){
					screenBlocks++;
				}	
			}
		}
		
		/* Añadimos ambas variables a nuestro array */
		line[l]= Integer.toString(screenCoins); ++l;
		line[l]= Integer.toString(screenBlocks); ++l;
		
		/*
			En cambio, para conocer si hay un enemigo o no en una celda
			de la matriz de manera sencilla, nos tenemos que mover hasta
			el nivel de detalle 2, que nos indica sólo si hay o no enemigo.
		*/
		
		/* NÚMERO DE ENEMIGOS EN PANTALLA */
		
		for (int i = 0; i < EnemiesObservationZ.length; ++i){
			for (int j = 0; j < EnemiesObservationZ.length; ++j){
				/* Los enemigos tienen el código 1 */
				if (EnemiesObservationZ[i][j] == 1){
					screenEnemies++;
				}
			}
		}
		
		/* 
			Hemos preferido usar las matrices por separado y no la merged 
			ya que necesitamos distintos tipos de nivel de detalle en cada
			una y queremos proveer el mismo nivel de detalle en los atributos
			pertenecientes a MergedObs.
		*/
		
		/* Añadimos la variable a nuestro array */
		line[l] = Integer.toString(screenEnemies);
		
		/* 
			Una vez tenemos todos los elementos del 'pasado', pasamos a crear un elemento
			auxiliar, de manera que lo podamos insertar luego al final de nuestra linked list.
		*/
		
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
						environment.getEnemiesObservationZ(2));
		
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
		
		/* 
			Comprobamos para la celda justo en frente de Mario si hay algún tipo de obstáculo que lo pudiera bloquear.
			Consideramos aquí a los pinchones porque no son enemigos a los que se les pueda matar. 
		*/
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == -60 || mergedObservation[marioPos[0]][marioPos[1] + 1] == -85 || 
		   mergedObservation[marioPos[0]][marioPos[1] + 1] == -24 || mergedObservation[marioPos[0]][marioPos[1] + 1] == -62 || 
		   mergedObservation[marioPos[0]][marioPos[1] + 1] == 93)
        	isBlocked = true;
        
		/* Comprobamos para la celda justo en frente de Mario y la posterior si hay algún enemigo. */
		if(mergedObservation[marioPos[0]][marioPos[1] + 1] == 80 || mergedObservation[marioPos[0]][marioPos[1] + 2] == 80)
        	isEnemy = true;
		
		/* Comprobamos si Mario se encuentra pisando el suelo. */
		isOnGround = marioState[2];
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