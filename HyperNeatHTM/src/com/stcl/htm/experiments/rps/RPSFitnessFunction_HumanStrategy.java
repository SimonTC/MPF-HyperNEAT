package com.stcl.htm.experiments.rps;

import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;

public class RPSFitnessFunction_HumanStrategy extends RPSFitnessFunction_Fitness {

	@Override
	protected RPS setupEvaluator(){
		RewardFunction[] functions = {new RewardFunction_Standard()};
		RPS eval = new RPS_HumanStrategy(possibleInputs, sequences, functions,  numExperimentsPerSequence, trainingIterations, evaluationIterations, rand.nextLong(), noiseMagnitude);
		return eval;
	}

}
