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

public class MonteCarloDepthLimit extends AbstractHeuristic implements PlayerHeuristic {
	private StateMachine stateMachine;
	private int numSamples = 1;
	private int maxDepth = Integer.MAX_VALUE;
	
	public MonteCarloDepthLimit(StateMachine sm) {
		stateMachine = sm;
	}
	@Override
	public void setStateMachine(StateMachine stateMachine) { this.stateMachine = stateMachine; }
	
	public void setSampleSize(int size) {
		numSamples = size;
	}
	public void setMaxDepth(int depth) { this.maxDepth = depth; }
	
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Integer score = null;
		int numUsefulSamples = 0;
		//System.out.println("heuristic used");
		for (int i = 0; i < numSamples; i++) {			
			MachineState currentState = state;
			for (int j = 0; j < maxDepth && !stateMachine.isTerminal(currentState); j ++) {				
				if (stopExecution) {
					return score == null ? null : score / i;
				}
				List<Move> randomMoves = stateMachine.getRandomJointMove(currentState);
				currentState = stateMachine.getNextState(currentState, randomMoves);
			}
			// If it gets to the end before max depth
			if (stateMachine.isTerminal(currentState)) {
				if (score == null)
					score = stateMachine.getGoal(currentState, role);
				else
					score += stateMachine.getGoal(currentState, role);
				numUsefulSamples ++;
			} else {
				
			}
		}		
		
		if (score == null) 
			return score;
		//System.out.println("Reaches the end");
		score = score / numUsefulSamples;
		// never ever override forced wins or losses
		if (score == 0)
			return 1;
		if (score == 100)
			return 99;
		return score;
	}

	@Override
	public String toString() {
		return "MonteCarlo";
	}
}
