import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	static int nbBombs = 2;

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
			bombMap.clear();
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

			// add links (in both ways) to factories
			if (first)
				Factory.updateLinks(factoryMap, links);

			if (first)
				debugSystem();
			else
//				debugTroops();
				debugBombs();

			String commands = MetaFactory.decide(factoryMap, troopMap, bombMap, nbBombs);
			System.out.println(commands);

			// decrease bomb number
			if (commands.contains("BOMB"))
			{
				int index1 = commands.indexOf("BOMB");
				int index2 = commands.indexOf("BOMB", index1 + 1);
				nbBombs--;
				nbBombs -= index2 > 0 ? 1 : 0;
			}

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

	private static void debugBombs()
	{
		System.err.println("Bombs :");
		bombMap.values().forEach(b -> System.err.println("\tBomb " + b.toString()));
	}

	static class MetaFactory
	{

		static String decide(Map<Integer, Factory> factoryMap, Map<Integer, Troop> troopMap, Map<Integer, Bomb> bombMap, int nbBombs)
		{
			List<Action> actions = new ArrayList<>();

			// ask each factory to do its job and return the command and its level of interest
			actions.addAll(factoryMap.values().stream()
					.filter(f -> Owner.isMine(f.owner))
					.flatMap(f -> f.computeDefaultActions(factoryMap, troopMap, bombMap).stream())
					.filter(a -> !a.which.equals("WAIT"))
					.collect(Collectors.toList()));

			// check if some of my troops already move to these targets, so reduce the number of cyborgs to move

			// check for HELP

			// Opportunity for BOMBs
			List<Action> bombActions = checkForBombs(factoryMap, troopMap, bombMap, nbBombs);
			// bomb actions have higher priority than move actions
			if (!bombActions.isEmpty())
			{
				Iterator<Action> iter = actions.iterator();
				while (iter.hasNext())
				{
					Action action = iter.next();
					if (!action.which.equals("MOVE"))
						continue;
					ActionMove move = (ActionMove) action;

					boolean bombActionMatch = bombActions.stream()
							.anyMatch(a -> ((ActionBomb) a).from == move.from && ((ActionBomb) a).to == move.to);
					if (bombActionMatch)
						iter.remove();
				}
				actions.addAll(bombActions);
			}

			// convert all actions into command
			String command = actions.stream()
					.map(Action::toCommand)
					.collect(Collectors.joining(";"));

			return command.length() > 0 ? command : "WAIT";
		}

		static List<Action> checkForBombs(Map<Integer, Factory> factoryMap, Map<Integer, Troop> troopMap, Map<Integer, Bomb> bombMap, int nbBombs)
		{
			List<Action> actions = new ArrayList<>();

			if (nbBombs == 0)
				return actions;
			System.err.println("Bomb evaluation ...");

			actions.addAll(factoryMap.values().stream()
					// target an opponent factory with 3 production units
					.filter(f -> Owner.isOpponent(f.owner) && f.productionUnits == 3)
					.peek(f -> System.err.println("\tOpponent factory candidate " + f.id))
					// ignore a factory already targeted by a bomb
					.filter(f -> !bombMap.values().stream()
							.anyMatch(b -> b.idFactoryTgt == f.id))
					// find the closest of my factories
					.map(f -> f.links.stream()
							.filter(l -> Owner.isMine(factoryMap.get(l.idFactoryTgt).owner))
							.sorted((l1, l2) -> l1.distance >= l2.distance ? 1 : -1)
							.peek(l -> System.err.println("\t\t Link candidate to my factory " + l.idFactoryTgt + " at " + l.distance))
							.findFirst())
					.filter(Optional::isPresent)
					.map(Optional::get)
					.peek(l -> System.err.println("\t Link found to my factory " + l.idFactoryTgt))
					.sorted((l1, l2) -> l1.distance >= l2.distance ? 1 : -1)
					.limit(nbBombs)
					.map(l -> (Action) new ActionBomb(0, l.idFactoryTgt, l.idFactorySrc))
					.collect(Collectors.toList()));

			actions.forEach(a -> System.err.println("\t-> " + a.toCommand()));

//			// target a factory with more than 50 cyborgs
//			List<Action> bombActions = factoryMap.values().stream()
//					.filter(f -> Owner.isOpponent(f.owner) && f.nbCyborgs > 50)
//					.sorted((f1, f2) -> f1.nbCyborgs > f2.nbCyborgs ? -1 : 1)
//					.limit(2 - nbBombs)
//					.map(f -> (Action) new ActionBomb(0, -1, f.id))
//					.collect(Collectors.toList());
//
//			// need to find the right number of my factories to send bombs
//			// check for the one with the less number of cyborgs
//			if (!bombActions.isEmpty())
//			{
//				List<Factory> myFactories = factoryMap.values().stream()
//						.filter(f -> Owner.isMine(f.owner))
//						.sorted((f1, f2) -> f1.nbCyborgs > f2.nbCyborgs ? 1 : -1)
//						.limit(bombActions.size())
//						.collect(Collectors.toList());
//
//				((ActionBomb) bombActions.get(0)).from = myFactories.get(0).id;
//				actions.add(bombActions.get(0));
//				if (bombActions.size() == 2)
//				{
//					((ActionBomb) bombActions.get(1)).from = myFactories.get(1).id;
//					actions.add(bombActions.get(1));
//				}
//			}

			return actions;
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

	List<Action> computeDefaultActions(Map<Integer, Factory> factoryMap, Map<Integer, Troop> troopMap, Map<Integer, Bomb> bombMap)
	{
		System.err.println("Factory " + this.id + " - evaluation ...");
		List<Action> actions = new ArrayList<>();

		// look for :
		// - the closest non-mine link
		// - the less numerous
		// - with less (cyborgs + production units) than current
		List<CandidateFactory> candidateTargetFactories = links.stream()
				// non-mine factories with less cyborgs than mine
				// Of course, ignore factories I have sent a bomb to !!
				.filter(l ->
				{
					Factory targetFactory = factoryMap.get(l.idFactoryTgt);
					boolean iGonnaBombThisOne = bombMap.values().stream()
							.anyMatch(b -> Owner.isMine(b.owner) && b.idFactoryTgt == targetFactory.id);
					return !Owner.isMine(targetFactory.owner) && targetFactory.nbCyborgs < this.nbCyborgs && !iGonnaBombThisOne;
				})
				// closest first
				.sorted((l1, l2) ->
				{
					if (l1.distance != l2.distance)
						return l1.distance >= l2.distance ? 1 : -1;
					else
					{
						Factory t1 = factoryMap.get(l1.idFactoryTgt);
						Factory t2 = factoryMap.get(l2.idFactoryTgt);
						return t1.nbCyborgs >= t2.nbCyborgs ? 1 : -1;
					}
				})
				.map(l -> new CandidateFactory(l.distance, factoryMap.get(l.idFactoryTgt)))
				.collect(Collectors.toList());
		if (candidateTargetFactories.isEmpty())
			return actions;

		// now, create a move action for each target factory, ensuring source factory have enough cyborgs to move
		// number of cyborgs available for move actions
		int nbCyborgsToDispatch = nbCyborgs - 1;
		for (CandidateFactory candidateFactory : candidateTargetFactories)
		{
			System.err.println("-> check target factory " + candidateFactory.factory.id + " at distance " + candidateFactory.distance);
			// compute the target factory's number of cyborgs when move ends
//			int nbOpponents = targetFactory.projects(l.distance, troopMap);
			int nbOpponents = candidateFactory.factory.nbCyborgs;

			// enough cyborgs left to fight ?
			if (nbCyborgsToDispatch <= nbOpponents)
				continue;

			// if other troops of mine already go there, then compute the difference
//					int alreadyGo = troopMap.values().stream()
//							.filter(t -> Owner.isMine(t.owner))

			// ok to move !
			int nb = nbOpponents + 1;
			nbCyborgsToDispatch -= nb;
			System.err.println("\ttry to move " + nb + " to target " + candidateFactory.factory.id + " at " + candidateFactory.distance + " (remains "
					+ nbCyborgsToDispatch + ")");
			actions.add(new ActionMove(10, this.id, candidateFactory.factory.id, nb));
		}

		// No move action found ? Some cyborgs remain ? Then attack also bigger factories
		if (actions.isEmpty() || nbCyborgsToDispatch > 0)
			actions.add(computeOtherActions(nbCyborgsToDispatch, factoryMap, troopMap, bombMap));

		System.err.println("\t--> " + actions.size() + " actions found");
		return actions;
	}

	Action computeOtherActions(final int nbCyborgsToDispatch, Map<Integer, Factory> factoryMap, Map<Integer, Troop> troopMap, Map<Integer, Bomb> bombMap)
	{
		System.err.println("Factory " + this.id + " - evaluation for other actions ...");

		// look for :
		// - the closest non-mine link
		final Action action;
		Optional<Link> candidateLink = links.stream()
				// non-mine factories
				// Of course, ignore factories I have sent a bomb to !!
				.filter(l ->
				{
					Factory targetFactory = factoryMap.get(l.idFactoryTgt);
					boolean iGonnaBombThisOne = bombMap.values().stream()
							.anyMatch(b -> Owner.isMine(b.owner) && b.idFactoryTgt == targetFactory.id);
					return !Owner.isMine(targetFactory.owner) && !iGonnaBombThisOne;
				})
				// closest first
				.sorted((l1, l2) ->
				{
					if (l1.distance != l2.distance)
						return l1.distance >= l2.distance ? 1 : -1;
					else
					{
						Factory t1 = factoryMap.get(l1.idFactoryTgt);
						Factory t2 = factoryMap.get(l2.idFactoryTgt);
						return t1.nbCyborgs >= t2.nbCyborgs ? 1 : -1;
					}
				})
				.findFirst();
		if (candidateLink.isPresent())
		{
			Link l = candidateLink.get();
			Factory targetFactory = factoryMap.get(l.idFactoryTgt);
			int nb = Math.min(nbCyborgsToDispatch, targetFactory.nbCyborgs);
			System.err.println("\ttry to move " + nb + " to target " + targetFactory.id + " at " + l.distance);
			return new ActionMove(10, this.id, targetFactory.id, nb);
		}

		// if enough cyborg, then increase the production
		if (nbCyborgsToDispatch >= 10)
			return new ActionIncreaseProduction(10, this.id);

		return new ActionWait(0);
	}

//	private int evaluateMove(Factory targetFactory, int distance, Map<Integer, Troop> troopMap)
//	{
//		int value = 100;
//
//		// neutral / opponent ?
//		value += Owner.isOpponent(targetFactory.owner) ? 10 : -10;
//
//		// distance
//		value -= 3 * distance;
//
//		// Nb Production units --> malus for 0 production units
//		if (targetFactory.productionUnits == 0)
//			value -= 10;
//		else
//			value += 15 * targetFactory.productionUnits;
//
//		// the greatest number of opponent there is, the best it is
//		int delta = targetFactory.nbCyborgs - nbCyborgs;
//
//		if (nbCyborgs <= targetFactory.nbCyborgs)
//			return -10;
//		int delta = nbCyborgs - targetFactory.nbCyborgs;
//		value += delta;
//
//		return value;
//	}

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

abstract class Action
{

	public int factoryId;
	public int interest;
	public String which;

	protected Action(String which, int interest)
	{
		this.which = which;
		this.interest = interest;
	}

	public abstract String toCommand();
}

class ActionMove extends Action
{

	int from;
	int to;
	int howmany;

	public ActionMove(int interest, int from, int to, int howmany)
	{
		super("MOVE", interest);
		this.from = from;
		this.to = to;
		this.howmany = howmany;
	}

	public String toCommand()
	{
		return "MOVE " + from + " " + to + " " + howmany;
	}
}

class ActionBomb extends Action
{

	int from;
	int to;

	public ActionBomb(int interest, int from, int to)
	{
		super("BOMB", interest);
		this.from = from;
		this.to = to;
	}

	public String toCommand()
	{
		return "BOMB " + from + " " + to;
	}
}

class ActionWait extends Action
{

	public ActionWait(int interest)
	{
		super("WAIT", interest);
	}

	public String toCommand()
	{
		return "WAIT";
	}
}

class ActionHelp extends Action
{

	int nbCyborgsExpected;
	int timeRemaining;

	public ActionHelp(int interest, int nbCyborgsExpected, int timeRemaining)
	{
		super("HELP", interest);
		this.nbCyborgsExpected = nbCyborgsExpected;
		this.timeRemaining = timeRemaining;
	}

	public String toCommand()
	{
		return "WAIT";
	}
}

class ActionIncreaseProduction extends Action
{

	int which;

	public ActionIncreaseProduction(int interest, int which)
	{
		super("INC", interest);
		this.which = which;
	}

	public String toCommand()
	{
		return "INC " + which;
	}
}

class CandidateFactory
{

	int distance;
	Factory factory;

	public CandidateFactory(int distance, Factory factory)
	{
		this.distance = distance;
		this.factory = factory;
	}
}