package com.stcl.htm.experiments.rps;

import java.io.IOException;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import com.anji.util.Randomizer;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.sequencecreation.SequenceBuilder;

public class RPS_Random{

	public static final String RPS_LEARNING_ITERATIONS_KEY = "rps.learning.iterations";
	public static final String RPS_TRAINING_ITERATIONS_KEY = "rps.training.iterations";
	public static final String RPS_EVALUATION_ITERATIONS_KEY = "rps.evaluation.iterations";
	public static final String RPS_SEQUENCES_NUMBER_KEY = "rps.sequences.number";
	public static final String RPS_SEQUENCES_ITERATIONS_KEY = "rps.sequences.iterations";
	public static final String RPS_SEQUENCES_LEVELS_KEY = "rps.sequences.levels";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MIN = "rps.sequences.blocklength.min";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MAX = "rps.sequences.blocklength.max";
	public static final String RPS_SEQUENCES_ALPHABET_SIZE = "rps.sequences.alphabet.size";
	public static final String RPS_SEQUENCES_RAND_SEED_KEY = "rps.sequences.rand.seed";

	private SimpleMatrix[] possibleInputs;
	private int[][] sequences;
	private SimpleMatrix rewardMatrix;
	private int evaluationIterations;
	private int numDifferentSequences;
	private int numIterationsPerSequence;
	private Random rand;
	
	public static void main(String[] args) {
		String propertiesFilePath = "D:/Users/Simon/Google Drev/Experiments/HTM/rps/rps_htm.properties";
		try {
			Properties props = new Properties(propertiesFilePath);
			RPS_Random r = new RPS_Random();
			r.init(props);
			r.initialiseEvaluation();
			double[] result = r.evaluate();
			System.out.println("Fitness: " + result[0]);
			System.out.println("Prediction: " + result[1]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void init(Properties props) {
		Randomizer randomizer = new Randomizer();
		randomizer.init(props);
		rand = randomizer.getRand();
		evaluationIterations = props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY, 100);
		numDifferentSequences = props.getIntProperty(RPS_SEQUENCES_NUMBER_KEY, 1);
		numIterationsPerSequence = props.getIntProperty(RPS_SEQUENCES_ITERATIONS_KEY, 10);
		
		//Create sequences
		long sequenceSeed = rand.nextLong();
		
		int sequenceLevels = props.getIntProperty(RPS_SEQUENCES_LEVELS_KEY);
		int blockLengthMin = props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MIN);
		int blockLengthMax = props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MAX);
		int alphabetSize = props.getIntProperty(RPS_SEQUENCES_ALPHABET_SIZE, 3); //Currently not in use
		Random sequenceRand = new Random(sequenceSeed);
		SequenceBuilder builder = new SequenceBuilder();
		sequences = new int[numDifferentSequences][];
		for ( int i = 0; i < numDifferentSequences; i++){
			sequences[i] = builder.buildSequence(sequenceRand, sequenceLevels, alphabetSize, blockLengthMin, blockLengthMax);
		}
		
	}

	public void initialiseEvaluation() {
		createInputs();
		createRewardMatrix();
		//createRewardMatrix_Scaled();
	}


	public double[] evaluate() {
		double totalFitness = 0;
		double totalPrediction = 0;
		for (int sequenceID = 0; sequenceID < numDifferentSequences; sequenceID++){
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			for (int sequenceIteration = 0; sequenceIteration < numIterationsPerSequence; sequenceIteration++){
				double[] scores = runExperiment(evaluationIterations, curSequence);
				double fitness = scores[1];
				double prediction = scores[0];
				sequenceFitness += fitness;
				sequencePrediction += prediction;
			}
			totalFitness += (sequenceFitness / (double)numIterationsPerSequence);
			totalPrediction += (sequencePrediction / (double)numIterationsPerSequence);
		}
		double avgFitness = totalFitness / (double)numDifferentSequences;
		double avgPrediction = totalPrediction / (double)numDifferentSequences;
		double[] result = {avgFitness, avgPrediction};
		return result;
	}
	
