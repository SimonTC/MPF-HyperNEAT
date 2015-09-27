package com.stcl.htm.experiments.rps;

import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.network.HTMNetwork;

import stcl.algo.poolers.Sequencer;

public class SequenceRunner_HumanStrategy extends SequenceRunner {

	public SequenceRunner_HumanStrategy(SimpleMatrix[] possibleInputs,
			Random rand, double noiseMagnitude) {
		super(possibleInputs, rand, noiseMagnitude);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public double[] runEpisode_random(){
		double totalPredictionError = 0;
		double totalGameScore = 0;
		
		//Collect initial prediction and action
		SimpleMatrix[] initialOutput = collectOutput_random();
		SimpleMatrix prediction = initialOutput[0];
		SimpleMatrix actionNextTimeStep = initialOutput[1];
		
		int opponentSymbol = 0;
		
		for (int i = 0; i < sequence.length; i++){
			//Get input			
			SimpleMatrix input = possibleInputs[opponentSymbol];
			
			double predictionError = calculatePredictionError(prediction, input);
			totalPredictionError += predictionError;
			
			SimpleMatrix actionThisTimestep = actionNextTimeStep;
						
			//Calculate reward
			double externalReward = calculateReward(actionThisTimestep, sequence[i]);
			totalGameScore += externalReward;			
			
			//Test if opponent should change its symbol
			if (externalReward > 0.5){
				opponentSymbol++;
				if (opponentSymbol > 2){
					opponentSymbol = 0;
				}
			}
			
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
	@Override
	public double[] runEpisode(HTMNetwork activator, GUI gui){
		double totalPredictionError = 0;
		double totalGameScore = 0;
		double reward_before = 0;
		boolean collectData = activator.getNetwork().getCollectData();
		
		SimpleMatrix actionBefore = new SimpleMatrix(5, 5);
		SimpleMatrix stateBefore = new SimpleMatrix(5, 5);
		
		int opponentSymbol = 0;
		
		int state = 1;
		activator.getNetwork().getActionNode().setPossibleActions(possibleActions);
		
		emptyInput(activator, 0, collectData);
		
		for (int i = 0; i < sequence.length; i++){
			
			//Get input			
			state = opponentSymbol;
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
			
			//Test if opponent should change its symbol
			if (reward_now > 0.5){
				opponentSymbol++;
				if (opponentSymbol > 2){
					opponentSymbol = 0;
				}
			}
			
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

}
