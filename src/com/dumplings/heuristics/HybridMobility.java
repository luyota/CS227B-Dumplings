package com.dumplings.heuristics;

import java.util.List;
import java.util.Random;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.AbstractHeuristic;
import com.dumplings.general.PlayerHeuristic;

public class HybridMobility extends AbstractHeuristic implements PlayerHeuristic {
	private StateMachine stateMachine = null;
	private List<Role> roles;
	private Random generator;
	private MachineState currentState;
	
	public HybridMobility(StateMachine stateMachine) {
		this.stateMachine = stateMachine;
		this.roles = stateMachine.getRoles();
		this.generator = new Random();
	}

	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		// Code review note by luyota: might have problems when returning goal value, especially 0. 
		// The client code uses the sign of the number to evaluate whether it's a heuristic value, and 0 doesn't have sign.
		// Our moves:
		currentState = state;
		List<Move> moves = getMoves(role);
		
		if (stateMachine.isTerminal(currentState)) { // oh, hey, this is actually a goal state. yippee!
			//System.out.println("Found a goal state while stepping ahead");
			return stateMachine.getGoal(currentState, role);
		}
		else {
			// Opponent moves:
			double avgOpponentMoves = 0;
			for (Role gameRole : this.roles) {
				if (this.stopExecution)
					return null;
				if (!role.equals(gameRole)) {
					List<Move> opponentMoves = getMoves(gameRole);
					if (opponentMoves != null)
						avgOpponentMoves += opponentMoves.size();
					else {
						//System.out.println("Found a goal state while stepping ahead");
						return stateMachine.getGoal(currentState, role);
						
					}
				}
			}
			if (avgOpponentMoves > 0)
				avgOpponentMoves /= (double)(this.roles.size() - 1);
			// Scoring our moves vs. opponent's moves
			double movesRatio = (moves.size() - avgOpponentMoves) / (moves.size() + avgOpponentMoves); // -1 => opponent(s) more mobile, 0 => balanced moves, 1 => self more mobile
			int score = (int)Math.round(98 * ((movesRatio + 1) / 2)) + 1; // normalize between 1 - 99
			//System.out.println(moves.size() + " vs. " + avgOpponentMoves + ", score: " + score);
			return score;
		}
	}
	
	private List<Move> getMoves(Role role) throws MoveDefinitionException, TransitionDefinitionException {
		List<Move> moves = stateMachine.getLegalMoves(currentState, role);
		while (moves.size() == 1 && !stateMachine.isTerminal(currentState)) { // if we don't have options, search randomly until we do
			if (this.stopExecution)
				return null;
			//System.out.println("Only have 1 move. Stepping forward for more choices for '" + role + "'...");
			List<List<Move>> allMoves = stateMachine.getLegalJointMoves(currentState, role, moves.get(0));
			currentState = stateMachine.getNextState(currentState, allMoves.get(generator.nextInt(allMoves.size())));
			if (stateMachine.isTerminal(currentState)) // oh, hey, this is actually a goal state. yippee!
				return null;
			moves = stateMachine.getLegalMoves(currentState, role);
		}
		return moves;
	}
	
	@Override
	public String toString() {
		return "HybridMobility";
	}
}
