package com.stcl.htm.experiments.rps.evaluationTests;

import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.RPS;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.experiments.rps.sequencecreation.SequenceBuilder;
import com.stcl.htm.network.HTMNetwork;

public abstract class Test {

	public static final String RPS_EVALUATION_ITERATIONS_KEY = "rps.evaluation.iterations";
	public static final String RPS_TRAINING_ITERATIONS_KEY = "rps.training.iterations";
	
	protected int[][] sequences;
	protected SimpleMatrix[] possibleInputs;
	protected RPS evaluator;
	protected int numSequences;

	public void setupTest(Properties props, int[][] sequences){
		this.sequences = sequences;
		possibleInputs = createInputs();
		evaluator = setupEvaluator(props, sequences, possibleInputs);
		this.numSequences = sequences.length;
		
	}
	
	private SimpleMatrix[] createInputs(){
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

		return tmp;
	}
	
	public abstract double[] test(HTMNetwork brain);
	
	protected abstract RPS setupEvaluator(Properties props, int[][] sequences, SimpleMatrix[] possibleInputs);
}
