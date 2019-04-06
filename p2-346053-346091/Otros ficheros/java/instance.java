package ch.idsia.agents.controllers;

/* 
	'instance' tiene el objetivo de dar formato a cada una de las instancias
	que posee Mario en su base de conocimiento.
*/
public class instance{

   
	/* Atributos necesarios para la función de similitud */
    public String isEnemy;
	public String isCoinMushroom;
	public String isBlocked;
	public String isOnGround;
	public String jumped;
	/* Acción de Mario */
	public String action;
	/* Evaluación de la instancia */
	public double evaluation;

    public instance(String e, String c, String b, String g, String j, String a, double eval){
        isEnemy = e;
		isCoinMushroom = c;
		isBlocked = b;
		isOnGround = g;
		jumped = j;
		action = a;
		evaluation = eval;
	}
}