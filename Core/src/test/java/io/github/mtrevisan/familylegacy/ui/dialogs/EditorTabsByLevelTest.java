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
	void conclusionsAreOfferedFromTheAdvancedLevelEvenInsideTheStandardResearchTab(){
		Assertions.assertEquals(UxLevel.ADVANCED, PanelKey.CONCLUSION_ON_RESOLVES.minimumLevel());
		Assertions.assertEquals(UxLevel.STANDARD, PanelKey.RESEARCH_QUESTION_ON_TARGET.minimumLevel());
		Assertions.assertEquals(UxLevel.STANDARD, PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY.minimumLevel());
		Assertions.assertEquals(UxLevel.ADVANCED, PanelKey.CONTEXT_IMPACT_ON_TARGET.minimumLevel());
		Assertions.assertEquals(UxLevel.ADVANCED, PanelKey.EVIDENCE.minimumLevel());
		Assertions.assertEquals(UxLevel.BASIC, PanelKey.SOURCE.minimumLevel());
		Assertions.assertEquals(UxLevel.BASIC, PanelKey.INDIVIDUAL_ATTRIBUTE.minimumLevel());

		final FeatureVisibilityService standard = FeatureVisibilityService.inMemory(UxLevel.STANDARD);
		Assertions.assertFalse(BaseRecordDialog.isOfferedAt(IndividualReader.TYPE, PanelKey.CONCLUSION_ON_RESOLVES.minimumLevel(), standard));
		Assertions.assertTrue(BaseRecordDialog.isOfferedAt(IndividualReader.TYPE, PanelKey.RESEARCH_QUESTION_ON_TARGET.minimumLevel(), standard));
		Assertions.assertTrue(BaseRecordDialog.isOfferedAt(IndividualReader.TYPE, PanelKey.CONCLUSION_ON_RESOLVES.minimumLevel(),
			FeatureVisibilityService.inMemory(UxLevel.ADVANCED)));
		//the editor of a research record shows all its panels
		Assertions.assertTrue(BaseRecordDialog.isOfferedAt(ConclusionReader.TYPE, PanelKey.CONCLUSION_ON_RESEARCH.minimumLevel(),
			FeatureVisibilityService.inMemory(UxLevel.BASIC)));
	}

	@Test
	void anEditorWithoutThePanelHasNone(){
		Assertions.assertNull(BaseRecordDialog.visibleAt(IndividualReader.TYPE, UxLevel.STANDARD, null,
			FeatureVisibilityService.inMemory(UxLevel.ADVANCED)));
	}

}
