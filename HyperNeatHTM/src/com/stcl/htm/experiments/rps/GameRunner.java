package com.stcl.htm.experiments.rps;

import java.io.IOException;

import stcl.algo.util.FileWriter;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.network.HTMNetwork;

public class GameRunner {
	protected double[][] gameScores;
	
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
	
	public double[] runGame_random(int numEpisodes, SequenceRunner runner){
		double totalPrediction = 0;
		double totalFitness = 0;
		gameScores = new double[numEpisodes][];
		for(int i = 0; i < numEpisodes; i++){
			double[] result = runner.runEpisode_random();
			totalPrediction += result[0];
			totalFitness += result[1];
			gameScores[i] = result;
		}
		
		double avgPrediction = totalPrediction / (double) numEpisodes;
		double avgFitness = totalFitness / (double) numEpisodes;
		
		double[] result = {avgPrediction, avgFitness};
		return result;
	}
	
	public void writeGameScoresToFile(String filename){
		try {
			FileWriter writer = new FileWriter(filename);
			writer.openFile(false);
			String headers = "Episode,prediction,fitness";
			writer.writeLine(headers);
			for (int episode = 0; episode < gameScores.length; episode++){
				double prediction = gameScores[episode][0];
				double fitness = gameScores[episode][1];
				writer.writeLine(episode + "," + prediction + "," + fitness);
			}
			writer.closeFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double[][] getGameScores(){
		return gameScores;
	}
}
