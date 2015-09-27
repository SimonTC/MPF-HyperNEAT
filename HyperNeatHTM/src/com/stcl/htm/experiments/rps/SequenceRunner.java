package com.stcl.htm.experiments.rps;

import java.util.ArrayList;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;
import stcl.algo.brain.Network_DataCollector;
import stcl.graphics.MPFGUI;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class SequenceRunner {
	
	protected SimpleMatrix[] possibleInputs;
	protected SimpleMatrix[] patternsToTestAgainst;
	private RewardFunction curRewardFunction;
	private int curRewardFunctionID;
	protected int[] sequence;
	private Random rand;
	protected ArrayList<SimpleMatrix> possibleActions;
	protected double noiseMagnitude;
	
	public SequenceRunner(SimpleMatrix[] possibleInputs, Random rand, double noiseMagnitude) {
		this.possibleInputs = possibleInputs;
		this.rand = rand;
		this.possibleActions = createPossibleActions();
		this.noiseMagnitude = noiseMagnitude;
		this.patternsToTestAgainst = createPatternsToTestAgainst(possibleInputs);
	}
	
	private SimpleMatrix[] createPatternsToTestAgainst(SimpleMatrix[] possibleInputs){
		int cols = possibleInputs[0].numCols();
		int rows = possibleInputs[0].numRows();
		SimpleMatrix[] testPatterns = new SimpleMatrix[possibleInputs.length * 2];
		int counter = 0;
		for (int i = 0; i < possibleInputs.length; i++){
			testPatterns[i] = new SimpleMatrix(rows, cols);
			for (int j = 0; j < testPatterns[i].getNumElements(); j++){
				testPatterns[i].set(j, rand.nextDouble());
			}
			counter = i;
		}
		counter += 1;
		for (int i = 0; i < possibleInputs.length; i++){
			testPatterns[i + counter] = new SimpleMatrix(possibleInputs[i]);
			
		}
		return testPatterns;
	}
	
	/**
	 * Sets new sequences and reward function.
	 * @param sequence
	 * @param rewardFunction
	 */
	public void changeRules(int[] sequence, RewardFunction rewardFunction){
		this.curRewardFunction = rewardFunction;
		this.sequence = sequence;
	}
	
	private ArrayList<SimpleMatrix> createPossibleActions(){
		double[][] rock = {{1,0,0}};
		double[][] paper = {{0,1,0}};
		double[][] scissors = {{0,0,1}};
		double[][] empty = {{0,0,0}};
		
		SimpleMatrix r = new SimpleMatrix(rock);
		SimpleMatrix p = new SimpleMatrix(paper);
		SimpleMatrix s = new SimpleMatrix(scissors);
		SimpleMatrix e = new SimpleMatrix(empty);
		ArrayList<SimpleMatrix> arr = new ArrayList<SimpleMatrix>();
		arr.add(r);
		arr.add(p);
		arr.add(s);
		arr.add(e);
		return arr;
	}
	
	
	public double[] runEpisode(HTMNetwork activator){
		return runEpisode(activator, null);
	}
	
	public double[] runEpisode_random(){
		double totalPredictionError = 0;
		double totalGameScore = 0;
		
		//Collect initial prediction and action
		SimpleMatrix[] initialOutput = collectOutput_random();
		SimpleMatrix prediction = initialOutput[0];
		SimpleMatrix actionNextTimeStep = initialOutput[1];
		
		
		for (int i = 0; i < sequence.length; i++){
			//Get input			
			SimpleMatrix input = possibleInputs[sequence[i]];
			
			double predictionError = calculatePredictionError(prediction, input);
			totalPredictionError += predictionError;
			
			SimpleMatrix actionThisTimestep = actionNextTimeStep;
						
			//Calculate reward
			double externalReward = calculateReward(actionThisTimestep, sequence[i]);
			totalGameScore += externalReward;			
			
			//Collect output
			SimpleMatrix[] output = collectOutput_random();
			prediction = output[0];
			actionNextTimeStep = output[1];
			
		}
		
		double avgPredictionError = totalPredictionError / (double) sequence.length;
		double avgScore = totalGameScore / (double) sequence.length;
		double predictionSuccess = 1 - avgPredictionError;
		
		//Scores can't be less than zero as the evolutionary algorithm can't work with that
		if (avgScore < 0) avgScore = 0;
		if (predictionSuccess < 0) predictionSuccess = 0;
		
		double[] result = {predictionSuccess, avgScore};
		return result;
	}
	/**
	 * Goes through the sequence once.
	 * Remember to call reset() if the evaluation should start from scratch
	 * @param activator
	 * @return Array containing prediction success and fitness in the form [prediction,fitness]
	 */
	public double[] runEpisode(HTMNetwork activator, GUI gui){
		double totalPredictionError = 0;
		double totalGameScore = 0;
		double reward_before = 0;
		boolean collectData = activator.getNetwork().getCollectData();
		SimpleMatrix actionBefore = new SimpleMatrix(5, 5);
		SimpleMatrix stateBefore = new SimpleMatrix(5, 5);		
		
		int state = 1;
		activator.getNetwork().getActionNode().setPossibleActions(possibleActions);
		
		emptyInput(activator, 0, collectData);
		
		for (int i = 0; i < sequence.length; i++){
			
			//Get input			
			state = sequence[i];
			SimpleMatrix input = possibleInputs[state];
			SimpleMatrix noisyInput = addNoise(input, noiseMagnitude);
						
			//Collect output
			activator.feedback();
			if (collectData) activator.getNetwork().collectFeedBackData();
			
			SimpleMatrix[] output = collectOutput(activator);
			SimpleMatrix prediction = output[0];
			SimpleMatrix myAction = output[1];
						
			activator.resetUnitActivity();
			if (collectData) activator.getNetwork().printDataToFiles();
			
			double reward_now = calculateReward(myAction, state);
			totalGameScore += reward_now;	
			
			double predictionError = calculatePredictionError(prediction, input);
			totalPredictionError += predictionError;
			
			giveInputsToActivator(activator, noisyInput, myAction);
			activator.feedForward(reward_before);
			if (collectData) activator.getNetwork().collectFeedForwardData();
			reward_before = reward_now;
			
			if (gui != null) {
				int actionMax = maxID(myAction);
				SimpleMatrix myAction_RPS = new SimpleMatrix(possibleInputs[actionMax]);
				
				gui.update(activator.getNetwork(), stateBefore, actionBefore, prediction, myAction_RPS, i);
				stateBefore = new SimpleMatrix(input);
				actionBefore = new SimpleMatrix(myAction_RPS);
			}
			
			
			
		}
		if (collectData) activator.getNetwork().printDataToFiles();
		emptyInput(activator, reward_before, collectData);
		
		double avgPredictionError = totalPredictionError / (double) sequence.length;
		double avgScore = totalGameScore / (double) sequence.length;
		double predictionSuccess = 1 - avgPredictionError;
		
		double[] result = {predictionSuccess, avgScore};
		return result;
	}
	
	protected void emptyInput(HTMNetwork activator, double reward, boolean collectData){
		//Give blank input and action to network
		SimpleMatrix initialInput = new SimpleMatrix(5, 5);
		SimpleMatrix initialAction = new SimpleMatrix(1, 3);
		giveInputsToActivator(activator, initialInput, initialAction);
		
		activator.feedForward(reward);
		
		if (collectData) activator.getNetwork().collectFeedForwardData();

	}
	
	protected void giveInputsToActivator(HTMNetwork activator, SimpleMatrix input, SimpleMatrix action){
		SimpleMatrix inputVector = new SimpleMatrix(1, input.getNumElements(), true, input.getMatrix().data);
		SimpleMatrix actionVector = new SimpleMatrix(1, action.getNumElements(), true, action.getMatrix().data);
		activator.setInput(inputVector.getMatrix().data);		
		activator.setAction(actionVector.getMatrix().data);
	}
	
	protected double calculatePredictionError(SimpleMatrix prediction, SimpleMatrix actual){
		double minError = Double.POSITIVE_INFINITY;
		SimpleMatrix bestMatch = null;
		
		for (SimpleMatrix m : patternsToTestAgainst){
			SimpleMatrix diff = m.minus(prediction);
			double d = diff.normF();	
			if (d < minError){
				minError = d;
				bestMatch = m;
			}
		}		
		
		double predictionError = 1;
		if (bestMatch.isIdentical(actual, 0.001)) {
			predictionError = 0;
		}
		
		return predictionError;
	}
	
	/**
	 * Creates a noisy matrix based on the given matrix. The noise added is in the range [-0.25, 0.25]
	 * The input matrix is not altered in this method.
	 * Values in the matrix will be in the range [0,1] after adding noise
	 * @param m
	 * @param noiseMagnitude
	 * @return noisy matrix
	 */
	protected SimpleMatrix addNoise(SimpleMatrix m, double magnitude){
		SimpleMatrix noisy = new SimpleMatrix(m);
		for (int i = 0; i < m.getNumElements(); i++){
			double d = m.get(i);
			double noise = magnitude * (rand.nextDouble() - 0.5) * 2;
			d = d + noise;
			if (d < 0) d = 0;
			if (d > 1) d = 1;
			noisy.set(i, d);
		}
		return noisy;
	}
	
	/**
	 * finds the action chosen by the player and returns the reward given for that action
	 * @param action
	 * @param inputID
	 * @return
	 */
	protected double calculateReward(SimpleMatrix action, int inputID){
		if (action.elementSum() < 0.001) return -1; //Make sure that null actions are punished
		
		int actionID = -1;
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int j = 0; j < action.getNumElements(); j++){
			double d = action.get(j);
			if (d > maxValue){
				maxValue = d;
				actionID = j;
			}
		}
		double reward = curRewardFunction.reward(inputID, actionID);
		return reward;
	}
	
	protected SimpleMatrix[] collectOutput_random(){
		SimpleMatrix prediction = possibleInputs[rand.nextInt(possibleInputs.length)];
		SimpleMatrix actionNextTimeStep = new SimpleMatrix(1, 3);
		actionNextTimeStep.set(rand.nextInt(3), 1);
		SimpleMatrix[] result = {prediction, actionNextTimeStep};
		return result;
	}
	
	/**
	 * Collects the output from the activator
	 * @param activator
	 * @return prediction and action for the next time step
	 */
	protected SimpleMatrix[] collectOutput(HTMNetwork activator){
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
	
	/**
	 * Return the id of the element with the max value in the matrix
	 * @param m
	 * @return
	 */
	protected int maxID(SimpleMatrix m){
		double[] vector = m.getMatrix().data;
		
		
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
	
	public void setNoiseMagnitude(double d){
		this.noiseMagnitude = d;
	}


}
