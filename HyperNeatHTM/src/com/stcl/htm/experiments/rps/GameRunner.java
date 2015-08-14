package com.stcl.htm.experiments.rps;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.network.HTMNetwork;

public class GameRunner {
	private double[][] gameScores;
	
	/**
	 * Evaluates the activator on the given number of sequences.
	 * Remember to reset the runner and set the sequence before running this method
	 * @param numEpisodes the number times the sequence is repeated
	 * @param activator
	 * @return the score given as [avgPredictionSuccess, avgFitness]
	 */
	public double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, GUI gui, String name){
		double totalPrediction = 0;
		double totalFitness = 0;
		gameScores = new double[numEpisodes][];
		for(int i = 0; i < numEpisodes; i++){
			activator.getNetwork().newEpisode();
			if (gui != null) gui.setSequenceName(name + " iteration " + i);
			double[] result = runner.runEpisode(activator, gui);
			totalPrediction += result[0];
			totalFitness += result[1];
			gameScores[i] = result;
		}
		
		double avgPrediction = totalPrediction / (double) numEpisodes;
		double avgFitness = totalFitness / (double) numEpisodes;
		
		double[] result = {avgPrediction, avgFitness};
		return result;
	}
	
	public double[][] getGameScores(){
		return gameScores;
	}
}
