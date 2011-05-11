package com.dumplings.players;

import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import apps.player.detail.DetailPanel;

import com.dumplings.general.DumplingPropNetStateMachine;
import com.dumplings.general.PlayerStrategy;
import com.dumplings.heuristics.HybridMobility;
import com.dumplings.strategies.IDSAlphaBeta;
import com.dumplings.strategies.MiniMax;

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
//		final PlayerStrategy behindTheScene = metaStrategy;
//		final MachineState state = getCurrentState();
//		final Role role = getRole();
//		
//		(new Thread(new Runnable() {
//			@Override
//			public void run() {
//				try {
//				behindTheScene.getBestMove(state, role, Integer.MAX_VALUE);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}			
//		})).start();
		
		
		long start = System.currentTimeMillis();
		metaStrategy.getBestMove(getCurrentState(), getRole(), timeout);
		long end = System.currentTimeMillis();		
		System.out.println("Complete search spent " + (start - end));
		
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
		return new DumplingPropNetStateMachine();
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
