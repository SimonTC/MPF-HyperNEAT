package com.stcl.htm.experiments.rps;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;

import com.anji.integration.Activator;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;

public class RPSFitnessFunction_Single extends RPSFitnessFunction_HTM {
	
	boolean speed = false;

	public static void main(String[] args) throws IOException {
		String experimentRun = "D:/Users/Simon/Google Drev/Experiments/HTM/rps/1433597079636/0";
		String propsFileName = experimentRun + "/run.properties";
		String genomeFile = experimentRun + "/best_performing-final-13879.txt";;

		RPSFitnessFunction_Single eval = new RPSFitnessFunction_Single();
		eval.run(propsFileName, genomeFile);
		
		System.exit(0);
	}
	
	public void run(String propsFileName, String genomeFile) throws IOException{
		Properties props = new Properties(propsFileName);
		
		this.init(props);
		
		double[][] result = this.evaluate(genomeFile);
		
		for (int i = 0; i < result.length; i++){
			
			if (speed){
				int timeToPrediction = (int) ((1- result[i][0]) * trainingIterations);
				int timeToFitness = (int) ((1- result[i][1]) * trainingIterations);				
				System.out.println("Sequence " + i + " TTP: " + timeToPrediction + " TTF: " + timeToFitness);
			} else {
				System.out.println("Sequence " + i + " Prediction: " + result[i][0] + " Fitness: " + result[i][1]);
			}
			
		}
	}
	

	protected double[][] evaluate(String genomeFile) throws FileNotFoundException {
		RPS eval;
		if (speed){
			double threshold = 0.8;
			eval = new RPS_Speed(possibleInputs, sequences, new RewardFunction_Standard(), rand.nextLong(), numExperimentsPerSequence, trainingIterations, evaluationIterations, threshold, threshold, 5);
		} else {
			eval = new RPS(possibleInputs, sequences, new RewardFunction_Standard(), rand.nextLong(), numExperimentsPerSequence, trainingIterations, evaluationIterations);
		}
		Network brain = new Network(genomeFile, rand);
		HTMNetwork network = new HTMNetwork(brain);
		
		eval.run(network);	
		double[][] result = eval.getSequenceScores();

		return result;
	}

}
