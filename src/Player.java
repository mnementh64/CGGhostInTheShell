import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player
{

	/**
	 Entrées d'initialisation
	 Ligne 1 : en entier factoryCount, le nombre d'usines.
	 Ligne 2 :linkCount, le nombre de liaisons entre les usines.
	 Les linkCount lignes suivantes : 3 entiers séparés par un espace factory1, factory2 et distance. distance est le nombre de tours nécessaires pour envoyer une troupe de factory1 à factory2 (et inversement).
	 Entrées pour un tour de jeu
	 Ligne 1 : un entier entityCount le nombre d'entités.
	 Les entityCount lignes suivantes : un entier entityId, une chaîne de caractères entityType suivi par 5 entiers arg1, arg2, arg3, arg4 et arg5.
	
	 Si entityType vaut FACTORY alors les arguments représentent :
	
	 arg1 : joueur qui possède l'usine : 1 pour vous, -1 pour l'adversaire et 0 si neutre
	 arg2 : nombre de cyborgs dans l'usine
	 arg3 : production de l'usine (entre 0 et 3)
	 arg4 : inutilisé
	 arg5 : inutilisé
	
	 Si entityType vaut TROOP alors les arguments représentent :
	
	 arg1 : joueur qui possède la troupe : 1 pour vous, -1 pour l'adversaire
	 arg2 : identifiant de l'usine de départ
	 arg3 : identifiant de l'usine d'arrivée
	 arg4 : nombre de cyborgs au sein de la troupe (entier strictement positif)
	 arg5 : nombre de tours avant d'arriver à destination (entier strictement positif)
	
	 La liste des actions possibles est :
	
	 MOVE source destination cyborgCount : crée une troupe de cyborgCount cyborgs partant de l'usine dont l'identifiant est source à destination de l'usine dont l'identifiant est destination. Exemple : MOVE 2 4 12 enverra 12 cyborgs de l'usine 2 vers l'usine 4.
	 WAIT : ne fait rien.
	 */

	static Map<Integer, Factory> factoryMap = new HashMap<>();
	static Map<Integer, Troop> troopMap = new HashMap<>();
	static Map<Integer, Bomb> bombMap = new HashMap<>();

	public static void main(String args[])
	{
		Scanner in = new Scanner(System.in);
		int factoryCount = in.nextInt(); // the number of factories

		// memorize links to set them into factories
		List<Link> links = new ArrayList<>();

		int linkCount = in.nextInt(); // the number of links between factories
		for (int i = 0; i < linkCount; i++)
		{
			int factory1 = in.nextInt();
			int factory2 = in.nextInt();
			int distance = in.nextInt();
			Link link = new Link();
			link.idFactorySrc = factory1;
			link.idFactoryTgt = factory2;
			link.distance = distance;
			links.add(link);
		}

		// game loop
		boolean first = true;
		while (true)
		{
			int entityCount = in.nextInt(); // the number of entities (e.g. factories and troops)
			troopMap.clear();
			for (int i = 0; i < entityCount; i++)
			{
				int entityId = in.nextInt();
				String entityType = in.next();
				int arg1 = in.nextInt();
				int arg2 = in.nextInt();
				int arg3 = in.nextInt();
				int arg4 = in.nextInt();
				int arg5 = in.nextInt();
				if (entityType.equals("FACTORY"))
				{
					Factory.update(factoryMap, entityId, arg1, arg2, arg3);
				}
				else if (entityType.equals("TROOP"))
				{
					Troop.update(troopMap, entityId, arg1, arg2, arg3, arg4, arg5);
				}
				else
				{
					Bomb.update(bombMap, entityId, arg1, arg2, arg3, arg4);
				}
			}

			// TODO : post add links (in both ways) to factories
			if (first)
				Factory.updateLinks(factoryMap, links);

			if (first)
				debugSystem();
			else
				debugTroops();

			System.out.println(MetaFactory.decide(factoryMap, troopMap));
			first = false;
		}
	}

	private static void debugSystem()
	{
		System.err.println("Factories :");
		factoryMap.values().forEach(f ->
		{
			System.err.println("\tFactory " + f.toString());
			f.links.forEach(l -> System.err.println("\t\tLink to " + l.idFactoryTgt + " at distance " + l.distance));
		});
		debugTroops();
	}

	private static void debugTroops()
	{
		System.err.println("Troops :");
		troopMap.values().forEach(t -> System.err.println("\tTroop " + t.toString()));
	}

	static class MetaFactory
	{

		static String decide(Map<Integer, Factory> factoryMap, Map<Integer, Troop> troopMap)
		{
			StringBuilder command = new StringBuilder("WAIT;");

			// ask each factory to do its job and return the command and its level of interest
			factoryMap.values().stream()
					.filter(f -> Owner.isMine(f.owner))
					.forEach(factory ->
					{
						Decision decision = factory.computeDecision(factoryMap, troopMap);
						command.append(decision.command).append(";");
					});
			if (command.toString().endsWith(";"))
				command.deleteCharAt(command.length() - 1);
			return command.length() > 0 ? command.toString() : "WAIT";
		}
	}
}

class Factory
{

	int id;
	int owner;
	int nbCyborgs;
	int productionUnits;
	List<Link> links = new ArrayList<>();

	static void update(Map<Integer, Factory> factoryMap, int id, int owner, int nbCyborgs, int productionUnits)
	{
		Factory factory = factoryMap.computeIfAbsent(id, integer -> new Factory());
		factory.id = id;
		factory.owner = owner;
		factory.nbCyborgs = nbCyborgs;
		factory.productionUnits = productionUnits;
	}

