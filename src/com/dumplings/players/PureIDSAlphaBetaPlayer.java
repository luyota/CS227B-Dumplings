package com.dumplings.players;

import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import apps.player.detail.DetailPanel;

import com.dumplings.general.DumplingPropNetStateMachine;
import com.dumplings.general.PlayerStrategy;
import com.dumplings.strategies.IDSAlphaBeta;

/**
 * AlphaBetaPlayer plays by using alpha-beta-pruning
 */
public final class PureIDSAlphaBetaPlayer extends StateMachineGamer
{
	PlayerStrategy strategy;
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		DumplingPropNetStateMachine fsm = ((DumplingPropNetStateMachine)getStateMachine()).factorPropNet(getRole());
		strategy = new IDSAlphaBeta(fsm == null ? getStateMachine() : fsm);
	}
	
	/**
	 * Selects the best legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("Selecting move...");

		long start = System.currentTimeMillis();
		
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = strategy.getBestMove(getCurrentState(), getRole(), timeout);

		long stop = System.currentTimeMillis();
		
		System.out.println("Total time (ms): " + (stop - start));

		notifyObservers(new ReflexMoveSelectionEvent(moves, selection, stop - start));
		
		/* Let's have some sound effects! */
		/*
		if (strategy.currentBestValue == 0) {
			playSound("lose.wav");
		}
		else if (strategy.currentBestValue == 100) {
			playSound("win.wav");
		}
		*/
		
		System.out.println("Finishing move!");
		return selection;
	}
	
	@Override
	public void stateMachineStop() {
		// Do nothing.
	}

	/**
	 * Uses a ProverStateMachine
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		//return new ProverStateMachine();
		return new DumplingPropNetStateMachine();
	}
	@Override
	public String getName() {
		return "Pure IDS Alpha-Beta";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
	
	public static synchronized void playSound(final String url) {
	    new Thread(new Runnable() {
	      public void run() {
	        try {
	          Clip clip = AudioSystem.getClip();
	          AudioInputStream inputStream = AudioSystem.getAudioInputStream(PureIDSAlphaBetaPlayer.class.getResourceAsStream(url));
	          clip.open(inputStream);
	          clip.start(); 
	        } catch (Exception e) {
	          e.printStackTrace();
	        }
	      }
	    }).start();
	  }
}
