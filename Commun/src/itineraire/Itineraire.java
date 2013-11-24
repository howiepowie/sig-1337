package itineraire;

import java.util.ArrayList;
import java.util.HashMap;

import base.Point;
import base.Segment;

/**
 * Classe permettant de calculer l'itinéraire entre un point départ et un point
 * arrivé
 */
public class Itineraire {
	/**
	 * Classe internet permettant de garder l'état d'un noeud
	 *
	 */
	private static class State implements Comparable<State> {
		/**
		 * Le chemin parcouru
		 */
		public ArrayList<Point> chemin;
		/**
		 * Le coût de ce chemin
		 */
		public double value;

		/**
		 * Crée l'état de départ
		 * @param depart le point de l'etat
		 */
		public State(Point depart) {
			chemin = new ArrayList<>();
			chemin.add(depart);
			this.value = 0;
		}

		/**
		 * Créer un noeud avec le chemin list et le coût value
		 * @param list le chemin
		 * @param value le coût du chemin
		 */
		public State(ArrayList<Point> list, double value) {
			chemin = list;
			this.value = value;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compareTo(State o) {
			if (value < o.value)
				return -1;
			else if (value == o.value)
				return 0;
			else
				return 1;
		}

		/**
		 * Retourne le dernier point du chemin
		 * @return
		 */
		public Point getHead() {
			return chemin.get(chemin.size() - 1);
		}
	}

	/**
	 * Calcul d'un itinéraire avec algo de type A* (fonction de décision : vol
	 * d'oiseau avec arrivé + distance parcouru)
	 * 
	 * @param depart
	 *            Le point de départ de l'itinéraire
	 * @param arrive
	 *            Le point d'arrivé de l'itinéaire
	 * @param listAdjacence
	 *            La liste d'adjacence (un graphe)
	 * @return la liste des points du parcours commençant par le point d'arrivé,
	 *         null si pas de chemin
	 */
	public static ArrayList<Point> CalculItineraire(Point depart, Point arrive,
			HashMap<Point, ArrayList<Point>> listAdjacence) {
		ArrayList<Point> res = new ArrayList<>();
		res.add(depart);
		if (!depart.equals(arrive)) {
			ArrayList<State> listState = new ArrayList<>();
			listState.add(new State(depart));
			return CalculItineraireRec(listState, arrive, listAdjacence);
		}
		return res;
	}

	/**
	 * 
	 * @param listState la liste des états à traiter classer par coût
	 * @param arrive Le point d'arrive
	 * @param listAdjacence la liste d'adjacence
	 * @return l'itinéraire calculé, null si aucun itinéraire
	 */
	private static ArrayList<Point> CalculItineraireRec(
			ArrayList<State> listState, Point arrive,
			HashMap<Point, ArrayList<Point>> listAdjacence) {
		if (listState.isEmpty())
			return null;
		else {
			// Je recupère le cas le plus intéressant
			State head = listState.remove(0);
			// je regarde si je suis à l'arrivé
			if (head.getHead().equals(arrive))
				return head.chemin;
			else {
				//je génère les nouveaux cas
				ArrayList<Point> voisin = listAdjacence.get(head.getHead());
				for (Point point : voisin) {
					// Evite de retourner dans un état précedant
					if (!head.chemin.contains(point)) {
						ArrayList<Point> newChemin = new ArrayList<>(
								head.chemin);
						newChemin.add(point);
						State ajout = new State(newChemin, calculValue(
								newChemin, arrive));
						//insertion dans la liste du nouveau état
						int i;
						for (i = 0; i < listState.size(); i++) {
							if (listState.get(i).compareTo(ajout) == 1) {
								listState.add(i, ajout);
								break;
							}
						}
						if(i == listState.size())
							listState.add(ajout);
					}
				}
				//appel récursif pour traiter les nouveaux états
				return CalculItineraireRec(listState, arrive, listAdjacence);
			}
		}
	}

	/**
	 * Calcul le coût d'un chemin
	 * @param chemin le chemin
	 * @param arrive le point d'arrivé 
	 * @return Le coût du chemin
	 */
	private static double calculValue(ArrayList<Point> chemin, Point arrive) {
		double res = 0;
		for (int i = 0; i < chemin.size() - 1; i++) {
			res += new Segment(chemin.get(i), chemin.get(i + 1)).longueur();
		}
		res += new Segment(chemin.get(chemin.size() - 1), arrive).longueur();
		return res;
	}

}
