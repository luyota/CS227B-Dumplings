package com.dumplings.assgn3;

import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.player.detail.DetailPanel;

import com.dumplings.general.PlayerStrategy;
import com.dumplings.heuristics.OpponentMobility;
import com.dumplings.heuristics.MonteCarlo;
import com.dumplings.strategies.AlphaBeta;

/**
 * AlphaBetaPlayer plays by using alpha-beta-pruning
 */
public final class AlphaBetaPlayer extends StateMachineGamer
{
	PlayerStrategy strategy;
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		//strategy = new AlphaBeta(getStateMachine(), Integer.MAX_VALUE);
		strategy = new AlphaBeta(getStateMachine(), 3);
		strategy.setHeuristic(new MonteCarlo(getStateMachine()));
	}
	
	/**
	 * Selects the best legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("Selecting move...");

		long start = System.currentTimeMillis();
		
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = strategy.getBestMove(getCurrentState(), getRole(), timeout);

		long stop = System.currentTimeMillis();
		
		System.out.println("Total time (ms): " + (stop - start));

		notifyObservers(new ReflexMoveSelectionEvent(moves, selection, stop - start));
		
		System.out.println("Finishing move!");
		return selection;
	}
	
	@Override
	public void stateMachineStop() {
		// Do nothing.
	}

	/**
	 * Uses a ProverStateMachine
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	@Override
	public String getName() {
		return "Alpha-Beta Dumplings";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}


}
