package com.stcl.htm.experiments.rps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;
import stcl.algo.brain.Network_DataCollector;
import stcl.algo.util.FileWriter;

import com.anji.integration.Activator;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Inverse;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;

public class RPSFitnessFunction_Single extends RPSFitnessFunction_Fitness {
	
	boolean speed = false;
	boolean collectData = false;
	boolean setSequencesManually = true;

	public static void main(String[] args) throws IOException {
		
		for (int i = 0; i < 10; i++){
			String experimentRun = "C:/Users/Simon/Google Drev/Experiments/HTM/rps_pc/1438176442018";
			String propsFileName = experimentRun + "/run.properties";
			String genomeFile = experimentRun + "/best_performing-8-2068.txt";;
	
			RPSFitnessFunction_Single eval = new RPSFitnessFunction_Single();
			eval.run(propsFileName, genomeFile);
			System.out.println();
			System.out.println();
		}
		
		System.exit(0);
	}
	
	public void run(String propsFileName, String genomeFile) throws IOException{
		Properties props = new Properties(propsFileName);
		props.remove(RPS_SEQUENCES_RAND_SEED_KEY);
		props.setProperty(RPS_SEQUENCES_NUMBER_KEY, "10");
		this.init(props);
		
		double[][] result = this.evaluate(genomeFile);
		double fitnessSum = 0;
		double predictionSum = 0;
		
		for (int i = 0; i < result.length; i++){
			
			if (speed){
				int timeToPrediction = (int) ((1- result[i][0]) * trainingIterations);
				int timeToFitness = (int) ((1- result[i][1]) * trainingIterations);				
				System.out.println("Sequence " + i + " TTP: " + timeToPrediction + " TTF: " + timeToFitness);
			} else {
				System.out.println("Sequence " + i + " Prediction: " + result[i][0] + " Fitness: " + result[i][1]);
				predictionSum += result[i][0];
				fitnessSum += result[i][1];
			}
			
		}
		double avgFitness = fitnessSum / (double) result.length;
		double avgPrediction = predictionSum / (double) result.length;
		System.out.println("Avg prediction: " + avgPrediction + " Avg fitness: " + avgFitness);
	}
	
	@Override
	protected int[][] createSequences(Properties props, Random rand){
		int[][] sequencesToReturn = null;
		int[][] generatedSequences = super.createSequences(props, rand);
		if (setSequencesManually){
			int[][] mySequence ={{0,1,2}};
			sequencesToReturn = mySequence;
		} else {
			sequencesToReturn = generatedSequences;
		}
		return sequencesToReturn;
	}
	

	protected double[][] evaluate(String genomeFile) throws FileNotFoundException {
		RPS eval;
		RewardFunction[] functions = {new RewardFunction_Standard(), new RewardFunction_Inverse()};
		if (speed){
			double fitnessThreshold = 0.9;
			double predictionThreshold = 0.0;
			eval = new RPS_Speed(possibleInputs, sequences, functions, numExperimentsPerSequence, trainingIterations, evaluationIterations, predictionThreshold, fitnessThreshold, 5);
		} else {
			eval = new RPS(possibleInputs, sequences, functions, numExperimentsPerSequence, trainingIterations, evaluationIterations);
		}
		//Network brain = new Network(genomeFile, rand);
		//Network_DataCollector brain = new Network_DataCollector(genomeFile, rand);
		long randSeed = 0;//new Random().nextLong();
		System.out.println("seed: " + randSeed);
		Network_DataCollector brain = new Network_DataCollector(genomeFile, new Random());
		HTMNetwork network = new HTMNetwork(brain);
		
		if (collectData){
			setupDataCollection(brain, genomeFile);
		}
		
		eval.run(network, 1);	
		double[][] result = eval.getSequenceScores();
		
		if(collectData){
			brain.closeFiles();
		}

		return result;
	}
	
	private void setupDataCollection(Network_DataCollector brain, String genomeFile){
		File f = new File(genomeFile);
		String parentFolder = f.getParent();
		String dataFolder = parentFolder + "/DataCollection";
		printBrain(brain, dataFolder);
		brain.initializeWriters(dataFolder, false);
	}
	
	private void printBrain(Network_DataCollector brain, String folder){
		String description = brain.toString();
		try {
			FileWriter fw = new FileWriter(folder + "/NetworkDescription.txt");
			fw.openFile(false);
			fw.write(description);
			fw.closeFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
