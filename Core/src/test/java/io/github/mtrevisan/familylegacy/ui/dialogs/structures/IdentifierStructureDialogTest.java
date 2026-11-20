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
package io.github.mtrevisan.familylegacy.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentifierHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryCitationHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.List;


/** The editor of an identifier: the schemes it offers first, and how the identifiers are listed. */
class IdentifierStructureDialogTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";


	private static FLEFModel model(){
		return new FLEFParser().parse("repository R1 {\n\tname.value Archivio di Stato\n" + AUDIT + "}\n"
			+ "source S1 {\n\ttitle.value Register\n"
			+ "\tidentifier {\n\t\tscheme isbn\n\t\tvalue 9780199535569\n\t}\n"
			+ "\trepository {\n\t\trepository R1\n"
			+ "\t\tidentifier {\n\t\t\tscheme call_number\n\t\t\tvalue MS 45/12\n\t\t}\n"
			+ "\t\tidentifier {\n\t\t\tscheme dewey\n\t\t\tvalue 929.2 ROS\n\t\t}\n"
			+ "\t\tlocator Page 27\n\t}\n" + AUDIT + "}\n");
	}


	@Test
	void everySchemeIsOfferedOnceWhateverTheOrder(){
		final List<String> schemes = List.of(IdentifierStructureDialog.schemesFor(null));

		Assertions.assertEquals(List.of(IdentifierReader.SCHEMES).size(), schemes.size());
		Assertions.assertTrue(schemes.containsAll(List.of(IdentifierReader.SCHEMES)));
	}

	@Test
	void theSchemesOfTheSourceItselfComeFirstOutsideARepositoryCitation(){
		final List<String> schemes = List.of(IdentifierStructureDialog.schemesFor(null));

		Assertions.assertEquals(List.of(IdentifierReader.SOURCE_SCHEMES), schemes.subList(0, IdentifierReader.SOURCE_SCHEMES.length));
		Assertions.assertEquals("isbn", schemes.getFirst());
	}

	@Test
	void theSchemesOfARepositoryComeFirstInTheDialogOfARepositoryCitation(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final JDialog parent = new JDialog((Window)null, "repository citation");
		try{
			parent.getRootPane().putClientProperty(IdentifierHandler.SCOPE_PROPERTY, IdentifierHandler.SCOPE_REPOSITORY);

			final List<String> schemes = List.of(IdentifierStructureDialog.schemesFor(parent));

			Assertions.assertEquals(List.of(IdentifierReader.REPOSITORY_SCHEMES), schemes.subList(0, IdentifierReader.REPOSITORY_SCHEMES.length));
			Assertions.assertEquals("call_number", schemes.getFirst());
			Assertions.assertEquals(IdentifierReader.SCHEMES.length, schemes.size());
		}
		finally{
			parent.dispose();
		}
	}

	@Test
	void theDialogOfAnIdentifierOpensFilledWithItsSchemeAndValue(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		final FLEFModel model = model();
		final FLEFRecord identifier = IdentifierReader.extractIdentifiers(model.getRecordById("S1")).getFirst();
		final JDialog parent = new JDialog((Window)null, "parent");
		parent.pack();
		try{
			final JDialog dialog = (JDialog)HandlerRegistry.getHandler(IdentifierHandler.TYPE)
				.createEditDialog(parent, model, identifier).window();
			try{
				Assertions.assertTrue(dialog.isDisplayable());
			}
			finally{
				dialog.dispose();
			}
		}
		finally{
			parent.dispose();
		}
	}

	@Test
	void anIdentifierIsListedWithTheNameOfItsScheme(){
		final FLEFModel model = model();
		final IdentifierHandler handler = IdentifierHandler.getInstance();
		final FLEFRecord isbn = IdentifierReader.extractIdentifiers(model.getRecordById("S1")).getFirst();

		Assertions.assertEquals(I18N.t("enum.identifier.scheme.isbn") + ": 9780199535569", handler.getDisplayText(isbn, model));
		//a scheme of the user is shown as it was written
		Assertions.assertEquals("Barcode: 0042", handler.getDisplayText(IdentifierReader.create("Barcode", "0042"), model));
		Assertions.assertEquals("--", handler.getDisplayText(null, model));
	}

	@Test
	void aRepositoryCitationIsListedWithWhereTheSourceIsFoundInTheRepository(){
		final FLEFModel model = model();
		final FLEFRecord citation = FLEFRecordHelper.findChild(model.getRecordById("S1"), "repository");

		final String text = RepositoryCitationHandler.getInstance().getDisplayText(citation, model);

		Assertions.assertTrue(text.contains("Archivio di Stato"), text);
		Assertions.assertTrue(text.endsWith("(MS 45/12, 929.2 ROS)"), text);
		//the locator is a position inside the source, not a way to find it in the repository
		Assertions.assertFalse(text.contains("Page 27"), text);
	}

}
