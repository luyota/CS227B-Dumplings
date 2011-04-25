package com.dumplings.heuristics;

import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;

import com.dumplings.general.PlayerHeuristic;

public class OpponentFocus implements PlayerHeuristic {
	private StateMachine stateMachine = null;
	public OpponentFocus(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	@Override
	public void onTimeout() {
	}
	
	@Override
	public int getScore(MachineState state, Role role) {
		try {
			List<Move> moveList = stateMachine.getLegalMoves(state, role);
			int sum = 0;
			for (Move move : moveList) {
				sum += stateMachine.getLegalJointMoves(state, role, move).size();
			}			
			int score = sum / moveList.size();
			System.out.println("OpponentFocus get score: " + score);
			return score;
		} catch (Exception e) {			
		}
		return 0;
	}
}
