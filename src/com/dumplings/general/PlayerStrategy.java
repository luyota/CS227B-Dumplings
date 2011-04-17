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
	
	public abstract Move getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException;
}
