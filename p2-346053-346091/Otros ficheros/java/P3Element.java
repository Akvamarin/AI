package ch.idsia.agents.controllers;

/* 
	Este 'Element' tiene el objetivo de guardar la tupla {id, instancia},
	que guardamos en la linked list del agente para obtener la información de hace seis ticks.
*/
public class P3Element{

    public  int id; /* Tick en la que se produjo la instancia. */
    public String[] line; /* Instancia con un atributo por posición del array. */

    public P2Element(int id, String[] l){
        this.id = id;
        line = l;
	}
}