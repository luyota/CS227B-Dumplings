package com.dumplings.players;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import player.gamer.Gamer;
import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.prover.aima.AimaProver;

/*
 * Legal Gamer plays the same move all the time.
 */
public final class LegalGamer extends Gamer {
	
	private AimaProver prover;
	private Set<GdlSentence> currentState;
	private List<GdlProposition> roles;

	@Override
	public String getName() {
		return "Legal Dumplings";
	}

	@Override
	public void metaGame(long timeout) throws MetaGamingException {
		prover = new AimaProver(new HashSet<Gdl>(getMatch().getGame().getRules()));
		GdlRelation initQuery = GdlPool.getRelation(GdlPool.getConstant("init"), new GdlTerm[] { GdlPool.getVariable("?x") });
		currentState = getState(prover.askAll(initQuery, new HashSet<GdlSentence>()));
		getMatch().appendState(currentState);
		roles = new ArrayList<GdlProposition>();
        for (Gdl gdl : getMatch().getGame().getRules()) {
            if (gdl instanceof GdlRelation) {
                GdlRelation relation = (GdlRelation) gdl;               
                if (relation.getName().getValue().equals("role")) {
                    roles.add((GdlProposition) relation.get(0).toSentence());
                }
            }
        }
	}
	
	private Set<GdlSentence> getState(Set<GdlSentence> results) {
		Set<GdlSentence> trues = new HashSet<GdlSentence>();
		for (GdlSentence result : results) {
			trues.add(GdlPool.getRelation(GdlPool.getConstant("true"), new GdlTerm[] { result.get(0) }));
		}
		return trues;
	}
	
	private Set<GdlSentence> getNextState(List<GdlSentence> moves) {
		GdlRelation nextQuery = GdlPool.getRelation(GdlPool.getConstant("next"), new GdlTerm[] { GdlPool.getVariable("?x") });
		return getState(prover.askAll(nextQuery, getContext(moves)));
	}
	
	private Set<GdlSentence> getContext(List<GdlSentence> moves) {
		Set<GdlSentence> context = new HashSet<GdlSentence>(currentState);
		for (int i = 0; i < roles.size(); i++) {
			GdlRelation action = GdlPool.getRelation(GdlPool.getConstant("does"), new GdlTerm[] { roles.get(i).toTerm(), moves.get(i).toTerm() });
			context.add(action);
		}
		return context;
	}

	@Override
	public GdlSentence selectMove(long timeout) throws MoveSelectionException {
		List<GdlSentence> lastMoves = getMatch().getMostRecentMoves();
		if (lastMoves != null) {
			currentState = getNextState(lastMoves);
			getMatch().appendState(currentState);
		}
		GdlRelation legalQuery = GdlPool.getRelation(GdlPool.getConstant("legal"), new GdlTerm[] { getRoleName().toTerm(), GdlPool.getVariable("?x")});
		Set<GdlSentence> legalMoves = prover.askAll(legalQuery, currentState);
		return legalMoves.iterator().next().get(1).toSentence();
	}

	@Override
	public void stop() {
		List<GdlSentence> lastMoves = getMatch().getMostRecentMoves();
		if (lastMoves != null) {
			currentState = getNextState(lastMoves);
			getMatch().appendState(currentState);
			List<Integer> goals = new ArrayList<Integer>();
			for (GdlProposition role : roles) {
				Set<GdlSentence> goalData = prover.askAll(GdlPool.getRelation(GdlPool.getConstant("goal"), new GdlTerm[] { role.toTerm(), GdlPool.getVariable("?x")}), getContext(lastMoves));
				GdlConstant constant = (GdlConstant) ((GdlRelation) goalData.iterator().next()).get(1);
				goals.add(Integer.parseInt(constant.toString()));
			}
			getMatch().markCompleted(goals);
		}
	}
}
