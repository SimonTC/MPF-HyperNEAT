package com.stcl.htm.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import stcl.algo.brain.Network;
import stcl.algo.brain.Network_DataCollector;
import stcl.algo.util.FileWriter;

public class NetworkVisualizer {
	private final int nodeSize = 10;
	private final int width = 100;

	public static void main(String[] args){
		String experimentRun = "C:/Users/Simon/Google Drev/Experiments/HTM/rps/1437817450998/0";
		String genomeFile = experimentRun + "/best_performing-final-25275.txt";;
		String outputFolder = experimentRun;
		try {
			Network network = new Network(genomeFile, new Random());
			NetworkVisualizer nv = new NetworkVisualizer();
			nv.visualizeNetwork(network, outputFolder);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public void visualizeNetwork(Network network, String outputFolder){
		try {
			String[] visualizationStrings = {network.toVisualString(nodeSize,width, false), network.toVisualString(nodeSize,width, true)};
			FileWriter[] writers = {new FileWriter(outputFolder + "//network_viz_2d.layout"),new FileWriter(outputFolder + "//network_viz_3d.layout") };
			for (int i = 0; i < 2; i++){
				FileWriter fw = writers[i];
				fw.openFile(false);
				fw.write(visualizationStrings[i]);
				fw.closeFile();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
