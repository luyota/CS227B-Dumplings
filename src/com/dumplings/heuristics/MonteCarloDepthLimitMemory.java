package com.dumplings.heuristics;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.PlayerHeuristic;
import com.dumplings.utils.Canonicalizer;

public class MonteCarloDepthLimitMemory extends AbstractHeuristic implements PlayerHeuristic {
	private StateMachine stateMachine;
	private int numSamples = 1;
	private int maxDepth = Integer.MAX_VALUE;
	private Set<String> stateMoveCache = new HashSet<String>();
	
	
	public MonteCarloDepthLimitMemory(StateMachine sm) {
		stateMachine = sm;
	}
	
	@Override
	public void setStateMachine(StateMachine stateMachine) { this.stateMachine = stateMachine; }
	
	public void setSampleSize(int size) {
		numSamples = size;
	}
	public void setMaxDepth(int depth) {
		this.maxDepth = depth;
	}
	public void cleanup() {
		this.stateMoveCache.clear();
	}
	
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Integer score = null;
		int numUsefulSamples = 0;
		Random randGen = new Random();
		for (int i = 0; i < numSamples; i++) {
			MachineState currentState = state;
			for (int j = 0; (maxDepth < 0 || j < maxDepth) && !stateMachine.isTerminal(currentState); j ++) {
				if (stopExecution) {
					return score == null ? null : score / i;
				}
				List<List<Move>> allMoves = stateMachine.getLegalJointMoves(currentState);
				List<Move> randomMoves = null;
				for (int mi = 0; mi < allMoves.size(); mi++) {
					randomMoves = allMoves.get(randGen.nextInt(allMoves.size()));
					String stateMovesString = Canonicalizer.stateMovesString(state, randomMoves);
					if (!this.stateMoveCache.contains(stateMovesString)) {
						this.stateMoveCache.add(stateMovesString);
						break;
					}
				}
				if (randomMoves != null)
					currentState = stateMachine.getNextState(currentState, randomMoves);
				else { 
					//TODO: This line has some problem. When should it be reached?
					System.out.println("Monte carlo null random moves: " + currentState);
					if (score == null) return null;
					score /= i;
					if (score == 0) score = 1;
					if (score == 100) score = 99;
					return score;
				}
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
