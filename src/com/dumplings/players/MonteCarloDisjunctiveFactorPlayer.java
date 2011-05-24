package com.dumplings.players;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.dumplings.strategies.IDSAlphaBetaFactor;

/**
 * AlphaBetaPlayer plays by using alpha-beta-pruning
 */
public final class MonteCarloDisjunctiveFactorPlayer extends StateMachineGamer
{
	PlayerStrategy strategy;
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("Finding latches");
		for (Proposition p: ((DumplingPropNetStateMachine)getStateMachine()).getLatches())
			System.out.println(p);
		System.out.println("Done");
		
		//DumplingPropNetStateMachine fsm = ((DumplingPropNetStateMachine)getStateMachine()).factorPropNet(getRole());
		Set<DumplingPropNetStateMachine> factors = 
			new HashSet<DumplingPropNetStateMachine>(((DumplingPropNetStateMachine)getStateMachine()).propNetFactors());
		//Set<DumplingPropNetStateMachine> factors = new HashSet<DumplingPropNetStateMachine>();
		//factors.add((DumplingPropNetStateMachine)getStateMachine());
		
		// If no factors are returned then use the original state machine
		if (factors == null) { 
			strategy = new IDSAlphaBeta(getStateMachine());
		} else {
			strategy = new IDSAlphaBetaFactor(getStateMachine(), factors);
		}
		
		
	
		AbstractHeuristic heuristic = new MonteCarloDepthLimitMemory(getStateMachine());
		((MonteCarloDepthLimitMemory)heuristic).setSampleSize(5);
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
		return "Monte Carlo Disjunctive Factor Dumpling";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}


}
