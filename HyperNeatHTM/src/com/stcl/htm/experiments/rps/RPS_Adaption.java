package com.stcl.htm.experiments.rps;

import java.util.LinkedList;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class RPS_Adaption extends RPS {

	public RPS_Adaption(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			RewardFunction[] rewardFunctions, 
			int numExperimentsPerSequence, 
			int trainingIterations,
			int evaluationIterations,
			long randSeed,
			double noiseMagnitude){
		super(possibleInputs, sequences, rewardFunctions, 
				numExperimentsPerSequence, trainingIterations,
				evaluationIterations, randSeed, noiseMagnitude);

	}
	
	@Override
	public double[] run(HTMNetwork brain, double explorationChance, boolean collectGameScores, String gameScoreFolder) {
		double totalFitness = 0;
		double totalPrediction = 0;
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			String sequenceFileName = gameScoreFolder + "/seq" + sequenceID + "_";
			//System.out.println("Starting on sequence " + sequenceID);
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);
			
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				String sequenceIterationFileName = sequenceFileName + "itr" + sequenceIteration;
				//System.out.println("Starting on iteration " + sequenceIteration);
				runner.reset(false);
				brain.getNetwork().reinitialize();
				
				String roundFileName = sequenceIterationFileName + "_round1.csv";
				double[] firstScores = runOneRound(brain, explorationChance, sequenceIteration, collectGameScores, roundFileName);
				
				runner.reset(true);
				
				roundFileName = sequenceIterationFileName + "_round2.csv";
				double[] secondScores = runOneRound(brain, explorationChance, sequenceIteration, collectGameScores, roundFileName);
				
				
				double fitness = sigmoid(secondScores[1] / firstScores[1]);
				double prediction = sigmoid(secondScores[0] / firstScores[0]);
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
	
	private double sigmoid(double n){
		double sigmoid = 1.0/(1.0 + Math.exp(-n));
		return sigmoid;
	}
	
	private double[] runOneRound(HTMNetwork brain, double explorationChance, int gameNumber, boolean collectGameScores, String roundFileName){
		//Let it train
		brain.getNetwork().setUsePrediction(true);
		brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
		runGame(trainingIterations, brain, runner, true, roundFileName, collectGameScores);
		
		//Evaluate
		brain.getNetwork().getActionNode().setExplorationChance(0.0);
		brain.getNetwork().setLearning(false);

		double[] scores = runGame(evaluationIterations, brain, runner, false, roundFileName, collectGameScores);

		return scores;
	}

}
