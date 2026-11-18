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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** The type of a repository: its reader constants must agree with the grammar. */
class RepositoryReaderTest{

	@Test
	void theTypesOfTheReaderAreTheEnumOfTheGrammar() throws IOException{
		final String schema;
		try(final InputStream is = RepositoryReaderTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			Assertions.assertNotNull(is, "schema not found");
			schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		final Matcher matcher = Pattern.compile("record RepositoryRecord \\{.*?type\\?: enum \\{(.*?)}", Pattern.DOTALL)
			.matcher(schema);
		Assertions.assertTrue(matcher.find(), "type of RepositoryRecord not found in schema");
		final Pattern comment = Pattern.compile("//[^\r\n]*");
		final List<String> schemaTypes = Arrays.stream(StringUtils.split(comment.matcher(matcher.group(1)).replaceAll(""), ','))
			.map(String::trim)
			.filter(token -> !token.isEmpty())
			.toList();

		Assertions.assertEquals(schemaTypes, List.of(RepositoryReader.TYPES));
	}

	@Test
	void everyTypeHasALabel() throws IOException{
		final Properties messages = new Properties();
		try(final InputStream is = RepositoryReaderTest.class.getResourceAsStream("/i18n/ui/messages.properties")){
			Assertions.assertNotNull(is, "messages not found");
			messages.load(new InputStreamReader(is, StandardCharsets.UTF_8));
		}

		Assertions.assertAll(Arrays.stream(RepositoryReader.TYPES)
			.map(type -> () -> Assertions.assertTrue(messages.containsKey("enum.repository.type." + type), type)));
	}

	@Test
	void theTypeIsReadFromTheRecord(){
		final FLEFRecord repository = FLEFRecord.createMainRecord("R1", RepositoryReader.TYPE);
		Assertions.assertNull(RepositoryReader.extractType(repository));

		repository.addChild(FLEFRecord.createChildWithTagAndValue(RepositoryReader.TAG_TYPE, RepositoryReader.ENUM_TYPE_PRIVATE_COLLECTION));
		Assertions.assertEquals("private_collection", RepositoryReader.extractType(repository));
	}

}
