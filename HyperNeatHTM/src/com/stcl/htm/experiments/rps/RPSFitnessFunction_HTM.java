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
	public static final String RPS_LOG_EVALUATION_TIME_KEY = "rps.log.evaluation.time";

	private static Logger logger = Logger.getLogger(RPSFitnessFunction_HTM.class);
	
	protected SimpleMatrix[] possibleInputs;
	protected int[][] sequences;
	protected SimpleMatrix rewardMatrix;
	protected int learningIterations;
	protected int trainingIterations;
	protected int evaluationIterations;
	protected int numDifferentSequences;
	protected int numIterationsPerSequence;
	protected Random rand;
	protected boolean logTime;
	
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
		logTime = props.getBooleanProperty(RPS_LOG_EVALUATION_TIME_KEY, false);
		
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
		
		createInputs();
		createRewardMatrix();
		//createRewardMatrix_Scaled();
		
	}
	
	@Override
	public void initialiseEvaluation() {
		//No need
	}
	
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		RPS eval = new RPS(possibleInputs, sequences, rewardMatrix, rand.nextLong(), learningIterations, trainingIterations, evaluationIterations, numDifferentSequences, numIterationsPerSequence);
		long start = System.currentTimeMillis();
		double avgFitness = eval.evaluate(genotype, activator, threadIndex);		
		double duration = (System.currentTimeMillis() - start) / 1000d;
		if (logTime) logger.info("Evaluation of genotype " + genotype.getId() + " on thread " + threadIndex + " took: " + duration + " seconds. It started at " + start);
		return avgFitness;
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
