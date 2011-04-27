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

public class FocusedMonteCarlo implements PlayerHeuristic {
	private StateMachine stateMachine;
	private Random generator = new Random();
	private Boolean stopExecution = false;
	private int numSamples = 4;
	
	public FocusedMonteCarlo(StateMachine sm) {
		stateMachine = sm;
	}
	
	public void setSampleSize(int size) {
		numSamples = size;
	}
	
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int score = 0;
		// First decide which next state to apply MonteCarlo to
		// Reasoning: MonteCarlo will be more accurate for the next state which has more focus
		List<List<Move>> allMoves= stateMachine.getLegalJointMoves(state);
		int focus = Integer.MAX_VALUE;
		for (List<Move> move : allMoves) {
			if (stopExecution)
				return null;
			
			MachineState nextState = stateMachine.getNextState(state, move);
			// Get mobility of nextState
			int newMobility = stateMachine.getLegalMoves(nextState, role).size();
			if (newMobility < focus) {
				focus = newMobility;
				state = nextState;
			}
		}
		if (stateMachine.isTerminal(state))
			return stateMachine.getGoal(state, role);
		
		for (int i = 0; i < numSamples; i++) {
			MachineState currentState = state;
			while (!stateMachine.isTerminal(currentState)) {
				if (stopExecution) {
					if (i == 0)
						return null;		// Couldn't even sample one depth charge!
					else
						return score / i;	// Return average score that we have seen so far
				}
				
				allMoves = stateMachine.getLegalJointMoves(currentState);
				List<Move> randomMoves = allMoves.get(generator.nextInt(allMoves.size()));
				currentState = stateMachine.getNextState(currentState, randomMoves);
			}
			score += stateMachine.getGoal(currentState, role);
		}
	
		return score / numSamples;
	}

	@Override
	public void onTimeout() {
		stopExecution = true;
	}
}
