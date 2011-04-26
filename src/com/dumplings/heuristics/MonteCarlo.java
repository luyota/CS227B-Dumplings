package com.dumplings.heuristics;

import java.util.List;
import java.util.Random;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerHeuristic;

public class MonteCarlo implements PlayerHeuristic {
	private StateMachine stateMachine;
	private Random generator = new Random();
	private Boolean stopExecution = false;
	
	public MonteCarlo(StateMachine sm) {
		stateMachine = sm;
	}
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (stateMachine.isTerminal(state))
			return stateMachine.getGoal(state, role);		
		if (stopExecution) return null;
		List<List<Move>> allMoves= stateMachine.getLegalJointMoves(state);
		List<Move> randomMoves = allMoves.get(generator.nextInt(allMoves.size()));
		MachineState newState = stateMachine.getNextState(state, randomMoves);
		return getScore(newState, role);
	}

	@Override
	public void onTimeout() {
		stopExecution = true;
	}
}
