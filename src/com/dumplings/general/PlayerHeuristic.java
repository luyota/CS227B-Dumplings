package com.dumplings.general;

import util.statemachine.MachineState;
import util.statemachine.Role;

public interface PlayerHeuristic extends TimeoutHandler {
	public int getScore(MachineState state, Role role);	
}
