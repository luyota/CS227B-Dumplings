package com.dumplings.heuristics;

import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.PlayerHeuristic;

public class OpponentFocus extends AbstractHeuristic implements PlayerHeuristic {
	private StateMachine stateMachine = null;
	
	public OpponentFocus(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	
	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<Move> moveList = stateMachine.getLegalMoves(state, role);
		int sum = 0;
		/*
		for (Move move : moveList) {
			sum += stateMachine.getLegalJointMoves(state, role, move).size();
		}			
		int score = Math.max(100 - sum / moveList.size(), 0);*/
		int score = Math.max(100 - stateMachine.getLegalJointMoves(state, role, moveList.get(0)).size(), 0);
		//System.out.println("OpponentFocus score: " + score);
		return score;
	}
}
