package com.stcl.htm.experiments.mario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.jgapcustomised.Chromosome;

import stcl.algo.brain.Network;

import com.anji.integration.Activator;
import com.stcl.htm.network.HTMNetwork;

public class Simulator extends MarioFitnessFunction{

	String agentFilePath = "C:/Users/Simon/Documents/Experiments/HTM/mario/1428734968087/best_performing-0-1079.txt";
	Random rand = new Random(1234);
	
	public static void main(String[] args) {
		Simulator sim = new Simulator();
		sim.run();

	}
	
	public void run(){
		
		try {
			String agentFile = readFile(agentFilePath);
			//Create agent
			Network brain = new Network(agentFile, rand);
			HTMNetwork network = new HTMNetwork(brain);
			this.evaluate(null, network,1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	@Override
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		double fitness = super.evaluate(genotype, activator, threadIndex);
		
		//Change all level parmeters to be visual
		for (int i = 0; i < levelParameters.length; i++){
			String s = levelParameters[i];
			s = s.replace("-vis off", "-vis on");
		}
		
		//Choose random level to visualize
		int levelID = rand.nextInt(levelParameters.length);
		String parameter = levelParameters[levelID];
		
		//Run one level
		runNormalRound(agent, parameter);
		
		return fitness;
	}
	
	private String readFile( String file ) throws IOException {
	    BufferedReader reader = new BufferedReader( new FileReader (file));
	    String         line = null;
	   String s = "";
	    // StringBuilder  stringBuilder = new StringBuilder();
	    String         ls = System.getProperty("line.separator");

	    while( ( line = reader.readLine() ) != null ) {
	        s += line;
	        s+= ls;
	    	//stringBuilder.append( line );
	       // stringBuilder.append( ls );
	    }
	    reader.close();
	    return s;//stringBuilder.toString();
	}

}
