/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * A made-up tree of any size, for the tests that need a big file (the timings of {@link LargeFileBenchmark}) or one full of cousins that
 * marry (the equivalence of two ways to compute the consanguinity).
 * <p>
 * It is a closed community: a stable population whose generations are born from the couples of the previous one, which marry mostly
 * inside their own village. It is the worst case for the coefficients of Wright, a lot of pedigree collapse. It is the same every time
 * for the same seed.
 * </p>
 */
public final class SyntheticTree{

	private static final String[] GIVEN = {"Anna", "Bruno", "Carla", "Dario", "Elena", "Franco", "Giulia", "Hugo", "Irene", "Luca",
		"Maria", "Nino", "Olga", "Piero", "Rosa", "Sergio", "Teresa", "Ugo", "Vera", "Zeno"};
	private static final String[] SURNAME = {"Rossi", "Bianchi", "Verdi", "Neri", "Gallo", "Conti", "Riva", "Costa", "Fabbri", "Moretti",
		"Serra", "Ferri", "Longo", "Bruno", "Greco"};
	/** How many individuals (by identifier) make a village. */
	private static final int VILLAGE = 40;


	private SyntheticTree(){}


	/**
	 * @param size           how many individuals
	 * @param perGeneration  how many individuals in each generation
	 * @param seed           what the random choices start from
	 * @return	The tree as a GEDCOM text: individuals with birth, death and place, and the families with their children.
	 */
	public static String gedcom(final int size, final int perGeneration, final long seed){
		final Random random = new Random(seed);
		final StringBuilder sb = new StringBuilder("0 HEAD\n1 CHAR UTF-8\n");
		final Map<String, StringBuilder> families = new LinkedHashMap<>();

		int nextIndividual = 1;
		int nextFamily = 1;
		List<Integer> previous = new ArrayList<>();
		for(int generation = 0; nextIndividual <= size; generation ++){
			final List<Integer> current = new ArrayList<>();
			final int year = 1650 + generation * 25;

			//the family each individual of the generation is born in (the first generation has no parents)
			final List<String> familyOfEach = new ArrayList<>();
			if(previous.isEmpty())
				for(int i = 0; i < perGeneration && nextIndividual + i <= size; i ++)
					familyOfEach.add(null);
			else{
				final List<Integer> males = new ArrayList<>();
				final List<Integer> females = new ArrayList<>();
				for(final int id : previous)
					(id % 2 == 0? males: females).add(id);
				Collections.shuffle(males, random);
				Collections.shuffle(females, random);
				//villages: sorted by village, the partner is taken from the same village most of the time
				males.sort((a, b) -> Integer.compare(a / VILLAGE, b / VILLAGE));
				females.sort((a, b) -> Integer.compare(a / VILLAGE, b / VILLAGE));
				int placed = 0;
				for(int m = 0; m < males.size() && placed < perGeneration && !females.isEmpty(); m ++){
					final int husband = males.get(m);
					final int wifeIndex = (random.nextInt(10) < 8
						? Math.min(females.size() - 1, m + random.nextInt(3))
						: random.nextInt(females.size()));
					final int wife = females.get(wifeIndex);
					final String family = "F" + nextFamily ++;
					families.put(family, new StringBuilder("0 @").append(family).append("@ FAM\n1 HUSB @I").append(husband)
						.append("@\n1 WIFE @I").append(wife).append("@\n1 MARR\n2 DATE ").append(year - 5 + random.nextInt(4))
						.append('\n'));
					final int children = 2 + random.nextInt(2);
					for(int c = 0; c < children && placed < perGeneration && nextIndividual + placed <= size; c ++, placed ++)
						familyOfEach.add(family);
				}
			}

			for(final String family : familyOfEach){
				if(nextIndividual > size)
					break;

				final int id = nextIndividual ++;
				sb.append("0 @I").append(id).append("@ INDI\n1 NAME ").append(GIVEN[random.nextInt(GIVEN.length)]).append(" /")
					.append(SURNAME[random.nextInt(SURNAME.length)]).append("/\n1 SEX ").append(id % 2 == 0? 'M': 'F')
					.append("\n1 BIRT\n2 DATE ").append(year + random.nextInt(10)).append("\n2 PLAC Villa ").append(id / VILLAGE)
					.append("\n1 DEAT\n2 DATE ").append(year + 40 + random.nextInt(40)).append('\n');
				if(family != null){
					sb.append("1 FAMC @").append(family).append("@\n");
					families.get(family).append("1 CHIL @I").append(id).append("@\n");
				}
				current.add(id);
			}
			if(current.isEmpty())
				break;
			previous = current;
		}
		families.values().forEach(sb::append);
		return sb.toString();
	}

	/**
	 * @param size  how many individuals
	 * @return	The model of a tree of that size, in generations of a sixteenth of it.
	 */
	public static FLEFModel model(final int size) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom(size, Math.max(8, size / 16), 42L)));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

}
