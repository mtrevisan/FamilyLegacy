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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;


/** The calculations that take time tell how far they are, so that the user knows when the data will be there. */
class ProgressTest{

	/** @return	A model of many individuals who share a few names, so that there are many pairs to compare. */
	private static FLEFModel model(final int individuals) throws IOException{
		final StringBuilder gedcom = new StringBuilder("0 HEAD\n1 CHAR UTF-8\n");
		final String[] names = {"Mario /Rossi/", "Anna /Rossi/", "Pietro /Bianchi/", "Maria /Bianchi/"};
		for(int i = 0; i < individuals; i ++)
			gedcom.append("0 @I").append(i + 1).append("@ INDI\n1 NAME ").append(names[i % names.length])
				.append("\n1 SEX M\n1 BIRT\n2 DATE 1 JAN ").append(1800 + i % 10).append("\n");
		return new GEDCOMToFLEFConverter().convert(new GEDCOMParser().parse(new StringReader(gedcom.toString())));
	}

	private static void assertMonotone(final List<Double> values){
		Assertions.assertFalse(values.isEmpty());
		double previous = 0.;
		for(final double value : values){
			Assertions.assertTrue(value >= 0. && value <= 1., String.valueOf(value));
			Assertions.assertTrue(value >= previous - 1e-12, value + " after " + previous);
			previous = value;
		}
	}


	@Test
	void theSearchForDuplicatesTellsHowFarItIsAndEndsWithAll() throws IOException{
		final List<Double> values = new ArrayList<>();

		DuplicateFinderService.find(model(400), new DuplicateFinderService.Options(), values::add);

		assertMonotone(values);
		Assertions.assertEquals(1., values.getLast(), 1e-12);
		//it reports many times, not only at the end
		Assertions.assertTrue(values.size() > 10, String.valueOf(values.size()));
		//and the first reports are of the description of the individuals, before the comparison of the pairs
		Assertions.assertTrue(values.getFirst() < 0.3, String.valueOf(values.getFirst()));
	}

	@Test
	void theResultOfTheSearchIsTheSameWithOrWithoutTheProgress() throws IOException{
		final FLEFModel model = model(120);

		final List<DuplicateFinderService.Candidate> without = DuplicateFinderService.find(model, new DuplicateFinderService.Options());
		final List<DuplicateFinderService.Candidate> with = DuplicateFinderService.find(model, new DuplicateFinderService.Options(),
			fraction -> {});

		Assertions.assertEquals(without.size(), with.size());
		for(int i = 0; i < with.size(); i ++){
			Assertions.assertEquals(without.get(i).left().id(), with.get(i).left().id());
			Assertions.assertEquals(without.get(i).score(), with.get(i).score());
		}
	}

	@Test
	void aSearchThatIsInterruptedStopsAndGivesNothing() throws Exception{
		final FLEFModel model = model(400);
		final List<DuplicateFinderService.Candidate>[] result = new List[1];
		final Thread thread = new Thread(() -> result[0] = DuplicateFinderService.find(model, new DuplicateFinderService.Options(),
			fraction -> Thread.currentThread().interrupt()));

		thread.start();
		thread.join(30_000);

		Assertions.assertFalse(thread.isAlive());
		Assertions.assertTrue(result[0].isEmpty());
	}

	@Test
	void theAssistantTellsHowFarItIsAndEachProviderIsAnEqualShare(){
		final ResearchAssistantService service = new ResearchAssistantService();
		service.register(new AssistantContributor(){
			@Override
			public List<AssistantAction> actions(){
				return List.of();
			}

			@Override
			public List<DiagnosticProvider> diagnosticProviders(){
				return List.of(
					//a provider that does not tell: it is done when it returns
					context -> List.of(),
					//a provider that does
					new DiagnosticProvider(){
						@Override
						public List<Diagnostic> diagnose(final AssistantContext context){
							return List.of();
						}

						@Override
						public List<Diagnostic> diagnose(final AssistantContext context, final DoubleConsumer progress){
							progress.accept(0.5);
							progress.accept(2.);
							return List.of();
						}
					});
			}
		});
		final List<Double> values = new ArrayList<>();

		service.diagnose(new AssistantContext(null, null, new FLEFModel()), values::add);

		assertMonotone(values);
		//two providers: the first one is half of the work, the second one tells half of its half, then all of it (a value out of range is cut)
		Assertions.assertEquals(List.of(0., 0.5, 0.75, 1., 1.), values);
	}

	@Test
	void theAssistantWithoutAListenerStillWorks(){
		final ResearchAssistantService service = new ResearchAssistantService();

		Assertions.assertTrue(service.diagnose(new AssistantContext(null, null, new FLEFModel())).isEmpty());
	}

}
