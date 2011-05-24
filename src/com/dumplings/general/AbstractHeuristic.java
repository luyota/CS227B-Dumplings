package com.dumplings.general;

import util.statemachine.StateMachine;

public abstract class AbstractHeuristic implements TimeoutHandler, PlayerHeuristic {
	protected boolean stopExecution;
	
	public void reset() {
		stopExecution = false;
	}
	public void setStateMachine(StateMachine stateMachine) { }	
	@Override
	public void onTimeout() {
		stopExecution = true;
	}
	
	public void cleanup() {};
}
