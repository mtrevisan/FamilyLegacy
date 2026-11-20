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
package io.github.mtrevisan.familylegacy.events;

import io.github.mtrevisan.familylegacy.events.EventTypeInfoService.ParticipantAdvisory;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;


class EventTypeInfoServiceTest{

	@Test
	void catalogCoversEveryStandardType(){
		for(final String type : EventReader.TYPES)
			Assertions.assertTrue(EventTypeInfoService.isBuiltIn(type), "Missing: " + type);
		Assertions.assertEquals(EventReader.TYPES.length, EventTypeInfoService.all().size());
		final Set<String> catalog = new HashSet<>();
		EventTypeInfoService.all().forEach(info -> catalog.add(info.type()));
		Assertions.assertEquals(Set.of(EventReader.TYPES), catalog);
	}

	@Test
	void everyEntryIsWellFormed(){
		for(final EventTypeInfo info : EventTypeInfoService.all()){
			Assertions.assertTrue(info.principals() == 1 || info.principals() == 2, info.type());
			Assertions.assertTrue(info.defaultRoles().contains("principal"), info.type());
			Assertions.assertEquals("principal", info.defaultRoles().iterator().next(), info.type());
			Assertions.assertEquals(info.marital(), info.principals() == 2, info.type());
		}
	}

	@Test
	void examplesFromTheSpecification(){
		assertInfo("birth", 1, false, false);
		assertInfo("marriage", 2, false, true);
		assertInfo("divorce", 2, false, true);
		assertInfo("burial", 1, true, false);
		assertInfo("probate", 1, true, false);
	}

	@Test
	void customTypesAreNeutral(){
		Assertions.assertTrue(EventTypeInfoService.find("anecdote").isEmpty());
		Assertions.assertTrue(EventTypeInfoService.find(null).isEmpty());
		Assertions.assertTrue(EventTypeInfoService.find("  ").isEmpty());
		Assertions.assertTrue(EventTypeInfoService.isAllowedAfterDeath("anecdote"));
		Assertions.assertFalse(EventTypeInfoService.isMarital("anecdote"));
		Assertions.assertTrue(EventTypeInfoService.defaultRoles("anecdote").isEmpty());
		Assertions.assertTrue(EventTypeInfoService.checkPrincipals("anecdote", 0).isEmpty());
	}

	@Test
	void marriageFormationExcludesDissolution(){
		Assertions.assertEquals(Set.of("engagement", "marriage_bann", "marriage_contract", "marriage_license", "marriage_settlement", "marriage"),
			EventTypeInfoService.marriageFormationTypes());
		Assertions.assertTrue(EventTypeInfoService.isMarriageFormation("marriage"));
		Assertions.assertFalse(EventTypeInfoService.isMarriageFormation("divorce"));
		Assertions.assertFalse(EventTypeInfoService.isMarriageFormation("anecdote"));
		Assertions.assertFalse(EventTypeInfoService.isMarriageFormation(null));
	}

	@Test
	void lookupIgnoresCaseAndSpaces(){
		Assertions.assertTrue(EventTypeInfoService.find(" Marriage ").isPresent());
	}

	@Test
	void participantAdvisories(){
		Assertions.assertEquals(ParticipantAdvisory.TOO_FEW_PRINCIPALS, EventTypeInfoService.checkPrincipals("marriage", 1).orElseThrow());
		Assertions.assertEquals(ParticipantAdvisory.NO_PRINCIPAL, EventTypeInfoService.checkPrincipals("burial", 0).orElseThrow());
		Assertions.assertEquals(ParticipantAdvisory.TOO_MANY_PRINCIPALS, EventTypeInfoService.checkPrincipals("birth", 2).orElseThrow());
		Assertions.assertTrue(EventTypeInfoService.checkPrincipals("marriage", 2).isEmpty());
		Assertions.assertTrue(EventTypeInfoService.checkPrincipals("birth", 1).isEmpty());
	}


	private static void assertInfo(final String type, final int principals, final boolean afterDeath, final boolean marital){
		final EventTypeInfo info = EventTypeInfoService.find(type).orElseThrow();
		Assertions.assertEquals(principals, info.principals(), type);
		Assertions.assertEquals(afterDeath, info.allowedAfterDeath(), type);
		Assertions.assertEquals(marital, info.marital(), type);
	}

}
