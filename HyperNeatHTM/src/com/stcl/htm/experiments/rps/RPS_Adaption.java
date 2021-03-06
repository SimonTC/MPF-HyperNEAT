package com.stcl.htm.experiments.rps;

import java.util.LinkedList;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class RPS_Adaption extends RPS {
	private int[][] sequences_changed;
	private boolean changeSequences;
	
	/**
	 * 
	 * @param possibleInputs
	 * @param sequences
	 * @param sequences_changed
	 * @param rewardFunctions
	 * @param numExperimentsPerSequence
	 * @param trainingIterations
	 * @param evaluationIterations
	 * @param randSeed
	 * @param noiseMagnitude
	 * @param changeSequences - If true adaption will be tested on sequence change. Otherwise on rule change
	 */
	public RPS_Adaption(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			int[][] sequences_changed,
			RewardFunction[] rewardFunctions, 
			int numExperimentsPerSequence, 
			int trainingIterations,
			int evaluationIterations,
			long randSeed,
			double noiseMagnitude,
			boolean changeSequences){
		super(possibleInputs, sequences, rewardFunctions, 
				numExperimentsPerSequence, trainingIterations,
				evaluationIterations, randSeed, noiseMagnitude);
		
		this.sequences_changed = sequences_changed;
		this.changeSequences = changeSequences;

	}
	
	@Override
	public double[] run(HTMNetwork brain, double explorationChance, boolean collectGameScores, String gameScoreFolder) {
		if (brain.getNetwork() == null){
			System.out.println("Brain is null. Running a random evaluation");
			return run_random(collectGameScores, gameScoreFolder);
		}
		
		double totalFitness = 0;
		double totalPrediction = 0;
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			String sequenceFileName = gameScoreFolder + "/seq" + sequenceID + "_";
			//System.out.println("Starting on sequence " + sequenceID);
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			int[] curSequence_changed = sequences_changed[sequenceID];
			
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				String sequenceIterationFileName = sequenceFileName + "itr" + sequenceIteration;
				//System.out.println("Starting on iteration " + sequenceIteration);
				runner.changeRules(curSequence, rewardFunctions[0]);
				brain.getNetwork().reinitialize();
				
				String roundFileName = sequenceIterationFileName + "_round1.csv";
				double[] firstScores = runOneRound(brain, explorationChance, sequenceIteration, collectGameScores, roundFileName);
				
				if (changeSequences){
					runner.changeRules(curSequence_changed, rewardFunctions[0]);
				} else {
					runner.changeRules(curSequence, rewardFunctions[1]);
				}
				
				roundFileName = sequenceIterationFileName + "_round2.csv";
				double[] secondScores = runOneRound(brain, explorationChance, sequenceIteration, collectGameScores, roundFileName);
				
				
				double fitness = (secondScores[1] + firstScores[1]) / 2.0;
				double prediction = (secondScores[0] + firstScores[0]) / 2.0;
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
	
	@Override
	public double[] run_random(boolean collectGameScores, String gameScoreFolder) {
		
		double totalFitness = 0;
		double totalPrediction = 0;
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			String sequenceFileName = gameScoreFolder + "/seq" + sequenceID + "_";
			//System.out.println("Starting on sequence " + sequenceID);
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			int[] curSequence_changed = sequences_changed[sequenceID];
			
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				String sequenceIterationFileName = sequenceFileName + "itr" + sequenceIteration;
				//System.out.println("Starting on iteration " + sequenceIteration);
				runner.changeRules(curSequence, rewardFunctions[0]);
				
				String roundFileName = sequenceIterationFileName + "_round1.csv";
				double[] firstScores = runOneRound_random(sequenceIteration, collectGameScores, roundFileName);
				
				if (changeSequences){
					runner.changeRules(curSequence_changed, rewardFunctions[0]);
				} else {
					runner.changeRules(curSequence, rewardFunctions[1]);
				}
				
				roundFileName = sequenceIterationFileName + "_round2.csv";
				double[] secondScores = runOneRound_random(sequenceIteration, collectGameScores, roundFileName);
				
				
				double fitness = (secondScores[1] + firstScores[1]) / 2.0;
				double prediction = (secondScores[0] + firstScores[0]) / 2.0;
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
		brain.getNetwork().setLearning(true);
		brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
		runGame(trainingIterations, brain, runner, true, roundFileName, collectGameScores);
		
		//Evaluate
		brain.getNetwork().getActionNode().setExplorationChance(0.0);
		brain.getNetwork().setLearning(false);

		double[] scores = runGame(evaluationIterations, brain, runner, false, roundFileName, collectGameScores);

		return scores;
	}
	
	private double[] runOneRound_random(int gameNumber, boolean collectGameScores, String roundFileName){
		//Let it train
		runGame_random(trainingIterations, runner, true, roundFileName, collectGameScores);
		
		//Evaluate
		double[] scores = runGame_random(evaluationIterations, runner, false, roundFileName, collectGameScores);

		return scores;
	}

}
