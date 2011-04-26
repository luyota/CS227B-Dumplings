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
	private int numSamples = 1;
	
	public MonteCarlo(StateMachine sm) {
		stateMachine = sm;
	}
	
	public void setSampleSize(int size) {
		numSamples = size;
	}
	
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		MachineState nextState = state;
		while (!stateMachine.isTerminal(nextState)) {
			if (stopExecution)
				return null;
			
			List<List<Move>> allMoves= stateMachine.getLegalJointMoves(nextState);
			List<Move> randomMoves = allMoves.get(generator.nextInt(allMoves.size()));
			nextState = stateMachine.getNextState(nextState, randomMoves);
		}
	
		return stateMachine.getGoal(nextState, role);
	}

	@Override
	public void onTimeout() {
		stopExecution = true;
	}
}
