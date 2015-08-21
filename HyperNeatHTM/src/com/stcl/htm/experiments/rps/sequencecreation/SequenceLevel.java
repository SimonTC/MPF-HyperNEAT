package com.stcl.htm.experiments.rps.sequencecreation;

import java.util.ArrayList;
import java.util.Random;


public class SequenceLevel{
	private int[][] blocks;
	private SequenceLevel child;
	private Random rand;
	private int alphabetSize;
	
	public SequenceLevel(int alphabetSize, int minBlockLength, int maxBlockLength, SequenceLevel child, Random rand) {
		this.child = child;
		this.alphabetSize = alphabetSize;
		this.rand = rand;
		blocks = createLevelBlocks(alphabetSize, minBlockLength, maxBlockLength, rand);		
	}
	
	/**
	 * Creates a deep copy of the given level
	 * @param original
	 */
	public SequenceLevel(SequenceLevel original){
		int[][] originalBlocks = original.getBlocks();
		blocks = copyLevelBlocks(originalBlocks);
		SequenceLevel originalChild = original.getChild();
		if (originalChild == null){
			this.child = originalChild;
		} else {
			this.child = new SequenceLevel(original.getChild());
		}
	}
	
	public void randomizeLevel(){
		for (int[] block : blocks){
			for (int i = 0; i < block.length; i++){
				int newSymbol = rand.nextInt(alphabetSize);
				block[i] = newSymbol;
			}
		}
	}
	
	public int[] unpackBlock(int blockID){
		int[] block = blocks[blockID];
		
		if (child == null) return block;
		ArrayList<int[]> blockList = new ArrayList<int[]>();
		int totalLength = 0;
		for (int i : block){
			int[] childBlock = child.unpackBlock(i);
			blockList.add(childBlock);
			totalLength += childBlock.length;
		}
		
		int[] unpackedBlock = new int[totalLength];
		int counter = 0;
		for (int[] childBlock : blockList){
			for (int i : childBlock){
				unpackedBlock[counter] = i;
				counter++;
			}
		}
		
		return unpackedBlock;
	}
	
	
	private int[][] createLevelBlocks(int alphabetSize, int minBlockLength, int maxBlockLength, Random rand){
		int numBlocks = alphabetSize;
		int[][] blocks = new int[numBlocks][];
		
		for (int blockID = 0; blockID < numBlocks; blockID++){
			int blockLength = minBlockLength + rand.nextInt(maxBlockLength - minBlockLength + 1);
			int[] block = new int[blockLength];
			for (int i = 0; i < blockLength; i++){
				block[i] = rand.nextInt(alphabetSize);
			}
			blocks[blockID] = block;
		}
		
		return blocks;		
	}
	
	private int[][] copyLevelBlocks(int[][] originalBlocks){
		int numBlocks = originalBlocks.length;
		int[][] blocks = new int[numBlocks][];
		
		for (int blockID = 0; blockID < numBlocks; blockID++){
			int blockLength = originalBlocks[blockID].length;
			int[] block = new int[blockLength];
			for (int i = 0; i < blockLength; i++){
				block[i] = originalBlocks[blockID][i];
			}
			blocks[blockID] = block;
		}
		
		return blocks;		
	}
	
	public SequenceLevel getChild(){
		return child;
	}
	
	public int[][] getBlocks(){
		return blocks;
	}
}
