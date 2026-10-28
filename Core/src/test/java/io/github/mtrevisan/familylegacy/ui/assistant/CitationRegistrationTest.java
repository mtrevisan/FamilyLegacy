package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.ui.assistant.app.DefaultResearchAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.util.List;


class CitationRegistrationTest{

	/** The default service offers the Source Citation Assistant in the editor of a citation, and nowhere else. */
	@Test
	void theCitationAssistantIsRegistered(){
		final ResearchAssistantService service = DefaultResearchAssistant.createDefault(IgnoredFindings.inMemory());

		final JPanel editor = new io.github.mtrevisan.familylegacy.ui.assistant.citation.CitationTestEditor();
		final List<ResearchAssistantService.RankedAction> inside = service.search("citation",
			new AssistantContext(null, editor, null));
		final List<ResearchAssistantService.RankedAction> outside = service.search("citation",
			new AssistantContext(null, new JPanel(), null));

		Assertions.assertTrue(inside.stream().anyMatch(ranked -> ranked.action().id().equals("sources.citation")),
			inside.toString());
		Assertions.assertTrue(outside.stream().noneMatch(ranked -> ranked.action().id().equals("sources.citation")));
	}

}
