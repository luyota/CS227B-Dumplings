package com.dumplings.heuristics;

import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerHeuristic;

public class Focus implements PlayerHeuristic {
	private boolean isExecuting = false;
	private StateMachine stateMachine = null;
	public Focus(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	@Override
	public void onTimeout() {
		isExecuting = true;
	}

	@Override
	public int getScore(MachineState state, Role role) {
		try {
			int score = 100 - stateMachine.getLegalMoves(state, role).size();
			System.out.println("Focus get score: " + score);
			return score;
		} catch (Exception e) {			
		}
		return 0;
	}
}
