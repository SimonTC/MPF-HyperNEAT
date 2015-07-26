package com.stcl.htm.experiments.rps.evaluationTests;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.RPS;
import com.stcl.htm.experiments.rps.RPS_Adaption;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Inverse;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;

public class Test_Adaption extends Test_Fitness {

	@Override
	protected RPS setupEvaluator(Properties props, int[][] sequences,
			SimpleMatrix[] possibleInputs) {
		RewardFunction[] functions = {new RewardFunction_Standard(), new RewardFunction_Inverse()};
		RPS eval = new RPS_Adaption(possibleInputs, sequences, functions,  1, props.getIntProperty(RPS_TRAINING_ITERATIONS_KEY), props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY));
		return eval;
	}

	

}
