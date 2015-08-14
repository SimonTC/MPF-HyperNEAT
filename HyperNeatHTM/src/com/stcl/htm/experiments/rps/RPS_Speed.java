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
	public double[] run(HTMNetwork brain, double explorationChance, boolean collectGameScores, String gameScoreFolder) {
		double totalFitness = 0;
		double totalPrediction = 0;
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			String sequenceFileName = gameScoreFolder + "/seq" + sequenceID + "_";
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);

			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				String sequenceIterationFileName = sequenceFileName + "itr" + sequenceIteration + ".csv";
				runner.reset(false);
				brain.getNetwork().reinitialize();
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
				brain.getNetwork().setLearning(true);
				brain.reset();

				double[] scores = runGame(trainingIterations + evaluationIterations, brain, runner, true, sequenceIterationFileName, false); 
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
	protected double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, boolean training, String gameScoreFile, boolean collectGameScores){
		GameRunner gr = new GameRunner();
		double[] result = gr.runGame(numEpisodes, activator, runner, null, null);
		if (training){
			if (collectGameScores) gr.writeGameScoresToFile(gameScoreFile);
		}
		
		return result;
	}

}
