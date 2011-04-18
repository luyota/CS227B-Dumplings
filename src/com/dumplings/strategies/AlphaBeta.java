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

public class AlphaBeta extends PlayerStrategy {
	
	Map<String, Integer> stateScores;	
	
	public AlphaBeta(StateMachine sm) {
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
				int value = minScore(role, move, state, Integer.MIN_VALUE, Integer.MAX_VALUE);
				if (value > bestValue) {
					bestValue = value;
					bestMove = move;
				}
			}
			System.out.println("Best: " + bestValue);
			return bestMove;
		}
	}
	private int minScore(Role role, Move move, MachineState state, int alpha, int beta) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		
		int worstScore = Integer.MAX_VALUE;
		List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : allJointMoves) {
			MachineState newState = stateMachine.getNextState(state, jointMove);
			
			/* Call maxScore on new state and minimize it */
			int newScore = maxScore(role, newState, alpha, beta);
			if (newScore < worstScore) {
				worstScore = newScore;
			}
			if (worstScore < alpha) return alpha;
			beta = Math.min(beta, worstScore);
		}
		return worstScore;
	}
	
	private int maxScore(Role role, MachineState state, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (stateMachine.isTerminal(state)) 			
			return stateMachine.getGoal(state, role);	
		
		String stateString = makeStateString(state, alpha, beta);
		if (stateScores.get(stateString) != null) {			
			return stateScores.get(stateString);
		}
					
		int bestValue = Integer.MIN_VALUE;
		for (Move move : stateMachine.getLegalMoves(state, role)) {
			int value = minScore(role, move, state, alpha, beta);
			if (value > bestValue) {
				bestValue = value;
			}
			if (bestValue > beta) {
				//System.out.println("Prune max");
				return beta;
			}
			alpha = Math.max(alpha, bestValue);
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
	private String makeStateString(MachineState state, int alpha, int beta) {
		return canonicalizeStateString(state) + " alpha: " + alpha + " beta: " + beta;
	}
}
