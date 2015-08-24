package com.stcl.htm.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import stcl.algo.brain.Network;
import stcl.algo.brain.nodes.UnitNode;

public class NodeCounter {

	public static void main(String[] args) {
		String experimentFolder = "C:/Users/Simon/Google Drev/Experiments/HTM/rps/1433597079636";
		String topFolder = "C:/Users/Simon/Google Drev/Experiments/HTM/rps/Master data/3-2-5/HTM/Experiments";
		NodeCounter nc = new NodeCounter();
		try {
			//nc.goThroughExperiment(experimentFolder);
			nc.goThroughAllExperiments(topFolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void goThroughAllExperiments(String topFolder) throws IOException{
		File[] files = new File(topFolder).listFiles();
		for (File f : files){
			if (f.isDirectory()){
				goThroughExperiment(f.getAbsolutePath());
			}
		}
		
	}
	
	public void goThroughExperiment(String experimentFolderPath) throws IOException{
		File[] files = new File(experimentFolderPath).listFiles();
		boolean foundFiles = false;
		for (File f : files){
			if (f.isDirectory() && StringUtils.isNumeric(f.getName())){
				//Enter one experiment run folder
				foundFiles = collectNodeCounts(f);
			}
		}
		if (!foundFiles) collectNodeCounts(new File(experimentFolderPath));
	}
	
	private boolean collectNodeCounts(File runDirectory) throws IOException{
		//Collect data		
		File[] files = runDirectory.listFiles();
		int[] finalGenInfo = null;
		ArrayList<int[]> countList = new ArrayList<int[]>();
		for (File f : files){
			if(f.getName().contains("best_performing")){
				//We only look at the genome files
				int[] counts = collectNodeCountFromFile(f.getAbsolutePath());
				
				//Collect generation
				String name = f.getName();
				int firstHyphen = name.indexOf("-", 0);
				int secondHyphen = name.indexOf("-", firstHyphen + 1);
				String generation = name.substring(firstHyphen + 1, secondHyphen);
				int gen = -1;
				if (StringUtils.isNumeric(generation)){
					gen = Integer.parseInt(generation);
					while (countList.size() < gen + 1) countList.add(null);
					countList.set(gen, counts);
				} else {
					finalGenInfo = counts;
				}
			}
		}
		
		if (!countList.isEmpty()){
			countList.add(finalGenInfo);
			
			//Create csv file to write information to
			File dataFile = new File(runDirectory.getAbsolutePath() + "/nodecounts.csv");
			FileWriter writer = new FileWriter(dataFile);
			
			String s = "Generation;";
			
			for (int i = 0; i < countList.get(0).length; i++) s += "Level " + (i+1) + ";";
			writer.write(s + "\n");
			
			for (int i = 0; i < countList.size(); i++){
				s = i + ";";
				for (int j : countList.get(i)) s += j + ";";
				writer.write(s + "\n");
			}
			
			writer.close();
			return true;
		}
		return false;
	}
	
	private int[] collectNodeCountFromFile(String networkFileName) throws FileNotFoundException{
		Network n = new Network(networkFileName, new Random());
		ArrayList<UnitNode> nodes = n.getUnitNodes();
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		int maxLevel = 0;
		for (UnitNode node : nodes){
			int[] coordinates = node.getCoordinates();
			int level = coordinates[2];
			if (level > maxLevel) maxLevel = level;
			Integer i = map.get(level);
			int count = 1;
			if (i != null){
				count += i;
			}
			map.put(level,count);
		}
		
		int[] counts = new int[maxLevel + 1];
		
		for (Integer i : map.keySet()){
			int count = map.get(i);
			counts[i] = count;
		}
		
		return counts;
		
	}

	

}
