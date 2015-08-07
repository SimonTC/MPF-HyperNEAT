package com.stcl.htm.experiments.rps;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Inverse;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;

public class RPSFitnessFunction_Adaption extends RPSFitnessFunction_Fitness {
	
	public void init(Properties props) {
		super.init(props);
	}
	
	@Override
	protected RPS setupEvaluator(){
		RewardFunction[] functions = {new RewardFunction_Standard(), new RewardFunction_Inverse()};
		RPS_Adaption eval = new RPS_Adaption(possibleInputs, sequences, functions, numExperimentsPerSequence, trainingIterations, evaluationIterations, rand.nextLong(), noiseMagnitude);
		return eval;
	}

}
