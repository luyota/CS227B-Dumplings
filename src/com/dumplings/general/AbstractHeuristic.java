package com.dumplings.general;

public abstract class AbstractHeuristic implements TimeoutHandler, PlayerHeuristic {
	protected boolean stopExecution;
	
	public void reset() {
		stopExecution = false;
	}
	
	@Override
	public void onTimeout() {
		stopExecution = true;
	}
	
	public void cleanup() {};
}
