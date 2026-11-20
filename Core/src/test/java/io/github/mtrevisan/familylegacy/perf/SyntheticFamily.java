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
package io.github.mtrevisan.familylegacy.perf;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A family file made up to measure the program on a big one (see {@link BigFileBenchmark}): people in generations, with their names, sex,
 * birth, baptism, death, marriage and residence (date and place), the parents and the spouses, and some more relationships.
 */
public class SyntheticFamily{

	static final String HEADER = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		""";
	static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

	public static final class Person{
		public String id;
		public boolean male;
		public int year;
		public int gen;
		public String surname;
		public int coupleOfParents = -1;
		public Person p1, p2;
		public java.util.List<Person> kids = new java.util.ArrayList<>();
	}

	public final List<Person> people = new ArrayList<>();
	public int individuals, events, participations, relationships, places, attributes, maxGen;
	public final Random rnd = new Random(42);
	public FLEFModel model;

	final String[] given = new String[400];
	final String[] family = new String[800];
	int ev, ep, rel, attr;
	public int inGenPercent = 30;


	public SyntheticFamily(){
		for(int i = 0; i < given.length; i ++)
			given[i] = "Given" + syll(i);
		for(int i = 0; i < family.length; i ++)
			family[i] = "Family" + syll(i * 7 + 3);
	}

	static String syll(int n){
		final String[] s = {"ba", "ro", "ni", "ca", "ti", "mo", "le", "su", "vi", "do", "fe", "gu"};
		final StringBuilder sb = new StringBuilder();
		do{
			sb.append(s[n % s.length]);
			n /= s.length;
		}while(n > 0);
		return sb.toString();
	}

	String date(final int year){
		return (1 + rnd.nextInt(28)) + " " + MONTHS[rnd.nextInt(12)] + " " + year;
	}

	static FLEFRecord dateNode(final String d){
		return FLEFRecord.createChildWithTag("date")
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point")
					.addChild(FLEFRecord.createChildWithTag("value")
						.addChild(FLEFRecord.createChildWithTagAndValue("full_date", d)))
					.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian"))));
	}

	static FLEFRecord mk(final String id, final String type){
		return FLEFRecord.createMainRecord(id, type).addChild(io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp.now());
	}

	void addEvent(final String type, final int year, final Person... who){
		final String id = "E" + (++ ev);
		final FLEFRecord e = mk(id, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type))
			.addChild(dateNode(date(year)))
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, "P" + (1 + rnd.nextInt(places)))));
		model.addRecord(e);
		events ++;
		for(final Person p : who){
			model.addRecord(mk("EP" + (++ ep), EventParticipationReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
					.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, p.id)))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, id))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
			participations ++;
		}
	}

	Person newPerson(final boolean male, final int year, final String surname, final int gen){
		final Person p = new Person();
		p.id = "I" + (++ individuals);
		p.male = male;
		p.year = year;
		p.gen = gen;
		p.surname = surname;
		maxGen = Math.max(maxGen, gen);
		people.add(p);
		model.addRecord(mk(p.id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_VALUE, given[rnd.nextInt(given.length)])))
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_VALUE, surname)))));
		model.addRecord(mk("A" + (++ attr), IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, p.id))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE,
				male? IndividualReader.ENUM_SEX_MALE: IndividualReader.ENUM_SEX_FEMALE)));
		attributes ++;
		addEvent(EventReader.ENUM_TYPE_BIRTH, year, p);
		if(rnd.nextInt(100) < 75)
			addEvent(EventReader.CUSTOM_TYPE_BAPTISM, year, p);
		if(rnd.nextInt(100) < 70)
			addEvent(EventReader.ENUM_TYPE_DEATH, year + 30 + rnd.nextInt(50), p);
		if(rnd.nextInt(100) < 25)
			addEvent("residence", year + 18 + rnd.nextInt(20), p);
		return p;
	}

	void link(final Person child, final Person parent, final String type){
		model.addRecord(mk("R" + (++ rel), RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, child.id)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, parent.id)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type)));
		relationships ++;
	}

	public FLEFModel generate(final int targetIndividuals, final int founders, final int places){
		this.places = places;
		model = new FLEFParser().parse(HEADER);
		for(int i = 1; i <= places; i ++)
			model.addRecord(mk("P" + i, PlaceReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
					.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, "Place" + syll(i)))));

		List<Person> current = new ArrayList<>();
		for(int i = 0; i < founders; i ++)
			current.add(newPerson(i % 2 == 0, 1600 + rnd.nextInt(10), family[rnd.nextInt(family.length)], 0));
		int couple = 0;
		int gen = 0;
		while(individuals < targetIndividuals){
			final List<Person> next = new ArrayList<>();
			final List<Person> males = new ArrayList<>();
			final List<Person> females = new ArrayList<>();
			for(final Person p : current)
				(p.male? males: females).add(p);
			java.util.Collections.shuffle(males, rnd);
			java.util.Collections.shuffle(females, rnd);
			final List<Person[]> couples = new ArrayList<>();
			//partners: 30% inside the generation (not siblings), the rest from outside; 85% of the people marry
			int fi = 0;
			for(final Person m : males){
				if(rnd.nextInt(100) >= 85)
					continue;
				Person w = null;
				if(rnd.nextInt(100) < inGenPercent){
					for(int tries = 0; tries < 5 && fi < females.size(); tries ++){
						final Person c = females.get(fi);
						if(c.coupleOfParents < 0 || c.coupleOfParents != m.coupleOfParents){
							w = c;
							fi ++;
							break;
						}
						fi ++;
					}
				}
				if(w == null && individuals >= targetIndividuals)
					continue;
				if(w == null)
					w = newPerson(false, m.year + rnd.nextInt(6) - 3, family[rnd.nextInt(family.length)], gen);
				couples.add(new Person[]{m, w});
			}
			//the women left marry outsiders
			for(; fi < females.size(); fi ++){
				final Person w = females.get(fi);
				if(rnd.nextInt(100) >= 85)
					continue;
				if(individuals >= targetIndividuals)
					continue;
				final Person m = newPerson(true, w.year + rnd.nextInt(6) - 3, family[rnd.nextInt(family.length)], gen);
				couples.add(new Person[]{m, w});
			}
			for(final Person[] c : couples){
				couple ++;
				link(c[0], c[1], RelationshipReader.ENUM_TYPE_SPOUSE);
				addEvent(EventReader.ENUM_TYPE_MARRIAGE, Math.max(c[0].year, c[1].year) + 22, c[0], c[1]);
				final int kids = new int[]{1, 2, 2, 2, 3, 3, 4}[rnd.nextInt(7)];
				for(int k = 0; k < kids && individuals < targetIndividuals; k ++){
					final Person ch = newPerson(rnd.nextBoolean(), (c[0].year + c[1].year) / 2 + 24 + k * 2, c[0].surname, gen + 1);
					ch.coupleOfParents = couple;
					ch.p1 = c[0];
					ch.p2 = c[1];
					c[0].kids.add(ch);
					c[1].kids.add(ch);
					link(ch, c[0], RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
					link(ch, c[1], RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
					next.add(ch);
				}
			}
			if(next.isEmpty())
				break;
			current = next;
			gen ++;
		}
		//a few more relationships (godparents, associates), to reach the requested volume
		return model;
	}

	/** Adds associate relationships between random individuals until the total relationships are the given ones. */
	public void padRelationships(final long target){
		while(relationships < target){
			final Person a = people.get(rnd.nextInt(people.size()));
			final Person b = people.get(rnd.nextInt(people.size()));
			if(a != b)
				link(a, b, RelationshipReader.ENUM_TYPE_ASSOCIATE);
		}
	}

	/** Pads the events with residences until the total events are the given ones. */
	public void padEvents(final long target){
		while(events < target){
			final Person a = people.get(rnd.nextInt(people.size()));
			addEvent("residence", a.year + 20 + rnd.nextInt(30), a);
		}
	}

	public static int ancestors(final Person p){
		final java.util.Set<Person> seen = new java.util.HashSet<>();
		final java.util.ArrayDeque<Person> q = new java.util.ArrayDeque<>();
		q.add(p);
		while(!q.isEmpty()){
			final Person x = q.poll();
			if(x.p1 != null && seen.add(x.p1))
				q.add(x.p1);
			if(x.p2 != null && seen.add(x.p2))
				q.add(x.p2);
		}
		return seen.size();
	}

	public static int descendants(final Person p){
		final java.util.Set<Person> seen = new java.util.HashSet<>();
		final java.util.ArrayDeque<Person> q = new java.util.ArrayDeque<>();
		q.add(p);
		while(!q.isEmpty())
			for(final Person k : q.poll().kids)
				if(seen.add(k))
					q.add(k);
		return seen.size();
	}

	public Person richestAncestry(){
		Person best = null;
		int bestCount = -1;
		for(final Person p : people)
			if(p.gen >= maxGen - 1 && p.p1 != null){
				final int c = ancestors(p);
				if(c > bestCount){
					best = p;
					bestCount = c;
				}
			}
		return best;
	}

	public Person richestDescendance(){
		Person best = null;
		int bestCount = -1;
		for(final Person p : people)
			if(p.gen == 1 && p.p1 != null && !p.kids.isEmpty()){
				final int c = descendants(p);
				if(c > bestCount){
					best = p;
					bestCount = c;
				}
			}
		return best;
	}
}
