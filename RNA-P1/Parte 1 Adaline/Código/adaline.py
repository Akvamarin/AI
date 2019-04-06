#!/usr/bin/env python
"""
Practica 1 - Redes de Neuronas Artificiales
Autores: Alba María García García & Antonio García Fernández
Aquí está implementado el algoritmo completo de Adaline,
desde que obtiene la información del fichero hasta que muestra
los resultados.
"""
import sys #Necesario para poder manejar los argumentos que pasamos por terminal
import random #Necesario para obtener números aleatorios
#Imports necesarios para poder imprimir las gráficas de resultados
import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import matplotlib.patches as mpatches

"""
Método que, utilizando un fichero de entrada, obtiene su información
y la guarda en una matriz; siempre que tenga la estructura adecuada.
"""
def get_matrix(infile):
	#Guardamos un elemento por cada línea del fichero en la lista
	lines = infile.read().splitlines()
	#Declaramos la matriz en la guardaremos la información
	matrix = []

	#Recorremos cada una de las líneas
	for word in lines:
		#Guardamos en word los elementos de la línea separados por comas
		word = word.split(",")
		#Añadimos una fila entera de la matriz (lista)
		matrix.append(word)
	
	#Borramos la información de la matriz desde el principio hasta
	#que donde se encuentre '@data' (el header), ya que sólo queremos los datos
	del matrix[0:matrix.index(['@data']) + 1]
	#Devolvemos la matriz
	return matrix

"""
Método que obtiene la lista de pesos iniciales para el método Adaline
Necesita el número de pesos que requiere la red como argumento.
"""	
def random_weights(length):
	#Inicializamos la lista de pesos
	weights = [0] * (length)
	#Recorremos el vector de pesos
	for i in range(len(weights)):
		#Obtenemos un valor aleatorio entre 0 y 1
		weights[i] = random.uniform(0,1)
		
	#Devolvemos la lista de pesos	
	return weights

"""
Método que obtiene un número aleatorio entre 0 y 1.
Utilizado para inicializar el umbral
"""
def random_threshold():
	return random.uniform(0,1)

"""
Método que devuelve delta, es decir, el cambio que se le debe aplicar
a un peso para ajustarlo. Atributos: tasa de aprendizaje, salida esperada,
salida obtenida por Adaline y el valor del atributo correspondiente al peso.
"""	
def get_delta(gamma, desired_result, obtained_result, data):
	return gamma*(desired_result - obtained_result)*data

"""
Método que devuelve delta para el caso el umbral, es decir, el cambio
que se le debe aplicar al umbral para ajustarlo. Atributos: tasa de
aprendizaje, salida esperada y salida obtenida por Adaline.
"""	
def get_delta_threshold(gamma, desired_result, obtained_result):
	return gamma*(desired_result - obtained_result)
"""
Método que obtiene el error cuadrático para los resultados
obtenidos en un patrón. Atributos: salida obtenida por Adaline y
salida esperada.
"""
def get_cuadratic_error(obtained_result, desired_result):
	return (obtained_result - desired_result)**2

"""
Método que desnormaliza un valor dado su rango mínimo y máximo.
"""
def denormalize(normal_val, min_val, max_val):
	return normal_val * (max_val - min_val) + min_val

#MAIN
#PARTE 1: Obtención de los datos de los ficheros de entrada

#Sólo seguimos con el programa si hay tres argumentos
if (len(sys.argv) < 4):
	print("ERROR: Se necesitan tres argumentos: fichero de entrenamiento, fichero de validación y fichero de test.")
	raise SystemExit

#Comprobamos que no ha habido un error al abrir el fichero
try: 
    #Abrimos el fichero con los datos de entrenamiento
	entr_name = open(sys.argv[1],'r')
	#Abrimos el fichero con los datos de validación
	val_name = open(sys.argv[2],'r')
	#Abrimos el fichero con los datos de test
	test_name = open(sys.argv[3],'r')
except:
    print("ERROR: Los argumentos deben ser ficheros de texto.")
    raise SystemExit

#Obtenemos su información en forma de matriz
entr_matrix = get_matrix(entr_name)

#Obtenemos su información en forma de matriz
val_matrix = get_matrix(val_name)

#Obtenemos su información en forma de matriz
test_matrix = get_matrix(test_name)

#Parte 2: Algoritmo de Adaline

#En estas dos listas vamos a guardar los errores de entrenamiento
#y validación, respectivamente
entr_error_list = []
val_error_list = []

