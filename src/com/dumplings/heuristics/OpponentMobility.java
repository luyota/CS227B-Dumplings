package com.dumplings.heuristics;

import util.statemachine.MachineState;
import util.statemachine.Role;

import com.dumplings.general.PlayerHeuristic;

public class OpponentMobility implements PlayerHeuristic {
	@Override
	public void onTimeout() {
	}

	@Override
	public int getScore(MachineState state, Role role) {
		return 0;
	}
}
