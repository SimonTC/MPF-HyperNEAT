package com.stcl.htm.experiments.mario;

import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import com.anji.integration.Activator;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class MarioFitnessFunction extends MarioFitnessFunction_Incremental {
	
	private static final long serialVersionUID = 1L;
	private int levelLength = 256;
	private int receptiveFieldSize = 5; //TODO: Parameter	
	
	@Override
	protected ArrayList<String[]> createLevelSet(Random levelRand, int numLevels){
		ArrayList<String[]> set = new ArrayList<String[]>();
		String flatNoBlock = "-vis off -lb off -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String flatBlocks = "-vis off -lb on -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String withGaps = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb off";
		String base = flatBlocks + " -ll " + levelLength + " -rfw " + receptiveFieldSize + " -rfh " + receptiveFieldSize;
		//String base = "-vis off -lb on -lca on -lco on -lde on -lf off -lg on -lhs on -ltb on -ll " + levelLength;
		//String base = "-vis off -ld 2 -lb off -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off -ll " + levelLength;
		//String base = "-vis off -lb on -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off -ll " + levelLength;
		set.add(createLevels(base,levelRand, numLevels));
		return set;
	}
}
