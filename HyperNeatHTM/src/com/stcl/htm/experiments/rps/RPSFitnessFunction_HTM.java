package com.stcl.htm.experiments.rps;

import java.util.Random;

import org.apache.log4j.Logger;
import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;
import stcl.algo.util.Orthogonalizer;

import com.anji.integration.Activator;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.evaluation.BulkFitnessFunctionMT;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.sequencecreation.SequenceBuilder;
import com.stcl.htm.network.HTMNetwork;

public class RPSFitnessFunction_HTM extends HyperNEATFitnessFunction {
	
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

	private static Logger logger = Logger.getLogger(RPSFitnessFunction_HTM.class);
	
	private SimpleMatrix[] possibleInputs;
	private int[][] sequences;
	private SimpleMatrix rewardMatrix;
	private int learningIterations;
	private int trainingIterations;
	private int evaluationIterations;
	private int numDifferentSequences;
	private int numIterationsPerSequence;
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
		learningIterations = props.getIntProperty(RPS_LEARNING_ITERATIONS_KEY, 100);
		trainingIterations = props.getIntProperty(RPS_TRAINING_ITERATIONS_KEY, 1000);
		evaluationIterations = props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY, 100);
		numDifferentSequences = props.getIntProperty(RPS_SEQUENCES_NUMBER_KEY, 1);
		numIterationsPerSequence = props.getIntProperty(RPS_SEQUENCES_ITERATIONS_KEY, 10);
		
		//Create sequences
		long sequenceSeed = 0;
		try{
			sequenceSeed = props.getLongProperty(RPS_SEQUENCES_RAND_SEED_KEY);
		} catch (IllegalArgumentException e){
			sequenceSeed = rand.nextLong();
			props.setProperty(RPS_SEQUENCES_RAND_SEED_KEY, "" +sequenceSeed);
		}
		
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
	
	@Override
	public void initialiseEvaluation() {
		createInputs();
		createRewardMatrix();
		//createRewardMatrix_Scaled();
	}
	
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		logger.info("Beginning evaluation of genotype " + genotype.getId() + " on thread " + threadIndex);
		HTMNetwork brain = (HTMNetwork) activator;
		String initializationString = brain.toString();
		long seed = rand.nextLong();
		double totalFitness = 0;
		double totalPrediction = 0;
		for (int sequenceID = 0; sequenceID < numDifferentSequences; sequenceID++){
			double sequenceFitness = 0;
			double sequencePrediction = 0;
			int[] curSequence = sequences[sequenceID];
			for (int sequenceIteration = 0; sequenceIteration < numIterationsPerSequence; sequenceIteration++){
				Network network = new Network();
				network.initialize(initializationString, new Random(seed));
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
		logger.info("Ending evaluation of genotype " + genotype.getId() + " on thread " + threadIndex);
		return avgFitness;
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
