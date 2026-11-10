package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** A name with a nickname, a title or a prefix is still a name: the parts that are not personal are left out, not the whole name. */
class IndividualFullNameTest{

	private static FLEFRecord part(final String type, final String value){
		return FLEFRecord.createChildWithTag(NameReader.TAG_PART)
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, type))
			.addChild(FLEFRecord.createChildWithTagAndValue("value", value));
	}

	private static FLEFRecord individual(final FLEFRecord... parts){
		final FLEFRecord name = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME);
		for(final FLEFRecord part : parts)
			name.addChild(part);
		return FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(name);
	}


	@Test
	void aNicknameIsOmittedButTheNameIsKept(){
		final FLEFRecord individual = individual(part(NameReader.ENUM_PART_TYPE_GIVEN, "Carlo Erminio"),
			part(NameReader.ENUM_PART_TYPE_NICKNAME, "Karleto"), part(NameReader.ENUM_PART_TYPE_FAMILY, "Masutti"));

		Assertions.assertEquals("Carlo Erminio Masutti", IndividualReader.extractPrimaryFullname(individual));
	}

	@Test
	void aNameOfOnlyExcludedPartsIsNoName(){
		Assertions.assertNull(IndividualReader.extractPrimaryFullname(individual(part(NameReader.ENUM_PART_TYPE_NICKNAME, "Karleto"))));
	}


	/** A name of a given type, made of its given name and its family name. */
	private static FLEFRecord name(final String type, final String given, final String family){
		final FLEFRecord name = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME);
		if(type != null)
			name.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, type));
		return name.addChild(part(NameReader.ENUM_PART_TYPE_GIVEN, given))
			.addChild(part(NameReader.ENUM_PART_TYPE_FAMILY, family));
	}

	private static FLEFRecord individualOf(final FLEFRecord... names){
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE);
		for(final FLEFRecord name : names)
			individual.addChild(name);
		return individual;
	}


	@Test
	void theNameToShowIsTheBirthNameThenTheOfficialOneThenTheLegalOne(){
		//the first name is a married name, the birth name is later
		Assertions.assertEquals("Maria Rossi", IndividualReader.extractPrimaryFullname(individualOf(
			name(NameReader.ENUM_PERSONAL_TYPE_MARRIED, "Maria", "Bianchi"), name(NameReader.ENUM_PERSONAL_TYPE_LEGAL, "Maria", "Verdi"),
			name(NameReader.ENUM_PERSONAL_TYPE_BIRTH, "Maria", "Rossi"))));
		Assertions.assertEquals("Maria Verdi", IndividualReader.extractPrimaryFullname(individualOf(
			name(NameReader.ENUM_PERSONAL_TYPE_LEGAL, "Maria", "Bianchi"), name(NameReader.ENUM_PERSONAL_TYPE_OFFICIAL, "Maria", "Verdi"))));
		Assertions.assertEquals("Maria Bianchi", IndividualReader.extractPrimaryFullname(individualOf(
			name(NameReader.ENUM_PERSONAL_TYPE_LEGAL, "Maria", "Bianchi"))));
	}

	@Test
	void withoutThoseTypesTheFirstNameThatIsNotAnAliasOrANicknameIsShown(){
		Assertions.assertEquals("Maria Rossi", IndividualReader.extractPrimaryFullname(individualOf(
			name(NameReader.ENUM_PERSONAL_TYPE_ALIAS, "Mariuccia", "Rossi"), name(NameReader.ENUM_PERSONAL_TYPE_NICKNAME, "Mary", "Rossi"),
			name(null, "Maria", "Rossi"), name(NameReader.ENUM_PERSONAL_TYPE_MARRIED, "Maria", "Bianchi"))));
		//only informal names: the first one
		Assertions.assertEquals("Mariuccia Rossi", IndividualReader.extractPrimaryFullname(individualOf(
			name(NameReader.ENUM_PERSONAL_TYPE_ALIAS, "Mariuccia", "Rossi"), name(NameReader.ENUM_PERSONAL_TYPE_NICKNAME, "Mary", "Rossi"))));
	}

}
