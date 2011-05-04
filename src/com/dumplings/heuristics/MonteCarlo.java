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

public class MonteCarlo extends AbstractHeuristic implements PlayerHeuristic {
	private StateMachine stateMachine;
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
		Integer score = null;
		for (int i = 0; i < numSamples; i++) {
			MachineState currentState = state;
			while (!stateMachine.isTerminal(currentState)) {
				if (stopExecution) {
					return score == null ? null : score / i;
				}

				List<Move> randomMoves = stateMachine.getRandomJointMove(currentState);
				currentState = stateMachine.getNextState(currentState, randomMoves);
			}
			if (score == null)
				score = stateMachine.getGoal(currentState, role);
			else
				score += stateMachine.getGoal(currentState, role);
			//System.out.println(role + " MonteCarlo sample!");
		}
		score = score / numSamples;
		
		// never ever override forced wins or losses
		if (score == 0)
			return 1;
		if (score == 100)
			return 99;
		else
			return score;
	}

	@Override
	public String toString() {
		return "MonteCarlo";
	}
}
