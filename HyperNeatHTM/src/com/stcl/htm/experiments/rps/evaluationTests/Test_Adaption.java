package com.stcl.htm.experiments.rps.evaluationTests;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.RPS;
import com.stcl.htm.experiments.rps.RPS_Adaption;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Inverse;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;
import com.stcl.htm.network.HTMNetwork;

public class Test_Adaption extends Test_Normal {
	
	private int[][] sequences_changed;
	private boolean changeSequences;
	
	public Test_Adaption(int[][] sequences_changed, boolean changeSequences) {
		this.changeSequences = changeSequences;
		this.sequences_changed = sequences_changed;
	}

	@Override
	protected RPS setupEvaluator(Properties props, int[][] sequences,
			SimpleMatrix[] possibleInputs) {
		RewardFunction[] functions = {new RewardFunction_Standard(), new RewardFunction_Inverse()};
		RPS eval = new RPS_Adaption(possibleInputs, sequences, sequences_changed, functions,  props.getIntProperty(RPS_SEQUENCES_ITERATIONS_KEY), props.getIntProperty(RPS_TRAINING_ITERATIONS_KEY), props.getIntProperty(RPS_EVALUATION_ITERATIONS_KEY), rand.nextLong(), props.getDoubleProperty(RPS_NOISE_MAGNITUDE), changeSequences);
		return eval;
	}
	
	protected void setName(){
		if (changeSequences){
			this.testName = "Adaption_SequenceChange";
		} else {
			this.testName = "Adaption_RuleChange";
		}
	}

	

}
