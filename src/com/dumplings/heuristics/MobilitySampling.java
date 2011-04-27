package com.dumplings.heuristics;

import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.PlayerHeuristic;

public class MobilitySampling extends AbstractHeuristic implements PlayerHeuristic {
	private StateMachine stateMachine;
	
	public MobilitySampling(StateMachine sm) {
		stateMachine = sm;
	}
	
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		MachineState currentState = state;
		while (!stateMachine.isTerminal(currentState)) {
			if (stopExecution) {
				return null;
			}
		
			List<List<Move>> allMoves= stateMachine.getLegalJointMoves(currentState);
			List<Move> nextMove = null; int mobility = Integer.MIN_VALUE;
			for (List<Move> move : allMoves) {
				MachineState nextState = stateMachine.getNextState(currentState, move);
				// Get mobility of nextState
				int newMobility = stateMachine.getLegalMoves(nextState, role).size();
				if (newMobility > mobility) {
					nextMove = move;
					mobility = newMobility;
				}
			}
			currentState = stateMachine.getNextState(currentState, nextMove);
		}
		return stateMachine.getGoal(currentState, role);
	}
}
