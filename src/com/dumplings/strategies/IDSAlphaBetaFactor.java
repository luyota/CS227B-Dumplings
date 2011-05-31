package com.dumplings.strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
		System.out.println("IDSAlphaBetaFactor getBestMove");
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
					if (result != null &&
							(result.value != null && newBestResult.value != null && newBestResult.value < result.value) ||
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
				System.out.println("Something bad happened...");
			}
			if (currentBestResult.value == 100)
				break;
			if (abc.stopExecution)
				break;
			if (abc.isSearchComplete) {
				System.out.println(role.toString() + ": Complete search at depth " + maxDepth + " from " + stateMachine.getLegalMoves(state, role).size() + " possible moves");
				break;
			}
			if (abc.isDeath())
				break;
			System.out.println(role.toString() + ": Best score at depth " + maxDepth + ": " + currentBestResult.value + ", move: " + currentBestResult.move);
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
		System.out.println(role.toString() + ": ========Summary======");
		System.out.println(role.toString() + ": Max Depth: " + maxDepth + "  Move: " + currentBestResult.move);		
		System.out.println(role.toString() + ": Playing move with score (0 might mean unknown): " + currentBestResult.value);
		System.out.println(role.toString() + ": Accumulative cache hit min/max/ext: " + minCacheHit + "/" + maxCacheHit + "/" + extCacheHit);
		System.out.println(role.toString() + ": # of entries in min/max/ext cache: " + minStateScores.size() + "/" + maxStateScores.size() + "/" + ((externalCache == null)?0:externalCache.size()));
		long stop = System.currentTimeMillis();
		System.out.println(role.toString() + ": time spent in getBestMove - " + (stop - start));
		System.out.println("");
		
		
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
		private boolean isDeathFound = false;
		
		public boolean isDeath() { return isDeathFound; }

		public AlphaBetaComputer(MachineState state, Role role) throws MoveDefinitionException {
			this.state = state;
			this.role = role;
			System.out.println("# of factors: " + factors.size());
			// Only search on the factors that the role can actually perform moves on
			for(DumplingPropNetStateMachine factor : factors)
				if (factor.getLegalMoves(state, role).size() > 0)
					effectiveFactors.add(factor);
			System.out.println("# of effective factors: " + effectiveFactors.size());
		}
		public HashMap<DumplingPropNetStateMachine, Result> getBestResults() {	return bestResults; }		

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
			
			long start, end;
			
			start = System.currentTimeMillis();			
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
				
				List<Move> jointMove = null;
				try {
					jointMove = differentFactor.getRandomJointMove(state);
				} catch (IllegalArgumentException e) {
					// some player doesn't have a legal move in differentFactor,
					// so pull in some random move from across other factors
					System.out.println(role + ": for death-factor search, cannot find random, complete joint move in single factor");
					jointMove = new ArrayList<Move>(stateMachine.getRoles().size());
					Map<Role, Integer> roleIndices = stateMachine.getRoleIndices();
					for (Role playerRole : stateMachine.getRoles()) {
						boolean moveFound = false;
						for (DumplingPropNetStateMachine otherFactor : factors) {
							if (otherFactor != factor) {
								List<Move> legalMoves = otherFactor.getLegalMoves(state, playerRole);
								if (!legalMoves.isEmpty()) {
									Move move = legalMoves.get(0);
									jointMove.set(roleIndices.get(playerRole), move);
									moveFound = true;
									break;
								}
							}
						}
						if (!moveFound)
							jointMove.clear();
					}
				}
				if (jointMove != null && jointMove.size() == stateMachine.getRoles().size()) {
					// all set -- go ahead and search for death
					System.out.println(role + ": searching for death factor with random first move");
					MachineState nextState = stateMachine.getNextState(state, jointMove);
					
					currentFactor = factor;
					Integer testValue = maxScore(role, nextState, Integer.MIN_VALUE, Integer.MAX_VALUE, 2);	
					
					if (testValue != null && testValue == 0) {
						isDeathFound = true;
						deathFactor = factor; 
						break;
					}
				} else {
					// still couldn't get a legal joint move from other factors,
					// so we must need a move from the current factor, which means
					// we have to check all legal joint moves from this factor in
					// order to properly determine its 'death' status
					List<List<Move>> allJointMoves = factor.getLegalJointMoves(state);
					if (!allJointMoves.isEmpty()) {
						System.out.println(role + ": searching through all of factor's joint moves for certain death");
						for (List<Move> factorJointMove : allJointMoves) {
							MachineState nextState = stateMachine.getNextState(state, factorJointMove);
							
							currentFactor = factor;
							Integer testValue = maxScore(role, nextState, Integer.MIN_VALUE, Integer.MAX_VALUE, 2);	
							
							if (testValue != null && testValue == 0) {
								isDeathFound = true;
								deathFactor = factor; 
								break;
							}
						}
					} else {
						// can't even get legal joint moves in *this* factor! so
						// search for legal joint moves across all factors that
						// include a legal move for out player in this factor
						System.out.println(role + ": searching for certain death through all of player's moves in factor combined w/ opponent moves from others");
						Map<Role, Integer> roleIndices = stateMachine.getRoleIndices();
						for (Move move : factor.getLegalMoves(state, role)) {
							jointMove = new ArrayList<Move>(stateMachine.getRoles().size());
							jointMove.set(roleIndices.get(role), move);
							for (Role playerRole : stateMachine.getRoles()) {
								if (!playerRole.equals(role)) {
									boolean moveFound = false;
									for (DumplingPropNetStateMachine otherFactor : factors) {
										List<Move> legalPlayerMoves = otherFactor.getLegalMoves(state, playerRole);
										if (!legalPlayerMoves.isEmpty()) {
											Move playerMove = legalPlayerMoves.get(0);
											jointMove.set(roleIndices.get(playerRole), playerMove);
											moveFound = true;
											break;
										}
									}
									if (!moveFound) {
										jointMove = null;
										break;
									}
								}
							}
							
							if (jointMove == null || jointMove.size() != stateMachine.getRoles().size()) {
								// ok...something is seriously wrong here
								System.out.println(role + ": absolutely could not build a legal joint move for dead-factor search! whaaaa...?");
							} else {
								MachineState nextState = stateMachine.getNextState(state, jointMove);
								
								currentFactor = factor;
								Integer testValue = maxScore(role, nextState, Integer.MIN_VALUE, Integer.MAX_VALUE, 2);	
								
								if (testValue != null && testValue == 0) {
									isDeathFound = true;
									deathFactor = factor; 
									break;
								}
							}
						}
					}
				}
				if (isDeathFound)
					break;
			}
			end = System.currentTimeMillis();
			System.out.println(role + ": looking for death state takes " + (end - start));
			
			start = System.currentTimeMillis();
			heuristic = tempHeuristic;
			if (deathFactor != null) {
				System.out.println(role + ": death factor found... only search in that factor.");
				searchFactor(deathFactor, state, role);				
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
			end = System.currentTimeMillis();
			System.out.println(role + ": real search takes " + (end - start));
		}
		
		private void searchFactor(DumplingPropNetStateMachine factor, MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			//The first move that results in an unknown state
			numStatesExpanded = 1;
						
			currentFactor = factor;
			//if (heuristic != null)
			//	heuristic.setStateMachine(factor);
			
			Result result = new Result(null, Integer.MIN_VALUE);
			List<Move> moves = currentFactor.getLegalMoves(state, role);
			Collections.shuffle(moves);
			for (Move move : moves) {
				if (stopExecution) {
					isSearchComplete = false;
					break;
				}
				if (heuristic != null)
					heuristic.cleanup();
				
				Integer testValue = minScore(role, move, state, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
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
			
			//System.out.println(role + ": factor searched " + result.value + " move " + result.move);
			bestResults.put(factor, result);	
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
			List<List<Move>> allJointMoves = currentFactor.getLegalJointMoves(state, role, move);
			if (allJointMoves.isEmpty()) {
				// build composite joint move with opponent moves from different factors
				//System.out.println(role + ": no joint moves, so build composite with opponent moves from other factors");
		        List<List<Move>> legals = new ArrayList<List<Move>>();
		        List<Role> missingRolesInCurrent = new ArrayList<Role>();
		        for (Role r : stateMachine.getRoles()) {
		            if (r.equals(role)) {
		                List<Move> m = new ArrayList<Move>();
		                m.add(move);
		                legals.add(m);
		            } else {
		            	List<Move> legalPlayerMoves = currentFactor.getLegalMoves(state, r);
		            	if (legalPlayerMoves.isEmpty()) {
		            		missingRolesInCurrent.add(r);
		            		legalPlayerMoves = null;
		            	}
		                legals.add(legalPlayerMoves);
		            }
		        }
		        allJointMoves = new ArrayList<List<Move>>();
		        // get joint moves, where some entries might be null
		        crossProductLegalMovesWithHoles(legals, allJointMoves, new LinkedList<Move>());
		        
		        // fill in empty joint move entries w/ moves from other factors (any moves)
				Map<Role, Integer> roleIndices = stateMachine.getRoleIndices();
		        for (Role playerRole : missingRolesInCurrent) {
		        	if (!playerRole.equals(role)) {
						Move legalMove = null;
						for (DumplingPropNetStateMachine otherFactor : factors) {
							if (otherFactor != currentFactor) {
								List<Move> legalPlayerMoves = otherFactor.getLegalMoves(state, playerRole);
								if (!legalPlayerMoves.isEmpty()) {
									legalMove = legalPlayerMoves.get(0);
									break;
								}
							}
						}
						if (legalMove == null) {
							System.out.println("...what??? no move in any factor for '" + playerRole + "'? something's fishy!");
							break;
						}
			        	for (List<Move> jointMove : allJointMoves) {
							int roleIndex = roleIndices.get(playerRole);
							if (jointMove.get(roleIndex) != null)
								System.out.println("...wait, expected " + playerRole + "'s move at index " + roleIndex + " in " + jointMove + " to be null... something's fishy!");
							else
								jointMove.set(roleIndex, legalMove);
			        	}
		        	}
		        }
			}
			
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
			List<Move> moves = currentFactor.getLegalMoves(state, role);
			if (moves.isEmpty()) {
				// hmmm...no moves for us in this factor in this state --
				// so play in another factor for now
				for (DumplingPropNetStateMachine factor : factors) {
					if (factor != currentFactor) {
						moves = factor.getLegalMoves(state, role);
						if (!moves.isEmpty())
							break;
					}
				}
			}
			if (depth > maxDepth) {
				// only apply heuristics when we've alpha-beta-ed as deep as we're going to go
				// so heuristics don't slow us down as we're IDS-ing
				//if (heuristic != null && isTimeout) {

				// this is as far as we go, so calculate heuristic and be done w/ it
				heuristicUsed = true;
				if (heuristic != null /*&& moves.size() > 1*/) {
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
								
				return heuristicUsed ? -bestValue : bestValue;
			}
		}
		
	    protected void crossProductLegalMovesWithHoles(List<List<Move>> legals, List<List<Move>> crossProduct, LinkedList<Move> partial)
	    {
	        if (partial.size() == legals.size()) {
	            crossProduct.add(new ArrayList<Move>(partial));
	        } else {
	        	if (legals.get(partial.size()) == null) {
	        		partial.add(null);
	                crossProductLegalMovesWithHoles(legals, crossProduct, partial);
	        	} else {
		            for (Move move : legals.get(partial.size())) {
		                partial.addLast(move);
		                crossProductLegalMovesWithHoles(legals, crossProduct, partial);
		                partial.removeLast();
		            }
	        	}
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
