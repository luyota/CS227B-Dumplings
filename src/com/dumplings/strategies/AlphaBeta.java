package com.dumplings.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerStrategy;

public class AlphaBeta extends PlayerStrategy {
	Map<String, Integer> maxStateScores;
	Map<String, Map<String, Integer>> minStateScores;
	private AlphaBetaComputer abc;
	private boolean useCaching = true;
	private int numStatesExpanded;
	
	static protected boolean stopExecution = false;
	
	public AlphaBeta(StateMachine sm) {
		super(sm);
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
	}
	
	public void enableCache(boolean flag) {
		useCaching = flag;
	}
	
	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException {
		try {
			System.out.println("Hold on just a moment..."); // why does this prevent lockups???
			Thread.sleep(500);
		} catch (InterruptedException e) {}
		
		// Call the thread that does the computation
		abc = new AlphaBetaComputer(state, role, Thread.currentThread());
		stopExecution = false;
		abc.start();
		
		// And go to sleep, but not longer than the timeout
		try {
			Thread.sleep((timeout - System.currentTimeMillis()) - 500);
			System.out.println("Timed out!");
			stopExecution = true;
		} catch (InterruptedException e) {
			System.out.println("Computer reached conclusion.");
		}
		
		// Make sure bestMove is not null
		System.out.println("Checking best move...");
		Move bestMove = abc.getBestMove();
		if (bestMove == null) {
			System.out.println("Falling back to legal strategy.");
			bestMove = stateMachine.getLegalMoves(state, role).get(0);
		}
		
		System.out.println("bestMove is " + bestMove.toString());
		return bestMove;
	}
	
	private class AlphaBetaComputer extends Thread {
		private MachineState state;
		private Role role;
		private Thread parent;
		private Move bestMove;
		
		public AlphaBetaComputer(MachineState state, Role role, Thread parent) {
			this.state = state;
			this.role = role;
			this.parent = parent;
		}
		
		public Move getBestMove() {
			return bestMove;
		}
		
		public void run() {
			System.out.println("Starting " + Thread.currentThread().getId());
			try {
				getBestMove(state, role);
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				e.printStackTrace();
			}
			parent.interrupt();
			System.out.println("Finished " + Thread.currentThread().getId());
		}
		
		public void getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException { 		
			List<Move> moves = stateMachine.getLegalMoves(state, role);
			if (moves.size() == 1) {
				System.out.println("Expanded 1 state");
				bestMove = moves.get(0);
			} else {
				bestMove = null;
				int bestValue = Integer.MIN_VALUE;
				numStatesExpanded = 1;
				
				for (Move move : moves) {
					int value = minScore(role, move, state, Integer.MIN_VALUE, Integer.MAX_VALUE);
					if (value > bestValue) {
						bestValue = value;
						bestMove = move;
					}
					if (stopExecution) {
						System.out.println("Stopping alpha-beta thread!");
						break;
					}
				}
				System.out.println("Best: " + bestValue);
				System.out.println(Thread.currentThread().getId()+ " Expanded " + numStatesExpanded + " states");
			}
		}
		
		private int minScore(Role role, Move move, MachineState state, int alpha, int beta) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			int worstScore = Integer.MAX_VALUE;
			String stateString = canonicalizeStateString(state, alpha, beta);
			String moveString = move.toString();
			Map<String, Integer> stateMoveScores = minStateScores.get(stateString);
			if (useCaching && stateMoveScores != null && stateMoveScores.get(moveString) != null)
				return stateMoveScores.get(moveString);
			List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
			for (List<Move> jointMove : allJointMoves) {
				MachineState newState = stateMachine.getNextState(state, jointMove);
				int newScore = maxScore(role, newState, alpha, beta);
				if (newScore < worstScore)
					worstScore = newScore;
				beta = Math.min(beta, worstScore);
				if (beta <= alpha) {
					worstScore = beta;
					break;
				}
				if (stopExecution) {
					System.out.println("Stopping minScore!");
					break;
				}
			}
			if (stateMoveScores == null) minStateScores.put(stateString, (stateMoveScores = new HashMap<String, Integer>()));
			stateMoveScores.put(moveString, worstScore);
			return worstScore;
		}
		
		private int maxScore(Role role, MachineState state, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
			if (stateMachine.isTerminal(state)) { 			
				numStatesExpanded++;
				return stateMachine.getGoal(state, role);		
			}
			String stateString = canonicalizeStateString(state, alpha, beta);
			if (useCaching && maxStateScores.get(stateString) != null) 			
				return maxStateScores.get(stateString);
			
			numStatesExpanded++;
			int bestValue = Integer.MIN_VALUE;
			for (Move move : stateMachine.getLegalMoves(state, role)) {
				int value = minScore(role, move, state, alpha, beta);
				if (value > bestValue)
					bestValue = value;
				alpha = Math.max(alpha, bestValue);
				if (alpha >= beta) {
					bestValue = alpha;
					break;
				}
				if (stopExecution) {
					System.out.println("Stopping maxScore!");
					break;
				}
			}
			maxStateScores.put(stateString, bestValue);
			return bestValue;
		}
		
		private String canonicalizeStateString(MachineState state, int alpha, int beta) {
			Set<String> sortedStateContents = new TreeSet<String>();
			for (GdlSentence gdl : state.getContents()) {
				sortedStateContents.add(gdl.toString());
			}
			return alpha + " " + beta + " " + sortedStateContents.toString();
		}
	}
	
}
