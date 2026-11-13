package io.github.mtrevisan.familylegacy.ui.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.util.Locale;


/** The totals are written with the thousands separator of the language, as every number of a message, and not with a space. */
class GUIHelperFormatTest{

	@Test
	void aTotalHasTheSeparatorOfTheLanguage(){
		final String expected = NumberFormat.getIntegerInstance(Locale.getDefault()).format(1_234_567);

		Assertions.assertEquals(expected, GUIHelper.format("%,d", 1_234_567));
		Assertions.assertFalse(GUIHelper.format("%,d", 1_234_567).contains("\u00A0"));
		Assertions.assertFalse(GUIHelper.format("%,d", 1_234_567).contains(" "));
	}

	@Test
	void aNumberWithoutGroupingHasNoSeparator(){
		Assertions.assertEquals("1234567", GUIHelper.format("%d", 1_234_567));
	}

}
