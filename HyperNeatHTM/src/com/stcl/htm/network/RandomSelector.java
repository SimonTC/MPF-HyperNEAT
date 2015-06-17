package com.stcl.htm.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jgapcustomised.Chromosome;
import org.jgapcustomised.Configuration;
import org.jgapcustomised.NaturalSelector;
import org.jgapcustomised.Species;

import com.anji.integration.SimpleSelector;

public class RandomSelector extends SimpleSelector {

	@Override
	/**
	 * Modified version of {@link com.anji.integration.SimpleSelector#select(Configuration)} that
	 * selects a number of parents randomly
	 */
	public List<Chromosome> select(Configuration config) {
		List<Chromosome> result = new ArrayList<Chromosome>();

		int numToSelect = (int) Math.round(numChromosomes * getSurvivalRate());
		if (numToSelect > 0) {
			Collections.shuffle(chromosomes, config.getRandomGenerator());
			Iterator<Chromosome> it = chromosomes.iterator();
			while (it.hasNext() && result.size() < numToSelect) {
				Chromosome c = it.next();
				if (!(result.contains(c))) {
					result.add(c);
				}
			}
		}

		return result;
	}

}
