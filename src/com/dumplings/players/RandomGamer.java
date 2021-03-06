package com.dumplings.players;

import java.util.List;
import java.util.Random;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import apps.player.detail.DetailPanel;

import com.dumplings.general.DumplingPropNetStateMachine;

/**
 * RandomGamer plays a random legal move
 */
public final class RandomGamer extends StateMachineGamer
{
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}
	
	/**
	 * Selects a random legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		Random generator = new Random();
		
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(generator.nextInt(moves.size()));

		long stop = System.currentTimeMillis();

		notifyObservers(new ReflexMoveSelectionEvent(moves, selection, stop - start));
		return selection;
	}
	
	@Override
	public void stateMachineStop() {
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new DumplingPropNetStateMachine();
	}
	
	@Override
	public String getName() {
		return "Random Dumplings";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}


}
