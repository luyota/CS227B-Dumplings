package com.dumplings.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerStrategy;

public class MiniMax extends PlayerStrategy {
	
	Map<String, Integer> minStateScores, maxStateScores;
	
	public MiniMax(StateMachine sm) {
		super(sm);
		minStateScores = new HashMap<String, Integer>();
		maxStateScores = new HashMap<String, Integer>();
	}
	
	public Move getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		minStateScores.clear();
		maxStateScores.clear();
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		if (moves.size() == 1) {
			return moves.get(0);
		} else {
			Move bestMove = null;
			int bestValue = Integer.MIN_VALUE;
			for (Move move : stateMachine.getLegalMoves(state, role)) {
				List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
				for (List<Move> jointMove : allJointMoves) {
					MachineState newState = stateMachine.getNextState(state, jointMove);
					
					/* Call minScore on new state and maximize it */					
					int newValue = minScore(role, newState);
					if (newValue > bestValue) {
						bestValue = newValue;
						bestMove = move;
					}
				}			
			}
			System.out.println("Best: " + bestValue);
			return bestMove;
		}
	}
	private int minScore(Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if (stateMachine.isTerminal(state))
			return stateMachine.getGoal(state, role);
		if (minStateScores.get(state.toString()) != null)
			return minStateScores.get(state.toString());
		int worstValue = Integer.MAX_VALUE;
		for (Move move : stateMachine.getLegalMoves(state, role)) {
			List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
			for (List<Move> jointMove : allJointMoves) {
				MachineState newState = stateMachine.getNextState(state, jointMove);
								
				/* Call maxScore on new state and minimize it */
				int newValue = maxScore(role, newState);
				if (newValue < worstValue) {
					worstValue = newValue;
				}
			}			
		}
		minStateScores.put(state.toString(), worstValue);
		return worstValue;
	}
	
	private int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (stateMachine.isTerminal(state))
			return stateMachine.getGoal(state, role);
		//if (maxStateScores.get(state.toString()) != null)
		//	return maxStateScores.get(state.toString());
		int bestValue = Integer.MIN_VALUE;
		for (Move move : stateMachine.getLegalMoves(state, role)) {
			List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
			for (List<Move> jointMove : allJointMoves) {
				MachineState newState = stateMachine.getNextState(state, jointMove);
				
				/* Call minScore on new state and maximize it */				
				int newValue = minScore(role, newState);
				if (newValue > bestValue) {
					bestValue = newValue;
				}
			}			
		}
		maxStateScores.put(state.toString(), bestValue);
		return bestValue;
	}
	
//	public Move getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
//		minStateScores.clear();
//		maxStateScores.clear();
//		List<Move> moves = stateMachine.getLegalMoves(state, role);
//		if (moves.size() == 1) {
//			return moves.get(0);
//		} else {
//			Move bestMove = null;
//			int bestValue = Integer.MIN_VALUE;
//			for (Move move : moves) {
//				int value = minScore(role, move, state);
//				if (value > bestValue) {
//					bestValue = value;
//					bestMove = move;
//				}
//			}
//			System.out.println("Best: " + bestValue);
//			return bestMove;
//		}
//	}
//	private int minScore(Role role, Move move, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
//		if (minStateScores.get(state.toString()) != null)
//			return minStateScores.get(state.toString());
//		int worstScore = Integer.MAX_VALUE;
//		List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
//		for (List<Move> jointMove : allJointMoves) {
//			MachineState newState = stateMachine.getNextState(state, jointMove);
//			
//			/* Call maxScore on new state and minimize it */
//			int newScore = maxScore(role, newState);
//			if (newScore < worstScore) {
//				worstScore = newScore;
//			}
//		}
//		minStateScores.put(state.toString(), worstScore);
//		return worstScore;
//	}
//	
//	private int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
//		if (stateMachine.isTerminal(state))
//			return stateMachine.getGoal(state, role);
//		if (maxStateScores.get(state.toString()) != null)
//			return maxStateScores.get(state.toString());
//		int bestValue = Integer.MIN_VALUE;
//		for (Move move : stateMachine.getLegalMoves(state, role)) {
//			int value = minScore(role, move, state);
//			if (value > bestValue) {
//				bestValue = value;
//			}
//		}
//		maxStateScores.put(state.toString(), bestValue);
//		return bestValue;
//	}
}
