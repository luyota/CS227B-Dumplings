package com.dumplings.strategies;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Timer;

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
	private boolean useCaching = true;
	private int numStatesExpanded;
	
	private Timer t = null;
	private ActionListener TimeoutHandler = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
    	  System.out.println("Timeout caught");
      }
	};
	  
	public AlphaBeta(StateMachine sm) {
		super(sm);
		maxStateScores = new HashMap<String, Integer>();
		minStateScores = new HashMap<String, Map<String, Integer>>();
		
	}
	
	public void enableCache(boolean flag) {
		useCaching = flag;
	}
	
	public Move getBestMove(MachineState state, Role role, long timeout) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException { 
		t = new Timer((int) (timeout - System.currentTimeMillis()), TimeoutHandler);
		t.setRepeats(false);
		//t.setDelay((int) (timeout - System.currentTimeMillis()));
		t.start();
		
		List<Move> moves = stateMachine.getLegalMoves(state, role);
		if (moves.size() == 1) {
			System.out.println("Expanded 1 state");
			return moves.get(0);
		}
		else {
			Move bestMove = null;
			int bestValue = Integer.MIN_VALUE;
			numStatesExpanded = 1;
			
			for (Move move : moves) {
				int value = minScore(role, move, state, Integer.MIN_VALUE, Integer.MAX_VALUE);
				if (value > bestValue) {
					bestValue = value;
					bestMove = move;
				}
			}
			System.out.println("Best: " + bestValue);
			System.out.println("Expanded " + numStatesExpanded + " states");
			return bestMove;
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
