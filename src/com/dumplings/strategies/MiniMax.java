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

public class MiniMax extends PlayerStrategy {
	public Map<String, Integer> maxStateScores;
	Map<String, Map<String, Integer>> minStateScores;
	private MiniMaxComputer mm;
	private boolean useCaching = true;
	private int numStatesExpanded;
	private Timer timer;

	public MiniMax(StateMachine sm) {
		super(sm);
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
	}

	public void enableCache(boolean flag) {
		useCaching = flag;
	}

	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		// Call the thread that does the computation
		 mm = new MiniMaxComputer(state, role);
		
		 mm.start();
		
		// And go to sleep, but not longer than the timeout
		try {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					System.out.println("Timed out!");
					mm.onTimeout(); // signal calculation thread to stop ASAP
				}		
			}, Math.max(timeout - System.currentTimeMillis() - 500, 0));
			mm.join(); // wait until calculation thread finishes
			timer.cancel(); // stop timer in case it's still going
		} catch (InterruptedException e) {}
		
		// Make sure bestMove is not null
		Move bestMove = mm.getBestMove();
		if (bestMove == null) {
			System.out.println("Didn't decide on any move. Playing first legal move.");
			bestMove = stateMachine.getLegalMoves(state, role).get(0);
		}
		
		return bestMove;
		
	}
	private class MiniMaxComputer extends Thread implements TimeoutHandler {
		private MachineState state;
		private Role role;
		private Move bestMove;
		private Integer bestValue;
		private boolean stopExecution = false;		

		@Override
		public void onTimeout() {
			stopExecution = true;
		}
		public MiniMaxComputer(MachineState state, Role role) {
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
			System.out.println("Getting best move...");
			List<Move> moves = stateMachine.getLegalMoves(state, role);
			if (moves.size() == 1) {
				System.out.println("Expanded 1 state");
				bestMove = moves.get(0);				
			} else {
				bestMove = null;
				bestValue = Integer.MIN_VALUE;
				numStatesExpanded = 1;
				for (Move move : moves) {
					if (stopExecution) {
						break;
					}
					int value = minScore(role, move, state);
					if (value > bestValue) {
						bestValue = value;
						bestMove = move;
					}
				}
			}
		}

		private int minScore(Role role, Move move, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
			String stateString = canonicalizeStateString(state);
			String moveString = move.toString();
			Map<String, Integer> stateMoveScores = minStateScores.get(stateString);

			if (useCaching && stateMoveScores != null && stateMoveScores.get(moveString) != null)
				return stateMoveScores.get(moveString);

			List<List<Move>> allJointMoves = stateMachine.getLegalJointMoves(state, role, move);
			int worstScore = Integer.MAX_VALUE;
			for (List<Move> jointMove : allJointMoves) {
				if (stopExecution) {
					break;
				}
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

			if (useCaching && maxStateScores.get(stateString) != null) 			
				return maxStateScores.get(stateString);

			numStatesExpanded++;
			int bestValue = Integer.MIN_VALUE;
			for (Move move : stateMachine.getLegalMoves(state, role)) {
				if (stopExecution) {
					break;
				}
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
	}
}