#Inicializamos los errores de entrenamiento y validación a 0
#Se usa past_val_error para poder comparar los errores de validación
#del ciclo actual y el anterior (necesario para el criterio de parada)
entr_error = 0
val_error = 0
past_val_error = 0

#Estas dos variables sirven para guardar los dos errores desnormalizados
#de entrenamiento y validación
entr_error_d = 0
val_error_d = 0

#Parte 2.1: Inicializamos aleatoriamente los pesos y el umbral

#Para inicializar los pesos, cogemos la longitud de la primera línea de 
#la matriz de entrenamiento (cualquier línea valdría) menos 1, ya
#que a todos los atributos les corresponde peso menos a la salida
weights = random_weights(len(entr_matrix[0]) - 1)
#Inicializamos el umbral
threshold = random_threshold()

#Parte 2.2: Presentamos los patrones de entrada a Adaline (aprendizaje)

#Inicializamos las variables que guardarán los resultados de Adaline
#tanto para el fichero de entrenamiento como el de validación
pattern_result_entr = 0
pattern_result_val = 0

#Inicializamos los iteradores y el contador de ciclos
i = 0
j = 0
cycle = 0

#Inicializamos la variable de control (necesaria para el criterio de parada)
error_ctrl = 0

#Abrimos min-max.txt, que contiene el valor mínimo y máximo de las salidas
min_max = open('min-max.txt','r')
lines = min_max.readlines()
#Guardamos el mínimo y el máximo en una lista
output = lines[0].split("\t")

min_denormalize = output[0]
max_denormalize = output[1]

#CRITERIO DE PARADA. Adaline no ejecutará ningún ciclo más cuando:
# - Nos encontremos en el ciclo inicial
# - El error de validación crece durante al menos 10 ciclos (no necesariamente consecutivos)
#y los dos últimos valores de validación no son iguales
#Esto quiere decir que siempre hará al menos un ciclo y que parará en cuanto los dos
#últimos errores de precisión son iguales con una precisión de 5 decimales o dicho
#error ha aumentado durante 10 ciclos
while (cycle < 1 or (error_ctrl < 10 and round(val_error, 5) != round(past_val_error, 5))):
	
	#Ejecución de 1 ciclo
	for i in range(len(entr_matrix)):
		
		#Reseteamos los valores obtenidos por Adaline para cada patrón
		pattern_result_entr = 0
		pattern_result_val = 0
			
		#Calculamos la salida de Adaline para cada patrón del fichero de entrenamiento
		#Primero sumamos los pesos multiplicados por los valores de los atributos
		for j in range(len(weights)):
			pattern_result_entr += float(weights[j])*float(entr_matrix[i][j])
		#Luego sumamos el umbral
		pattern_result_entr += threshold
		
		#La funcion de activacion de Adaline es lineal => f(x)=x
		#Ajustamos los pesos y el umbral
		for j in range(len(weights)):
			#Calculamos el nuevo valor de cada uno de los pesos
			weights[j] += get_delta(0.001, float(entr_matrix[i][len(weights)]), float(pattern_result_entr), float(entr_matrix[i][j]))
		#Ajustamos el umbral
		threshold += get_delta_threshold(0.001, float(entr_matrix[i][len(weights)]), float(pattern_result_entr))
	
	#Actualizamos el error de validación del ciclo anterior
	past_val_error = val_error
	#Reseteamos los errores de entrenamiento y validación
	entr_error = 0
	val_error = 0
	entr_error_d = 0
	val_error_d = 0
	
	#Cálculo del error de entrenamiento y validación para un ciclo
	for i in range (len(entr_matrix)):
		
		#Reseteamos los valores obtenidos por Adaline para cada patrón
		pattern_result_entr = 0
		pattern_result_val = 0
		
		#Volvemos a calcular el resultado de Adaline para nuestros datos,
		#pero esta vez con los pesos obtenidos al final del ciclo y con
		#los ficheros de entrenamiento y validación
		for j in range(len(weights)):
			#Fichero de entrenamiento
			pattern_result_entr += float(weights[j])*float(entr_matrix[i][j])
			#Fichero de validación (es más pequeño, así que comprobamos que
			#estamos dentro del rango de datos)
			if i < len(val_matrix):
				pattern_result_val += float(weights[j])*float(val_matrix[i][j])

		#Añadimos el umbral para el fichero de entrenamiento
		pattern_result_entr += threshold
		#Añadimos el umbral para el fichero de validación
		if i < len(val_matrix):
			pattern_result_val += threshold
		
		#Obtenemos el error cuadrático en el fichero de entrenamiento del fichero de entrenamiento
		entr_error += get_cuadratic_error(float(entr_matrix[i][len(weights)]), float(pattern_result_entr))
		entr_error_d += get_cuadratic_error(denormalize(float(entr_matrix[i][len(weights)]), float(min_denormalize), float(max_denormalize)), denormalize(float(pattern_result_entr), float(min_denormalize), float(max_denormalize)))
		#Error cuadrático medio del fichero de validación
		if i < len(val_matrix):
			val_error += get_cuadratic_error(float(val_matrix[i][len(weights)]), float(pattern_result_val))
			val_error_d += get_cuadratic_error(denormalize(float(val_matrix[i][len(weights)]), float(min_denormalize), float(max_denormalize)), denormalize(float(pattern_result_val), float(min_denormalize), float(max_denormalize)))
	
	#Calculamos el error cuadrático medio	
	entr_error /= (len(entr_matrix))
	val_error /= (len(val_matrix))

	entr_error_d /= (len(entr_matrix))
	val_error_d /= (len(val_matrix))
	
	#Añadimos estos últimos valores a la lista de errores de entrenamiento y validación
	entr_error_list.append(entr_error)
	val_error_list.append(val_error)
	
	#Nos movemos al siguiente ciclo
	cycle += 1
	#Imprimimos los resultados para este ciclo
	print('Ciclo ' + str(cycle) + '. Error entrenamiento : ' + str(entr_error) + ' Error validacion : ' + str(val_error))
	
	#Finalmente, comprobamos que el error de validación no ha subido (y que no estamos
	#en el primer ciclo, ya que el error inicial es 0 y siempre va a aumentar)
	if (val_error > past_val_error and cycle != 0):
		error_ctrl += 1

