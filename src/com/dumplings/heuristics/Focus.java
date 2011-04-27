package com.dumplings.heuristics;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.PlayerHeuristic;

public class Focus extends AbstractHeuristic implements PlayerHeuristic {
	private StateMachine stateMachine = null;
	
	public Focus(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}

	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int score = Math.max(100 - stateMachine.getLegalMoves(state, role).size(), 0);
		//System.out.println("Focus score: " + score);
		return score;
		
	}
}
