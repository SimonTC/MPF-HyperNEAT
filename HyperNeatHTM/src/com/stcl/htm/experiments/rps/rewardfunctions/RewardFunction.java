package com.stcl.htm.experiments.rps.rewardfunctions;

import org.ejml.simple.SimpleMatrix;

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
