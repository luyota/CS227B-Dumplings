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
	
	Map<String, Integer> stateScores;
	
	public MiniMax(StateMachine sm) {
		super(sm);
		stateScores = new HashMap<String, Integer>();
	}
	
	public Move getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		stateScores.clear();
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		if (moves.size() == 1) {
			return moves.get(0);
		} else {
			Move bestMove = null;
			int bestValue = Integer.MIN_VALUE;
			for (Move move : moves) {
				int value = minScore(role, move, state);
				if (value > bestValue) {
					bestValue = value;
					bestMove = move;
				}
			}
			System.out.println("Best: " + bestValue);
			return bestMove;
		}
	}
	private int minScore(Role role, Move move, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		int worstScore = Integer.MAX_VALUE;
		List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : allJointMoves) {
			MachineState newState = stateMachine.getNextState(state, jointMove);
			int newScore = maxScore(role, newState);
			if (newScore < worstScore) {
				worstScore = newScore;
			}
		}
		return worstScore;
	}
	
	private int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (stateMachine.isTerminal(state)) 			
			return stateMachine.getGoal(state, role);		
		String stateString = canonicalizeStateString(state);
		if (stateScores.get(stateString) != null) 			
			return stateScores.get(stateString);
					
		int bestValue = Integer.MIN_VALUE;
		for (Move move : stateMachine.getLegalMoves(state, role)) {
			int value = minScore(role, move, state);
			if (value > bestValue) {
				bestValue = value;
			}
		}
		stateScores.put(stateString, bestValue);
		return bestValue;
	}
	
	private String canonicalizeStateString(MachineState state) {
		Set<String> sortedStateContents = new TreeSet<String>();
		for (GdlSentence gdl : state.getContents())
			sortedStateContents.add(gdl.toString());
		return sortedStateContents.toString();
	}
}
