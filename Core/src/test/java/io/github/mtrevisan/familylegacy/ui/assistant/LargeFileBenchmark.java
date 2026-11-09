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

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguineousUnionDetector;
import io.github.mtrevisan.familylegacy.consanguinity.ConsanguinityReport;
import io.github.mtrevisan.familylegacy.consanguinity.KinshipCalculator;
import io.github.mtrevisan.familylegacy.ego.EgoNetworkService;
import io.github.mtrevisan.familylegacy.ego.EgoNode;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.tree.GenealogyRepository;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.ChronologyAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.citation.CitationAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.conclusion.ConclusionAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.consanguinity.ConsanguinityAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.context.ContextAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.identity.IdentityAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.place.PlaceAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.planner.PlannerAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.relationship.RelationshipAssistantContributor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;


/**
 * Times the heavy computations of the application on a big file (tens of thousands of individuals), to see what has to stay off the
 * event dispatch thread. It does not assert anything and it is skipped unless it is asked for:
 * {@code mvn -pl Core test -Dtest=LargeFileBenchmark -Dfamilylegacy.benchmark=true [-Dfamilylegacy.benchmark.size=30000]}.
 * <p>
 * The tree is the one of {@link SyntheticTree}: a closed community, the worst case for the coefficients of Wright (a lot of pedigree
 * collapse).
 * </p>
 */
class LargeFileBenchmark{

	private static <T> T timed(final String what, final Supplier<T> work){
		final long start = System.nanoTime();
		final T result = work.get();
		System.out.printf("BENCH %-52s %8d ms%n", what, (System.nanoTime() - start) / 1_000_000L);
		return result;
	}


	@Test
	void timings() throws Exception{
		Assumptions.assumeTrue(Boolean.getBoolean("familylegacy.benchmark"), "only on request");

		final int size = Integer.getInteger("familylegacy.benchmark.size", 30_000);
		final FLEFModel model = timed("build the model (%d individuals)".formatted(size), () -> {
			try{
				return SyntheticTree.model(size);
			}
			catch(final IOException ioe){
				throw new IllegalStateException(ioe);
			}
		});
		System.out.printf("BENCH records: %d%n", model.getRecordCount());

		final ConsanguinityReport report = timed("ConsanguineousUnionDetector.detect (whole file)",
			() -> ConsanguineousUnionDetector.detect(model));
		System.out.printf("BENCH unions: %d, inbred: %d%n", report.unions().size(), report.inbredIndividuals().size());

		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualReader.TYPE);
		final String egoId = individuals.get(individuals.size() - 1).getId();
		final String otherId = individuals.get(individuals.size() - 2).getId();

		timed("KinshipCalculator.forModel", () -> KinshipCalculator.forModel(model));
		final KinshipCalculator kinship = KinshipCalculator.forModel(model);
		timed("KinshipCalculator.calculate (two of the last generation)", () -> kinship.calculate(egoId, otherId));

		final EgoNetworkService ego = new EgoNetworkService(new GenealogyRepository(RelationshipReader.FAMILY, model), model);
		//the first ego with a partner: the whole file is looked at
		String withPartner = null;
		for(int i = individuals.size() - 1; i >= 0 && withPartner == null; i --){
			final String id = individuals.get(i).getId();
			final EgoNode node = ego.buildEgoNetwork(id);
			if(!node.getRelatedNodes(EgoNode.RelationshipCategory.PARTNER).isEmpty())
				withPartner = id;
		}
		final String egoWithPartner = withPartner;
		//a service that has not looked at the file yet: the first ego with a partner pays for it
		final EgoNetworkService fresh = new EgoNetworkService(new GenealogyRepository(RelationshipReader.FAMILY, model), model);
		timed("EgoNetworkService.buildEgoNetwork (first, with a partner)", () -> fresh.buildEgoNetwork(egoWithPartner));
		timed("EgoNetworkService.buildEgoNetwork (next ego)", () -> fresh.buildEgoNetwork(otherId));

		final AssistantContext context = new AssistantContext(null, null, model, null);
		final List<AssistantContributor> contributors = List.of(new ChronologyAssistantContributor(),
			new ConclusionAssistantContributor(), new RelationshipAssistantContributor(), new ConsanguinityAssistantContributor(),
			new IdentityAssistantContributor(), new GapAssistantContributor(), new CitationAssistantContributor(),
			new PlaceAssistantContributor(), new PlannerAssistantContributor(), new ContextAssistantContributor());
		for(final AssistantContributor contributor : contributors)
			for(final DiagnosticProvider provider : contributor.diagnosticProviders()){
				final List<Diagnostic> found = timed("diagnose: " + contributor.getClass().getSimpleName(),
					() -> provider.diagnose(context));
				System.out.printf("BENCH   findings: %d%n", found.size());
			}

		final ModelHistory history = timed("new ModelHistory (the first snapshot)", () -> new ModelHistory(model));
		timed("ModelHistory.checkpoint (nothing changed)", () -> {
			history.checkpoint();
			return null;
		});
	}

}
