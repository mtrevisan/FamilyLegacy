package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;


class EditorTabsByLevelTest{

	private static final JPanel PANEL = new JPanel();

	private static JPanel at(final UxLevel level, final String recordType, final UxLevel minimum){
		return BaseRecordDialog.visibleAt(recordType, minimum, PANEL, FeatureVisibilityService.inMemory(level));
	}

	@Test
	void theResearchTabIsOfferedFromTheStandardLevel(){
		Assertions.assertNull(at(UxLevel.BASIC, IndividualReader.TYPE, UxLevel.STANDARD));
		Assertions.assertSame(PANEL, at(UxLevel.STANDARD, IndividualReader.TYPE, UxLevel.STANDARD));
	}

	@Test
	void theContextTabIsOfferedFromTheAdvancedLevel(){
		Assertions.assertNull(at(UxLevel.STANDARD, IndividualReader.TYPE, UxLevel.ADVANCED));
		Assertions.assertSame(PANEL, at(UxLevel.ADVANCED, IndividualReader.TYPE, UxLevel.ADVANCED));
	}

	@Test
	void theEditorOfAResearchRecordKeepsAllItsTabs(){
		Assertions.assertSame(PANEL, at(UxLevel.BASIC, ConclusionReader.TYPE, UxLevel.ADVANCED));
	}

	@Test
	void anEditorWithoutThePanelHasNone(){
		Assertions.assertNull(BaseRecordDialog.visibleAt(IndividualReader.TYPE, UxLevel.STANDARD, null,
			FeatureVisibilityService.inMemory(UxLevel.ADVANCED)));
	}

}
