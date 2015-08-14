package com.stcl.htm.experiments.rps;

import java.util.LinkedList;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.network.HTMNetwork;

public class GameRunner_Speed extends GameRunner {
	
	private int averageOver;
	private double predictionThreshold, fitnessThreshold;
	
	public GameRunner_Speed(int averageOver, double predictionThreshold, double fitnessThreshold){
		this.averageOver = averageOver;
		this.predictionThreshold = predictionThreshold;
		this.fitnessThreshold = fitnessThreshold;
	}
	
	@Override
	public double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, GUI gui, String name){
		int firstPredictionHit = -1;
		int firstFitnessHit = -1;
		boolean cont = true;
		int counter = 0;
		LinkedList<Double> fitnessList = new LinkedList<Double>();
		LinkedList<Double> predictionList = new LinkedList<Double>();
		double totalPrediction = 0;
		double totalFitness = 0;
		double[][] gameScores = new double[numEpisodes][];

		do{
			activator.getNetwork().newEpisode();
			double[] result = runner.runSequence(activator);
			gameScores[counter] = result;
			double prediction = result[0];
			double fitness = result[1];
			totalFitness += fitness;
			totalPrediction += prediction;
			fitnessList.addLast(fitness);
			predictionList.addLast(prediction);
			if (fitnessList.size() > averageOver) totalFitness -= fitnessList.removeFirst();
			if (predictionList.size() > averageOver) totalPrediction -= predictionList.removeFirst();
			double avgFitness = totalFitness / (double) averageOver;
			double avgPrediction= totalPrediction/ (double) averageOver;
			boolean predictionGood, fitnessGood;
			
			if (avgPrediction >= predictionThreshold){
				predictionGood = true;
				if (firstPredictionHit == -1) firstPredictionHit = counter;
			}else{
				predictionGood = false;
				firstPredictionHit = -1;
			}
			
			if (avgFitness >= fitnessThreshold){
				fitnessGood = true;
				if (firstFitnessHit == -1) firstFitnessHit = counter;
			} else {
				fitnessGood = false;
				firstFitnessHit = -1;
			}
			
			if (predictionGood && fitnessGood){
				cont = false;
			}
			
			counter++;
		} while (counter < numEpisodes && cont);
		
		double timeToPrediction = firstPredictionHit == -1? 1 : firstPredictionHit / (double) numEpisodes;
		double timeToFitness = firstFitnessHit == -1? 1 : firstFitnessHit / (double) numEpisodes;
		
		
		double predictionScore = 1 - timeToPrediction;
		double fitnessScore = 1 - timeToFitness;
		if(predictionScore < 0) predictionScore = 0;
		if(fitnessScore < 0) fitnessScore = 0;
		
		double[] result = {predictionScore, fitnessScore};

		
		return result;
	}

}
