package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


class RecordNormalizerTest{

	private static FLEFGrammar grammar;


	@BeforeAll
	static void loadGrammar() throws IOException{
		try(final InputStream is = RecordNormalizerTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			grammar = FLEFGrammarParser.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}


	private static List<String> tags(final FLEFRecord record){
		return record.getChildren().stream()
			.map(FLEFRecord::getTag)
			.toList();
	}


	@Test
	void childrenFollowTheOrderOfTheGrammar(){
		//the grammar declares: name, sex, source, note, preferred_image, privacy, audit
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", "individual")
			.addChild(FLEFRecord.createChildWithTag("audit"))
			.addChild(FLEFRecord.createChildWithTagAndValue("note", "n"))
			.addChild(FLEFRecord.createChildWithTagAndValue("sex", "male"))
			.addChild(FLEFRecord.createChildWithTag("name"));

		Assertions.assertEquals(List.of("name", "sex", "note", "audit"), tags(RecordNormalizer.normalize(record, grammar)));
	}

	@Test
	void unknownChildrenComeLastAndRepeatedChildrenAreSorted(){
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", "individual")
			.addChild(FLEFRecord.createChildWithTagAndValue("zzz", "1"))
			.addChild(FLEFRecord.createChildWithTagAndValue("note", "b"))
			.addChild(FLEFRecord.createChildWithTagAndValue("note", "a"))
			.addChild(FLEFRecord.createChildWithTagAndValue("sex", "male"));

		final FLEFRecord normalized = RecordNormalizer.normalize(record, grammar);

		Assertions.assertEquals(List.of("sex", "note", "note", "zzz"), tags(normalized));
		Assertions.assertEquals("a", normalized.getChildren().get(1).getValue());
	}

	@Test
	void withoutGrammarTheChildrenAreSortedByTag(){
		final FLEFRecord record = FLEFRecord.createMainRecord("I1", "individual")
			.addChild(FLEFRecord.createChildWithTag("name"))
			.addChild(FLEFRecord.createChildWithTagAndValue("note", "n"))
			.addChild(FLEFRecord.createChildWithTag("audit"));

		Assertions.assertEquals(List.of("audit", "name", "note"), tags(RecordNormalizer.normalize(record, null)));
	}

}
