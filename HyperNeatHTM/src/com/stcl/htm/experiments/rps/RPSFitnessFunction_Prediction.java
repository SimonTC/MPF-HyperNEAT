package com.stcl.htm.experiments.rps;

import org.jgapcustomised.Chromosome;

public class RPSFitnessFunction_Prediction extends RPSFitnessFunction_Fitness {

	@Override
	protected double collectFitness(double[] result, Chromosome genotype){
		double fitness = result[0]; //Collect prediction
		genotype.setPerformanceValue(fitness);
		genotype.setFitnessValue(fitness);
		return fitness;
	}

}