	@Override
	public String toString()
	{
		return id + "/" + Owner.getOwner(owner) + " : " + nbCyborgs + " cyborgs / " + productionUnits + " units";
	}

	static void updateLinks(Map<Integer, Factory> factoryMap, List<Link> links)
	{
		links.forEach(link ->
		{
			// create a link from src to tgt
			Factory srcFactory = factoryMap.get(link.idFactorySrc);
			srcFactory.links.add(link);

			// create a link from tgt to src
			Link reverseLink = new Link();
			reverseLink.idFactorySrc = link.idFactoryTgt;
			reverseLink.idFactoryTgt = link.idFactorySrc;
			reverseLink.distance = link.distance;
			Factory tgtFactory = factoryMap.get(link.idFactoryTgt);
			tgtFactory.links.add(reverseLink);
		});
	}

	Decision computeDecision(Map<Integer, Factory> factoryMap, Map<Integer, Troop> troopMap)
	{
		Link bestLink = null;
		int bestInterest = -Integer.MAX_VALUE;
		System.err.println("Factory " + this.id + " - evaluation ...");
		for (Link link : this.links)
		{
			int interest = evaluateMove(link.idFactoryTgt, link.distance, factoryMap, troopMap);
			System.err.println("\tCheck link to " + link.idFactoryTgt + " - interest : " + interest);
			if (interest > bestInterest)
			{
				bestInterest = interest;
				bestLink = link;
			}
		}
		System.err.println("\t--> best is link to " + bestLink.idFactoryTgt + " - interest : " + bestInterest);
		return new Decision(bestInterest, from(bestLink, factoryMap.get(bestLink.idFactoryTgt)));
	}

	private int evaluateMove(int idFactoryTgt, int distance, Map<Integer, Factory> factoryMap, Map<Integer, Troop> troopMap)
	{
		// if some troops of mine already move to target, then abort
		List<Troop> movingTroops = troopMap.values().stream()
				.filter(t -> t.idFactoryTgt == idFactoryTgt && Owner.isMine(t.owner))
				.collect(Collectors.toList());
		if (!movingTroops.isEmpty())
			return -80;

		Factory targetFactory = factoryMap.get(idFactoryTgt);
		int value = 100;

		// neutral / opponent ? skip own factories
		if (Owner.isMine(targetFactory.owner))
			return -100;
		value += Owner.isOpponent(targetFactory.owner) ? 10 : -10;

		// distance
		value -= 3 * distance;

		// Nb Production units --> malus for 0 production units
		if (targetFactory.productionUnits == 0)
			value -= 10;
		else
			value += 15 * targetFactory.productionUnits;

		// can move only half the cyborgs --> no need to move if they are more numerous
//		int half = nbCyborgs / 2;
//		if (half <= targetFactory.nbCyborgs)
//			return 0;
//		int delta = half - targetFactory.nbCyborgs;
//		value += delta;
		if (nbCyborgs <= targetFactory.nbCyborgs)
			return -10;
		int delta = nbCyborgs - targetFactory.nbCyborgs;
		value += delta;

		return value;
	}

	private String from(Link link, Factory targetFactory)
	{
		return link == null ? "WAIT" : "MOVE " + link.idFactorySrc + " " + link.idFactoryTgt + " " + (targetFactory.nbCyborgs + 2);
	}
}

class Link
{

	int idFactorySrc;
	int idFactoryTgt;
	int distance;
}

class Troop
{

	int id;
	int owner;
	int idFactorySrc;
	int idFactoryTgt;
	int nbCyborgs;
	int timeRemaining;

	static void update(Map<Integer, Troop> troopMap, int id, int owner, int idFactorySrc, int idFactoryTgt, int nbCyborgs, int timeRemaining)
	{
		Troop troop = troopMap.computeIfAbsent(id, integer -> new Troop());
		troop.id = id;
		troop.owner = owner;
		troop.idFactorySrc = idFactorySrc;
		troop.idFactoryTgt = idFactoryTgt;
		troop.nbCyborgs = nbCyborgs;
		troop.timeRemaining = timeRemaining;
	}

	@Override
	public String toString()
	{
		return id + "/" + Owner.getOwner(owner) + " -> " + nbCyborgs + " cyborgs from " + idFactorySrc + " to " + idFactoryTgt + ". Reach in " + timeRemaining;
	}
}

class Bomb
{

	int id;
	int owner;
	int idFactorySrc;
	int idFactoryTgt;
	int timeRemaining;

	static void update(Map<Integer, Bomb> bombMap, int id, int owner, int idFactorySrc, int idFactoryTgt, int timeRemaining)
	{
		Bomb bomb = bombMap.computeIfAbsent(id, integer -> new Bomb());
		bomb.id = id;
		bomb.owner = owner;
		bomb.idFactorySrc = idFactorySrc;
		bomb.idFactoryTgt = idFactoryTgt;
		bomb.timeRemaining = timeRemaining;
	}

	@Override
	public String toString()
	{
		return id + "/" + Owner.getOwner(owner) + " -> from " + idFactorySrc + " to " + idFactoryTgt + ". Reach in " + timeRemaining;
	}
}

class Owner
{

	static boolean isMine(int owner)
	{
		return owner == 1;
	}

	static boolean isOpponent(int owner)
	{
		return owner == -1;
	}

	static boolean isNeutral(int owner)
	{
		return owner == 0;
	}

	static String getOwner(int owner)
	{
		return owner == 1 ? "me" : owner == -1 ? "opp" : "neu";
	}
}

class Decision
{

	int interest;
	String command;

	Decision()
	{
	}

	Decision(int interest, String command)
	{
		this.interest = interest;
		this.command = command;
	}
}