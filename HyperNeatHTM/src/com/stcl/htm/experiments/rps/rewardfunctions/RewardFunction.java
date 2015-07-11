package com.stcl.htm.experiments.rps.rewardfunctions;

import org.ejml.simple.SimpleMatrix;

/**
 * The reward function class is used to calculate the reward for a given pair of hands in the RPS experiment.
 * The reward is found by looking at a reward matrix.
 * Extend this class by implementing the initializeRewardMatrix() to create the reward matrix used in reward(int,int)
 * @author Simon
 *
 */
public abstract class RewardFunction {
	protected SimpleMatrix rewardMatrix;
	
	public RewardFunction() {
		initializeRewardMatrix();
	}
	
	public double reward(int opponentSymbol, int playerSymbol){
		double reward = rewardMatrix.get(playerSymbol, opponentSymbol);
		return reward;
	}
	
	protected abstract void initializeRewardMatrix();

}
