package com.stcl.htm.experiments.rps.evaluationTests;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.RPS;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;

public class Test_Normal extends Test {

	@Override
	public double[][] test(HTMNetwork brain, double explorationChance, boolean collectGameScores, String scoreFolderName ) {
		double[][] results = new double[numSequences][];
		evaluator.run(brain, explorationChance, collectGameScores, scoreFolderName);
		double[][] sequenceScores = evaluator.getSequenceScores();
		for (int i = 0; i < numSequences; i++){
			results[i] = sequenceScores[i];
		}
		return results;
	}

	@Override
	protected RPS setupEvaluator(Properties props, int[][] sequences,
			SimpleMatrix[] possibleInputs) {
		RewardFunction[] functions = {new RewardFunction_Standard()};
		RPS eval = new RPS(possibleInputs, sequences, functions,  props.getIntProperty(RPS_SEQUENCES_ITERATIONS_KEY), props.getIntProperty(RPS_TRAINING_ITERATIONS_KEY), props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY), rand.nextLong(), props.getDoubleProperty(RPS_NOISE_MAGNITUDE));
		return eval;
	}
	
	protected void setName(){
		this.testName = "Normal";
	}

}
