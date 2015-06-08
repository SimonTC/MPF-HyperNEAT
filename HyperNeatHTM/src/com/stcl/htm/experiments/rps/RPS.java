package com.stcl.htm.experiments.rps;

import java.util.Random;

import javax.swing.text.Position;

import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;

import com.anji.integration.Activator;
import com.stcl.htm.network.HTMNetwork;

public class RPS {
	
	private SimpleMatrix[] possibleInputs;
	private int[][] sequences;
	private SimpleMatrix rewardMatrix;
	private int learningIterations;
	private int trainingIterations;
	private int evaluationIterations;
	private int numDifferentSequences;
	private int numIterationsPerSequence;
	private long randSeed;
	private Random rand;
	
	public RPS(SimpleMatrix[] possibleInputs, 
			int[][] sequences,
			SimpleMatrix rewardMatrix, 
			long randSeed, 
			int learningIterations,
			int trainingIterations,
			int evaluationIterations,
			int numDifferentSequences,
			int numIterationsPerSequence){
		
		this.possibleInputs = copyPossibleInputs(possibleInputs);
		this.sequences = copySequences(sequences);
		this.rewardMatrix = new SimpleMatrix(rewardMatrix);
		this.learningIterations = learningIterations;
		this.trainingIterations = trainingIterations;
		this.evaluationIterations = evaluationIterations;
		this.numDifferentSequences = numDifferentSequences;
		this.numIterationsPerSequence = numIterationsPerSequence;
		this.randSeed = randSeed;
	}
	
	private SimpleMatrix[] copyPossibleInputs(SimpleMatrix[] arr){
		SimpleMatrix[] result = new SimpleMatrix[arr.length];
		for (int i = 0; i < arr.length; i++){
			SimpleMatrix m = new SimpleMatrix(arr[i]);
			result[i] = m;
		}
		return result;
	}
	
	private int[][] copySequences(int[][] arr){
		int[][] result = new int[arr.length][];
		for (int i = 0; i < result.length; i++){
			result[i] = new int[arr[i].length];
			for (int j = 0; j < arr[i].length; j++){
				result[i][j] = arr[i][j];
			}
		}
		return result;
	}

	public double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		rand = new Random(randSeed);
		HTMNetwork brain = (HTMNetwork) activator;
		String initializationString = brain.toString();
		double totalFitness = 0;
		double totalPrediction = 0;
		for (int sequenceID = 0; sequenceID < numDifferentSequences; sequenceID++){
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			for (int sequenceIteration = 0; sequenceIteration < numIterationsPerSequence; sequenceIteration++){
				Network network = new Network();
				network.initialize(initializationString, rand);
				brain.setNetwork(network);
				//Show good and bad actions
				/*
				brain.getNetwork().setUsePrediction(false);
				runLearning(learningIterations, brain);
				brain.reset();
				*/
				//Let it train
				brain.getNetwork().setUsePrediction(true);
				runExperiment(trainingIterations, brain, curSequence);
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0);
				brain.getNetwork().setLearning(false);
				brain.reset();
				double[] scores = runExperiment(evaluationIterations, brain, curSequence);
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
		genotype.setPerformanceValue(avgPrediction);
		genotype.setFitnessValue(avgFitness);
		return avgFitness;
		
	}
	
	public double[][] evaluate(HTMNetwork brain) {
		rand = new Random(randSeed);
		String initializationString = brain.toString();
		double[][] result = new double[numDifferentSequences][numIterationsPerSequence];
		for (int sequenceID = 0; sequenceID < numDifferentSequences; sequenceID++){
			System.out.println("Starting on sequence " + sequenceID);
			int[] curSequence = sequences[sequenceID];
			for (int sequenceIteration = 0; sequenceIteration < numIterationsPerSequence; sequenceIteration++){
				System.out.println("Starting on iteration " + sequenceIteration);
				Network network = new Network();
				network.initialize(initializationString, rand);
				brain.setNetwork(network);
				//Show good and bad actions
				/*
				brain.getNetwork().setUsePrediction(false);
				runLearning(learningIterations, brain);
				brain.reset();
				*/
				//Let it train
				brain.getNetwork().setUsePrediction(true);
				runExperiment(trainingIterations, brain, curSequence);
				
				//Evaluate
				brain.getNetwork().getActionNode().setExplorationChance(0);
				brain.getNetwork().setLearning(false);
				brain.reset();
				double[] scores = runExperiment(evaluationIterations, brain, curSequence);
				double fitness = scores[1];
				double prediction = scores[0];
				result[sequenceID][sequenceIteration] = fitness;
			}
		}
		return result;
		
	}
	