	private double[] runExperiment(int maxIterations, int[] sequence){
		int curInput = 0;
		double externalReward = 0;
		
		double[][] tmp = {{1,0,0}};
		SimpleMatrix actionNextTimeStep = new SimpleMatrix(tmp); //m(t)
		//SimpleMatrix actionAfterNext = new SimpleMatrix(tmp); //m(t+2)

		SimpleMatrix prediction = possibleInputs[0];
		
		double totalPredictionError = 0;
		double totalGameScore = 0;
		
		for (int i = 0; i < maxIterations; i++){
			//if (i % 500 == 0) System.out.println("Iteration: " + i);
			
			//Get input			
			SimpleMatrix input = new SimpleMatrix(possibleInputs[sequence[curInput]]);
			
			double predictionError = 0;
			if (i > 0){ //First prediction will always be wrong so we don't count it
				SimpleMatrix diff = input.minus(prediction);
				predictionError = diff.normF();	
				if (predictionError > 0.1) predictionError = 1; //TODO: Maybe change threshold of error	
			}
			totalPredictionError += predictionError;
			SimpleMatrix actionThisTimestep = actionNextTimeStep;
			double rewardForBeingInCurrentState = externalReward;
			  
						
			//Calculate reward
			if ( i > 0){ //First action is always wrong so don't punish
				externalReward = calculateReward(actionThisTimestep, sequence[curInput]);
			} else {
				externalReward = 1;
			}
			totalGameScore += externalReward;			

			
			//Collect output
			prediction = possibleInputs[rand.nextInt(possibleInputs.length)];
			int actionID = rand.nextInt(3);
			actionNextTimeStep.set(0);
			actionNextTimeStep.set(actionID, 1);
			
			curInput++;
			if (curInput >= sequence.length){
				curInput = 0;
			}			
		}
		
		double avgPredictionError = totalPredictionError / (double) maxIterations;
		double avgScore = totalGameScore / (double) maxIterations;
		double predictionSuccess = 1 - avgPredictionError;
		
		//Scores can't be less than zero as the evolutionary algorithm can't work with that
		if (avgScore < 0) avgScore = 0;
		if (predictionSuccess < 0) predictionSuccess = 0;
		
		double[] result = {predictionSuccess, avgScore};
		return result;
	}
	
	private double calculateReward(SimpleMatrix action, int inputID){
		int actionID = -1;
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int j = 0; j < action.getNumElements(); j++){
			double d = action.get(j);
			if (d > maxValue){
				maxValue = d;
				actionID = j;
			}
		}
		double result = reward(inputID, actionID);
		return result;
	}
	
	private double reward(int opponentSymbol, int playerSymbol){
		double reward = rewardMatrix.get(playerSymbol, opponentSymbol);
		return reward;
	}
	
	private void createInputs(){
		double[][] rockData = {
				{0,0,0,0,0},
				{0,1,1,1,0},
				{0,1,1,1,0},
				{0,1,1,1,0},
				{0,0,0,0,0}
		};
		
		SimpleMatrix rock = new SimpleMatrix(rockData);
		
		double[][] paperData = {
				{1,1,1,1,1},
				{1,1,1,1,1},
				{1,1,1,1,1},
				{1,1,1,1,1},
				{1,1,1,1,1}
		};
		
		SimpleMatrix paper = new SimpleMatrix(paperData);
		
		double[][] scissorsData = {
				{0,0,0,1,0},
				{1,0,1,0,0},
				{0,1,0,0,0},
				{1,0,1,0,0},
				{0,0,0,1,0}
		};
		
		SimpleMatrix scissors = new SimpleMatrix(scissorsData);		

		SimpleMatrix[] tmp = {rock, paper, scissors};

		possibleInputs = tmp;
	}
	
	private void createRewardMatrix_Scaled(){
		double[][]data = {
				{0.5,0,1},
				{1,0.5,0},
				{0,1,0.5}
		};
		
		rewardMatrix = new SimpleMatrix(data);
	}
	
	private void createRewardMatrix(){
		double[][]data = {
				{0,-1,1},
				{1,0,-1},
				{-1,1,0}
		};
		
		rewardMatrix = new SimpleMatrix(data);
	}

}
