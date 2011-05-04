package com.dumplings.players;

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
import com.dumplings.heuristics.HybridMobility;
import com.dumplings.strategies.IDSAlphaBeta;
import com.dumplings.strategies.IDSAlphaBetaSimpleCache;
import com.dumplings.strategies.MiniMax;
import com.dumplings.strategies.MonteCarloMiniMax;

/**
 * AlphaBetaPlayer plays by using alpha-beta-pruning
 */
public final class CompleteSearchHeadStartPlayer extends StateMachineGamer
{
	PlayerStrategy strategy, metaStrategy;
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		
//		strategy = new IDSAlphaBetaSimpleCache(getStateMachine());
//		metaStrategy = new IDSAlphaBetaSimpleCache(getStateMachine());
//		((IDSAlphaBetaSimpleCache)metaStrategy).setInitialDepth(Integer.MAX_VALUE - 2);
//		
//		((IDSAlphaBetaSimpleCache)strategy).setLogging(true);
//		metaStrategy.getBestMove(getCurrentState(), getRole(), timeout);
//		strategy.setExternalCache(((IDSAlphaBetaSimpleCache)metaStrategy).getMaxStateScores());
		
		strategy = new IDSAlphaBeta(getStateMachine());
		metaStrategy = new MiniMax(getStateMachine());		

		metaStrategy.getBestMove(getCurrentState(), getRole(), timeout);
		strategy.setExternalCache(((MiniMax)metaStrategy).maxStateScores);
		strategy.setHeuristic(new HybridMobility(getStateMachine()));
		
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
		return "Complete Search Head Start Dumplings";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
}
