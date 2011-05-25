package com.dumplings.strategies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.DumplingPropNetStateMachine;
import com.dumplings.general.PlayerStrategy;
import com.dumplings.general.TimeoutHandler;
import com.dumplings.utils.Canonicalizer;

public class IDSAlphaBetaFactor extends PlayerStrategy {
	private class Result {
		public Move move = null;
		public Integer value = null;
		//public int depth = 0;		
		public Result(Move move, Integer value) { 
			this.move = move;
			this.value = value;
			//this.depth = depth;
		}
	}
	
	private Map<String, Integer> maxStateScores;
	private Map<String, Map<String, Integer>> minStateScores;
		
	private int initialDepth = 1;
	private int hardMaxDepth = 128;
	
	private Set<DumplingPropNetStateMachine> factors;
	
	private AlphaBetaComputer abc = null;
	private boolean useCaching = true;
	private int numStatesExpanded;
	private int maxDepth;
	private int minCacheHit = 0, maxCacheHit = 0, extCacheHit = 0;

	private Timer timer;
	
	public Map<String, Integer> getMaxStateScores() { return maxStateScores; }
	public void enableCache(boolean flag) { useCaching = flag; }
	public void setInitialDepth(int d) { this.initialDepth = d; }
	
	public IDSAlphaBetaFactor(StateMachine original, Set<DumplingPropNetStateMachine> factors) {
		super(original);
		this.factors = factors;
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
	}
	
	@Override
	public void cleanup() {
		if (maxStateScores != null)
			maxStateScores.clear();
		if (minStateScores != null)
			minStateScores.clear();
		super.cleanup();
	}
	
	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException {
		long start = System.currentTimeMillis();		

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
		
		Result currentBestResult = new Result(null, Integer.MIN_VALUE);
		
		// Used to cache previous results.
		//HashMap<DumplingPropNetStateMachine, Result> prevBestResults = null; 
		
		while (true) {
			if (maxDepth > hardMaxDepth) break;
			// maxDepth will be very big when it's a no-op or after reaching the real max depth of the search tree.
			System.out.println(role.toString() + ": Current depth - " + maxDepth);
			
			abc = new AlphaBetaComputer(state, role);
			abc.start();			
			try {				
				abc.join(Math.max((timeout - System.currentTimeMillis() - 100), 0)); // wait until calculation thread finishes
			} catch (InterruptedException e) {}
						
			//DumplingPropNetStateMachine forceDeathFactor = null;
			boolean isFirst = true;
			HashMap<DumplingPropNetStateMachine, Result> newBestResults = abc.getBestResults();			
			Result newBestResult = null;
			
			// We find the best result among all the factors							
			for (DumplingPropNetStateMachine factor : newBestResults.keySet()) {				
				Result result = newBestResults.get(factor);
				//System.out.println(result.value);
				
				if (isFirst) {
					newBestResult = result;
				} else {
					if ((result.value != null && newBestResult.value != null && newBestResult.value < result.value) ||
							(result.value != null && newBestResult.value == null)) {
						//System.out.println("hhh");
						newBestResult = result;
					}
				}
				isFirst = false;				
			}
			//System.out.println("new best result: " + newBestResult.value);
			if (newBestResult != null) {					
				if (maxDepth == 1 || (!abc.stopExecution && newBestResult.move != null)) {					
					// This is not perfect because, when deeper search returns the move that has the same score as the previous depth,
					// It might be the case that the move is different from the move in the previous depth. 
					// However, it prevents always choosing the move with deeper depth, especially when the path leads to an infinite game playing.

					if (newBestResult.value != null && newBestResult.value != currentBestResult.value) {
						System.out.println(role + ": updated to " + newBestResult.value + " from " + currentBestResult.value);
						currentBestResult.value = newBestResult.value;
						currentBestResult.move = newBestResult.move;
					}									
				}
			} else {
				// This shouldn't happen since newBestResult is as least assigned with the first factor's result.
				System.out.println("Something bad happens...");
			}
			if (currentBestResult.value == 100)
				break;
			if (abc.stopExecution)
				break;
			if (abc.isSearchComplete) {
				System.out.println(role.toString() + ": Complete search at depth " + maxDepth + " from " + stateMachine.getLegalMoves(state, role).size() + " possible moves");
				break;
			}
			System.out.println(role.toString() + ": Best score at depth " + maxDepth + ": " + currentBestResult.value);
			//prevBestResults = newBestResults;

			maxDepth++;
		}
		
		// Make sure bestMove is not null
		if (currentBestResult.move == null) {
			List<Move> moves = stateMachine.getLegalMoves(state, role);
			System.out.println(role.toString() + ": Didn't decide on any move. Playing randomly from " + moves.size() + " move(s).");
			
			Random generator = new Random();
			currentBestResult.move = moves.get(generator.nextInt(moves.size()));			
		}
		timer.cancel();
		System.out.println(role.toString() + ": Max Depth: " + maxDepth + "  Move: " + currentBestResult.move);		
		System.out.println(role.toString() + ": Playing move with score (0 might mean unknown): " + currentBestResult.value);
		System.out.println(role.toString() + ": Accumulative cache hit min/max/ext: " + minCacheHit + "/" + maxCacheHit + "/" + extCacheHit);
		System.out.println(role.toString() + ": # of entries in min/max/ext cache: " + minStateScores.size() + "/" + maxStateScores.size() + "/" + ((externalCache == null)?0:externalCache.size()));
		long stop = System.currentTimeMillis();
		System.out.println(role.toString() + ": time spent in getBestMove - " + (stop - start));
		
		
		return currentBestResult.move;
	}

