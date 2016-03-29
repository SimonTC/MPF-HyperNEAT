package com.stcl.htm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import com.stcl.htm.network.HTMNetwork;

import dk.stcl.core.basic.containers.SomNode;
import stcl.algo.brain.Network_DataCollector;
import stcl.algo.brain.nodes.UnitNode;
import stcl.algo.util.FileWriter;

public class Temp {

	public static void main(String[] args) {
		String file_dir = "/media/simon/Data/Dropbox/ITU/Master thesis/Conference article/Genomes/Only_Q";
		String genomeFile =  file_dir + "/Only_Q.txt";
		String outputFile = file_dir + "/Only_Q_2.txt";
		Random rand = new Random();
		Network_DataCollector brain;
		String description = "";
		try {
			brain = new Network_DataCollector(genomeFile, rand);
			HTMNetwork network = new HTMNetwork(brain);
			UnitNode n = network.getNetwork().getUnitNodes().get(0);
			SomNode[] som_nodes = n.getUnit().getSpatialPooler().getSOM().getNodes();
			SimpleMatrix[] inputs = createInputs();
			for (int i=0; i < 3; i++){
				SomNode node = som_nodes[i];
				SimpleMatrix input = inputs[i];
				SimpleMatrix m = new SimpleMatrix(1, input.getNumElements(), true, input.getMatrix().data);
				node.setVector(m);
				//System.out.println(node.getVector());
			}
			
			n = network.getNetwork().getUnitNodes().get(0);
			System.out.println(n.toInitializationString());
			description = network.toString();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FileWriter writer = new FileWriter(outputFile);
		try {
			writer.openFile(false);
			writer.write(description);
			writer.closeFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private static SimpleMatrix[] createInputs(){
		double[][] rockData = {
				{0,0,0,0,0},
				{0,1,1,1,0},
				{0,1,1,1,0},
				{0,1,1,1,0},
				{0,0,0,0,0}
		};
		
		SimpleMatrix rock = new SimpleMatrix(rockData);
		
		double[][] paperData = {
				{1,1,1,1,1},
				{1,1,1,1,1},
				{1,1,1,1,1},
				{1,1,1,1,1},
				{1,1,1,1,1}
		};
		
		SimpleMatrix paper = new SimpleMatrix(paperData);
		
		double[][] scissorsData = {
				{0,0,0,1,0},
				{1,0,1,0,0},
				{0,1,0,0,0},
				{1,0,1,0,0},
				{0,0,0,1,0}
		};
		
		SimpleMatrix scissors = new SimpleMatrix(scissorsData);		

		SimpleMatrix[] tmp = {rock, paper, scissors};

		return tmp;
	}


}
