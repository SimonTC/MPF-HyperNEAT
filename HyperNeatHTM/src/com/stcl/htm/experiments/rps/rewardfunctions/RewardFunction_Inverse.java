package com.stcl.htm.experiments.rps.rewardfunctions;

import org.ejml.simple.SimpleMatrix;

/**
 * Implementation of the RewardFunction class that follows the inverse rules of RPS (Scissors > Rock > Paper > Scissors)
 * @author Simon
 *
 */
public class RewardFunction_Inverse extends RewardFunction {

	@Override
	protected void initializeRewardMatrix() {
		double[][]data = {
				{0.5,1,0},
				{0,0.5,1},
				{1,0,0.5}
		};
		
		rewardMatrix = new SimpleMatrix(data);

	}

}
