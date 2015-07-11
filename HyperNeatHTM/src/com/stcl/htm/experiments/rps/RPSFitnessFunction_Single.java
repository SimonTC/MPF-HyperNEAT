package com.stcl.htm.experiments.rps;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;

import com.anji.integration.Activator;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class RPSFitnessFunction_Single extends RPSFitnessFunction_HTM {

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
			String s = "Sequence " + i + ": ";
			double total = 0;
			for (int j = 0; j < result[i].length; j++){
				double d = result[i][j];
				s += d + "  ";
				total += d;
			}
			double avg = total / (double) result[i].length;
			System.out.println(s + "  Avg: " + avg);
		}
	}
	

	protected double[][] evaluate(String genomeFile) throws FileNotFoundException {
		RPS eval = new RPS(possibleInputs, sequences, rewardMatrix, rand.nextLong(), learningIterations, trainingIterations, evaluationIterations, numDifferentSequences, numExperimentsPerSequence);
		
		Network brain = new Network(genomeFile, rand);
		HTMNetwork network = new HTMNetwork(brain);
		
		double[][] result = eval.run(network);		

		return result;
	}

}
