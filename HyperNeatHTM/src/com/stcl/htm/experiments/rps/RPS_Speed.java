package com.stcl.htm.experiments.rps;

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
			RewardFunction rewardFunction, 
			long randSeed,
			int numExperimentsPerSequence, 
			int trainingIterations,
			int evaluationIterations,
			double predictionThreshold,
			double fitnessThreshold,
			int averageOver) {
		super(possibleInputs, sequences, rewardFunction, randSeed,
				numExperimentsPerSequence, trainingIterations,
				evaluationIterations);
		this.predictionThreshold = predictionThreshold;
		this.fitnessThreshold = fitnessThreshold;
		this.averageOver = averageOver;
	}
	
	@Override
	public double[] run(HTMNetwork brain) {
		double totalFitness = 0;
		double totalPrediction = 0;
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);
			runner.reset();
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				runner.reset();
				brain.getNetwork().reinitialize();
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0.0);
				brain.getNetwork().setLearning(true);
				brain.reset();
				runner.reset();
				double[] scores = runExperiment(trainingIterations, brain, runner); //TODO: How many iterations?
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
	 * @param numSequences the number times the sequence is repeated
	 * @param activator
	 * @return the score given as [avgPredictionSuccess, avgFitness]
	 */
	@Override
	protected double[] runExperiment(int numSequences, HTMNetwork activator, SequenceRunner runner){
		int numPredictionHits = 0;
		int numFitnessHits = 0;
		int firstPredictionHit = -1;
		int firstFitnessHit = -1;
		boolean cont = true;
		int counter = 0;

		do{
			activator.getNetwork().newEpisode();
			double[] result = runner.runSequence(activator);
			double prediction = result[0];
			double fitness = result[1];
			
			if (prediction >= predictionThreshold){
				numPredictionHits++;
				if (firstPredictionHit == -1) firstPredictionHit = counter;
			}else{
				numPredictionHits = 0;
				firstPredictionHit = -1;
			}
			
			if (fitness >= fitnessThreshold){
				numFitnessHits++;
				if (firstFitnessHit == -1) firstFitnessHit = counter;
			} else {
				numFitnessHits = 0;
				firstFitnessHit = -1;
			}
			
			if ( numFitnessHits >= averageOver){
				cont = false;
			}
			
			counter++;
		} while (counter < numSequences && cont);
		
		double timeToPrediction = firstPredictionHit == -1? 1 : firstPredictionHit / (double) numSequences;
		double timeToFitness = firstFitnessHit == -1? 1 : firstFitnessHit / (double) numSequences;
		
		
		double predictionScore = 1 - timeToPrediction;
		double fitnessScore = 1 - timeToFitness;
		if(predictionScore < 0) predictionScore = 0;
		if(fitnessScore < 0) fitnessScore = 0;
		
		double[] result = {predictionScore, fitnessScore};
		return result;
	}

}