	private double[] runExperiment(int maxIterations, HTMNetwork activator, int[] sequence){
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
			
			//Give inputs to brain
			SimpleMatrix inputVector = new SimpleMatrix(1, input.getNumElements(), true, input.getMatrix().data);
			activator.setInput(inputVector.getMatrix().data);
			activator.setAction(actionThisTimestep.getMatrix().data);
			
			//Do one step
			activator.step(rewardForBeingInCurrentState);
			
			//Collect output
			SimpleMatrix[] output = collectOutput(activator);
			prediction = output[0];
			actionNextTimeStep = output[1];
			
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
	
	private SimpleMatrix[] collectOutput(HTMNetwork activator){
		double[] predictionData = activator.getOutput();
		SimpleMatrix prediction = new SimpleMatrix(1, predictionData.length, true, predictionData);
		prediction.reshape(5, 5);
		
		double[] actionData = activator.getAction();
		SimpleMatrix actionNextTimeStep = new SimpleMatrix(1, actionData.length, true, actionData);

		//Set max value of action to 1. The rest to zero
		int max = maxID(actionNextTimeStep);
		if (max != -1){
			actionNextTimeStep.set(0);
			actionNextTimeStep.set(max, 1);
		}				
		SimpleMatrix[] result = {prediction, actionNextTimeStep};
		return result;
	}
	
	private void runLearning(int iterations, HTMNetwork activator){
		int[][] positiveExamples = {{0,1},{1,2},{2,0}};
		int[][] negativeExamples = {{1,0},{2,1},{0,2}};
		int[][] neutralExamples = {{0,0},{1,1},{2,2}};
		
		double externalReward = 0;
		
		double[][] tmp = {{1,0,0}};
		SimpleMatrix actionNow = new SimpleMatrix(tmp);
		
		for (int i = 0; i < iterations; i++){
			//Decide which kind of example to show
			int exampleType = rand.nextInt(3);
			
			//Decide specific example
			int[] example = null;
			switch(exampleType){
			case 0: example = positiveExamples[rand.nextInt(3)]; break;
			case 1: example = negativeExamples[rand.nextInt(3)]; break;
			case 2: example = neutralExamples[rand.nextInt(3)]; break;
			}
			
			//Get input and corresponding action	
			int inputID = example[0];
			SimpleMatrix input = new SimpleMatrix(possibleInputs[inputID]);
			int actionID = example[1];
			actionNow.set(0);
			actionNow.set(actionID, 1);
			
			double rewardForBeingInCurrentState = externalReward;

			//Calculate reward			
			externalReward = reward(inputID, actionID);
			
			//Give inputs to brain
			SimpleMatrix inputVector = new SimpleMatrix(1, input.getNumElements(), true, input.getMatrix().data);
			activator.setInput(inputVector.getMatrix().data);
			activator.setAction(actionNow.getMatrix().data);
			
			//Do one step
			activator.step(rewardForBeingInCurrentState);
		}
	}
	
	
	/**
	 * 
	 * @param opponentSymbol Symbol played by opponent
	 * @param playerSymbol SYmbol played by AI
	 * @return
	 */
	private double reward(int opponentSymbol, int playerSymbol){
		double reward = rewardMatrix.get(playerSymbol, opponentSymbol);
		return reward;
	}

	
	private int maxID(SimpleMatrix m){
		//Transform bias matrix into vector
				double[] vector = m.getMatrix().data;
				
				//Go through bias vector until value is >= random number
				double max = Double.NEGATIVE_INFINITY;
				int maxID = -1;
				int id = 0;
				
				for (double d : vector){
					if (d > max){
						max = d;
						maxID = id;
					}
					id++;
				}
				return maxID;
	}
}
