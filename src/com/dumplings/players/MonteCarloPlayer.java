package com.dumplings.players;

import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.propnet.architecture.components.Proposition;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import apps.player.detail.DetailPanel;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.DumplingPropNetStateMachine;
import com.dumplings.general.PlayerStrategy;
import com.dumplings.heuristics.MonteCarloDepthLimitMemory;
import com.dumplings.strategies.IDSAlphaBeta;

/**
 * AlphaBetaPlayer plays by using alpha-beta-pruning
 */
public final class MonteCarloPlayer extends StateMachineGamer
{
	PlayerStrategy strategy;
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{	
		//DumplingPropNetStateMachine fsm = ((DumplingPropNetStateMachine)getStateMachine()).factorPropNet(getRole());
		//strategy = new IDSAlphaBeta(fsm == null ? getStateMachine() : fsm);
		strategy = new IDSAlphaBeta(getStateMachine());
		
		//((DumplingPropNetStateMachine)getStateMachine()).propNetFactors();
		
		//strategy = new IDSAlphaBeta(getStateMachine());
		//AbstractHeuristic heuristic = new MonteCarloDepthLimit(getStateMachine());
		AbstractHeuristic heuristic = new MonteCarloDepthLimitMemory(getStateMachine());
		((MonteCarloDepthLimitMemory)heuristic).setSampleSize(1);
		((MonteCarloDepthLimitMemory)heuristic).setMaxDepth(64);
		strategy.setHeuristic(heuristic);
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
		//System.out.println("Moves available: " + moves.size());
		//for (Move m : moves)
		//	System.out.println(m);
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
		//return new ProverStateMachine();
		return new DumplingPropNetStateMachine();
	}
	@Override
	public String getName() {
		return "Monte Carlo Dumplings";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}


}
