package org.folg.gedcom.parser;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GeneratorCorporation;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.Submitter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;


public class ModelParserTest{

	@Test
	public void testAddressStructureParsing() throws Exception{
		URL gedcomUrl = this.getClass().getClassLoader().getResource("Case001-AddressStructure.ged");
		File gedcomFile = new File(gedcomUrl.toURI());
		ModelParser modelParser = new ModelParser();

		Gedcom gedcom = modelParser.parseGedcom(gedcomFile);
		Assertions.assertNotNull(gedcom);

		gedcom.createIndexes();

		Assertions.assertNotNull(gedcom.getHeader());
		Assertions.assertNotNull(gedcom.getHeader().getGenerator());
		GeneratorCorporation generatorCorporation = gedcom.getHeader().getGenerator().getGeneratorCorporation();
		Assertions.assertNotNull(generatorCorporation);
		Assertions.assertEquals(generatorCorporation.getAddress().getValue(), "5000 MyCorpCampus Dr\n" + "Hometown, ZZ  99999\n" + "United States");
		Assertions.assertEquals(generatorCorporation.getAddress().getAddressLine1(), "__ADR1_VALUE__");
		Assertions.assertEquals(generatorCorporation.getAddress().getAddressLine2(), "__ADR2_VALUE__");
		Assertions.assertEquals(generatorCorporation.getAddress().getAddressLine3(), "5000 MyCorpCampus Dr");
		Assertions.assertEquals(generatorCorporation.getAddress().getCity(), "Hometown");
		Assertions.assertEquals(generatorCorporation.getAddress().getState(), "ZZ");
		Assertions.assertEquals(generatorCorporation.getAddress().getPostalCode(), "99999");
		Assertions.assertEquals(generatorCorporation.getAddress().getCountry(), "United States");
		Assertions.assertEquals(generatorCorporation.getPhone(), "866-000-0000");
		Assertions.assertEquals(generatorCorporation.getEmail(), "info@mycorporation.com");
		Assertions.assertEquals(generatorCorporation.getFax(), "866-111-1111");
		Assertions.assertEquals(generatorCorporation.getWww(), "http://www.mycorporation.org/");

		Submitter submitter = gedcom.getSubmitter("SUB1");
		Assertions.assertNotNull(submitter);
		Assertions.assertEquals(submitter.getAddress().getValue(), "5000 MyCorpCampus Dr\n" + "Hometown, ZZ  99999\n" + "United States");
		Assertions.assertEquals(submitter.getAddress().getAddressLine1(), "__ADR1_VALUE__");
		Assertions.assertEquals(submitter.getAddress().getAddressLine2(), "__ADR2_VALUE__");
		Assertions.assertEquals(submitter.getAddress().getAddressLine3(), "5000 MyCorpCampus Dr");
		Assertions.assertEquals(submitter.getAddress().getCity(), "Hometown");
		Assertions.assertEquals(submitter.getAddress().getState(), "ZZ");
		Assertions.assertEquals(submitter.getAddress().getPostalCode(), "99999");
		Assertions.assertEquals(submitter.getAddress().getCountry(), "United States");
		Assertions.assertEquals(submitter.getPhone(), "866-000-0000");
		Assertions.assertEquals(submitter.getEmail(), "info@mycorporation.com");
		Assertions.assertEquals(submitter.getFax(), "866-111-1111");
		Assertions.assertEquals(submitter.getWww(), "http://www.mycorporation.org/");

		Assertions.assertNotNull(gedcom.getPeople());
		Assertions.assertEquals(gedcom.getPeople().size(), 1);
		Person person = gedcom.getPeople().get(0);
		Assertions.assertNotNull(person);
		Assertions.assertEquals(person.getAddress().getValue(), "5000 MyCorpCampus Dr\n" + "Hometown, ZZ  99999\n" + "United States");
		Assertions.assertEquals(person.getAddress().getAddressLine1(), "__ADR1_VALUE__");
		Assertions.assertEquals(person.getAddress().getAddressLine2(), "__ADR2_VALUE__");
		Assertions.assertEquals(person.getAddress().getAddressLine3(), "5000 MyCorpCampus Dr");
		Assertions.assertEquals(person.getAddress().getCity(), "Hometown");
		Assertions.assertEquals(person.getAddress().getState(), "ZZ");
		Assertions.assertEquals(person.getAddress().getPostalCode(), "99999");
		Assertions.assertEquals(person.getAddress().getCountry(), "United States");
		Assertions.assertEquals(person.getPhone(), "866-000-0000");
		Assertions.assertEquals(person.getEmail(), "info@mycorporation.com");
		Assertions.assertEquals(person.getFax(), "866-111-1111");
		Assertions.assertEquals(person.getWww(), "http://www.mycorporation.org/");

		Assertions.assertNotNull(person.getEventsFacts());
		Assertions.assertEquals(person.getEventsFacts().size(), 1);
		EventFact eventFact = person.getEventsFacts().get(0);
		Assertions.assertEquals(eventFact.getAddress().getValue(), "Arlington National Cemetery\n" + "State Hwy 110 & Memorial Dr\n" + "Arlington, VA  22211\n" + "United States");
		Assertions.assertEquals(eventFact.getAddress().getAddressLine1(), "__ADR1_VALUE__");
		Assertions.assertEquals(eventFact.getAddress().getAddressLine2(), "__ADR2_VALUE__");
		Assertions.assertEquals(eventFact.getAddress().getAddressLine3(), "__ADR3_VALUE__");
		Assertions.assertEquals(eventFact.getAddress().getCity(), "Arlington");
		Assertions.assertEquals(eventFact.getAddress().getState(), "VA");
		Assertions.assertEquals(eventFact.getAddress().getPostalCode(), "22211");
		Assertions.assertEquals(eventFact.getAddress().getCountry(), "United States");
		Assertions.assertEquals(eventFact.getPhone(), "877-907-8585");
		Assertions.assertEquals(eventFact.getEmail(), "info@arlingtoncemetery.mil");
		Assertions.assertEquals(eventFact.getFax(), "877-111-1111");
		Assertions.assertEquals(eventFact.getWww(), "http://www.arlingtoncemetery.mil/");

		Assertions.assertNotNull(gedcom.getRepositories());
		Assertions.assertEquals(gedcom.getRepositories().size(), 1);
		Repository repository = gedcom.getRepositories().get(0);
		Assertions.assertEquals(repository.getAddress().getValue(), "5000 MyCorpCampus Dr\n" + "Hometown, ZZ  99999\n" + "United States");
		Assertions.assertEquals(repository.getAddress().getAddressLine1(), "__ADR1_VALUE__");
		Assertions.assertEquals(repository.getAddress().getAddressLine2(), "__ADR2_VALUE__");
		Assertions.assertEquals(repository.getAddress().getAddressLine3(), "5000 MyCorpCampus Dr");
		Assertions.assertEquals(repository.getAddress().getCity(), "Hometown");
		Assertions.assertEquals(repository.getAddress().getState(), "ZZ");
		Assertions.assertEquals(repository.getAddress().getPostalCode(), "99999");
		Assertions.assertEquals(repository.getAddress().getCountry(), "United States");
		Assertions.assertEquals(repository.getPhone(), "866-000-0000");
		Assertions.assertEquals(repository.getEmail(), "info@mycorporation.com");
		Assertions.assertEquals(repository.getFax(), "866-111-1111");
		Assertions.assertEquals(repository.getWww(), "https://www.mycorporation.com/");
	}

	@Test
	public void testParse_withInputStream() throws Exception{
		URL gedcomUrl = this.getClass().getClassLoader().getResource("Case001-AddressStructure.ged");
		ModelParser modelParser = new ModelParser();

		InputStream is = gedcomUrl.openStream();
		Assertions.assertNotNull(is);

		Gedcom gedcom = modelParser.parseGedcom(is);
		Assertions.assertNotNull(gedcom);
	}

	@Test
	public void testParse_withReader() throws Exception{
		URL gedcomUrl = this.getClass().getClassLoader().getResource("Case001-AddressStructure.ged");
		ModelParser modelParser = new ModelParser();

		InputStream is = gedcomUrl.openStream();
		Assertions.assertNotNull(is);

		Reader reader = new InputStreamReader(is, "UTF-8");

		Gedcom gedcom = modelParser.parseGedcom(reader);
		Assertions.assertNotNull(gedcom);
	}

}