	private class AlphaBetaComputer extends Thread implements TimeoutHandler {
		
		private MachineState state;
		private Role role;		
		//This data structure stores the best results of each factor
		private HashMap<DumplingPropNetStateMachine, Result> bestResults = new HashMap<DumplingPropNetStateMachine, Result>();
		// This is used to track the force death moves
		//private HashMap<DumplingPropNetStateMachine, Integer> worstValues = new HashMap<DumplingPropNetStateMachine, Integer>();
		private boolean stopExecution = false;
		private boolean isSearchComplete = true;
		private Set<DumplingPropNetStateMachine> effectiveFactors = new HashSet<DumplingPropNetStateMachine>();
		private DumplingPropNetStateMachine currentFactor;
		private boolean isDeathSearch = false;

		public AlphaBetaComputer(MachineState state, Role role) {
			this.state = state;
			this.role = role;
			// Only search on the factors that the role can actually perform moves on
			for(DumplingPropNetStateMachine factor : factors) {
				try {
					if (factor.getLegalMoves(state, role).size() > 0)
						effectiveFactors.add(factor);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		public HashMap<DumplingPropNetStateMachine, Result> getBestResults() {	return bestResults; }		
		//public HashMap<DumplingPropNetStateMachine, Integer> getWorstValues() { return worstValues; }

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
			if (heuristic != null) {
				heuristic.reset();
			}
			
			DumplingPropNetStateMachine deathFactor = null;
			
			//We don't need heuristic when looking for force death
			AbstractHeuristic tempHeuristic = heuristic;
			heuristic = null;			
			
			for (DumplingPropNetStateMachine factor : effectiveFactors) {
				// Look for the force death in other factors to see if this one results in a force death
				// We pick up whatever joint move because the legal move in a factor won't affect other factors (in theory)
				DumplingPropNetStateMachine differentFactor = null;
				for (DumplingPropNetStateMachine anotherFactor : effectiveFactors) {
					if (anotherFactor != factor) {
						differentFactor = anotherFactor;
						break;
					}
				}
				
				if (differentFactor == null)
					break;
				
				List<Move> jointMove = differentFactor.getRandomJointMove(state);
				MachineState nextState = stateMachine.getNextState(state, jointMove);
				
				currentFactor = factor;
				Integer testValue = maxScore(role, nextState, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);	
				
				if (testValue != null && testValue == 0) {
					deathFactor = factor; 
					break;
				}
			}
			
			heuristic = tempHeuristic;
			if (deathFactor != null) {
				System.out.println(role + ": death factor found... only search in that factor.");
				searchFactor(deathFactor, state, role);
				System.out.println(role + ": Best factor size " + bestResults.size());
			}
			else { 
				for (DumplingPropNetStateMachine factor : effectiveFactors) {			
					searchFactor(factor, state, role);
					if (stopExecution) {
						isSearchComplete = false;
						break;
					}
				}
			}
		}
		
		private void searchFactor(DumplingPropNetStateMachine factor, MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			//The first move that results in an unknown state
			
			numStatesExpanded = 1;
						
			currentFactor = factor;
			if (heuristic != null)
				heuristic.setStateMachine(factor);
			
			Result result = new Result(null, Integer.MIN_VALUE);
			List<Move> moves = factor.getLegalMoves(state, role);
			for (Move move : moves) {
				if (stopExecution) {
					isSearchComplete = false;
					break;
				}
				if (heuristic != null)
					heuristic.cleanup();
				
				Integer testValue = minScore(role, move, state, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
				// If not heuristic
				if (testValue != null) {						
					if (testValue < 0)
						isSearchComplete = false;
					// value could be negative if heuristic was used, so use absolute value
					
					int value = Math.abs(testValue);
					//System.out.println(role + ": value returned " + value);
					if (value > result.value) {
						result.value = value;
						result.move = move;
					}
				} else {
					isSearchComplete = false;					
				}
			}
			
			System.out.println(role + ": factor searched " + result.value + " move " + result.move);
			bestResults.put(factor, result);	
		}

		private Integer minScore(Role role, Move move, MachineState state, int alpha, int beta, int depth) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			if (isDeathSearch)
				System.out.println(role + ": min state " + state + ", move:" + move);
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
			List<List<Move>> allJointMoves = currentFactor.getLegalJointMoves(state, role, move);
			
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
			if (nullValueReturned && worstScore == 100) {
				//System.out.println(role + " mark 1");
				return null;
			}
			//If not even able to reach the end then mark this step as unknown
			if (worstScore == Integer.MAX_VALUE) {
				//System.out.println(role + " mark 2 " + depth);
				return null;
			}

			if (!heuristicUsed) { // don't cache if we're not 100% sure this is the best value
				if (stateMoveScores == null) minStateScores.put(alphaBetaStateString, (stateMoveScores = new HashMap<String, Integer>()));
				stateMoveScores.put(moveString, worstScore);
			}
			if (isDeathSearch)
				System.out.println(role + ": min " + state + " returns worstScore " + worstScore);
			return heuristicUsed ? -worstScore : worstScore;
		}

		private Integer maxScore(Role role, MachineState state, int alpha, int beta, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
			if (isDeathSearch)
				System.out.println(role + ": max state " + state );
			/* First check if this state is terminal */
			if (currentFactor.isTerminal(state)) {				
				numStatesExpanded++;
				return currentFactor.getGoal(state, role);		
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
			List<Move> moves = currentFactor.getLegalMoves(state, role);
			if (depth > maxDepth) {
				// only apply heuristics when we've alpha-beta-ed as deep as we're going to go
				// so heuristics don't slow us down as we're IDS-ing
				//if (heuristic != null && isTimeout) {

				// this is as far as we go, so calculate heuristic and be done w/ it
				heuristicUsed = true;
				if (heuristic != null /*&& moves.size() > 1*/) {
					Integer value = heuristic.getScore(state, role);
					if (isDeathSearch)
						System.out.println(role + ": max " + state + " heuristic returns worstScore " + value);
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
				for (Move move : moves) {
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
				if (bestValue == 0 && nullValueReturned) {
					//System.out.println(role + " mark 3");
					return null;
				}

				//If not even able to reach the end then mark this step as unknown which is still better than 0
				if (bestValue == Integer.MIN_VALUE) {
					//System.out.println(role + " mark 4 " + depth);
					return null;
				}

				if (!stopExecution && !heuristicUsed) // don't cache if we're not 100% sure this is the best value
					maxStateScores.put(alphaBetaStateString, bestValue);
				
				if (isDeathSearch)
					System.out.println(role + ": max " + state + " returns worstScore " + bestValue);
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
