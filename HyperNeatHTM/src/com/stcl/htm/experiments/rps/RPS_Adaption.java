package com.stcl.htm.experiments.rps;

import java.util.LinkedList;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;

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
	public double[] run(HTMNetwork brain, double explorationChance) {
		double totalFitness = 0;
		double totalPrediction = 0;
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			//System.out.println("Starting on sequence " + sequenceID);
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);
			
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				//System.out.println("Starting on iteration " + sequenceIteration);
				runner.reset(false);
				brain.getNetwork().reinitialize();
				
				double[] firstScores = runOneRound(brain, explorationChance);
				
				runner.reset(true);
				
				double[] secondScores = runOneRound(brain, explorationChance);
				
				double fitness = secondScores[1] / firstScores[1];
				double prediction = secondScores[0] / firstScores[0];
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
	
	private double[] runOneRound(HTMNetwork brain, double explorationChance){
		//Let it train
		brain.getNetwork().setUsePrediction(true);
		brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
		runExperiment(trainingIterations, brain, runner);
		
		//Evaluate
		brain.getNetwork().getActionNode().setExplorationChance(0.0);
		brain.getNetwork().setLearning(false);

		double[] scores = runExperiment(evaluationIterations, brain, runner);

		return scores;
		
		
	}

}
