package com.stcl.htm.experiments.rps;

import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import stcl.algo.util.Orthogonalizer;

import com.anji.integration.Activator;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;

public class RPSFitnessFunction extends HyperNEATFitnessFunction {

	private SimpleMatrix[] sequence;
	private int[] labelSequence;
	private int[] lblCounter;
	private SimpleMatrix rewardMatrix;
	private int trainingIterations = 1000;
	
	/**
	 * See <a href=" {@docRoot} /params.htm" target="anji_params">Parameter Details </a> for specific property settings.
	 * 
	 * @param props configuration parameters
	 */
	public void init(Properties props) {
		super.init(props);
	}
	
	@Override
	public void initialiseEvaluation() {
		createInputs();
		//createRewardMatrix();
		createRewardMatrix_Scaled();
	}
	
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		double fitness = runExperiment(trainingIterations, activator);
		genotype.setPerformanceValue(fitness);
		return fitness;
	}
	
	private double runExperiment(int maxIterations, Activator activator){
		int curInput = 0;
		double externalReward = 0;
		
		double[][] tmp = {{1,0,0}};
		SimpleMatrix actionNextTimeStep = new SimpleMatrix(tmp); //m(t)
		
		double totalGameScore = 0;
		
		for (int i = 0; i < maxIterations; i++){
			
			//Get input			
			SimpleMatrix input = new SimpleMatrix(sequence[curInput]);
			
			SimpleMatrix actionThisTimestep = actionNextTimeStep;
			
			actionThisTimestep = Orthogonalizer.aggressiveOrthogonalization(actionThisTimestep);
			
			//Calculate reward			
			if ( i > 3){ //To get out of wrong actions
				int actionID = -1;
				if (actionThisTimestep.get(0) > 0.1) actionID = 0; //Using > 0.1 to get around doubles not always being == 0
				if (actionThisTimestep.get(1) > 0.1 ) actionID = 1;
				if (actionThisTimestep.get(2) > 0.1 ) actionID = 2;
				int inputID = labelSequence[curInput];
				externalReward = reward(inputID, actionID);
			}		
			
			totalGameScore += externalReward;			
			
			//Give inputs to brain
			SimpleMatrix inputVector = new SimpleMatrix(1, input.getNumElements(), true, input.getMatrix().data);
						
			//Do one step
			double[] output = activator.next(inputVector.getMatrix().data);
			
			//Collect output
			actionNextTimeStep = new SimpleMatrix(1, output.length, true, output);

			//Set max value of action to 1. The rest to zero
			int max = maxID(actionNextTimeStep);
			if (max != -1){
				actionNextTimeStep.set(0);
				actionNextTimeStep.set(max, 1);
			}				
			
			curInput++;
			if (curInput >= sequence.length){
				curInput = 0;
			}			
		}
		
		double avgScore = totalGameScore / (double) maxIterations;
		
		double result = avgScore;
		return result;
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
		
		SimpleMatrix[] tmp = {rock, paper, paper, scissors, paper, paper, scissors, rock};
		int[] lbl = {0,1,1,2,1,1,2,0};
		int[] lbl_counter = {1,2,2,0,2,2,0,1};
		
		lblCounter = lbl_counter;
		labelSequence = lbl;
		sequence = tmp;			
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
