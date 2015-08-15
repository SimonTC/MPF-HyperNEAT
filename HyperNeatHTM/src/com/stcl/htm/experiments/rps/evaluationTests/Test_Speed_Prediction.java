package com.stcl.htm.experiments.rps.evaluationTests;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.RPS;
import com.stcl.htm.experiments.rps.RPS_Speed;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;

public class Test_Speed_Prediction extends Test {
	
	private double predictionThreshold = 0.9;

	@Override
	public double[] test(HTMNetwork brain, double explorationChance, boolean collectGameScores, String scoreFolderName) {
		double[] results = new double[numSequences];
		evaluator.run(brain, explorationChance, collectGameScores, scoreFolderName);
		double[][] sequenceScores = evaluator.getSequenceScores();
		for (int i = 0; i < numSequences; i++){
			results[i] = sequenceScores[i][0];
		}
		return results;
	}

	@Override
	protected RPS setupEvaluator(Properties props, int[][] sequences,
			SimpleMatrix[] possibleInputs) {
		RewardFunction[] functions = {new RewardFunction_Standard()};
		RPS eval = new RPS_Speed(possibleInputs, sequences, functions, props.getIntProperty(RPS_SEQUENCES_ITERATIONS_KEY), props.getIntProperty(RPS_TRAINING_ITERATIONS_KEY), props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY), rand.nextLong(), props.getDoubleProperty(RPS_NOISE_MAGNITUDE), predictionThreshold, 0, 5);
		return eval;
	}
	
	protected void setName(){
		this.testName = "Prediction_Speed";
	}
	

}
