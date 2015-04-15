package com.stcl.htm.experiments.mario;

import vikrasim.agents.GapAgent;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class MarioFitnessFunction_Teacher extends MarioFitnessFunction_Incremental {

	private static final long serialVersionUID = 1L;
	public static final String KEY_MARIO_TEACHER_FILE = "mario.teacher.file";
	
	@Override
	protected void runTrainingRound(MPFAgent agent, int leveltype){
		HTMNetwork brain = agent.brain;
		brain.reset();
		brain.getNetwork().setLearning(true);
		brain.getNetwork().getActionNode().setExplorationChance(0.05);
		GapAgent teacher = new GapAgent("Teacher", props.getProperty(KEY_MARIO_TEACHER_FILE), 1, 1, 7, 7);
		teacher.createBrain();
		for (int level = 0; level < numTrainingLevels; level++){
			String levelParams = trainingSet.get(leveltype)[level];
			runTeachingRound(agent, levelParams, teacher);
		}
	}
	
	protected int[] runTeachingRound(MPFAgent agent, String levelOptions, GapAgent teacher){
		Environment environment = new MarioEnvironment();
		environment.setAgent(teacher);
		environment.reset(levelOptions);		
		
		int distanceNow = 0;
		int distanceBefore = 0;
		boolean[] action = null;
		while (!environment.isLevelFinished()) {	
			environment.tick(); // Execute one tick in the game //STC
			int[] ev = environment.getEvaluationInfoAsInts();
			distanceNow = ev[0];
			double reward = distanceNow - distanceBefore;
			reward = reward - 0.5; //Punish it for not moving
			agent.giveReward(reward);
			teacher.integrateObservation(environment);
			agent.integrateObservation(environment);
			action = teacher.getAction();
			agent.setAction(action);
			distanceBefore = distanceNow;
			environment.performAction(action);				
		}
		
		int[] ev = environment.getEvaluationInfoAsInts();
		return ev;
	}

}
