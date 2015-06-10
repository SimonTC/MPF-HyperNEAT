package com.stcl.htm.experiments.rps;

import java.util.Random;

import org.apache.log4j.Logger;
import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;
import stcl.algo.util.Orthogonalizer;

import com.anji.integration.Activator;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.evaluation.BulkFitnessFunctionMT;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.sequencecreation.SequenceBuilder;
import com.stcl.htm.network.HTMNetwork;

public class RPSFitnessFunction_Sequential extends RPSFitnessFunction_HTM {


	private static Logger logger = Logger.getLogger(RPSFitnessFunction_Sequential.class);

	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		RPS eval = new RPS_sequential(possibleInputs, sequences, rewardMatrix, rand.nextLong(), learningIterations, trainingIterations, evaluationIterations, numDifferentSequences, numIterationsPerSequence);
		long start = System.currentTimeMillis();
		double avgFitness = eval.evaluate(genotype, activator, threadIndex);		
		double duration = (System.currentTimeMillis() - start) / 1000d;
		if (logTime) logger.info("Evaluation of genotype " + genotype.getId() + " on thread " + threadIndex + " took: " + duration + " seconds. It started at " + start);
		return avgFitness;
	}
}
