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
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.player.detail.DetailPanel;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.PlayerHeuristic;
import com.dumplings.general.PlayerStrategy;
import com.dumplings.heuristics.Focus;
import com.dumplings.heuristics.Mobility;
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
		List<AbstractHeuristic> heuristics = new ArrayList<AbstractHeuristic>();
		heuristics.add(new MonteCarlo(this.getStateMachine()));
		heuristics.add(new Mobility(this.getStateMachine()));
		heuristics.add(new Focus(this.getStateMachine()));
		
		Map<AbstractHeuristic, Double> weightMap = strategy.metaGamer.evaluateHeuristics(heuristics, this.getRole(), (int)timeout);
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
		return new ProverStateMachine();
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
