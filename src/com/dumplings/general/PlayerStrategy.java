package com.dumplings.general;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public abstract class PlayerStrategy {
	protected StateMachine stateMachine;
	
	public PlayerStrategy(StateMachine sm) {
		this.stateMachine = sm;
	}
	
	public abstract void enableCache(boolean flag);
	
	public abstract Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException;
	
	protected PlayerHeuristic heuristic;
	protected Integer heuristicExpansion;
	public void setHeuristic(PlayerHeuristic heuristic) {
		setHeuristic(heuristic, 1);
	}
	public void setHeuristic(PlayerHeuristic heuristic, Integer expansion) {
		this.heuristic = heuristic;
		this.heuristicExpansion = expansion;
	}
}
