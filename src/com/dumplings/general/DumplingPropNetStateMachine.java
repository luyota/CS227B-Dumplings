package com.dumplings.general;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import util.propnet.factory.OptimizingPropNetFactory;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.PropNetMachineState;
import util.statemachine.implementation.propnet.PropNetMove;
import util.statemachine.implementation.propnet.PropNetRole;
import util.statemachine.implementation.prover.query.ProverQueryBuilder;

public class DumplingPropNetStateMachine extends StateMachine {
	/** The underlying proposition network */
	private PropNet propNet;
	/** The topological ordering of the propositions */
	private List<Proposition> ordering;
	/** The player roles */
	private List<Role> roles;
	/*
	 * This is used to cache the state when calling updateState so that it
	 * doesn't have to recompute that everytime.
	 */
	private MachineState savedState = null;

	/*
	 * The propositions, stored here so we don't have to load everytime it's
	 * used
	 */
	private Map<GdlTerm, Proposition> inputPropositions = null;
	private Map<GdlTerm, Proposition> basePropositions = null;
	private Proposition initProposition = null;
	private Proposition terminalProposition = null;
	private Map<Role, Set<Proposition>> legalPropositions = null;
	private Map<Role, Set<Proposition>> goalPropositions = null;
	
	public boolean isInhibiting(Proposition p, Proposition q) {
		// TODO: check whether p inhibits q
		// Problem: enumerate all possible states? Seems inefficient.
		
		return false;
	}
	
