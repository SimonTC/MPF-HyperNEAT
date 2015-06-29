package com.stcl.htm.experiments.rps;

import org.ejml.simple.SimpleMatrix;

import com.stcl.htm.network.HTMNetwork;

public class RPS_sequential extends RPS {

	public RPS_sequential(SimpleMatrix[] possibleInputs, int[][] sequences,
			SimpleMatrix rewardMatrix, long randSeed, int learningIterations,
			int trainingIterations, int evaluationIterations,
			int numDifferentSequences, int numIterationsPerSequence) {
		super(possibleInputs, sequences, rewardMatrix, randSeed,
				learningIterations, trainingIterations, evaluationIterations,
				numDifferentSequences, numIterationsPerSequence);
	}
	
	@Override
	protected double[] runExperiment(int maxIterations, HTMNetwork activator, int[] sequence){
		double externalReward = 0;
		
		double[][] tmp = {{1,0,0}};
		SimpleMatrix actionNextTimeStep = new SimpleMatrix(tmp); //m(t)
		//SimpleMatrix actionAfterNext = new SimpleMatrix(tmp); //m(t+2)

		SimpleMatrix prediction = possibleInputs[0];
		
		double totalPredictionError = 0;
		double totalGameScore = 0;
		int sequenceLength = sequence.length;
		
		
		for (int i = 0; i < maxIterations; i++){
			for (int curInput = 0; curInput < sequenceLength; curInput++){
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
			}
			
			activator.getNetwork().newEpisode();
			if (training) activator.getNetwork().getActionNode().setExplorationChance(1 - i / maxIterations);

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


}