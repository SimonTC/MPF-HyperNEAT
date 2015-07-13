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
	protected boolean training;
	protected int numExperimentsPerSequence;
	protected int trainingIterations;
	protected int evaluationIterations;
	protected double[][] sequenceScores;
	
	protected SequenceRunner runner;
	
	
	public RPS(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			RewardFunction rewardFunction, 
			long randSeed,
			int numExperimentsPerSequence,
			int trainingIterations,
			int evaluationIterations){
		rand = new Random(randSeed);
		runner = new SequenceRunner(null, possibleInputs, rewardFunction, rand);
		this.numExperimentsPerSequence = numExperimentsPerSequence;
		this.trainingIterations = trainingIterations;
		this.evaluationIterations = evaluationIterations;
		this.training = true;
		sequenceScores = new double[sequences.length][2];
		this.sequences = sequences;

	}
	
	public double[] run(HTMNetwork brain) {
		System.out.println("Start run on brain ");
		//String initializationString = brain.toString();
		double totalFitness = 0;
		double totalPrediction = 0;
		
		printInfo(brain);
		
		for (int sequenceID = 0; sequenceID < sequences.length; sequenceID++){
			System.out.println("Start on sequence " + sequenceID);
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			runner.setSequence(curSequence);
			runner.reset();
			for (int sequenceIteration = 0; sequenceIteration < numExperimentsPerSequence; sequenceIteration++){
				
				Network_DataCollector collector = (Network_DataCollector) brain.getNetwork();
				String path = "D:/Users/Simon/Google Drev/Experiments/HTM/rps/reporting/sequence " + sequenceID + "/iteration " + sequenceIteration;
				//(use relative path for Unix systems)
				File f = new File(path);
				//(works for both Windows and Linux)
				f.getParentFile().mkdirs(); 
				collector.initializeWriters(path, false);
				collector.openFiles(true);
				
				System.out.println("Iteration " + sequenceIteration);
				//Network network = new Network();
				//network.initialize(initializationString, rand);
				//brain.setNetwork(network);
				brain.getNetwork().reinitialize();
				
				printInfo(brain);
				
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
				
				System.out.println("Fitness: " + fitness);
				System.out.println("Prediction: " + prediction);
				System.out.println();
				collector.closeFiles();
				
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
	
	private void printInfo(HTMNetwork brain){
		
		System.out.println("SOM models in Action Node:");
		brain.getNetwork().getActionNode().printSomModels();
		
		/*
		System.out.println("Prediction models in Unit nodes:");
		for (UnitNode n : brain.getNetwork().getUnitNodes()){
			n.getUnit().printPredictionModel();
			System.out.println();
		}
		*/
		
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
