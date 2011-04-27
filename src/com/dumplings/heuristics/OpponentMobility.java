package com.dumplings.heuristics;

import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.PlayerHeuristic;

public class OpponentMobility implements PlayerHeuristic {
	private StateMachine stateMachine = null;
	
	public OpponentMobility(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	
	@Override
	public void onTimeout() {
	}
	
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<Move> moveList = stateMachine.getLegalMoves(state, role);
		int sum = 0;
		for (Move move : moveList) {
			sum += stateMachine.getLegalJointMoves(state, role, move).size();
		}			
		int score = Math.min(sum / moveList.size(), 100);
		
		System.out.println("OpponentMobiility score: " + score);
		return score;
		
	}
}
