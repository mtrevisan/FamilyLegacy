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
