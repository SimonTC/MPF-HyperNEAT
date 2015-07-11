package com.stcl.htm.experiments.rps;

import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.network.HTMNetwork;

public class SequenceRunner {
	
	private SimpleMatrix[] possibleInputs;
	private int[] sequence;
	private RewardFunction rewardFunction;
	private Random rand;
	
	//Variables have to be saved here to remember values between sequence runs
	private double externalReward;
	private SimpleMatrix actionNextTimeStep;
	private SimpleMatrix prediction;

	public SequenceRunner(int[] sequence, SimpleMatrix[] possibleInputs, RewardFunction rewardFunction, Random rand) {
		this.possibleInputs = possibleInputs;
		this.rand = rand;
		setSequence(sequence);
		setRewardFunction(rewardFunction);
		reset();
	}
	
	/**
	 * Reset all variables to their initial values.
	 */
	public void reset(){
		externalReward = 0;
		double[][] tmp = {{1,0,0}};
		actionNextTimeStep = new SimpleMatrix(tmp);
		prediction = possibleInputs[0];		
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
		
		for (int i = 0; i < sequence.length; i++){
			//Get input			
			SimpleMatrix input = possibleInputs[sequence[i]];
			SimpleMatrix noisyInput = addNoise(input);
			
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
				externalReward = calculateReward(actionThisTimestep, sequence[i]);
			} else {
				externalReward = 1;
			}
			totalGameScore += externalReward;			
			
			//Give inputs to brain
			SimpleMatrix inputVector = new SimpleMatrix(1, noisyInput.getNumElements(), true, noisyInput.getMatrix().data);
			activator.setInput(inputVector.getMatrix().data);
			activator.setAction(actionThisTimestep.getMatrix().data);
			
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
	
	/**
	 * Creates a noisy matrix based on the given matrix. The noise added is in the range [-0.25, 0.25]
	 * The input matrix is not altered in this method.
	 * Values in the matrix will be in the range [0,1] after adding noise
	 * @param m
	 * @param noiseMagnitude
	 * @return noisy matrix
	 */
	private SimpleMatrix addNoise(SimpleMatrix m){
		SimpleMatrix noisy = new SimpleMatrix(m);
		for (int i = 0; i < m.getNumElements(); i++){
			double d = m.get(i);
			d = d + (rand.nextDouble() - 0.25);
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
		int actionID = -1;
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int j = 0; j < action.getNumElements(); j++){
			double d = action.get(j);
			if (d > maxValue){
				maxValue = d;
				actionID = j;
			}
		}
		double reward = rewardFunction.reward(inputID, actionID);
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
	
	public void setRewardFunction(RewardFunction rewardFunction){
		this.rewardFunction = rewardFunction;
	}

}
