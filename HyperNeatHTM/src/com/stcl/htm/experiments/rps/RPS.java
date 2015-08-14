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
import com.stcl.htm.experiments.rps.gui.GUI;
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
	protected GUI gui;
	
	public RPS(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			RewardFunction[] rewardFunctions, 
			int numExperimentsPerSequence,
			int trainingIterations,
			int evaluationIterations,
			long randSeed,
			double noiseMagnitude){
		this(possibleInputs, sequences, rewardFunctions, numExperimentsPerSequence, trainingIterations, evaluationIterations, randSeed, noiseMagnitude, null);
	}
	
	
	public RPS(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			RewardFunction[] rewardFunctions, 
			int numExperimentsPerSequence,
			int trainingIterations,
			int evaluationIterations,
			long randSeed,
			double noiseMagnitude,
			GUI gui){
		rand = new Random(randSeed);
		runner = new SequenceRunner(null, possibleInputs, rewardFunctions, rand, noiseMagnitude);
		this.numExperimentsPerSequence = numExperimentsPerSequence;
		this.trainingIterations = trainingIterations;
		this.evaluationIterations = evaluationIterations;
		sequenceScores = new double[sequences.length][2];
		this.sequences = sequences;
		this.gui = gui;

	}
	
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
				String name = sequenceID + " test " + sequenceIteration;
				//System.out.println("Starting on iteration " + sequenceIteration);
				runner.reset(false);
				brain.getNetwork().reinitialize();
				
				//Let it train
				brain.getNetwork().setUsePrediction(true);
				brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
				runExperiment(trainingIterations, brain, runner);
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0.0);
				brain.getNetwork().setLearning(false);
				brain.reset();
				runner.reset(false);
				double[] scores = runExperiment(evaluationIterations, brain, runner, gui,name);
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
	
	public double[] run(HTMNetwork brain) {
		return this.run(brain,0);
		
	}
	
	
	protected double[] runExperiment(int numSequences, HTMNetwork activator, SequenceRunner runner){
		return this.runExperiment(numSequences, activator, runner, null, null);
	}
	
	/**
	 * Evaluates the activator on the given number of sequences.
	 * Remember to reset the runner and set the sequence before running this method
	 * @param numEpisodes the number times the sequence is repeated
	 * @param activator
	 * @return the score given as [avgPredictionSuccess, avgFitness]
	 */
	protected double[] runExperiment(int numEpisodes, HTMNetwork activator, SequenceRunner runner, GUI gui, String name){
		double totalPrediction = 0;
		double totalFitness = 0;
		for(int i = 0; i < numEpisodes; i++){
			activator.getNetwork().newEpisode();
			if (gui != null) gui.setSequenceName(name + " iteration " + i);
			double[] result = runner.runEpisode(activator, gui);
			totalPrediction += result[0];
			totalFitness += result[1];
		}
		
		double avgPrediction = totalPrediction / (double) numEpisodes;
		double avgFitness = totalFitness / (double) numEpisodes;
		
		double[] result = {avgPrediction, avgFitness};
		return result;
	}
	
	public double[][] getSequenceScores(){
		return sequenceScores;
	}
}
