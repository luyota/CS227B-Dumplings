package com.dumplings.strategies;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

public class MonteCarloMiniMax extends PlayerStrategy {
	public Map<String, Integer> maxStateScores;
	private Map<String, Map<String, Integer>> minStateScores;
		
	@Override
	public void cleanup() {
		if (maxStateScores != null)
			maxStateScores.clear();
		if (minStateScores != null)
			minStateScores.clear();
		super.cleanup();
	}
	
	private int numStatesExpanded;
	private int historyLength;
	
	private MonteCarloMiniMaxComputer mcmmc = null;
	
	private Timer timer;
	
	public MonteCarloMiniMax(StateMachine sm, Integer hl) {
		super(sm);
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
		historyLength = hl;
	}
	
	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		mcmmc = new MonteCarloMiniMaxComputer(state, role);
		mcmmc.start();
		
		try {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					System.out.println("Timed out!");
					mcmmc.onTimeout(); // signal calculation thread to stop ASAP					
				}		
			}, Math.max((timeout - System.currentTimeMillis() - 500), 0));
			mcmmc.join();
			timer.cancel(); // stop timer in case it's still going
		} catch (InterruptedException e) {}
		
		return null;
	}
	
	
	private class MonteCarloMiniMaxComputer extends Thread implements TimeoutHandler {
		private MachineState state;
		private Role role;
		private boolean stopExecution = false;
		private Random generator = new Random();
		private LinkedList<MachineState> stateHistory;
		
		public MonteCarloMiniMaxComputer(MachineState state, Role role) {
			this.state = state;
			this.role = role;
			this.stateHistory = new LinkedList<MachineState>();
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
		
		public Move getBestMove(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			List<List<Move>> allMoves = stateMachine.getLegalJointMoves(state);
			MachineState currentState;
			int depth;
			while (!stopExecution) {
				stateHistory.clear();
				for (List<Move> moves : allMoves) {
					if (stopExecution)
						break;
					
					currentState = stateMachine.getNextState(state, moves);
					depth = 1;
					while (!stateMachine.isTerminal(currentState)) {
						if (stopExecution)
							break;
						
						// manage history queue
						if (stateHistory.size() >= historyLength)
							stateHistory.poll();
						stateHistory.offer(currentState);
						
						List<Move> randomMoves = allMoves.get(generator.nextInt(allMoves.size()));
						currentState = stateMachine.getNextState(currentState, randomMoves);
						depth++;
					}
					if (stateMachine.isTerminal(currentState)) {
						System.out.println("Stepping back " + historyLength + " moves from goal at depth " + depth);
						MachineState fakeRoot = stateHistory.peek();
						
						List<Move> miniMaxMoves = stateMachine.getLegalMoves(fakeRoot, role);
						for (Move move : miniMaxMoves) {
							if (stopExecution)
								break;
							minScore(role, move, fakeRoot);
						}
					}
				}
				historyLength++;
			}
			System.out.println("Cache has " + maxStateScores.size() + " entries");
			return null;
		}
		
		private int minScore(Role role, Move move, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			String stateString = canonicalizeStateString(state);
			String moveString = move.toString();
			Map<String, Integer> stateMoveScores = minStateScores.get(stateString);
			
			if (stateMoveScores != null && stateMoveScores.get(moveString) != null)
				return stateMoveScores.get(moveString);
			
			List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
			int worstScore = Integer.MAX_VALUE;
			for (List<Move> jointMove : allJointMoves) {
				if (stopExecution)
					break;
				MachineState newState = stateMachine.getNextState(state, jointMove);
				int newScore = maxScore(role, newState);
				if (newScore < worstScore)
					worstScore = newScore;
			}
			
			if (stateMoveScores == null) {
				minStateScores.put(stateString, (stateMoveScores = new HashMap<String, Integer>()));
			}
			
			stateMoveScores.put(moveString, worstScore);
			return worstScore;
		}
		
		private int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
			if (stateMachine.isTerminal(state)) {			
				numStatesExpanded++;
				return stateMachine.getGoal(state, role);
			}
			
			String stateString = canonicalizeStateString(state);
			
			if (maxStateScores.get(stateString) != null) 			
				return maxStateScores.get(stateString);
			
			numStatesExpanded++;
			int bestValue = Integer.MIN_VALUE;
			for (Move move : stateMachine.getLegalMoves(state, role)) {
				if (stopExecution)
					break;
				int value = minScore(role, move, state);
				if (value > bestValue)
					bestValue = value;
			}
			maxStateScores.put(stateString, bestValue);
			return bestValue;
		}
		
		private String canonicalizeStateString(MachineState state) {
			Set<String> sortedStateContents = new TreeSet<String>();
			for (GdlSentence gdl : state.getContents())
				sortedStateContents.add(gdl.toString());
			return sortedStateContents.toString();
		}

		@Override
		public void onTimeout() {
			stopExecution = true;
		}
	}

	@Override
	public void enableCache(boolean flag) {
		// cache is always enabled -- THAT'S THE POINT!
	}
}
