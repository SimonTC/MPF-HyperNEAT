package com.stcl.htm.experiments.rps.evaluationTests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import stcl.algo.brain.Network_DataCollector;

import com.stcl.htm.network.HTMNetwork;

public class TestSuite_SimpleNetwork extends TestSuite {
	
	public static void main(String[] args)  {
		String topFolder = args[0];
		int numSequences = Integer.parseInt(args[1]);
		
		 File dir = new File(topFolder);
		 File[] directoryListing = dir.listFiles();
		 boolean collectScores = Boolean.parseBoolean(args[2]);
		 
		 if (directoryListing.length == 0){
			 System.out.println("No files found in directory " + dir.getAbsolutePath());
		 } else {
		 
			 ExecutorService executor = Executors.newFixedThreadPool(4);
			 try {
				 for (int i = 0; i < directoryListing.length; i++){
					  File f = directoryListing[i];
					  if (f.isDirectory()){
						  String path = f.getAbsolutePath() + "/evaluation";
						  TestSuite ts = new TestSuite_SimpleNetwork(path, numSequences, collectScores);
							executor.execute(ts);
					  }
				  }
				 
				 executor.shutdown();
				 
				 executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			 	} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 
			 
			
			 System.out.println("Finished tests");
		 }
		

	}

	public TestSuite_SimpleNetwork(String testFolder, int numSequences,
			boolean collectGameScores) throws IOException {
		super(testFolder, numSequences, collectGameScores);

	}


	
	@Override
	protected Network_DataCollector buildBrain(String genomeFile){
		Network_DataCollector brain = null;
		try {
			brain = new Network_DataCollector(genomeFile, new Random());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return brain;
	}

}
