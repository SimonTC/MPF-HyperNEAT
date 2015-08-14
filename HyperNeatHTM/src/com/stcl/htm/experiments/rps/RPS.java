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
	protected double[][][] gameScores_Sequence;
	protected double[][][][] gamescores_experiment;

	
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
	
	public double[] run(HTMNetwork brain, double explorationChance, boolean collectGameScores) {
		double totalFitness = 0;
		double totalPrediction = 0;
		if (collectGameScores) gamescores_experiment = new double[sequences.length][][][];
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			//System.out.println("Starting on sequence " + sequenceID);
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);
			if (collectGameScores) gameScores_Sequence = new double[numExperimentsPerSequence][][];
			
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				String name = sequenceID + " test " + sequenceIteration;
				//System.out.println("Starting on iteration " + sequenceIteration);
				runner.reset(false);
				brain.getNetwork().reinitialize();
				
				//Let it train
				brain.getNetwork().setUsePrediction(true);
				brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
				runGame(trainingIterations, brain, runner, true, sequenceIteration, collectGameScores);
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0.0);
				brain.getNetwork().setLearning(false);
				brain.reset();
				runner.reset(false);
				double[] scores = runGame(evaluationIterations, brain, runner, false, sequenceIteration, collectGameScores, gui,name);
				double fitness = scores[1];
				double prediction = scores[0];
				sequenceFitness += fitness;
				sequencePrediction += prediction;				
			}
			
			if (collectGameScores) gamescores_experiment[sequenceID] = gameScores_Sequence;
			
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
		return this.run(brain,0, false);
		
	}
	
	
	protected double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, boolean training, int gameNumber, boolean collectGameScores){
		return this.runGame(numEpisodes, activator, runner, training, gameNumber, collectGameScores, null, null);
	}
	
	/**
	 * Evaluates the activator on the given number of sequences.
	 * Remember to reset the runner and set the sequence before running this method
	 * @param numEpisodes the number times the sequence is repeated
	 * @param activator
	 * @return the score given as [avgPredictionSuccess, avgFitness]
	 */
	protected double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, boolean training, int gameNumber, boolean collectGameScores, GUI gui, String name){
		GameRunner gr = new GameRunner();
		double[] result = gr.runGame(numEpisodes, activator, runner, gui, name);
		if (training){
			if (collectGameScores) gameScores_Sequence[gameNumber] = gr.getGameScores();
		}
		
		return result;
	}
	
	public double[][] getSequenceScores(){
		return sequenceScores;
	}
	
	public double[][][][] getGmeScores(){
		return this.gamescores_experiment;
	}
}