	public List<Proposition> getLatches() {
		List<Proposition> propositions = new ArrayList<Proposition>(
				propNet.getPropositions());
		List<Proposition> latches = new ArrayList<Proposition>();
		
		for (Proposition p : propositions) {
			try {
				if (isLatch(p))
					latches.add(p);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return latches;
	}

	public boolean isLatch(Proposition p) throws MoveDefinitionException, TransitionDefinitionException {
		Set<Proposition> determinants = getDeterminants(p);
		
		// Enumerate all possible values for determinants
		// In each case, check whether "p true => p true in next states" holds			
		List<Boolean[]> combinations = getCombinations(determinants.size());
		for (Boolean[] b : combinations) {
			// Set the propositions to the corresponding truth values
			int i = 0;
			for (Proposition pp : determinants) {
				pp.setValue(b[i]);
				i++;
			}
			
			// Propagate the values
			for (Proposition pp : ordering) {
				if (pp.getInputs().size() == 1) {
					pp.setValue(pp.getSingleInput().getValue());
				}
			}
			
			// Check whether p is true
			if (p.getValue()) {
				// Check if p is true in all next states
				for (MachineState state : getNextStates(getStateFromBase())) {
					updateState(state, null);
					if (!p.getValue())
						return false;
				}
			}
		}
		return true;
	}
	
	/*
	 * Given an integer n, this computes the power set of a set of length n
	 * It returns a list of boolean arrays of length n
	 */
	private List<Boolean[]> getCombinations(int n) {
		List<Boolean[]> list = new ArrayList<Boolean[]>();
		int size = (int) Math.pow(2, n);
		
		// Populate the power set
		for (int i = 0; i < size; i++) {
			Boolean[] b = new  Boolean[n];
			// Fill this entry of size n
			for (int j = 0; j < n; j++) {
				// Check whether j-th bit is true or false
				if ((i & (1 << j)) > 0)
					b[j] = true;
				else
					b[j] = false;
			}
			list.add(b);
		}
		return list;
	}
	
	/*
	 * Finds input and base propositions that determine p
	 */
	private Set<Proposition> getDeterminants(Component p) {
		Set<Proposition> determinants = new HashSet<Proposition>();
		
		Set<Component> inputs = p.getInputs();
		for (Component c : inputs) {
			if (c instanceof Proposition) {
				if (inputPropositions.containsValue(c))
					determinants.add((Proposition) c);
				else if (basePropositions.containsValue(c))
					determinants.add((Proposition) c);
			}
			else {
				determinants.addAll(getDeterminants(c));
			}
		}
		
		return determinants;
	}

	/**
	 * Initializes the PropNetStateMachine. You should compute the topological
	 * ordering here. Additionally you may compute the initial state here, at
	 * your discretion.
	 */
	@Override
	public void initialize(List<Gdl> description) {
		propNet = OptimizingPropNetFactory.create(description);

		propNet.renderToFile(new File(System.getProperty("user.home"),
				"propnet.dot").toString());
		roles = propNet.getRoles();

		savedState = null;
		inputPropositions = propNet.getInputPropositions();
		basePropositions = propNet.getBasePropositions();
		legalPropositions = propNet.getLegalPropositions();
		goalPropositions = propNet.getGoalPropositions();

		initProposition = propNet.getInitProposition();
		terminalProposition = propNet.getTerminalProposition();

		ordering = getOrdering();
		/*
		 * System.out.println("Ordering:"); for (Component c : ordering) {
		 * System.out.println(c); }
		 */
	}

	/**
	 * Computes if the state is terminal. Should return the value of the
	 * terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		if (!state.equals(savedState))
			updateState(state, null);
		// System.out.println("isTerminal: " + getStateFromBase());
		return terminalProposition.getValue();
	}

	/**
	 * Computes the goal for a role in the current state. Should return the
	 * value of the goal proposition that is true for that role. If there is not
	 * exactly one goal proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public int getGoal(MachineState state, Role role)
			throws GoalDefinitionException {
		if (!state.equals(savedState))
			updateState(state, null);

		Integer goalValue = null;
		for (Proposition p : goalPropositions.get(role)) {
			// Check if more than two goal propositions are true
			if (p.getValue()) {
				if (goalValue != null) {
					throw new GoalDefinitionException(state, role);
				}
				goalValue = getGoalValue(p);
			}
		}

		// If there is no goal, throw exception
		if (goalValue == null)
			throw new GoalDefinitionException(state, role);
		return goalValue;
	}

	/**
	 * Returns the initial state. The initial state can be computed by only
	 * setting the truth value of the INIT proposition to true, and then
	 * computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		savedState = null;
		for (Proposition p : inputPropositions.values()) {
			p.setValue(false);
		}
		for (Proposition p : basePropositions.values()) {
			p.setValue(false);
		}

		initProposition.setValue(true);

		for (Proposition p : ordering) {
			if (p.getInputs().size() == 1) {
				p.setValue(p.getSingleInput().getValue());
			}
		}
		System.out.println("getInitialState " + getStateFromBase());
		return getStateFromBase();
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {
		if (!state.equals(savedState))
			updateState(state, null);

		List<Move> moves = new ArrayList<Move>();;
		for (Proposition p : legalPropositions.get(role)) {
			if (p.getValue()) {
				moves.add(getMoveFromProposition(p));
			}
		}
		return moves;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		updateState(state, moves);
		return getStateFromBase();
	}

	public void updateState(MachineState state, List<Move> moves) {
		// Set base propositions
		for (Proposition p : basePropositions.values()) {
			p.setValue(false);
		}

		for (GdlSentence s : state.getContents()) {
			basePropositions.get(s.toTerm()).setValue(true);
		}
	

		// Set input propositions
		for (Proposition p : inputPropositions.values()) {
			p.setValue(false);
		}

		if (moves != null) {
			List<GdlTerm> does = toDoes(moves);
			for (GdlTerm term : does) {
				Proposition p = inputPropositions.get(term);
				p.setValue(true);
			}
		}

		initProposition.setValue(false);

		// Propagate the values
		for (Proposition p : ordering) {
			if (p.getInputs().size() == 1) {
				p.setValue(p.getSingleInput().getValue());
			}
		}

		// When moves = null, clear the cache since it's already one move ahead
		// of the state.
		if (moves != null)
			savedState = null;
		else
			savedState = state;
	}

	/**
	 * This should compute the topological ordering of propositions. Each
	 * component is either a proposition, logical gate, or transition. Logical
	 * gates and transitions only have propositions as inputs.
	 * 
	 * The base propositions and input propositions should always be exempt from
	 * this ordering.
	 * 
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from the
	 * Moves that operations are performed on as well (if any).
	 * 
	 * @return The order in which the truth values of propositions need to be
	 *         set.
	 */
	public List<Proposition> getOrdering() {
		// List to contain the topological ordering.
		List<Proposition> order = new LinkedList<Proposition>();

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(
				propNet.getPropositions());

		/*
		 * We maintain a set of propositions that we have visited, and a set of
		 * unvisited propositions. Initially, we add the base and input
		 * propositions to the visited set. Next, we go through all unvisited
		 * propositions and check for which proposition all inputs have already
		 * been visited. We then add that proposition both to the visited set
		 * and to the order
		 */
		Set<Proposition> visitedPropositions = new HashSet<Proposition>();
		Set<Proposition> unvisitedPropositions = new HashSet<Proposition>();

		visitedPropositions.addAll(basePropositions.values());
		visitedPropositions.addAll(inputPropositions.values());
		visitedPropositions.add(propNet.getInitProposition()); // not sure if
																// necessary,
																// but shouldn't
																// hurt

		unvisitedPropositions.addAll(propositions);
		unvisitedPropositions.removeAll(visitedPropositions);

		while (!unvisitedPropositions.isEmpty()) {
			// Pick next proposition whose inputs have all been visited
			Proposition nextProposition = null;
			for (Proposition unvisitedProp : unvisitedPropositions) {
				// Calculate all propositional inputs of unvisitedProp
				Set<Proposition> inputs = new HashSet<Proposition>();
				Set<Component> toCheck = new HashSet<Component>();

				toCheck.add(unvisitedProp);
				while (!toCheck.isEmpty()) {
					Component comp = toCheck.iterator().next();
					toCheck.remove(comp);
					for (Component input : comp.getInputs()) {
						if (input instanceof Proposition)
							inputs.add((Proposition) input);
						else
							toCheck.add(input);
					}
				}

				if (visitedPropositions.containsAll(inputs)) {
					nextProposition = unvisitedProp;
					break;
				}
			}

			// Add to visited set and remove from unvisited set
			visitedPropositions.add(nextProposition);
			unvisitedPropositions.remove(nextProposition);

			// Add to order
			order.add(nextProposition);
		}

		return order;
	}

	/* Already implemented for you */
	@Override
	public Move getMoveFromSentence(GdlSentence sentence) {
		return new PropNetMove(sentence);
	}

	/* Already implemented for you */
	@Override
	public MachineState getMachineStateFromSentenceList(
			Set<GdlSentence> sentenceList) {
		return new PropNetMachineState(sentenceList);
	}

	/* Already implemented for you */
	@Override
	public Role getRoleFromProp(GdlProposition proposition) {
		return new PropNetRole(proposition);
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 * 
	 * This translates a list of Moves (backed by a sentence that is simply
	 * ?action) into GdlTerms that can be used to get Propositions from
	 * inputPropositions. and accordingly set their values etc. This is a naive
	 * implementation when coupled with setting input values, feel free to
	 * change this for a more efficient implementation.
	 * 
	 * @param moves
	 * @return
	 */
	private List<GdlTerm> toDoes(List<Move> moves) {
		List<GdlTerm> doeses = new ArrayList<GdlTerm>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++) {
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder
					.toDoes(roles.get(i), moves.get(index)).toTerm());
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding
	 * Move
	 * 
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p) {
		return new PropNetMove(p.getName().toSentence().get(1).toSentence());
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * 
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
	private int getGoalValue(Proposition goalProposition) {
		GdlRelation relation = (GdlRelation) goalProposition.getName()
				.toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState from the true
	 * BasePropositions. This is correct but slower than more advanced
	 * implementations You need not use this method!
	 * 
	 * @return PropNetMachineState
	 */
	public PropNetMachineState getStateFromBase() {
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values()) {
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue()) {
				contents.add(p.getName().toSentence());
			}

		}
		return new PropNetMachineState(contents);
	}
	
	public Set<DumplingPropNetStateMachine> propNetFactors() {
		Set<DumplingPropNetStateMachine> factors = new HashSet<DumplingPropNetStateMachine>();
		Set<String> factoredPropositionTerms = new HashSet<String>();
		
		Map<GdlTerm, Proposition> universalInputs = new HashMap<GdlTerm, Proposition>();
		for (Proposition input : this.inputPropositions.values()) {
			if (input.getOutputs().size() == 0)
				universalInputs.put(input.getName(), input);
		}
		
		for (Set<Proposition> goalSet : this.goalPropositions.values()) {
			for (Proposition goalProposition : goalSet) {
				Set<Proposition> tailPropositions = new HashSet<Proposition>();
				Or disjunction = findDisjunction(goalProposition, tailPropositions);
				
				Set<Component> goalComponents = new HashSet<Component>();
				if (disjunction != null) {
					for (Component disjunctionInput : disjunction.getInputs()) {
						if (disjunctionInput instanceof Proposition) {
							String termString = ((Proposition)disjunctionInput).getName().toString();
							if (!factoredPropositionTerms.contains(termString)) {
								factoredPropositionTerms.add(termString);
								goalComponents.add(disjunctionInput);
							}
						} else {
							goalComponents.add(disjunctionInput);
						}
					}
				} else {
					factoredPropositionTerms.add(goalProposition.getName().toString());
					goalComponents.add(goalProposition);
				}
				
				for (Component goalComponent : goalComponents) {
					Set<Proposition> visitedPropositions = new HashSet<Proposition>();
					if (disjunction != null)
						visitedPropositions.addAll(tailPropositions);
					
					stepBackToAllInputs(goalComponent, visitedPropositions);
			
					Map<GdlTerm, Proposition> inputs = filterInputs(this.inputPropositions, visitedPropositions);
			
					DumplingPropNetStateMachine factor = new DumplingPropNetStateMachine();
								
					factor.inputPropositions = inputs;
					factor.inputPropositions.putAll(universalInputs);
			
					factor.legalPropositions = new HashMap<Role, Set<Proposition>>();
					for (Proposition input : inputs.values()) {
						Proposition legalProp = propNet.getLegalInputMap().get(input);
						Role r = new Role((GdlProposition)((GdlFunction)legalProp.getName()).get(0).toSentence());
						Set<Proposition> roleProps = factor.legalPropositions.get(r);
						if (roleProps == null) {
							roleProps = new HashSet<Proposition>();
							factor.legalPropositions.put(r, roleProps);
						}
						roleProps.add(legalProp);
					}
			
					factor.propNet = this.propNet;
					factor.roles = this.roles;
			
					factor.savedState = null;
					factor.basePropositions = this.basePropositions;
					factor.goalPropositions = this.goalPropositions;
			
					factor.initProposition = this.initProposition;
					factor.terminalProposition = this.terminalProposition;
			
					factor.ordering = this.ordering;
			
					System.out.println("Found a factor, reduced input propositions from " + this.inputPropositions.size() + " to " + factor.inputPropositions.size() + " for goal: " + goalComponent);
					for (GdlTerm inputTerm : factor.inputPropositions.keySet()) {
						System.out.println("\t" + inputTerm);
					}
					/*
					for (Set<Proposition> legalSet : factor.legalPropositions.values()) {
						for (Proposition legal : legalSet) {
							System.out.println("\t" + legal.getName());
						}
					}
					*/
		
					factors.add(factor);
				}
			}
		}
		
		return factors;
	}
	
	private Or findDisjunction(Component comp, Set<Proposition> tailPropositions) {
		if (comp instanceof Or) {
			return (Or)comp;
		} else if (comp instanceof Proposition) {
			tailPropositions.add((Proposition)comp);
			return findDisjunction(comp.getSingleInput(), tailPropositions);
		} else if (comp instanceof Constant || comp instanceof Transition) {
			return findDisjunction(comp.getSingleInput(), tailPropositions);
		} else {
			return null;
		}
	}

	public DumplingPropNetStateMachine factorPropNet(Role role) {
		// For now, we only factor single-player games
		if (roles.size() > 1)
			return null;

		Proposition goalProposition = null;

		for (Proposition proposition : this.goalPropositions.get(role)) {
			if (getGoalValue(proposition) == 100) {
				goalProposition = proposition;
				break;
			}
		}

		if (goalProposition == null) {
			return this;
		}

		Set<Proposition> visitedPropositions = new HashSet<Proposition>();
		stepBackToInputs(goalProposition, visitedPropositions);

		Map<GdlTerm, Proposition> inputs = filterInputs(this.inputPropositions, visitedPropositions);

		Set<Proposition> legalProps = new HashSet<Proposition>();
		for (Proposition input : inputs.values()) {
			legalProps.add(propNet.getLegalInputMap().get(input));
		}

		DumplingPropNetStateMachine factor = new DumplingPropNetStateMachine();

		factor.inputPropositions = inputs;
		factor.legalPropositions = new HashMap<Role, Set<Proposition>>();
		factor.legalPropositions.put(role, legalProps);
		System.out.println(legalProps.size());

		factor.propNet = this.propNet;
		factor.roles = this.roles;

		factor.savedState = null;
		factor.basePropositions = this.basePropositions;
		factor.goalPropositions = this.goalPropositions;

		factor.initProposition = this.initProposition;
		factor.terminalProposition = this.terminalProposition;

		factor.ordering = this.ordering;

		System.out.println("Found a factor, reduced input propositions from "
					+ this.inputPropositions.size() + " to "
					+ factor.inputPropositions.size());
		return factor;
	}

	private void stepBackToInputs(Component comp, Set<Proposition> visitedPropositions) {
		if (comp instanceof Proposition && visitedPropositions.contains((Proposition) comp)) {
			return;
		} else if (comp instanceof Proposition) {
			visitedPropositions.add((Proposition) comp);
		}

		Set<Component> inputs;
		if (comp instanceof Or) {
			inputs = new HashSet<Component>(Arrays.asList(new Component[] { comp.getInputs().iterator().next() }));
		} else {
			inputs = comp.getInputs();
		}

		for (Component inputComp : inputs) {
			stepBackToInputs(inputComp, visitedPropositions);
		}
	}
	
	private void stepBackToAllInputs(Component comp, Set<Proposition> visitedPropositions) {
		if (comp instanceof Proposition && visitedPropositions.contains((Proposition) comp)) {
			return;
		} else if (comp instanceof Proposition) {
			visitedPropositions.add((Proposition) comp);
		}

		for (Component inputComp : comp.getInputs()) {
			stepBackToAllInputs(inputComp, visitedPropositions);
		}
	}

	private Map<GdlTerm, Proposition> filterInputs(Map<GdlTerm, Proposition> source, Set<Proposition> factorPropositions) {
		Map<GdlTerm, Proposition> retVal = new HashMap<GdlTerm, Proposition>();
		for (Proposition prop : factorPropositions) {
			if (source.containsKey(prop.getName())) {
				retVal.put(prop.getName(), prop);
			}
		}

		return retVal;
	}
}