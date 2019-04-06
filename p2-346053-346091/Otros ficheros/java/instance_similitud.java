package ch.idsia.agents.controllers;

/* 
	'instance_similitud' tiene el objetivo de dar formato a cada uno de los
	elementos que usamos para trabajar en la función de similitud.
*/
public class instance_similitud{

   
	/* Atributos necesarios para la función de similitud */
	public int id;
	public int similitud;

    public instance_similitud(int id, int similitud){
		this.id = id;
		this.similitud = similitud;
	}
}