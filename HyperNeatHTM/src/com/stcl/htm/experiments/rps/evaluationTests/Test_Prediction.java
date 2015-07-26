package com.stcl.htm.experiments.rps.evaluationTests;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.RPS;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;

public class Test_Prediction extends Test {

	@Override
	public double[] test(HTMNetwork brain) {
		double[] results = new double[numSequences];
		evaluator.run(brain, 0);
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
		RPS eval = new RPS(possibleInputs, sequences, functions,  1, props.getIntProperty(RPS_TRAINING_ITERATIONS_KEY), props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY));
		return eval;
	}

}
