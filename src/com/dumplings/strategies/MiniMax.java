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
	
	public MiniMax(StateMachine sm) {
		super(sm);
	}
	
	public Move getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		Move bestMove = null;
		int bestValue = Integer.MIN_VALUE;
		for (Move move : moves) {
			int value = minScore(role, move, state);
			if (value > bestValue) {
				bestValue = value;
				bestMove = move;
			}
		}
		return bestMove;
	}
	
	/*
	 * minScore(role, move, state) looks at all possible next states resulting from role making move in state
	 * and returns the minimum possible score
	 */
	private int minScore(Role role, Move move, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
	
		int worstScore = Integer.MAX_VALUE;
		for (List<Move> jointMove : allJointMoves) {
			MachineState newState = stateMachine.getNextState(state, jointMove);
			
			/* Call maxScore on new state and minimize it */
			int newScore = maxScore(role, newState);
			if (newScore < worstScore) {
				worstScore = newScore;
			}
		}
		return worstScore;
	}
	
	/*
	 * maxScore(role, newState) looks at all possible next states and
	 * 
	 */
	private int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (stateMachine.isTerminal(state)) {
			return stateMachine.getGoal(state, role);
		}		
		int bestValue = Integer.MIN_VALUE;
		for (Move move : stateMachine.getLegalMoves(state, role)) {
			int value = minScore(role, move, state);
			if (value > bestValue) {
				bestValue = value;
			}
		}
		return bestValue;
	}
}
