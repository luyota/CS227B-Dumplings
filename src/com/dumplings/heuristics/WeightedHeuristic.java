package com.dumplings.heuristics;

import java.util.ArrayList;
import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Role;

import com.dumplings.general.PlayerHeuristic;

public class WeightedHeuristic implements PlayerHeuristic {
	List<PlayerHeuristic> heuristics = new ArrayList<PlayerHeuristic>();
	List<Double> weights = new ArrayList<Double>();
	
	public void addHeuristic(PlayerHeuristic heuristic, double weight) {
		heuristics.add(heuristic);
		weights.add(weight);
	}
	
	public void removeHeuristic(PlayerHeuristic heuristic) {
		int index = heuristics.indexOf(heuristic);
		if (index != -1) {
			heuristics.remove(index);
			weights.remove(index);
		}
	}
	
	@Override
	public void onTimeout() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getScore(MachineState state, Role role) {
		int score = 0;
		
		int index = 0;
		for (PlayerHeuristic heuristic : heuristics) {
			score += weights.get(index) * heuristic.getScore(state, role);
		
			index++;
		}
		return score;
	}

}
