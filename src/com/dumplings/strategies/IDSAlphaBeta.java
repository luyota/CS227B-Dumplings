package com.dumplings.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
import com.dumplings.general.TimeoutHandler;

public class IDSAlphaBeta extends PlayerStrategy {
	Map<String, Integer> maxStateScores;
	Map<String, Map<String, Integer>> minStateScores;
	
	private AlphaBetaComputer abc = null;
	private boolean useCaching = true;
	private int numStatesExpanded;
	private int maxDepth;	
	private boolean isTimeout = false;
	
	private Timer timer;
	
		
	public IDSAlphaBeta(StateMachine sm) {
		super(sm);		
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
	
	}
	
	public void enableCache(boolean flag) {
		useCaching = flag;
	}
	
	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException {
		Move bestMove = null;
		isTimeout = false;
		
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("Timed out!");
				if (abc != null)
					abc.onTimeout(); // signal calculation thread to stop ASAP					
			}		
		}, Math.max((timeout - System.currentTimeMillis() - 500), 0));
		
		maxDepth = 0;
		currentBestValue = Integer.MIN_VALUE;
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		if (moves.size() == 1) { // don't keep searching multiple depths if we can only do one thing...
			bestMove = moves.get(0);
			currentBestValue = -1;
		} else {
			while (true) {
				// maxDepth will be very big when it's a no-op or after reaching the real max depth of the search tree.
				maxDepth++;
				//System.out.println("Searching to max depth == " + maxDepth);
				// The cached value in the previous iteration shouldn't last to the next.
				// Not clearing the cache will result in problems. For example, when maxDepth = 2, the values cached are only valid with depth 2.			
				maxStateScores.clear();
				minStateScores.clear();
				
				abc = new AlphaBetaComputer(state, role);
				abc.start();			
				try {				
					abc.join(); // wait until calculation thread finishes
				} catch (InterruptedException e) {}
				
				if (abc.getBestMove() != null && 
					((abc.getBestValue() != null && abc.getBestValue() > currentBestValue) ||
					(abc.getBestValue() == null && currentBestValue <= 0))) {
					bestMove = abc.getBestMove();
					if (abc.getBestValue() != null)
						currentBestValue = abc.getBestValue();
					else 
						//Prevent the move that leads to an unknown state from substituted 
						//by the move that leads to a losing state.
						currentBestValue = 0;
				}
				
				if (abc.stopExecution)
					break;
				if (abc.isSearchComplete) {
					System.out.println("COMPLETE SEARCH at depth " + maxDepth);
					break;
				}
			}
		}
		// Make sure bestMove is not null
		if (bestMove == null) {
			System.out.println(role.toString() + " Didn't decide on any move. Playing first legal move.");
			bestMove = stateMachine.getLegalMoves(state, role).get(0);
		}
		System.out.println(role.toString() + " Max Depth: " + maxDepth);
		timer.cancel();		
		System.out.println(role.toString() + " Playing move with score: " + currentBestValue);
		return bestMove;
	}
	
	private class AlphaBetaComputer extends Thread implements TimeoutHandler {
		private MachineState state;
		private Role role;
		private Move bestMove;
		private Integer bestValue;
		private boolean stopExecution = false;
		private boolean isSearchComplete = true;
		
		public AlphaBetaComputer(MachineState state, Role role) {
			this.state = state;
			this.role = role;
		}
		
		public Move getBestMove() {
			return bestMove;
		}
		public Integer getBestValue() {
			return bestValue;
		}
		
		public void run() {
			try {
				getBestMove(state, role);
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				e.printStackTrace();
			}
		}
		
		public void getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException { 		
			stopExecution = false;
			if (heuristic != null)
				heuristic.reset();
			List<Move> moves = stateMachine.getLegalMoves(state, role);
			if (moves.size() == 1) {
				bestMove = moves.get(0);
			} else {
				bestMove = null;
				bestValue = Integer.MIN_VALUE;
				//The first move that results in an unknown state
				Move nullMove = null;
				numStatesExpanded = 1;
				
				for (Move move : moves) {
					if (stopExecution) {
						break;
					}

					Integer testValue = minScore(role, move, state, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
					if (testValue != null) {
						if (testValue < 0)
							isSearchComplete = false;
						// value could be negative if heuristic was used, so use absolute value
						int value = Math.abs(testValue);
						if (value > bestValue) {
							bestValue = value;
							bestMove = move;
						}
					} else {
						isSearchComplete = false;
						if (nullMove == null)
							nullMove = move;
					}
				}
								
				
				if (bestValue <= 0 && nullMove != null) {
					bestValue = null;
					bestMove = nullMove;
				}
			}
		}
		
		private Integer minScore(Role role, Move move, MachineState state, int alpha, int beta, int depth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			/* Check if we already have this state in cache */
			String stateString = canonicalizeStateString(state, alpha, beta);
			String moveString = move.toString();
			Map<String, Integer> stateMoveScores = minStateScores.get(stateString);
			if (useCaching && stateMoveScores != null && stateMoveScores.get(moveString) != null)
				return stateMoveScores.get(moveString);
			
			/* Compute minScore */
			List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
			int worstScore = Integer.MAX_VALUE;
			boolean heuristicUsed = false, nullValueReturned = false;
			for (List<Move> jointMove : allJointMoves) {
				if (stopExecution) {
					break;
				}
				MachineState newState = stateMachine.getNextState(state, jointMove);
				Integer newScore = maxScore(role, newState, alpha, beta, depth + 1);
				if (newScore != null) {
					int testScore = newScore;
					if (newScore < 0) { // it's a heuristic
						testScore = -testScore;
						heuristicUsed = true;
					}
					if (testScore < worstScore)
						worstScore = testScore;
					beta = Math.min(beta, worstScore);
					if (beta <= alpha) {
						worstScore = beta;
						break;
					}
				} else {
					nullValueReturned = true;
				}
			}
			// If there exists some unknown state choose this state at least it's better 
			// than 100
			if (nullValueReturned && worstScore == 100) 
				return null;
			//If not even able to reach the end then mark this step as unknown
			if (worstScore == Integer.MAX_VALUE)
				return null;
			
			if (!heuristicUsed) { // don't cache if we're not 100% sure this is the best value
				if (stateMoveScores == null) minStateScores.put(stateString, (stateMoveScores = new HashMap<String, Integer>()));
				stateMoveScores.put(moveString, worstScore);
			}

			return heuristicUsed ? -worstScore : worstScore;
		}
		
		private Integer maxScore(Role role, MachineState state, int alpha, int beta, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
			/* First check if this state is terminal */
			if (stateMachine.isTerminal(state)) { 			
				numStatesExpanded++;
				return stateMachine.getGoal(state, role);		
			}
			
			String stateString = canonicalizeStateString(state, alpha, beta);
			if (useCaching && maxStateScores.get(stateString) != null) 			
				return maxStateScores.get(stateString);
			
			numStatesExpanded++;
			int bestValue = Integer.MIN_VALUE;
			boolean heuristicUsed = false, nullValueReturned = false;
			if (depth > maxDepth) {
				// only apply heuristics when we've alpha-beta-ed as deep as we're going to go
				// so heuristics don't slow us down as we're IDS-ing
				//if (heuristic != null && isTimeout) {
				
				// this is as far as we go, so calculate heuristic and be done w/ it
				heuristicUsed = true;
				if (heuristic != null) {
					Integer value = heuristic.getScore(state, role);
					if (value != null)
						return -value; // return heuristic scores as negative to differentiate for caching purposes
					return null;
				} else {
					//Originally I returned Integer.MIN_VALUE; but then I found out that although this move's result is unknown, it's still
					//better than choosing a move that your opponent will have chance to win, in which the value would be 0 which is larger
					//than Interger.MIN_VALUE.
					return null;
				}
			} 
			else {
				for (Move move : stateMachine.getLegalMoves(state, role)) {
					if (stopExecution) {
						break;
					}
					Integer value = minScore(role, move, state, alpha, beta, depth);
					if (value != null) {
						int testValue = value;
						if (value < 0) { // it's a heuristic
							testValue = -testValue;
							heuristicUsed = true;
						}
						if (testValue > bestValue)
							bestValue = testValue;
						alpha = Math.max(alpha, bestValue);
						if (alpha >= beta) {
							bestValue = alpha;
							break;
						}
					} else {
						nullValueReturned = true;
					}
				}
				// If no moves better than 0 and there exists some unknown states 
				if (bestValue == 0 && nullValueReturned)
					return null;
				
				//If not even able to reach the end then mark this step as unknown which is still better than 0
				if (bestValue == Integer.MIN_VALUE)
					return null;
					
				if (!stopExecution && !heuristicUsed) // don't cache if we're not 100% sure this is the best value
					maxStateScores.put(stateString, bestValue);
				
				return heuristicUsed ? -bestValue : bestValue;
			}
		}
		
		/*
		 * This function makes sure we don't distinguish between same states
		 */
		private String canonicalizeStateString(MachineState state, int alpha, int beta) {
			Set<String> sortedStateContents = new TreeSet<String>();
			for (GdlSentence gdl : state.getContents()) {
				sortedStateContents.add(gdl.toString());
			}
			return alpha + " " + beta + " " + sortedStateContents.toString();
		}

		@Override
		public void onTimeout() {
			/*
			 * Using a flag for now
			 */
			stopExecution = true;
			if (heuristic != null) heuristic.onTimeout();
		}
	}
}
