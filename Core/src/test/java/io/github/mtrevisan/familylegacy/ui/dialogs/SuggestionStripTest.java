package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.features.Suggestion;
import io.github.mtrevisan.familylegacy.ui.features.SuggestionKind;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class SuggestionStripTest{

	/** The width of a narrow editor: the longest wording of the strip must fit it, not be cut. */
	private static final int NARROW_EDITOR = 428;

	@Test
	void everyWordingFitsANarrowEditor(){
		for(final SuggestionKind kind : SuggestionKind.values())
			for(final UxLevel level : UxLevel.values()){
				final SuggestionStrip strip = new SuggestionStrip();
				strip.show(List.of(Suggestion.forLevel(kind, level, "explanation", () -> {})));

				Assertions.assertTrue(strip.getPreferredSize().width <= NARROW_EDITOR,
					kind + StringUtils.SPACE + level + ": " + strip.getPreferredSize().width);
				Assertions.assertTrue(strip.isVisible());
			}
	}

	@Test
	void withoutSuggestionsTheStripIsHidden(){
		final SuggestionStrip strip = new SuggestionStrip();
		strip.show(List.of());
		Assertions.assertFalse(strip.isVisible());
	}

}
