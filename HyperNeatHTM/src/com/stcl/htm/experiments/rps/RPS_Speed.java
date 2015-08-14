package com.stcl.htm.experiments.rps;

import java.util.LinkedList;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;

import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class RPS_Speed extends RPS {
	
	private int averageOver;
	private double predictionThreshold;
	private double fitnessThreshold;

	public RPS_Speed(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			RewardFunction[] rewardFunctions, 
			int numExperimentsPerSequence, 
			int trainingIterations,
			int evaluationIterations,
			long randSeed,
			double noiseMagnitude,
			double predictionThreshold,
			double fitnessThreshold,
			int averageOver) {
		super(possibleInputs, sequences, rewardFunctions, 
				numExperimentsPerSequence, trainingIterations,
				evaluationIterations, randSeed, noiseMagnitude);
		this.predictionThreshold = predictionThreshold;
		this.fitnessThreshold = fitnessThreshold;
		this.averageOver = averageOver;
	}
	
	@Override
	public double[] run(HTMNetwork brain, double explorationChance) {
		double totalFitness = 0;
		double totalPrediction = 0;
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);

			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				runner.reset(false);
				brain.getNetwork().reinitialize();
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
				brain.getNetwork().setLearning(true);
				brain.reset();

				double[] scores = runGame(trainingIterations + evaluationIterations, brain, runner, false); 
				double fitness = scores[1];
				double prediction = scores[0];
				sequenceFitness += fitness;
				sequencePrediction += prediction;
			}
			double avgSequenceFitness = (sequenceFitness / (double)numExperimentsPerSequence);
			double avgSequencePrediction = (sequencePrediction / (double)numExperimentsPerSequence);
			totalFitness += avgSequenceFitness;
			totalPrediction += avgSequencePrediction;
			sequenceScores[sequenceID][0] = avgSequencePrediction;
			sequenceScores[sequenceID][1] = avgSequenceFitness;
		}
		double avgFitness = totalFitness / (double)sequences.length;
		double avgPrediction = totalPrediction / (double)sequences.length;
		double[] result = {avgPrediction, avgFitness};
		
		return result;
		
	}
	
	/**
	 * Evaluates the activator on the given number of sequences.
	 * Remember to reset the runner and set the sequence before running this method
	 * @param numEpisodes the number times the sequence is repeated
	 * @param activator
	 * @return the score given as [avgPredictionSuccess, avgFitness]
	 */
	@Override
	protected double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, boolean training){
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
		
		if (training){
			gameScores_Sequence = gameScores;
		} else {
			gameScores_evaluation = gameScores;
		}
		
		return result;
	}

}
