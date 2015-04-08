package com.stcl.htm.experiments.mario;

import java.util.Random;

import org.jgapcustomised.Chromosome;

import vikrasim.agents.MPFAgent;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import com.anji.integration.Activator;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class MarioFitnessFunction extends HyperNEATFitnessFunction {

	private static final long serialVersionUID = 4426806925845602500L;
	private Random rand;
	private String[] levelParameters;
	private int numLevels = 5; //TODO: Take from parameter
	private int difficulty = 2; //TODO: Should grow the better the agents are
	private int numTrainingLevels = 2;
	private int levelLength = 256;

	/**
	 * See <a href=" {@docRoot} /params.htm" target="anji_params">Parameter Details </a> for specific property settings.
	 * 
	 * @param props configuration parameters
	 */
	public void init(Properties props) {
		super.init(props);
		Randomizer randomizer = new Randomizer();
		randomizer.init(props);
		rand = randomizer.getRand();
	}
	
	@Override
	public void initialiseEvaluation() {
		createLevels();
	}
	
	private void createLevels(){
		levelParameters = new String[numLevels];
		String base = "-vis off -echo off -lb off -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off -ll " + levelLength;
		for (int i = 0; i < numLevels; i++){
			String s = base + " -ls " + rand.nextInt(100);
			levelParameters[i] = s;
		}
	}
	
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		HTMNetwork brain = (HTMNetwork) activator;
		
		ScannerAgent agent = new ScannerAgent("Scanner", brain, 1, 1, 7, 7);
		
		//Training
		brain.getNetwork().getActionNode().setExplorationChance(0.05);
		for (int level = 0; level < numTrainingLevels; level++){
			String levelParams = levelParameters[rand.nextInt(levelParameters.length)];
			runNormalRound(agent, levelParams);
		}
		
		//Evaluation
		brain.reset();
		brain.getNetwork().getActionNode().setExplorationChance(0.0);
		brain.getNetwork().setLearning(false);
		int travelDistance = 0;
		for (int level = 0; level < numLevels; level++){
			String levelParams = levelParameters[level];
			int[] ev = runNormalRound(agent, levelParams);
			travelDistance += ev[0];
		}
		
		double fitness = travelDistance / (double) numLevels;
		fitness = fitness / (double) levelLength; 
		genotype.setPerformanceValue(fitness);
		return fitness;
	}
	
	private int[] runNormalRound(ScannerAgent agent, String levelOptions){
		Environment environment = MarioEnvironment.getInstance();
		environment.reset(levelOptions);
		
		int distanceNow = 0;
		int distanceBefore = 0;
		boolean[] action = null;
		while (!environment.isLevelFinished()) {			
			environment.tick(); // Execute one tick in the game //STC
			int[] ev = environment.getEvaluationInfoAsInts();
			distanceNow = ev[0];
			double reward = distanceNow - distanceBefore;
			agent.giveReward(reward);
			agent.integrateObservation(environment);
			action = agent.getAction();
			distanceBefore = distanceNow;
			environment.performAction(action);				
		}
		
		int[] ev = environment.getEvaluationInfoAsInts();
		return ev;
	}

}
