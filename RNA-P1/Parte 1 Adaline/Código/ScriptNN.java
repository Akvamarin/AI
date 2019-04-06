import java.io.*; 
/*
	Práctica 1 - Redes de Neuronas Artificiales
	Autores: Antonio García Fernández & Alba María García García
	
	Este fichero contiene la implementación del 'script' que se encarga
	de pasar el fichero 'DatosPracticaEnergia', que no es compatible con Weka
	a uno equivalente y compatible con dicha herramienta.
*/

 public class ScriptNN{
	
	public static void main (String []args){
		
		/* Usamos try-catch para evitar errores derivados de la manipulación de los ficheros. */
		try{
			/* Comenzamos creando un buffer para leer el archivo de entrada. */
			FileReader input = new FileReader("DatosPracticaEnergia.txt");
			BufferedReader bufRead = new BufferedReader(input);
			
			/* Guardamos en myLine la primera línea de dicho archivo, ya que ésta contiene
			el nombre de todas las entradas de cada uno de los patrones; necesarias para el
			'header' del fichero .arff. */
			String myLine = bufRead.readLine();
			/* line guarda cada una de esas entradas en una posición del vector. */
			String[] line = myLine.split(" ");
			
			/* Creamos un buffer para escribir en el archivo de destino. */
			BufferedWriter tFile_header = new BufferedWriter(new FileWriter("DatosPracticaEnergia.arff", true), 30000);
	
			/* Escribimos el 'header' antes descrito en el fichero a partir del método getHeader(). */
			tFile_header.write(getHeader(line) + "\n");

			/* Ahora ya podemos leer el resto de líneas del archivo, una a una, de modo que
			cada uno de sus valores numéricos queden separados por comas. */
			while ((myLine = bufRead.readLine()) != null){  
				/* Guardamos en line los valores de la línea correspondiente en forma de
				vector, de modo que cada elemento está dividido por un espacio. */
				line = myLine.split(" ");
				
				/* Escribimos en el archivo de salida cada uno de dichos valores separados
				por comas, ignorando el primero, ya que es simplemente un identificador. */
				for(int j = 1; line.length - 1 > j; ++j){
					
					/* Escribimos en el archivo .arff. */
					tFile_header.write(line[j] + ",");
					
				}
				/* El último valor no debe tener una coma detrás, así que el 'for loop'
				lo ignora para escribir en su lugar un salto de línea. */
				tFile_header.write(line[line.length - 1] + "\n");
				
			}
			
			/* Cerramos ambos ficheros. */
			input.close();
			tFile_header.close();

		}
		
		/* En caso de error. */
		catch(IOException ex){
			/* Mostramos por el standard output el mensaje de error. */
			System.out.println (ex.toString());
		}
	}

	/**
	*	getHeader(): Este método se encarga de obtener en forma de String[] todo el texto que necesita el
	*	archivo 'DatosPracticaEnergia.arff' para poder procesar las entradas de los patrones en Weka.
	*
	*	@authors: Alba María García García & Antonio García Fernández
	*/
	public static String getHeader(String[] line){
		
		String header = ""; /* Inicializamos la variable a devolver */
		
		header += "@RELATION P1RedesNeuronas\n\n"; /* Nombre del fichero */
		
		/* Escribimos las líneas del 'header' correspondientes a las entradas usando siempre el mismo patrón,
		ya que todas son numéricas y su nombre ya se encuentra entrecomillado. */
		for(int i = 0; i < line.length; ++i){
			header +=  "@ATTRIBUTE " + line[i] + " NUMERIC\n";
		}
		
		header +=  "@data\n"; /* Grabamos @data en el buffer */
		return header; /* Devolvemos nuestro 'header' */
	}
}