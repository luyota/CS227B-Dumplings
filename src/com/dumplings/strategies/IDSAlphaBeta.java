package com.dumplings.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerStrategy;
import com.dumplings.general.TimeoutHandler;
import com.dumplings.utils.Canonicalizer;

public class IDSAlphaBeta extends PlayerStrategy {
	private Map<String, Integer> maxStateScores;
	private Map<String, Map<String, Integer>> minStateScores;
	private int initialDepth = 0;
	private int hardMaxDepth = 128;
	
		
	@Override
	public void cleanup() {
		if (maxStateScores != null)
			maxStateScores.clear();
		if (minStateScores != null)
			minStateScores.clear();
		super.cleanup();
	}
	
	private AlphaBetaComputer abc = null;
	private boolean useCaching = true;
	private int numStatesExpanded;
	private int maxDepth;
	private int minCacheHit = 0, maxCacheHit = 0, extCacheHit = 0;

	private Timer timer;
	public Map<String, Integer> getMaxStateScores() { return maxStateScores; }
	public IDSAlphaBeta(StateMachine sm) {
		super(sm);		
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
	}
	
	public void enableCache(boolean flag) {
		useCaching = flag;
	}
	public void setInitialDepth(int d) { this.initialDepth = d; }
	
	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException {
		long start = System.currentTimeMillis();
		
		Move bestMove = null;		

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("Timed out!");
				if (abc != null)
					abc.onTimeout(); // signal calculation thread to stop ASAP					
			}		
		}, Math.max((timeout - System.currentTimeMillis() - 500), 0));

		maxDepth = initialDepth;
		Integer currentBestValue = Integer.MIN_VALUE;
		
		while (true) {
			if (maxDepth > hardMaxDepth) break;
			// maxDepth will be very big when it's a no-op or after reaching the real max depth of the search tree.
			System.out.println(role.toString() + ": Current depth - " + maxDepth);
			//System.out.println("Searching to max depth == " + maxDepth);
			// The cached value in the previous iteration shouldn't last to the next.
			// Not clearing the cache will result in problems. For example, when maxDepth = 2, the values cached are only valid with depth 2.
			//TODO: Remove these two lines.
			//maxStateScores.clear();
			//minStateScores.clear();

			abc = new AlphaBetaComputer(state, role);
			abc.start();			
			try {				
				abc.join(Math.max((timeout - System.currentTimeMillis() - 100), 0)); // wait until calculation thread finishes
			} catch (InterruptedException e) {}

			Integer newBestValue = abc.getBestValue();
			Move newBestMove = abc.getBestMove();
			if (!abc.stopExecution && newBestMove != null/* && 
				((bestValue != null && bestValue > currentBestValue) ||
				(bestValue == null && currentBestValue <= 0))*/) { // <== condition triggered by currentBestValue = 0, below
				
				// This is not perfect because, when deeper search returns the move that has the same score as the previous depth,
				// It might be the case that the move is different from the move in the previous depth. 
				// However, it prevents always choosing the move with deeper depth, especially when the path leads to an infinite game playing.
				
				if (newBestValue != null && newBestValue != currentBestValue) {
					System.out.println(role + " updated " + newBestValue + " " + currentBestValue);
					currentBestValue = newBestValue;
					bestMove = abc.getBestMove();
				}
				else {
					
					//Prevent the move that leads to an unknown state from substituted 
					//by the move that leads to a losing state.
					//currentBestValue = 0; // <== don't currentBestValue be <= 0 anyway if we're ever in here with a null bestValue? 
										  // answer: if it's originally < 0, it means that we at least have an unknown state, so set it to 0. 
										  // If it's originally == 0, it can be originally either a losing move or an unknown state, and it's always better to use an unknown move. 
				}
			}

			if (abc.stopExecution)
				break;
			if (abc.isSearchComplete) {
				System.out.println(role.toString() + ": Complete search at depth " + maxDepth + " from " + stateMachine.getLegalMoves(state, role).size() + " possible moves");
				break;
			}
			maxDepth++;
		}
		
		// Make sure bestMove is not null
		if (bestMove == null) {
			List<Move> moves = stateMachine.getLegalMoves(state, role);
			System.out.println(role.toString() + ": Didn't decide on any move. Playing randomly from " + moves.size() + " move.");
			
			Random generator = new Random();
			bestMove = moves.get(generator.nextInt(moves.size()));			
		}
		timer.cancel();
		System.out.println(role.toString() + ": Max Depth: " + maxDepth + "  Move: " + bestMove);		
		System.out.println(role.toString() + ": Playing move with score (0 might mean unknown): " + currentBestValue);
		System.out.println(role.toString() + ": Accumulative cache hit min/max/ext: " + minCacheHit + "/" + maxCacheHit + "/" + extCacheHit);
		System.out.println(role.toString() + ": # of entries in min/max/ext cache: " + minStateScores.size() + "/" + maxStateScores.size() + "/" + ((externalCache == null)?0:externalCache.size()));
		long stop = System.currentTimeMillis();
		System.out.println(role.toString() + ": time spent in getBestMove - " + (stop - start));
		
		
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

			bestMove = null;
			bestValue = Integer.MIN_VALUE;
			//The first move that results in an unknown state
			Move nullMove = null;
			numStatesExpanded = 1;

			for (Move move : moves) {
				if (stopExecution) {
					isSearchComplete = false;
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

		private Integer minScore(Role role, Move move, MachineState state, int alpha, int beta, int depth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			/* Check if we already have this state in cache */
			Integer cacheValue;
			String moveString = move.toString();
			String alphaBetaStateString = Canonicalizer.stateStringAlphaBeta(state, alpha, beta);
			Map<String, Integer> stateMoveScores = minStateScores.get(alphaBetaStateString);
			if (useCaching && stateMoveScores != null && (cacheValue = stateMoveScores.get(moveString)) != null) {
				//System.out.println(role.toString() + ": INTERMEDIATE CACHE HIT");
				minCacheHit++;
				return cacheValue;
			}			

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
				if (stateMoveScores == null) minStateScores.put(alphaBetaStateString, (stateMoveScores = new HashMap<String, Integer>()));
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

			String stateString = Canonicalizer.stateString(state);
			Integer cacheValue;
			if (externalCache != null && (cacheValue = externalCache.get(stateString)) != null) {
				extCacheHit ++;
				return cacheValue;
			}
			String alphaBetaStateString = Canonicalizer.stateStringAlphaBeta(stateString, alpha, beta);
			if (useCaching && (cacheValue = maxStateScores.get(alphaBetaStateString)) != null) {
				maxCacheHit ++;
				return cacheValue;
			}			

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
					maxStateScores.put(alphaBetaStateString, bestValue);

				return heuristicUsed ? -bestValue : bestValue;
			}
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
