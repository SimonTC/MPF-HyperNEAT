package com.stcl.htm.network;

import org.apache.log4j.Logger;
import org.jgapcustomised.*;

import com.anji.integration.Activator;
import com.anji.integration.Transcriber;
import com.anji.integration.TranscriberException;
import com.anji.nn.activationfunction.ActivationFunction;
import com.anji.nn.activationfunction.ActivationFunctionFactory;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.ojcoleman.ahni.nn.GridNet;
import com.ojcoleman.ahni.transcriber.HyperNEATTranscriber;

/**
 * Constructs a {@link com.ojcoleman.ahni.nn.GridNet} neural network from a chromosome using the hypercube (from HyperNEAT)
 * encoding scheme. An {@link com.anji.integration.ActivatorTranscriber} should be used to construct an instance of this
 * class.
 * 
 * To transcribe the neural network from a {@link Chromosome} a connective pattern producing network (CPPN) is created
 * from the Chromosome, and then this is "queried" to determine the weight of each connection in the neural network. The
 * CPPN is an {@link com.anji.nn.AnjiNet}.
 * 
 * @author Oliver Coleman
 */
public class HyperNEATTranscriberHTMNet extends HyperNEATTranscriber {
	public static final String HYPERNEAT_ACTIVATION_FUNCTION_KEY = "ann.hyperneat.activation.function";

	private final static Logger logger = Logger.getLogger(HyperNEATTranscriberHTMNet.class);

	private ActivationFunction activationFunction;
	private boolean layerEncodingIsInput = false;

	public HyperNEATTranscriberHTMNet() {
	}

	public HyperNEATTranscriberHTMNet(Properties props) {
		init(props);
	}

	public void init(Properties props) {
		super.init(props);
		activationFunction = ActivationFunctionFactory.getInstance().get(props.getProperty(HYPERNEAT_ACTIVATION_FUNCTION_KEY));
	}

	/**
	 * @see Transcriber#transcribe(Chromosome)
	 */
	public GridNet transcribe(Chromosome genotype) throws TranscriberException {
		return newGridNet(genotype, null);
	}

	public GridNet transcribe(Chromosome genotype, Activator substrate) throws TranscriberException {
		return newGridNet(genotype, (GridNet) substrate);
	}

	/**
	 * Create a new neural network from the a genotype.
	 * 
	 * @param genotype chromosome to transcribe
	 * @return phenotype If given this will be updated and returned, if NULL then a new network will be created.
	 * @throws TranscriberException
	 */
	public GridNet newGridNet(Chromosome genotype, HTMBrain phenotype) throws TranscriberException {
		CPPN cppn = new CPPN(genotype);

		int numParameters = 2; //TODO: Figure out how to get this number from the CPPN
		
		int connectionRange = this.connectionRange == -1 ? Integer.MAX_VALUE / 4 : this.connectionRange;

		boolean[][][][][][] connectionMatrix;
		int[][][][] parameterMatrix;
		double[][][] bias;
		boolean createNewPhenotype = (phenotype == null);

		if (createNewPhenotype) {
			connectionMatrix = new boolean[depth - 1][][][][][];
			parameterMatrix = new int[depth - 1][][][];
			for (int l = 1; l < depth; l++){
				parameterMatrix[l - 1] = new int[height[l]][width[l]][numParameters];
				connectionMatrix[l - 1] = new boolean[height[l]][width[l]][1][][];
			}
		} else {
			parameterMatrix = phenotype.getParameterMatrix();
			connectionMatrix = phenotype.getConnectionMatrix();
		}

		// query CPPN for substrate connections and node parameters
		for (int tz = 1; tz < depth; tz++) {
			for (int ty = 0; ty < height[tz]; ty++) {
				for (int tx = 0; tx < width[tz]; tx++) {
					cppn.setTargetCoordinatesFromGridIndices(tx, ty, tz);

					//Decide on spatial and temporal mapsize of node
					cppn.setSourceCoordinatesFromGridIndices(tx, ty, tz);
					cppn.query();
					int spatialMapSize = (int) Math.round(cppn.getRangedNeuronParam(0, 0));
					int temporalMapSize = (int) Math.round(cppn.getRangedNeuronParam(0, 1));
					parameterMatrix[tz][ty][tx][0] = spatialMapSize;
					parameterMatrix[tz][ty][tx][1] = temporalMapSize;

					// calculate dimensions of this weight target matrix
					// (bounded by grid edges)
					int dy = Math.min(height[tz - 1] - 1, ty + connectionRange) - Math.max(0, ty - connectionRange) + 1;
					int dx = Math.min(width[tz - 1] - 1, tx + connectionRange) - Math.max(0, tx - connectionRange) + 1;

					if (createNewPhenotype){
						connectionMatrix[tz - 1][ty][tx][0] = new boolean[dy][dx];
					}
					
					boolean[][] w = connectionMatrix[tz - 1][ty][tx][0];

					// System.out.println("\tsy0 = " + Math.max(0,
					// ty-connectionRange) + ", sx0 = " + Math.max(0,
					// tx-connectionRange));

					// for each connection to zyx
					// w{y,x} is index into weight matrix
					// s{y,x} is index of source neuron
					for (int wy = 0, sy = Math.max(0, ty - connectionRange); wy < dy; wy++, sy++) {
						for (int wx = 0, sx = Math.max(0, tx - connectionRange); wx < dx; wx++, sx++) {
							cppn.setSourceCoordinatesFromGridIndices(sx, sy, tz-1);

							cppn.query();

							// Determine weight for synapse from source to target.
							int cppnOutputIndex = layerEncodingIsInput ? 0 : tz-1;
							
							w[wy][wx] = cppn.getLEO(cppnOutputIndex) ? cppn.getRangedWeight(cppnOutputIndex) : 0;
							
						}
					}
				}
			}
		}


		int[][][] connectionMaxRanges = new int[depth - 1][3][2];
		for (int l = 0; l < depth - 1; l++) {
			connectionMaxRanges[l][0][0] = -1; // no connections to previous or own layer
			connectionMaxRanges[l][0][1] = 1;
			connectionMaxRanges[l][1][0] = connectionRange;
			connectionMaxRanges[l][1][1] = connectionRange;
			connectionMaxRanges[l][2][0] = connectionRange;
			connectionMaxRanges[l][2][1] = connectionRange;
		}
		int[][] layerDimensions = new int[2][depth];
		for (int l = 0; l < depth; l++) {
			layerDimensions[0][l] = width[l];
			layerDimensions[1][l] = height[l];
		}

		if (createNewPhenotype) {
			//phenotype = new GridNet(connectionMaxRanges, layerDimensions, weights, bias, activationFunction, 1, "network " + genotype.getId());
			logger.info("New substrate has input size " + width[0] + "x" + height[0] + " and " + phenotype.getConnectionCount(true) + " connections.");
		} else {
			phenotype.setName("network " + genotype.getId());
		}

		return phenotype;
	}

	/**
	 * @see com.anji.integration.Transcriber#getPhenotypeClass()
	 */
	public Class getPhenotypeClass() {
		return GridNet.class;
	}
}
