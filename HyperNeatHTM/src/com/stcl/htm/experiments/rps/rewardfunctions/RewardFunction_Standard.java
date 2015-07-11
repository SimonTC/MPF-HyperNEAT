package com.stcl.htm.experiments.rps.rewardfunctions;

import org.ejml.simple.SimpleMatrix;

/**
 * Implementation of the RewardFunction class that follows the normal rules of RPS (Rock > Scissors > Paper > Rock)
 * @author Simon
 *
 */
public class RewardFunction_Standard extends RewardFunction {

	@Override
	protected void initializeRewardMatrix() {
		double[][]data = {
				{0.5,0,1},
				{1,0.5,0},
				{0,1,0.5}
		};
		
		rewardMatrix = new SimpleMatrix(data);
	}

}
