package com.dumplings.heuristics;

import util.statemachine.MachineState;
import util.statemachine.Role;

import com.dumplings.general.PlayerHeuristic;

public class MonteCarlo implements PlayerHeuristic {
	@Override
	public int getScore(MachineState state, Role role) {
		return 0;
	}

	@Override
	public void onTimeout() {
	}
}
