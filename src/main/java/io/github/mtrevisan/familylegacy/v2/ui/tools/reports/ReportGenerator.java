package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Orchestrates the generation of a {@link ReportDocument} for a root
 * record, which may be an individual or a group.
 *
 * <p>The set of section builders is chosen dynamically:</p>
 * <ul>
 *   <li><b>common</b> — header, context/research, cultural norms, sources,
 *       media, places, indexes;</li>
 *   <li><b>individual</b> — introduction, life story, ancestry, descendants,
 *       direct/indirect relations, groups of the individual;</li>
 *   <li><b>group</b> — group introduction, group story (members, subgroups,
 *       attributes, events), event participation of the group.</li>
 * </ul>
 */
public final class ReportGenerator{

	private final ReportContext ctx;


	public ReportGenerator(final FLEFModel model, final FLEFRecord root,
		final ReportConfig config){
		this.ctx = ReportContext.build(
			Objects.requireNonNull(model),
			Objects.requireNonNull(root),
			Objects.requireNonNull(config));
	}


	public ReportDocument generate(){
		final List<ReportSection> out = new ArrayList<>();
		for(final SectionBuilder b : builders())
			out.addAll(b.build());

		return new ReportDocument(ctx.displayText(ctx.root), ctx.labels.subtitle(), out);
	}


	private List<SectionBuilder> builders(){
		final List<SectionBuilder> out = new ArrayList<>();
		out.add(new HeaderSection(ctx));

		switch(ctx.rootKind()){
			case INDIVIDUAL -> {
				out.add(new IntroductionSection(ctx));
				out.add(new LifeStorySection(ctx));
				out.add(new AncestrySection(ctx));
				out.add(new DescendantsSection(ctx));
				out.add(new RelationsSection(ctx));
			}
			case GROUP               -> {
				out.add(new GroupIntroductionSection(ctx));
				out.add(new GroupStorySection(ctx));
			}
			case EVENT               -> out.add(new EventRootSection(ctx));
			case SOURCE              -> out.add(new SourceRootSection(ctx));
			case PLACE               -> out.add(new PlaceRootSection(ctx));
			case REPOSITORY          -> out.add(new RepositoryRootSection(ctx));
			case DOCUMENT            -> out.add(new DocumentRootSection(ctx));
			case RESEARCH_QUESTION   -> out.add(new ResearchQuestionRootSection(ctx));
			case RESEARCH_ACTIVITY   -> out.add(new ResearchActivityRootSection(ctx));
			case RESEARCH_TASK       -> out.add(new ResearchTaskRootSection(ctx));
			case CONCLUSION          -> out.add(new ConclusionRootSection(ctx));
			case IDENTITY_HYPOTHESIS -> out.add(new IdentityHypothesisRootSection(ctx));
			case CULTURAL_NORM       -> out.add(new CulturalNormRootSection(ctx));
			case HISTORIC_EVENT      -> out.add(new HistoricEventRootSection(ctx));
			case OTHER               -> { /* common sections only */ }
		}

		if(ctx.config.timeline())
			out.add(new TimelineSection(ctx));

		out.add(new ContextResearchSection(ctx));
		out.add(new CulturalNormSection(ctx));
		out.add(new HistoricEventSection(ctx));
		out.add(new RepositorySection(ctx));
		out.add(new SourcesSection(ctx));
		out.add(new MediaSection(ctx));
		out.add(new PlacesSection(ctx));
		if(ctx.config.coverageReport())
			out.add(new CoverageSection(ctx));
		out.add(new IndexSection(ctx));

		return out;
	}

}
