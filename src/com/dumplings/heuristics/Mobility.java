package com.dumplings.heuristics;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

import com.dumplings.general.PlayerHeuristic;

public class Mobility implements PlayerHeuristic {
	private boolean isExecuting = false;
	private StateMachine stateMachine = null;
	public Mobility(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	@Override
	public void onTimeout() {
		isExecuting = true;
	}

	@Override
	public Integer getScore(MachineState state, Role role) {
		try {
			int score = stateMachine.getLegalMoves(state, role).size();
			System.out.println("Mobiility get score: " + score);
			return score;
		} catch (Exception e) {			
		}
		return 0;
	}
}
