package com.stcl.htm.experiments.rps.evaluationTests;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.RPS;
import com.stcl.htm.experiments.rps.RPS_HumanStrategy;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;

public class Test_HumanStrategy extends Test_Normal {

	@Override
	protected RPS setupEvaluator(Properties props, int[][] sequences,
			SimpleMatrix[] possibleInputs) {
		RewardFunction[] functions = {new RewardFunction_Standard()};
		RPS eval = new RPS_HumanStrategy(possibleInputs, sequences, functions,  props.getIntProperty(RPS_SEQUENCES_ITERATIONS_KEY), props.getIntProperty(RPS_TRAINING_ITERATIONS_KEY), props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY), rand.nextLong(), props.getDoubleProperty(RPS_NOISE_MAGNITUDE));
		return eval;
	}
	
	protected void setName(){
		this.testName = "Human_Strategy";
	}

}
