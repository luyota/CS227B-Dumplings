package com.dumplings.utils;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;
import util.statemachine.Move;

public class Canonicalizer {
	public static String stateString(MachineState state) {
		Set<String> sortedStateContents = new TreeSet<String>();
		for (GdlSentence gdl : state.getContents())
			sortedStateContents.add(gdl.toString());
		return sortedStateContents.toString();
	}
	
	public static String stateStringAlphaBeta(MachineState state, int alpha, int beta) {
		return alpha + " " + beta + " " + stateString(state);
	}
	
	public static String stateStringAlphaBeta(String stateString, int alpha, int beta) {
		return alpha + " " + beta + " " + stateString;
	}
	
	public static String stateMovesString(MachineState state, List<Move> moves) {
		Set<String> moveStrings = new TreeSet<String>();
		for (Move move : moves) {
			moveStrings.add(move.toString());
		}
		return stateString(state) + moveStrings.toString();
	}
}
