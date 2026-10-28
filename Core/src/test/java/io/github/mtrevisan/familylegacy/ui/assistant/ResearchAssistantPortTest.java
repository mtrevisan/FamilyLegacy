package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.ui.assistant.app.FrameAssistantPort;
import io.github.mtrevisan.familylegacy.ui.components.projections.ResearchAssistantPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ResearchAssistantPortTest{

	/** The main window does not name the assistant: it finds it, so the provider has to be declared. */
	@Test
	void theMainWindowFindsTheResearchAssistant(){
		final ResearchAssistantPort port = ResearchAssistantPort.find().orElse(null);

		Assertions.assertNotNull(port, "No provider of the Research Assistant is declared");
		Assertions.assertInstanceOf(FrameAssistantPort.class, port);
	}

}
