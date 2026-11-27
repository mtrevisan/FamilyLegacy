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
package io.github.mtrevisan.familylegacy.ui.components.quickentry;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;


/** The short way to cite a source: a title, a locator and, if known, a repository; the repositories of the file are never matched silently. */
class QuickCitationDialogTest{

	private FLEFModel model;
	private QuickCitationDialog dialog;
	private List<String> reported;
	private List<List<String>> asked;


	@BeforeEach
	void setUp(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");
		model = new FLEFModel();
		dialog = new QuickCitationDialog(null, model);
		reported = new ArrayList<>();
		asked = new ArrayList<>();
		dialog.setReporter((parent, message) -> reported.add(message));
	}


	private List<JTextField> fields(){
		final List<JTextField> fields = new ArrayList<>();
		collect(dialog.getContentPane(), fields);
		return fields;
	}

	private static void collect(final Container container, final List<JTextField> fields){
		for(final Component component : container.getComponents()){
			if(component instanceof JTextField field)
				fields.add(field);
			if(component instanceof Container child)
				collect(child, fields);
		}
	}

	/** Title, locator, repository. */
	private void type(final String title, final String locator, final String repository){
		fields().get(0).setText(title);
		fields().get(1).setText(locator);
		fields().get(2).setText(repository);
	}

	private void addRepository(final String id, final String name){
		model.addRecord(FLEFRecord.createMainRecord(id, RepositoryReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RepositoryReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_VALUE, name))));
	}

	private void answer(final int index){
		dialog.setAsker((parent, message, title, options) -> {
			asked.add(options);
			return index;
		});
	}


	@Test
	void theTitleAndTheLocatorAreTrimmedAndTheRepositoryIsOptional(){
		type("  Parish of San   Martino ", " book 2,  page 143 ", "");

		dialog.onOk();

		final QuickCitationDialog.Result result = dialog.result();
		Assertions.assertEquals("Parish of San Martino", result.title());
		Assertions.assertEquals("book 2, page 143", result.locator());
		Assertions.assertTrue(result.repository().isNone());
		Assertions.assertTrue(reported.isEmpty());
	}

	@Test
	void withoutATitleTheUserIsToldAndTheDialogStaysOpen(){
		type("   ", "page 1", "");

		dialog.onOk();

		Assertions.assertNull(dialog.result());
		Assertions.assertEquals(1, reported.size());
		Assertions.assertTrue(dialog.isDisplayable());
		dialog.dispose();
	}

	@Test
	void aRepositoryThatTheFileDoesNotHaveIsNew(){
		type("Register", "", "State Archive of Padova");

		dialog.onOk();

		Assertions.assertEquals("State Archive of Padova", dialog.result().repository().newName());
		Assertions.assertNull(dialog.result().repository().existingId());
	}

	@Test
	void aRepositoryThatTheFileHasIsOfferedAndTheUserMayUseIt(){
		addRepository("R1", "State Archive");
		addRepository("R2", "State Archive");
		answer(1);
		type("Register", "", "state archive");

		dialog.onOk();

		Assertions.assertEquals(1, asked.size());
		//both repositories, then the new one
		Assertions.assertEquals(3, asked.getFirst().size());
		Assertions.assertEquals("R2", dialog.result().repository().existingId());
	}

	@Test
	void theUserMayCreateANewRepositoryInstead(){
		addRepository("R1", "State Archive");
		answer(1);
		type("Register", "", "State Archive");

		dialog.onOk();

		Assertions.assertEquals("State Archive", dialog.result().repository().newName());
		Assertions.assertNull(dialog.result().repository().existingId());
	}

	@Test
	void notChoosingARepositoryKeepsTheDialogOpen(){
		addRepository("R1", "State Archive");
		answer(-1);
		type("Register", "", "State Archive");

		dialog.onOk();

		Assertions.assertNull(dialog.result());
		Assertions.assertTrue(dialog.isDisplayable());
		dialog.dispose();
	}

}
