package com.stcl.htm.experiments.mario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;

import com.anji.integration.Activator;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class Simulator extends MarioFitnessFunction_Incremental{

	Random rand = new Random(1234);
	
	public static void main(String[] args) throws IOException {
		Simulator sim = new Simulator();
		Properties props = new Properties(args[0]);
		sim.init(props);
		sim.initialiseEvaluation();
		sim.run(args[1]);

	}
	
	public void run(String agentFilePath){
		
		try {
			//Create agent
			Network brain = new Network(agentFilePath, rand);
			HTMNetwork network = new HTMNetwork(brain);
			this.evaluate(null, network,1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	@Override
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		HTMNetwork brain = (HTMNetwork) activator;
		ScannerAgent agent = new ScannerAgent("Scanner", brain, 1, 1, 7, 7);
		double fitness = super.runEvaluation(agent, true, 1);
		
		System.out.println("Fitness: " + fitness);
		
		/*
		//Change all level parameters to be visual
		for (int i = 0; i < levelParameters.length; i++){
			String s = levelParameters[i];
			s = s.replace("-vis off", "-vis on");
			levelParameters[i] = s;
		}
		
		//Choose random level to visualize
		int levelID = rand.nextInt(levelParameters.length);
		String parameter = levelParameters[levelID];
		*/

		//MPFAgent agent = new EnvironmentAgent("Environment", brain,2,2);
		
		//Run one level
		String parameter = levels.get(levels.size()-1)[0]; 
		parameter = parameter.replace("-vis off", "-vis on");
		runNormalRound(agent, parameter);
		
		return fitness;
	}

}
