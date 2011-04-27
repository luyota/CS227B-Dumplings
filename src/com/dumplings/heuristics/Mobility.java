package com.dumplings.heuristics;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerHeuristic;

public class Mobility implements PlayerHeuristic {
	private StateMachine stateMachine = null;
	public Mobility(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	@Override
	public void onTimeout() {
	}

	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int score = Math.min(stateMachine.getLegalMoves(state, role).size(), 100);
		System.out.println("Mobiility score: " + score);
		return score;
	}
}
