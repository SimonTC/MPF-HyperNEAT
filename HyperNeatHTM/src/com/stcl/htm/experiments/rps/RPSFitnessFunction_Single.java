package com.stcl.htm.experiments.rps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;
import stcl.algo.brain.Network_DataCollector;
import stcl.algo.util.FileWriter;
import stcl.graphics.MPFGUI;

import com.anji.integration.Activator;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.experiments.rps.gui.GUI_Overview;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Inverse;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;
import com.thoughtworks.xstream.security.ExplicitTypePermission;

public class RPSFitnessFunction_Single extends RPSFitnessFunction_Fitness {
	
	boolean speed = false;
	boolean collectData = true;
	boolean setSequencesManually = true;
	////////////////////////////////////////
	boolean initializeRandomly = true; //Set to false when testing genomes created by evolution
	////////////////////////////////////////
	private String sequencePath;
	
	boolean visualize = false;
	private int framesPerSecond = 1;
	
	public static void main(String[] args) throws IOException {
		double fitnessSum = 0;
		double predictionSum = 0;
		int numRepetitions = 1;
		for (int i = 0; i < numRepetitions; i++){
			/*
			String experimentRun = "C:/Users/Simon/Google Drev/Experiments/HTM/rps/Master data/0 Normal run/HTM/Experiments/1438935911798/0";
			String propsFileName = experimentRun + "/run.properties";
			String genomeFile = experimentRun + "/best_performing-final-12291.txt";
			*/
			String sequencePath = "";//"C:/Users/Simon/Google Drev/Experiments/HTM/rps/Master data/evaluation/results 2-2-2/sequences.txt";
			String experimentRun = "C:/Users/Simon/Google Drev/Experiments/HTM/rps/Master data/evaluation/genomes/0 Simple Network";
			String propsFileName = experimentRun + "/props.properties";
			String genomeFile = experimentRun + "/SimpleNetwork_1.txt";
			
			RPSFitnessFunction_Single eval = new RPSFitnessFunction_Single(sequencePath);
			double[] result = eval.run(propsFileName, genomeFile, sequencePath);
			fitnessSum+= result[1];
			predictionSum+= result[0];
			System.out.println();
			System.out.println();
		}
		
		double avgFitness = fitnessSum / (double) numRepetitions;
		double avgPrediction = predictionSum / (double) numRepetitions;
		System.out.println();
		System.out.println("**************** Final result ***************");
		System.out.println("Avg prediction: " + avgPrediction + " Avg fitness: " + avgFitness);
		System.out.println("*********************************************");
		
		System.exit(0);
	}
	
	public RPSFitnessFunction_Single(String sequencePath) {
		this.sequencePath = sequencePath;
	}
	
	public double[] run(String propsFileName, String genomeFile, String sequencePath) throws IOException{
		Properties props = new Properties(propsFileName);
		//props.setProperty("fitness.max_threads", "1");
		//props.remove(RPS_SEQUENCES_RAND_SEED_KEY);
		//props.setProperty(RPS_SEQUENCES_NUMBER_KEY, "100");
		props.setProperty(RPS_TRAINING_ITERATIONS_KEY, "1000");
		this.init(props);
		this.sequencePath = sequencePath;
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
		double[] retVal = {avgPrediction, avgFitness};
		return retVal;
	}
	
	@Override
	protected int[][] createSequences(Properties props, Random rand){
		int[][] sequencesToReturn = null;
		if (this.sequencePath.equalsIgnoreCase("")){
			int[][] generatedSequences = super.createSequences(props, rand);
			if (setSequencesManually){
				int[][] mySequence ={{2, 2, 2, 0, 0, 1, 1, 2, 1, 2, 2, 2, 0, 0, 1, 1, 2, 1}};
				sequencesToReturn = mySequence;
			} else {
				sequencesToReturn = generatedSequences;
			}
			
			for (int i = 0; i < sequencesToReturn.length; i++){
				String s  = "Sequence " + i + ": ";
				for (int j : sequencesToReturn[i]) s+= " " + j;
				System.out.println(s);
			}
		} else {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(sequencePath));
				String line = "";
				ArrayList<int[]> sequences = new ArrayList<int[]>();
				while( ( line = reader.readLine() ) != null) {
					int[] sequence = new int[line.length()];
					String[] arr = line.split(" ");
					for (int i = 0; i < arr.length; i++){
						sequence[i] = Integer.parseInt(arr[i]);
					}
					sequences.add(sequence);
				}
				sequencesToReturn = new int[sequences.size()][];
				for (int i = 0; i < sequences.size(); i++){
					sequencesToReturn[i] = sequences.get(i);
				}
				reader.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return sequencesToReturn;
	}
	

	protected double[][] evaluate(String genomeFile) throws FileNotFoundException {
		GUI gui = null;
		if (visualize){
			gui = new GUI_Overview();
			gui.initialize(5, 2, framesPerSecond);
		}
		
		RPS eval;
		RewardFunction[] functions = {new RewardFunction_Standard(), new RewardFunction_Inverse()};
		eval = new RPS(possibleInputs, sequences, functions, numExperimentsPerSequence, trainingIterations, evaluationIterations, rand.nextLong(), noiseMagnitude, gui);
		//Network brain = new Network(genomeFile, rand);
		//Network_DataCollector brain = new Network_DataCollector(genomeFile, rand);
		long randSeed = new Random().nextLong();
		System.out.println("seed: " + randSeed);
		Random rand = new Random(randSeed);
		Network_DataCollector brain = new Network_DataCollector(genomeFile, rand);
		if (!initializeRandomly){
			brain.initialize(genomeFile, rand, true);
		}
		HTMNetwork network = new HTMNetwork(brain);
		
		if (collectData){
			setupDataCollection(brain, genomeFile);
		}
		
		eval.run(network, exploreChance, false, ""); //Currently we dn't want to collect game scores	
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