#Parte 3: Gráfica de evolución del error

#Multiplicamos las listas que contienen los errores de entrenamiento y validación
#por el número de entradas, de modo que el error que guarden no sea el error cuadrático
#medio, sino el error cuadrático a secas
entr_error_list[:] = [x * (len(entr_matrix)) for x in entr_error_list]
val_error_list[:] = [x * (len(val_matrix)) for x in val_error_list]

#Guardamos todos los ciclos que hemos ido recorriendo durante el período de aprendizaje
#(empezando por 1)
cycles_list = []
cycles_list.extend(range(1, cycle + 1))

#Creamos las variables que guardan la información en x e y de
#las dos series que queremos representar
x1 = np.array(cycles_list)
y1 = np.array(entr_error_list)

x2 = np.array(cycles_list)
y2 = np.array(val_error_list)

plt.title("Evolución del error a lo largo del aprendizaje") #Título de la gráfica
plt.grid(True) #Añadimos una red
plt.xlabel("Ciclos") #Valor eje x
plt.ylabel("Error") #Valor eje y
#Leyenda
blue_patch = mpatches.Patch(color="#E5276F", label='Datos de entrenamiento')
orange_patch = mpatches.Patch(color="#35BBE0", label='Datos de validación')
plt.legend(handles=[blue_patch, orange_patch])
#Límites de la gráfica
plt.xlim(0, cycle)
plt.ylim(0, max(entr_error_list[0], val_error_list[0]))

#Dibujamos la gráfica (elegimos color)
plt.plot(x1, y1, color="#E5276F")
plt.plot(x2, y2, color="#35BBE0")

plt.show()

#Parte 4: Escribimos los resultados en el fichero de salida

print('APREDIZAJE ACABADO : Escribiendo pesos y umbral en fichero...')
archivo_aprendizaje = open('pesos_umbral.txt','w')

#Escribimos cada uno de los pesos y el umbral en una línea del fichero
for item in weights:
	archivo_aprendizaje.write(str(item) + "\n")
archivo_aprendizaje.write("\n" + str(threshold))

#Parte 5: Guardamos los resultados obtenidos por Adaline en un fichero
test_error = 0 #Error para el fichero de test
test_error_d = 0 #Error para el fichero de test desnormalizado

#Encabezados de los resultados para entrenamiento, validación y test
denormalized_entr = "Resultados desnormalizados del fichero de ENTRENAMIENTO:\n\n"
denormalized_val = "\nResultados desnormalizados del fichero de VALIDACION:\n\n"
denormalized_test = "\nResultados desnormalizados del fichero de TEST:\n\n"

