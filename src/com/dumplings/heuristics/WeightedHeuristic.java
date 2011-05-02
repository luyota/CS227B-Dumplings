package com.dumplings.heuristics;

import java.util.HashMap;
import java.util.Map;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import com.dumplings.general.AbstractHeuristic;

public class WeightedHeuristic extends AbstractHeuristic {
	Map<AbstractHeuristic, Double> weightMap = new HashMap<AbstractHeuristic, Double>();
	
	public WeightedHeuristic(Map<AbstractHeuristic, Double> weightMap) {
		this.weightMap = weightMap;
	}
	
	public void addHeuristic(AbstractHeuristic heuristic, double weight) {
		weightMap.put(heuristic, weight);
	}
	
	public void removeHeuristic(AbstractHeuristic heuristic) {
		weightMap.remove(heuristic);
	}

	@Override
	public Integer getScore(MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int score = 0;
		
		for (AbstractHeuristic heuristic : weightMap.keySet()) {
			if (stopExecution)
				break;
			score += weightMap.get(heuristic) * heuristic.getScore(state, role);
		}
		
		return score;
	}

	@Override
	public void onTimeout() {
		stopExecution = true;
		for (AbstractHeuristic heuristic : weightMap.keySet()) {
			heuristic.onTimeout();
		}
	}
}
