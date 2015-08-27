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
	protected RewardFunction[] rewardFunctions;
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
		runner = new SequenceRunner(possibleInputs, rand, noiseMagnitude);
		this.numExperimentsPerSequence = numExperimentsPerSequence;
		this.trainingIterations = trainingIterations;
		this.evaluationIterations = evaluationIterations;
		sequenceScores = new double[sequences.length][2];
		this.sequences = sequences;
		this.gui = gui;
		this.rewardFunctions = rewardFunctions;

	}
	
	public double[] run_random(boolean collectGameScores, String gameScoreFolder){
		double totalFitness = 0;
		double totalPrediction = 0;
				
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			String sequenceFileName = gameScoreFolder + "/seq" + sequenceID + "_";
			//System.out.println("Starting on sequence " + sequenceID);
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.changeRules(curSequence, rewardFunctions[0]);
						
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				String sequenceIterationFileName = sequenceFileName + "itr" + sequenceIteration + ".csv";
				String name = sequenceID + " test " + sequenceIteration;
				//System.out.println("Starting on iteration " + sequenceIteration);
				//Let it train
				runGame_random(trainingIterations, runner, true, sequenceIterationFileName, collectGameScores);
				
				//Evaluate
				double[] scores = runGame_random(evaluationIterations, runner, false, sequenceIterationFileName, collectGameScores);
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
			runner.changeRules(curSequence, rewardFunctions[0]);
						
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				String sequenceIterationFileName = sequenceFileName + "itr" + sequenceIteration + ".csv";
				String name = sequenceID + " test " + sequenceIteration;
				//System.out.println("Starting on iteration " + sequenceIteration);
				brain.getNetwork().reinitialize();
				
	brain.getNetwork().setCollectData(false);
				
				//Let it train
				brain.getNetwork().setUsePrediction(true);
				brain.getNetwork().getActionNode().setExplorationChance(explorationChance);
				runGame(trainingIterations, brain, runner, true, sequenceIterationFileName, collectGameScores);
				
	brain.getNetwork().setCollectData(true);
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0.0);
				brain.getNetwork().setLearning(false);
				brain.reset();
				double[] scores = runGame(evaluationIterations, brain, runner, false, sequenceIterationFileName, collectGameScores, gui,name);
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
		return this.run(brain,0, false, "");
		
	}
	
	
	protected double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, boolean training, String gameScoreFile, boolean collectGameScores){
		return this.runGame(numEpisodes, activator, runner, training, gameScoreFile, collectGameScores, null, null);
	}
	
	protected double[] runGame_random(int numEpisodes, SequenceRunner runner, boolean training, String gameScoreFile, boolean collectGameScores){
		GameRunner gr = new GameRunner();
		double[] result = gr.runGame_random(numEpisodes, runner);
		if (training){
			if (collectGameScores) gr.writeGameScoresToFile(gameScoreFile);
		}
		
		return result;
	}
	
	/**
	 * Evaluates the activator on the given number of sequences.
	 * Remember to reset the runner and set the sequence before running this method
	 * @param numEpisodes the number times the sequence is repeated
	 * @param activator
	 * @return the score given as [avgPredictionSuccess, avgFitness]
	 */
	protected double[] runGame(int numEpisodes, HTMNetwork activator, SequenceRunner runner, boolean training, String gameScoreFile, boolean collectGameScores, GUI gui, String name){
		GameRunner gr = new GameRunner();
		double[] result = gr.runGame(numEpisodes, activator, runner, gui, name);
		if (training){
			if (collectGameScores) gr.writeGameScoresToFile(gameScoreFile);
		}
		
		return result;
	}
	
	public double[][] getSequenceScores(){
		return sequenceScores;
	}
}
