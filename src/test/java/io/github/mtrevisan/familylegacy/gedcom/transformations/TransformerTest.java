package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TransformerTest{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Test
	void valueConcatenationContinuationFlef(){
		final GedcomNode node = transformerTo.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT", node.getValue());
	}

	@Test
	void valueConcatenationFlef(){
		final GedcomNode node = transformerTo.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY\nVERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY\nVERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT", node.getValue());
	}

	@Test
	void value(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("SHORT TEXT");

		Assertions.assertEquals("SHORT TEXT", node.getValue());
	}

	@Test
	void valueContinuation(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("SHORT\nTEXT");

		Assertions.assertEquals("SHORT\nTEXT", node.getValue());
	}

	@Test
	void valueConcatenationContinuation(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY LONG TEXT");

		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE\nRY LONG TEXT", node.getValue());
	}

	@Test
	void valueConcatenationWithSpace(){
		final GedcomNode node = transformerFrom.create("NOTE")
			.withValue("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE Y LONG TEXT");

		Assertions.assertEquals("VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VERY VE\n Y LONG TEXT", node.getValue());
	}

}