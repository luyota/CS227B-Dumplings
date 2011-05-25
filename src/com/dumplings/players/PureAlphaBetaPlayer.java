package com.dumplings.players;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import apps.player.detail.DetailPanel;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.DumplingPropNetStateMachine;
import com.dumplings.general.PlayerStrategy;
import com.dumplings.heuristics.MonteCarlo;
import com.dumplings.heuristics.WeightedHeuristic;
import com.dumplings.strategies.AlphaBeta;

/**
 * AlphaBetaPlayer plays by using alpha-beta-pruning
 */
public final class PureAlphaBetaPlayer extends StateMachineGamer
{
	PlayerStrategy strategy;
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		strategy = new AlphaBeta(getStateMachine(), Integer.MAX_VALUE);
		
		long start = System.currentTimeMillis();
		Move m = strategy.getBestMove(getCurrentState(), getRole(), timeout);
		long stop = System.currentTimeMillis();
		System.out.println("Finished in " + (stop - start) + " ms, " + ((AlphaBeta) strategy).getNumStatesExpanded());
		
		List<AbstractHeuristic> heuristics = new ArrayList<AbstractHeuristic>();
		
		heuristics.add(new MonteCarlo(this.getStateMachine()));
		
		Map<AbstractHeuristic, Double> weightMap = strategy.metaGamer.evaluateHeuristics(heuristics, this.getRole(), timeout);
		for (AbstractHeuristic heuristic : weightMap.keySet()) {
			System.out.println(heuristic.toString()+": "+weightMap.get(heuristic));
		}
		strategy.setHeuristic(new WeightedHeuristic(weightMap));
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
		return "Pure Alpha-Beta";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}


}