#Listas que guardan los valores obtenidos para el fichero de test y los deseados
test_value_list = [0] * len(test_matrix)
test_desired_list = [0] * len(test_matrix)

#Recorremos el fichero de entrenamiento, ya que es el más largo
for i in range(len(entr_matrix)):

	#Reseteamos los valores obtenidos para entrenamiento, validación y test
	pattern_result_entr = 0
	pattern_result_val = 0
	pattern_result_test = 0
	
	#Calculamos la salida de Adaline para cada patrón de cada fichero
	for j in range(len(weights)):
		#Fichero de entrenamiento
		pattern_result_entr += float(weights[j])*float(entr_matrix[i][j])
		#Fichero de validación (comprobando que no nos salimos de sus límites)
		if i < len(val_matrix):
			pattern_result_val += float(weights[j])*float(val_matrix[i][j])
		#Fichero de test (comprobando que no nos salimos de sus límites)
		if i < len(test_matrix):
			pattern_result_test += float(weights[j])*float(test_matrix[i][j])
	
	#Sumamos el umbral al resultado anterior	
	pattern_result_entr += threshold #Entrenamiento
	if i < len(val_matrix):
		pattern_result_val += threshold #Validación
	if i < len(test_matrix):
		pattern_result_test += threshold #Test
	
	#Obtenemos el error cuadrático del fichero de test, que no lo habíamos calculado antes	
	if i < len(test_matrix):
		test_error += get_cuadratic_error(float(test_matrix[i][len(weights)]), float(pattern_result_test))
		test_error_d += get_cuadratic_error(denormalize(float(test_matrix[i][len(weights)]), float(min_denormalize), float(max_denormalize)), denormalize(float(pattern_result_test), float(min_denormalize), float(max_denormalize)))
	
	#Desnormalizamos los resultados de entrenamiento
	denormalized_entr += str(denormalize(float(pattern_result_entr), float(min_denormalize), float(max_denormalize))) + '\n'
	if i < len(val_matrix):
		#Desnormalizamos los resultados de validación
		denormalized_val += str(denormalize(float(pattern_result_val), float(min_denormalize), float(max_denormalize))) + '\n'
	if i < len(test_matrix):
		#Desnormalizamos los resultados de test
		denormalized_test += str(denormalize(float(pattern_result_test), float(min_denormalize), float(max_denormalize))) + '\n'
		#Guardamos los valores obtenidos para el fichero de test y los deseados para hacer la última gráfica
		test_value_list[i] = denormalize(float(pattern_result_test), float(min_denormalize), float(max_denormalize))
		test_desired_list[i] = denormalize(float(test_matrix[i][len(weights)]), float(min_denormalize), float(max_denormalize))

test_error /= (len(test_matrix))
test_error_d /= (len(test_matrix))
#Parte 6: Gráfica que compara los resultados para el fichero de test

#Creamos las variables que guardan la información en x e y de
#las dos series que queremos representar
x1 = np.array(test_value_list)
y1 = np.array(test_desired_list)

x2 = [0, max(test_value_list) + 1]
y2 = [0, max(test_desired_list) + 1]

plt.title("Salidas obtenidas vs deseadas") #Título de la gráfica
plt.grid(True) #Añadimos una red
plt.xlabel("Obtenidos") #Valor eje x
plt.ylabel("Deseados") #Valor eje y
#Límites de la gráfica
plt.xlim(0, max(test_value_list) + 1)
plt.ylim(0, max(test_desired_list) + 1)

#Dibujamos la gráfica (elegimos color)
plt.scatter(x1, y1, color="#E5276F")
plt.plot(x2, y2, color="#35BBE0")

plt.show()

#Imprimimos los errores finales para entrenamiento, validación y test
print('----FINAL----\nError entrenamiento : ' + str(entr_error) + '\nError validacion : ' + str(val_error) + '\nError test : ' + str(test_error))	
print('----FINAL----\nError entrenamiento desnorm. : ' + str(entr_error_d) + '\nError validacion desnorm. : ' + str(val_error_d) + '\nError test desnorm. : ' + str(test_error_d))

#Escribimos en el fichero "resultados_finales.txt" las salidas desnormalizadasn
dernormalized_file = open('resultados_finales.txt','w')
dernormalized_file.write(denormalized_entr) #Entrenamiento
dernormalized_file.write(denormalized_val) #Validación
dernormalized_file.write(denormalized_test) #Test