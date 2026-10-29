package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;


/** The address a place is looked for with is made of its name and of the names of the places that contain it. */
class PlaceAddressTest{

	private static final String GEDCOM = """
		0 HEAD
		1 CHAR UTF-8
		0 @I1@ INDI
		1 NAME Anna /Rossi/
		1 BIRT
		2 PLAC via Mattielli 3, Vicenza, VI, Italia
		0 @I2@ INDI
		1 NAME Bruno /Rossi/
		1 BIRT
		2 PLAC Vicenza, VI, Italia
		0 @I3@ INDI
		1 NAME Carlo /Rossi/
		1 BIRT
		2 PLAC Treviso, Treviso, Italia
		""";


	private static FLEFModel convert() throws IOException{
		return convert(GEDCOM);
	}

	private static FLEFModel convert(final String gedcom) throws IOException{
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		return new GEDCOMToFLEFConverter().convert(roots);
	}

	/** The place that has the name, whatever it is contained in (the first one). */
	private static FLEFRecord placeNamed(final FLEFModel model, final String name){
		return model.getRecordsByType(PlaceReader.TYPE).stream()
			.filter(place -> name.equals(PlaceReader.extractPrimaryName(place)))
			.findFirst()
			.orElseThrow();
	}


	@Test
	void aStreetIsLookedForWithItsTownItsProvinceAndItsCountry() throws IOException{
		final FLEFModel model = convert();
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, null);

		Assertions.assertEquals(List.of("via Mattielli 3, Vicenza, VI, Italia"),
			resolver.fullAddresses(placeNamed(model, "via Mattielli 3")));
	}

	@Test
	void everyLevelIsCompleteWithTheLevelsAboveIt() throws IOException{
		final FLEFModel model = convert();
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, null);

		Assertions.assertEquals(List.of("Vicenza, VI, Italia"), resolver.fullAddresses(placeNamed(model, "Vicenza")));
		Assertions.assertEquals(List.of("VI, Italia"), resolver.fullAddresses(placeNamed(model, "VI")));
		Assertions.assertEquals(List.of("Italia"), resolver.fullAddresses(placeNamed(model, "Italia")));
	}

	@Test
	void aTownThatIsNamedLikeItsProvinceIsNotRepeated() throws IOException{
		final FLEFModel model = convert();
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, null);

		final List<String> addresses = model.getRecordsByType(PlaceReader.TYPE).stream()
			.filter(place -> "Treviso".equals(PlaceReader.extractPrimaryName(place)))
			.flatMap(place -> resolver.fullAddresses(place).stream())
			.toList();

		//the town and the province: two places, and one address for both
		Assertions.assertEquals(List.of("Treviso, Italia", "Treviso, Italia"), addresses);
	}

	@Test
	void anAddressWithAQuestionMarkIsNotGeocoded() throws IOException{
		final FLEFModel model = convert("""
			0 HEAD
			1 CHAR UTF-8
			0 @I1@ INDI
			1 NAME Anna /Rossi/
			1 BIRT
			2 PLAC via Roma ?, Vicenza, VI, Italia
			""");
		final PlaceCoordinateResolver resolver = new PlaceCoordinateResolver(model, null);

		Assertions.assertEquals(List.of(), resolver.fullAddresses(placeNamed(model, "via Roma ?")));
		Assertions.assertTrue(resolver.extractMissingPlaces().stream()
			.noneMatch(place -> "via Roma ?".equals(PlaceReader.extractPrimaryName(place))));
	}

	@Test
	void aNameThatIsAlreadyAnAddressIsNotRepeatedByTheContainers(){
		Assertions.assertEquals("via Pasubio, Treviso, TV, Italia",
			PlaceCoordinateResolver.compose("via Pasubio, Treviso", List.of("Treviso", "TV", "Italia")));
		Assertions.assertEquals("Treviso", PlaceCoordinateResolver.compose("Treviso", List.of()));
		//the spaces are normalized, the case of the containers is kept
		Assertions.assertEquals("via Pasubio, treviso, Italia",
			PlaceCoordinateResolver.compose("  via   Pasubio ", List.of("treviso", " Italia ")));
		//but a part the name has already is not added, whatever its case
		Assertions.assertEquals("via Pasubio, Treviso, Italia",
			PlaceCoordinateResolver.compose("via Pasubio, Treviso", List.of("treviso", "Italia")));
	}

}
