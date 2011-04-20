package com.dumplings.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerStrategy;

public class MiniMax extends PlayerStrategy {
	
	Map<String, Integer> maxStateScores;
	Map<String, Map<String, Integer>> minStateScores;
	private boolean useCaching = true;
	private int numStatesExpanded;
	
	public MiniMax(StateMachine sm) {
		super(sm);
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
	}
	
	public void enableCache(boolean flag) {
		useCaching = flag;
	}
	
	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		System.out.println("Getting best move...");
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		if (moves.size() == 1) {
			System.out.println("Expanded 1 state");
			return moves.get(0);
		}
		else {
			Move bestMove = null;
			int bestValue = Integer.MIN_VALUE;
			numStatesExpanded = 1;
			for (Move move : moves) {
				int value = minScore(role, move, state);
				if (value > bestValue) {
					bestValue = value;
					bestMove = move;
				}
			}
			System.out.println("(" + bestValue + ") " + "Expanded " + numStatesExpanded + " states");
			return bestMove;
		}
	}
	
	private int minScore(Role role, Move move, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		String stateString = canonicalizeStateString(state);
		String moveString = move.toString();
		Map<String, Integer> stateMoveScores = minStateScores.get(stateString);
		
		if (useCaching && stateMoveScores != null && stateMoveScores.get(moveString) != null)
			return stateMoveScores.get(moveString);
		
		List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
		int worstScore = Integer.MAX_VALUE;
		for (List<Move> jointMove : allJointMoves) {
			MachineState newState = stateMachine.getNextState(state, jointMove);
			int newScore = maxScore(role, newState);
			if (newScore < worstScore)
				worstScore = newScore;
		}
		
		if (stateMoveScores == null) {
			minStateScores.put(stateString, (stateMoveScores = new HashMap<String, Integer>()));
		}
		
		stateMoveScores.put(moveString, worstScore);
		return worstScore;
	}
	
	private int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (stateMachine.isTerminal(state)) {			
			numStatesExpanded++;
			return stateMachine.getGoal(state, role);
		}
		
		String stateString = canonicalizeStateString(state);
		
		if (useCaching && maxStateScores.get(stateString) != null) 			
			return maxStateScores.get(stateString);
		
		numStatesExpanded++;
		int bestValue = Integer.MIN_VALUE;
		for (Move move : stateMachine.getLegalMoves(state, role)) {
			int value = minScore(role, move, state);
			if (value > bestValue)
				bestValue = value;
		}
		maxStateScores.put(stateString, bestValue);
		return bestValue;
	}
	
	private String canonicalizeStateString(MachineState state) {
		Set<String> sortedStateContents = new TreeSet<String>();
		for (GdlSentence gdl : state.getContents())
			sortedStateContents.add(gdl.toString());
		return sortedStateContents.toString();
	}
}
