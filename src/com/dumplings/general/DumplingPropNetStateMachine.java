package com.dumplings.general;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.Proposition;
import util.propnet.factory.CachedPropNetFactory;
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
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;
    
    /* The propositions, stored here so we don't have to load everytime it's used */
    private Map<GdlTerm, Proposition> inputPropositions;
    private Map<GdlTerm, Proposition> basePropositions;
    
    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        propNet = CachedPropNetFactory.create(description);
        roles = propNet.getRoles();
        inputPropositions = propNet.getInputPropositions();
        basePropositions = propNet.getBasePropositions();
        ordering = getOrdering();
    }    
    
	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		return propNet.getTerminalProposition().getValue();
	}
	
	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined. 
	 */
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		Set<Proposition> goalPropositions = propNet.getGoalPropositions().get(role);
		if (goalPropositions.size() != 1)
			throw new GoalDefinitionException(state, role);
		
		return getGoalValue(goalPropositions.iterator().next());
	}
	
	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		// TODO: Compute the initial state.
		return null;
	}
	
	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		// TODO: Compute legal moves.
		List<Move> moves = new ArrayList<Move>();
		Set<Proposition> legalPropositions = propNet.getLegalPropositions().get(role);
		while (legalPropositions.iterator().hasNext()) {
			Proposition p = legalPropositions.iterator().next();
			moves.add(DumplingPropNetStateMachine.getMoveFromProposition(p));
		}
		return moves;
	}
	
	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		// TODO: Compute the next state.
		return null;
	}
	
	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 * 
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 * 
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 * 
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();
		
		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());
		
		/*
		 *  We maintain a set of propositions that we have visited, and a set of unvisited propositions.
		 *  Initially, we add the base and input propositions to the visited set.
		 *  Next, we go through all unvisited propositions and check for which proposition all inputs
		 *  have already been visited. We then add that proposition both to the visited set and to the order
		 */
		Set<Proposition> visitedPropositions = new HashSet<Proposition>();
		Set<Proposition> unvisitedPropositions = new HashSet<Proposition>();
		
		visitedPropositions.addAll(basePropositions.values());
		visitedPropositions.addAll(inputPropositions.values());
		visitedPropositions.add(propNet.getInitProposition()); // not sure if necessary, but shouldn't hurt
		
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
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlTerms that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with 
	 * setting input values, feel free to change this for a more efficient implementation.
	 * 
	 * @param moves
	 * @return
	 */
	private List<GdlTerm> toDoes(List<Move> moves)
	{
		List<GdlTerm> doeses = new ArrayList<GdlTerm>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();
		
		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)).toTerm());
		}
		return doeses;
	}
	
	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new PropNetMove(p.getName().toSentence().get(1).toSentence());
	}
	
	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */	
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}
	
	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */	
	public PropNetMachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName().toSentence());
			}

		}
		return new PropNetMachineState(contents);
	}
}