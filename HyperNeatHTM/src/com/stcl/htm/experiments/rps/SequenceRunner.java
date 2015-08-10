package com.stcl.htm.experiments.rps;

import java.util.ArrayList;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class SequenceRunner {
	
	private SimpleMatrix[] possibleInputs;
	private int[] sequence;
	private RewardFunction[] rewardFunctions;
	private RewardFunction curRewardFunction;
	private int curRewardFunctionID;
	private Random rand;
	private ArrayList<SimpleMatrix> possibleActions;
	private double noiseMagnitude;
	
	//Variables have to be saved here to remember values between sequence runs
	private double externalReward;
	private SimpleMatrix actionNextTimeStep;
	private SimpleMatrix prediction;

	public SequenceRunner(int[] sequence, SimpleMatrix[] possibleInputs, RewardFunction[] rewardFunctions, Random rand, double noiseMagnitude) {
		this.possibleInputs = possibleInputs;
		this.rand = rand;
		setSequence(sequence);
		setRewardFunctions(rewardFunctions);
		reset(false);
		this.possibleActions = createPossibleActions();
		this.noiseMagnitude = noiseMagnitude;
	}
	
	/**
	 * Reset all variables to their initial values.
	 */
	public void reset(boolean gotoNextRewardFunction){
		externalReward = 0;
		double[][] tmp = {{1,0,0}};
		actionNextTimeStep = new SimpleMatrix(tmp);
		prediction = possibleInputs[0];		
		if (gotoNextRewardFunction){
			curRewardFunctionID++;
			if (curRewardFunctionID == rewardFunctions.length) curRewardFunctionID = 0;
			curRewardFunction = rewardFunctions[curRewardFunctionID];
		}
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
	
	/**
	 * Goes through the sequence once.
	 * Remember to call reset() if the evaluation should start from scratch
	 * @param activator
	 * @return Array containing prediction success and fitness in the form [prediction,fitness]
	 */
	public double[] runSequence(HTMNetwork activator){
		double totalPredictionError = 0;
		double totalGameScore = 0;
		
		activator.getNetwork().getActionNode().setPossibleActions(possibleActions);
		
		//Give blank input and action to network
		SimpleMatrix initialInput = new SimpleMatrix(5, 5);
		SimpleMatrix initialAction = new SimpleMatrix(1, 3);
		giveInputsToActivator(activator, initialInput, initialAction);
		
		activator.step(0);
		
		//Collect initial prediction and action
		SimpleMatrix[] initialOutput = collectOutput(activator);
		prediction = initialOutput[0];
		actionNextTimeStep = initialOutput[1];
		
		for (int i = 0; i < sequence.length; i++){
			//Get input			
			SimpleMatrix input = possibleInputs[sequence[i]];
			SimpleMatrix noisyInput = addNoise(input, noiseMagnitude);
			
			double predictionError = 0;
			SimpleMatrix diff = input.minus(prediction);
			predictionError = diff.normF();	
			//predictionError = (predictionError > 0.1) ? 1 : 0;
			totalPredictionError += predictionError;
			
			SimpleMatrix actionThisTimestep = actionNextTimeStep;
			double rewardForBeingInCurrentState = externalReward;
			  
						
			//Calculate reward
			externalReward = calculateReward(actionThisTimestep, sequence[i]);
			totalGameScore += externalReward;			
			
			//Give inputs to brain
			giveInputsToActivator(activator, noisyInput, actionThisTimestep);
			
			//Do one step
			activator.step(rewardForBeingInCurrentState);
			
			//Collect output
			SimpleMatrix[] output = collectOutput(activator);
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
	
	private void giveInputsToActivator(HTMNetwork activator, SimpleMatrix input, SimpleMatrix action){
		SimpleMatrix inputVector = new SimpleMatrix(1, input.getNumElements(), true, input.getMatrix().data);
		SimpleMatrix actionVector = new SimpleMatrix(1, action.getNumElements(), true, action.getMatrix().data);
		activator.setInput(inputVector.getMatrix().data);		
		activator.setAction(actionVector.getMatrix().data);
	}
	
	/**
	 * Creates a noisy matrix based on the given matrix. The noise added is in the range [-0.25, 0.25]
	 * The input matrix is not altered in this method.
	 * Values in the matrix will be in the range [0,1] after adding noise
	 * @param m
	 * @param noiseMagnitude
	 * @return noisy matrix
	 */
	private SimpleMatrix addNoise(SimpleMatrix m, double magnitude){
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
	private double calculateReward(SimpleMatrix action, int inputID){
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
	
	/**
	 * Collects the output from the activator
	 * @param activator
	 * @return prediction and action for the next time step
	 */
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
	
	/**
	 * Return the id of the element with the max value in the matrix
	 * @param m
	 * @return
	 */
	private int maxID(SimpleMatrix m){
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
	
	public void setSequence(int[] sequence){
		this.sequence = sequence;
	}
	
	public void setNoiseMagnitude(double d){
		this.noiseMagnitude = d;
	}
	
	public void setRewardFunctions(RewardFunction[] rewardFunctions){
		this.rewardFunctions = rewardFunctions;
		curRewardFunctionID = 0;
		curRewardFunction = rewardFunctions[curRewardFunctionID];
	}

}
