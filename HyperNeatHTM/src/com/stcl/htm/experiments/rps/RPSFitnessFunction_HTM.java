package com.stcl.htm.experiments.rps;

import java.util.Random;

import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import stcl.algo.util.Orthogonalizer;

import com.anji.integration.Activator;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMBrain;

public class RPSFitnessFunction_HTM extends HyperNEATFitnessFunction {

	private SimpleMatrix[] sequence;
	private int[] labelSequence;
	private int[] lblCounter;
	private SimpleMatrix rewardMatrix;
	private int learningIterations = 100;
	private int trainingIterations = 1000;
	private int evaluationIterations = 100;
	private Random rand;
	
	/**
	 * See <a href=" {@docRoot} /params.htm" target="anji_params">Parameter Details </a> for specific property settings.
	 * 
	 * @param props configuration parameters
	 */
	public void init(Properties props) {
		super.init(props);
		Randomizer randomizer = new Randomizer();
		randomizer.init(props);
		rand = randomizer.getRand();
	}
	
	@Override
	public void initialiseEvaluation() {
		createInputs();
		//createRewardMatrix();
		createRewardMatrix_Scaled();
	}
	
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		HTMBrain brain = (HTMBrain) activator;
		//Show good and bad actions
		runLearning(learningIterations, brain);
		
		//Let it train
		runExperiment(trainingIterations, brain);
		
		//Evaluate
		brain.getNetwork().getActionNode().setExplorationChance(0);
		brain.getNetwork().setLearning(false);
		brain.reset();
		double[] scores = runExperiment(evaluationIterations, brain);
		double fitness = scores[1];
		genotype.setPerformanceValue(fitness);
		return fitness;
	}
	
	private double[] runExperiment(int maxIterations, HTMBrain activator){
		int curInput = 0;
		double externalReward = 0;
		
		double[][] tmp = {{1,0,0}};
		SimpleMatrix actionNextTimeStep = new SimpleMatrix(tmp); //m(t)
		//SimpleMatrix actionAfterNext = new SimpleMatrix(tmp); //m(t+2)

		SimpleMatrix prediction = sequence[0];
		
		double totalPredictionError = 0;
		double totalGameScore = 0;
		
		for (int i = 0; i < maxIterations; i++){
			//if (i % 500 == 0) System.out.println("Iteration: " + i);
			
			//Get input			
			SimpleMatrix input = new SimpleMatrix(sequence[curInput]);
			
			//Calculate prediction error
			SimpleMatrix diff = input.minus(prediction);
			double predictionError = diff.normF();	
			totalPredictionError += predictionError;
			
			SimpleMatrix actionThisTimestep = actionNextTimeStep;
			double rewardForBeingInCurrentState = externalReward;
			
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
			activator.setInput(inputVector.getMatrix().data);
			activator.setAction(actionThisTimestep.getMatrix().data);
			
			//Do one step
			activator.step(rewardForBeingInCurrentState);
			
			//Collect output
			double[] predictionData = activator.getOutput();
			prediction = new SimpleMatrix(1, predictionData.length, true, predictionData);
			prediction.reshape(5, 5);
			
			double[] actionData = activator.getAction();
			actionNextTimeStep = new SimpleMatrix(1, actionData.length, true, actionData);

			//Set max value of action to 1. The rest to zero
			int max = maxID(actionNextTimeStep);
			if (max != -1){
				actionNextTimeStep.set(0);
				actionNextTimeStep.set(max, 1);
			}				
				
			/*
			if (i > maxIterations - 100){
				actionNode.setExplorationChance(0);
				if (printError) System.out.println(i + " Error: " + predictionError + " Reward: " + externalReward);
			}
			*/
			
			curInput++;
			if (curInput >= sequence.length){
				curInput = 0;
			}			
		}
		
		double avgPredictionError = totalPredictionError / (double) maxIterations;
		double avgScore = totalGameScore / (double) maxIterations;
		
		double[] result = {avgPredictionError, avgScore};
		return result;
	}
	
	private void runLearning(int iterations, HTMBrain activator){
		int[][] positiveExamples = {{0,1},{1,2},{2,0}};
		int[][] negativeExamples = {{1,0},{2,1},{0,2}};
		int[][] neutralExamples = {{0,0},{1,1},{2,2}};
		
		double externalReward = 0;
		SimpleMatrix prediction = sequence[0];
		
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
			SimpleMatrix input = new SimpleMatrix(sequence[inputID]);
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
