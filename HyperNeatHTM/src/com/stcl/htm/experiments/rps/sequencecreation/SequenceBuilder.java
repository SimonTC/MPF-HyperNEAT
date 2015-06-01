package com.stcl.htm.experiments.rps.sequencecreation;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

public class SequenceBuilder {
	private Stack<SequenceLevel> levels;
	private Random rand;
	private int[] finalSequence;
	private SequenceLevel topLevel;
	/**
	 * Creates blocks of length minBlockLength >= length <= maxBlockLength
	 * @param rand
	 * @param numLevels
	 * @param alphabetSize
	 * @param minBlockLength
	 * @param maxBlockLength
	 * @return
	 */
	public int[] buildSequence(Random rand, int numLevels, int alphabetSize, int minBlockLength, int maxBlockLength ){
				
		this.rand = rand;
		levels = createLevels(numLevels, alphabetSize, minBlockLength, maxBlockLength, rand);
		SequenceLevel topLevel = levels.peek();
		finalSequence = topLevel.unpackBlock(0);
		
		return finalSequence;
	}
	
	public int[] getFinalSequence(){
		return finalSequence;
	}
	
	public Stack<SequenceLevel> getLevels(){
		return levels;
	}
	
	/**
	 * Creates numLevels + 1 levels. The top level is only used to call when writing the sequence 
	 * @param numLevels
	 * @param alphabetSize
	 * @param minBlockLength
	 * @param maxBlockLength
	 * @return The top level
	 */
	private Stack<SequenceLevel>  createLevels(int numLevels, int alphabetSize, int minBlockLength, int maxBlockLength, Random rand){
		Stack<SequenceLevel> levels = new Stack<SequenceLevel>();
		
		SequenceLevel firstLevel = new SequenceLevel(alphabetSize, minBlockLength, maxBlockLength, null, rand);
		levels.push(firstLevel);		
		
		SequenceLevel lastLevel = null;
		for (int i = 0; i < numLevels - 1; i++){
			lastLevel = new SequenceLevel(alphabetSize, minBlockLength, maxBlockLength, levels.peek(), rand);
			levels.push(lastLevel);
		}
		
		topLevel = new SequenceLevel(1, 1, 1, lastLevel, rand);
		levels.push(topLevel);
		return levels;
	}
	
	public SequenceLevel getTopLevel(){
		return topLevel;
	}
	
	
	
	
}
