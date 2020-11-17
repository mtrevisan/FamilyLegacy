/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TransformerTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void valueNoContinuationFlef(){
		final GedcomNode node = transformerTo.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertFalse(node.hasChildren());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT", node.getRawValue());
	}

	@Test
	void valueContinuationFlef(){
		final GedcomNode node = transformerTo.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY\nVERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY", node.getRawValue());
		Assertions.assertEquals("CONTINUATION", node.getChildren().get(0).getTag());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT", node.getChildren().get(0).getRawValue());
	}

	@Test
	void value(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("SHORT TEXT");

		Assertions.assertFalse(node.hasChildren());
		Assertions.assertEquals("SHORT TEXT", node.getRawValue());
	}

	@Test
	void valueContinuation(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("SHORT\nTEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("SHORT", node.getRawValue());
		Assertions.assertEquals("CONT", node.getChildren().get(0).getTag());
		Assertions.assertEquals("TEXT", node.getChildren().get(0).getRawValue());
	}

	@Test
	void valueConcatenationWithoutSpace(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
		Assertions.assertEquals("RY LONG TEXT", node.getChildren().get(0).getRawValue());
	}

	@Test
	void valueConcatenationWithSpace(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE Y LONG TEXT");

		Assertions.assertEquals(1, node.getChildren().size());
		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE", node.getRawValue());
		Assertions.assertEquals("CONC", node.getChildren().get(0).getTag());
		Assertions.assertEquals(" Y LONG TEXT", node.getChildren().get(0).getRawValue());
	}

}