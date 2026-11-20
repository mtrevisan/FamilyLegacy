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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


class RequiredParentSexTest{

	private static FLEFModel load() throws Exception{
		try(final var is = RequiredParentSexTest.class.getResourceAsStream("/tests/TGMZ.flef")){
			return new FLEFParser().parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	private static String idOf(final FLEFModel model, final String name){
		return model.getRecordsByType(IndividualReader.TYPE).stream()
			.filter(r -> name.equals(IndividualReader.extractPrimaryFullname(r)))
			.findFirst().get().getId();
	}

	private static void removeParent(final FLEFModel model, final String childId, final String parentName){
		for(final FLEFRecord r : new ArrayList<>(model.getRecordsByType("RELATIONSHIP")))
			if(childId.equals(r.extractReferencedId("subject", "INDIVIDUAL"))
					&& parentName.equals(IndividualReader.extractPrimaryFullname(model.getRecordById(r.extractReferencedId("target", "INDIVIDUAL")))))
				model.removeRecord(r.getId());
	}

	@Test
	void theNewParentHasTheSexOppositeToTheExistingOne() throws Exception{
		final FLEFModel model = load();
		final String child = idOf(model, "Emilio Giovanni Trevisan");

		removeParent(model, child, "Regina Elena Gallinaro");
		Assertions.assertEquals(SexType.FEMALE, AbstractRelativeTool.requiredParentSex(child, model));

		final FLEFModel other = load();
		removeParent(other, child, "Giuseppe Vittorio Trevisan");
		Assertions.assertEquals(SexType.MALE, AbstractRelativeTool.requiredParentSex(child, other));
	}

	@Test
	void noParentMeansNoConstraint() throws Exception{
		final FLEFModel model = load();
		final String child = idOf(model, "Emilio Giovanni Trevisan");

		removeParent(model, child, "Regina Elena Gallinaro");
		removeParent(model, child, "Giuseppe Vittorio Trevisan");
		Assertions.assertNull(AbstractRelativeTool.requiredParentSex(child, model));
	}
}
