package com.dumplings.general;

import java.util.ArrayList;
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

import com.dumplings.strategies.AlphaBeta;

public abstract class PlayerStrategy {
	protected StateMachine stateMachine;
	protected AbstractHeuristic heuristic;
	public MetaGamer metaGamer = new MetaGamer();
	protected Map<String, Integer> externalCache;
	
	public PlayerStrategy(StateMachine sm) {
		this.stateMachine = sm;
	}
	
	public abstract void enableCache(boolean flag);
	
	public abstract Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException;
	
	public void setHeuristic(AbstractHeuristic heuristic) {
		this.heuristic = heuristic;
	}
	
	/*
	 * Used during start-clock
	 */
	public class MetaGamer {
		private Map<AbstractHeuristic, List<Integer>> scoreMap = new HashMap<AbstractHeuristic, List<Integer>>();
		private Map<AbstractHeuristic, Double> weightMap = new HashMap<AbstractHeuristic, Double>();
		private boolean stopExecution = false;
		
		public Map<AbstractHeuristic, Double> evaluateHeuristics(List<AbstractHeuristic> heuristics, Role role, long timeout) {
			/* Set timer to make sure we don't time out during the clock */
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					weightMap.clear();
					
					// OK, time to stop playing and calculate the weights
					// First, calculate the average scores
					double totalAvgScore = 0;
					Map<AbstractHeuristic, Double> avgScoreMap = new HashMap<AbstractHeuristic, Double>();
					for (AbstractHeuristic heuristic : scoreMap.keySet()) {
						// Calculate average score for this heuristic
						List<Integer> heuristicScores = scoreMap.get(heuristic); double avg = 0;
						for (Integer i : heuristicScores) {
							avg += i;
						}
						avg /= heuristicScores.size();
						avgScoreMap.put(heuristic, avg);
						
						totalAvgScore += avg;
						
						System.out.println("Played " + heuristicScores.size() + " games with "
									+ heuristic.toString());
					}
					
					// Now calculate the weights
					for (AbstractHeuristic heuristic : avgScoreMap.keySet()) {
						double weight = avgScoreMap.get(heuristic) / totalAvgScore;
						weightMap.put(heuristic, weight);
					}
					stopExecution = true;
				}		
			}, Math.max(timeout - System.currentTimeMillis() - 2000, 0));
			
			// Initialize the score map that will give rise to the weights
			for (AbstractHeuristic heuristic : heuristics) {
				scoreMap.put(heuristic, new ArrayList<Integer>());
			}
			
			int ourRoleIndex = stateMachine.getRoleIndices().get(role);
			try {
				while (!stopExecution) {
					for (AbstractHeuristic heuristic : heuristics) {
						if (stopExecution)
							break;
						
						// We are essentially playing AlphaBeta with max depth 0
						PlayerStrategy ourPlayer = new AlphaBeta(stateMachine, 0);
						ourPlayer.enableCache(false);
						ourPlayer.setHeuristic(heuristic);
						
						// Pick moves for all players
						MachineState currentState = stateMachine.getInitialState();
						while (!stopExecution && !stateMachine.isTerminal(currentState)) {
							List<Move> moves = new ArrayList<Move>();				
							for (Role playerRole : stateMachine.getRoles()) {
								if (stopExecution)
									break;
								
								if (playerRole.equals(role)) {
									// Let's use our heuristic player
									moves.add(ourRoleIndex, ourPlayer.getBestMove(currentState, role, Long.MAX_VALUE));
								}
								else {
									// Everyone else is a random player
									List<Move> legalMoves = stateMachine.getLegalMoves(currentState, playerRole);
									Random generator = new Random();
									moves.add(legalMoves.get(generator.nextInt(legalMoves.size())));
								}
							}
							if (stopExecution)
								break;
							
							// Advance to next state
							currentState = stateMachine.getNextState(currentState, moves);	
						}
						
						// Store score of this match
						if (stateMachine.isTerminal(currentState)) {
							int score = stateMachine.getGoal(currentState, role);
							scoreMap.get(heuristic).add(score);
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return weightMap;
		}
	}
	
	public void setExternalCache(Map<String, Integer> cache) {
		this.externalCache = cache;
	}
}
