package com.stcl.htm.experiments.rps;

import java.io.File;
import java.util.Random;

import javax.swing.text.Position;

import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;
import stcl.algo.brain.Network_DataCollector;
import stcl.algo.brain.nodes.UnitNode;

import com.anji.integration.Activator;
import com.ojcoleman.bain.misc.PerformanceTest;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class RPS {
	
	protected int[][] sequences;
	protected Random rand;
	protected int numExperimentsPerSequence;
	protected int trainingIterations;
	protected int evaluationIterations;
	protected double[][] sequenceScores;
	
	protected SequenceRunner runner;
	
	
	public RPS(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			RewardFunction[] rewardFunctions, 
			int numExperimentsPerSequence,
			int trainingIterations,
			int evaluationIterations){
		rand = null; // new Random(randSeed);
		runner = new SequenceRunner(null, possibleInputs, rewardFunctions, rand);
		this.numExperimentsPerSequence = numExperimentsPerSequence;
		this.trainingIterations = trainingIterations;
		this.evaluationIterations = evaluationIterations;
		sequenceScores = new double[sequences.length][2];
		this.sequences = sequences;

	}
	
	public double[] run(HTMNetwork brain) {
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
				brain.getNetwork().setUseExternalReward(true);
				
				//Let it train
				brain.getNetwork().setUsePrediction(true);
				brain.getNetwork().getActionNode().setExplorationChance(0.0);
				runExperiment(trainingIterations, brain, runner);
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0.0);
				brain.getNetwork().setLearning(true);
				brain.reset();
				runner.reset(false);
				double[] scores = runExperiment(evaluationIterations, brain, runner);
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
	
	public double[][] getSequenceScores(){
		return sequenceScores;
	}
}
