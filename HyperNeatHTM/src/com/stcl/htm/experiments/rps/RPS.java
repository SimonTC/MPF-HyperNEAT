package com.stcl.htm.experiments.rps;

import java.util.Random;

import javax.swing.text.Position;

import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;

import com.anji.integration.Activator;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class RPS {
	
	protected SimpleMatrix[] possibleInputs;
	protected int[][] sequences;
	protected long randSeed;
	protected Random rand;
	protected boolean training;
	protected int numExperimentsPerSequence;
	protected int trainingIterations;
	protected int evaluationIterations;
	
	private SequenceRunner runner;
	
	
	public RPS(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			RewardFunction rewardFunction, 
			long randSeed,
			int numExperimentsPerSequence,
			int trainingIterations,
			int evaluationIterations){
		
		runner = new SequenceRunner(null, possibleInputs, rewardFunction, rand);
		this.numExperimentsPerSequence = numExperimentsPerSequence;
		this.trainingIterations = trainingIterations;
		this.evaluationIterations = evaluationIterations;
		this.training = true;
		this.randSeed = randSeed;

	}
	
	public double[] evaluate(HTMNetwork brain) {
		rand = new Random(randSeed);
		String initializationString = brain.toString();
		double totalFitness = 0;
		double totalPrediction = 0;
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);
			runner.reset();
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				Network network = new Network();
				network.initialize(initializationString, rand);
				brain.setNetwork(network);
				
				//Let it train
				training = true;
				brain.getNetwork().setUsePrediction(true);
				runExperiment(trainingIterations, brain, runner);
				training = false;
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0);
				brain.getNetwork().setLearning(false);
				brain.reset();
				runner.reset();
				double[] scores = runExperiment(evaluationIterations, brain, runner);
				double fitness = scores[1];
				double prediction = scores[0];
				sequenceFitness += fitness;
				sequencePrediction += prediction;
			}
			totalFitness += (sequenceFitness / (double)numExperimentsPerSequence);
			totalPrediction += (sequencePrediction / (double)numExperimentsPerSequence);
		}
		double avgFitness = totalFitness / (double)sequences.length;
		double avgPrediction = totalPrediction / (double)sequences.length;
		double[] result = {avgPrediction, avgFitness};
		
		return result;
		
	}
	
	/**
	 * Evaluates the activator on the given number of sequences.
	 * Remember to reset the runner and set the sequence before running this method
	 * @param numSequences
	 * @param activator
	 * @return the score given as [avgPredictionSuccess, avgFitness]
	 */
	protected double[] runExperiment(int numSequences, HTMNetwork activator, SequenceRunner runner){
		double totalPrediction = 0;
		double totalFitness = 0;
		for(int i = 0; i < numSequences; i++){
			activator.getNetwork().newEpisode();
			double[] result = runner.runSequence(activator);
			totalPrediction += result[0];
			totalFitness += result[1];
		}
		
		double avgPrediction = totalPrediction / (double) numSequences;
		double avgFitness = totalFitness / (double) numSequences;
		
		double[] result = {avgPrediction, avgFitness};
		return result;
	}
}
