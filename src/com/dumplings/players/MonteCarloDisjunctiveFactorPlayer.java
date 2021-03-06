package com.dumplings.players;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		//System.out.println("Finding latches");
		//for (Proposition p: ((DumplingPropNetStateMachine)getStateMachine()).getLatches())
		//	System.out.println(p);
		//System.out.println("Done");
		StateMachine sm = getStateMachine();
		if (sm.getRoles().size() == 1) {
			sm = ((DumplingPropNetStateMachine)getStateMachine()).factorPropNet(getRole());
			strategy = new IDSAlphaBeta(sm);
		} else {
			Set<DumplingPropNetStateMachine> factors = 
				new HashSet<DumplingPropNetStateMachine>(((DumplingPropNetStateMachine)getStateMachine()).propNetFactors());
			System.out.println("# of factors: " + factors.size());
			if (factors.size() > 1)
				strategy = new IDSAlphaBetaFactor(sm, factors);
			else
				strategy = new IDSAlphaBeta(sm);
		}
		
		//Set<DumplingPropNetStateMachine> factors = new HashSet<DumplingPropNetStateMachine>();
		//factors.add((DumplingPropNetStateMachine)getStateMachine());
		
		AbstractHeuristic heuristic = new MonteCarloDepthLimitMemory(getStateMachine());
		((MonteCarloDepthLimitMemory)heuristic).setSampleSize(4);
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
		Move selection;
		if (moves.size() == 1)
			selection = moves.get(0);
		else 
			selection = strategy.getBestMove(getCurrentState(), getRole(), timeout);

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
