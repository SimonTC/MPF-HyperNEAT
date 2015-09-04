package com.stcl.htm.experiments.rps;

import org.ejml.simple.SimpleMatrix;

import com.stcl.htm.experiments.rps.gui.GUI;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;

public class RPS_HumanStrategy extends RPS {

	public RPS_HumanStrategy(SimpleMatrix[] possibleInputs, int[][] sequences,
			RewardFunction[] rewardFunctions, int numExperimentsPerSequence,
			int trainingIterations, int evaluationIterations, long randSeed,
			double noiseMagnitude) {
		super(possibleInputs, sequences, rewardFunctions,
				numExperimentsPerSequence, trainingIterations,
				evaluationIterations, randSeed, noiseMagnitude);
		this.runner = new SequenceRunner_HumanStrategy(possibleInputs, rand, noiseMagnitude);
	}

	public RPS_HumanStrategy(SimpleMatrix[] possibleInputs, int[][] sequences,
			RewardFunction[] rewardFunctions, int numExperimentsPerSequence,
			int trainingIterations, int evaluationIterations, long randSeed,
			double noiseMagnitude, GUI gui) {
		super(possibleInputs, sequences, rewardFunctions,
				numExperimentsPerSequence, trainingIterations,
				evaluationIterations, randSeed, noiseMagnitude, gui);
		this.runner = new SequenceRunner_HumanStrategy(possibleInputs, rand, noiseMagnitude);
	}

}
