package io.github.mtrevisan.familylegacy.gedcom.transformations2;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

import static io.github.mtrevisan.familylegacy.gedcom.transformations2.TransformationHelper.extractSubStructure;


public class RepositoryTransformation implements Transformation<Gedcom, Flef>{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("CONT", "ADR1", "ADR2", "ADR3"));


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> repositories = origin.getRepositories();
		for(final GedcomNode repository : repositories)
			repositoryTo(repository, destination);
	}

	private void repositoryTo(final GedcomNode repository, final Flef destination){
		final GedcomNode destinationRepository = GedcomNode.create("REPOSITORY")
			.withID(repository.getID());
		final String name = extractSubStructure(repository, "NAME")
			.getValue();
		destinationRepository.addChildValue("NAME", name);
		addressStructureTo(repository, destinationRepository, destination);
		contactStructureTo(repository, destinationRepository);
		notesTo(repository, destinationRepository, destination);
		destination.addRepository(destinationRepository);
	}

	private void addressStructureTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final GedcomNode address = extractSubStructure(parent, "ADDR");
		final StringJoiner sj = new StringJoiner(" - ");
		final String wholeAddress = address.extractValueConcatenated();
		if(wholeAddress != null)
			sj.add(wholeAddress);
		for(final GedcomNode child : address.getChildren())
			if(ADDRESS_TAGS.contains(child.getTag())){
				final String value = child.getValue();
				if(value != null)
					sj.add(value);
			}

		final String placeID = destination.getNextPlaceID();
		destination.addPlace(GedcomNode.create("PLACE")
			.withID(placeID)
			.addChildValue("ADDRESS", sj.toString())
			.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
			.addChildValue("STATE", extractSubStructure(address, "STAE").getValue())
			.addChildValue("COUNTRY", extractSubStructure(address, "CTRY").getValue())
		);
		destinationNode.addChildReference("PLACE", placeID);
	}

	private void contactStructureTo(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode destinationContact = GedcomNode.create("CONTACT");
		final List<GedcomNode> phones = parent.getChildrenWithTag("PHON");
		for(final GedcomNode phone : phones)
			destinationContact.addChildValue("PHONE", phone.getValue());
		final List<GedcomNode> emails = parent.getChildrenWithTag("EMAIL");
		for(final GedcomNode email : emails)
			destinationContact.addChildValue("EMAIL", email.getValue());
		final List<GedcomNode> faxes = parent.getChildrenWithTag("FAX");
		for(final GedcomNode fax : faxes)
			destinationContact.addChild(GedcomNode.create("PHONE")
				.withValue(fax.getValue())
				.addChild(GedcomNode.create("TYPE")
					.withValue("fax")));
		final List<GedcomNode> urls = parent.getChildrenWithTag("WWW");
		for(final GedcomNode url : urls)
			destinationContact.addChildValue("URL", url.getValue());
		destinationNode.addChild(destinationContact);
	}

	private void notesTo(final GedcomNode parent, final GedcomNode destinationNode, final Flef destination){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes){
			String noteID = note.getID();
			if(noteID == null){
				noteID = destination.getNextNoteID();

				destination.addNote(GedcomNode.create("NOTE", noteID, note.getValue()));
			}
			destinationNode.addChildReference("NOTE", noteID);
		}
	}


	@Override
	public void from(final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final List<GedcomNode> repositories = origin.getRepositories();
		for(final GedcomNode repository : repositories)
			repositoryFrom(repository, origin, destination);
	}

	private void repositoryFrom(final GedcomNode repository, final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final String name = extractSubStructure(repository, "NAME")
			.getValue();
		final GedcomNode destinationRepository = GedcomNode.create("REPO")
			.withID(repository.getID())
			.addChildValue("NAME", name);
		addressStructureFrom(repository, destinationRepository, origin);
		contactStructureFrom(repository, destinationRepository);
		notesFrom(repository, destinationRepository);
		destination.addRepository(destinationRepository);
	}

	private void addressStructureFrom(final GedcomNode parent, final GedcomNode destinationNode, final Flef origin)
			throws GedcomGrammarParseException{
		final GedcomNode place = extractSubStructure(parent, "PLACE");
		if(!place.isEmpty()){
			final GedcomNode placeRecord = origin.getPlace(place.getID());
			if(placeRecord == null)
				throw GedcomGrammarParseException.create("Place with ID {} not found", place.getID());

			final GedcomNode address = extractSubStructure(placeRecord, "ADDRESS");
			destinationNode.addChild(GedcomNode.create("ADDR")
				.withValue(placeRecord.getValue())
				.addChildValue("CITY", extractSubStructure(address, "CITY").getValue())
				.addChildValue("STAE", extractSubStructure(address, "STATE").getValue())
				.addChildValue("CTRY", extractSubStructure(address, "COUNTRY").getValue()));
		}
	}

	private void contactStructureFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode contact = extractSubStructure(parent, "CONTACT");
		final List<GedcomNode> phones = contact.getChildrenWithTag("PHONE");
		for(final GedcomNode phone : phones)
			if(!"FAX".equals(extractSubStructure(phone, "TYPE").getValue()))
				destinationNode.addChildValue("PHONE", phone.getValue());
		final List<GedcomNode> emails = contact.getChildrenWithTag("EMAIL");
		for(final GedcomNode email : emails)
			destinationNode.addChildValue("EMAIL", email.getValue());
		for(final GedcomNode phone : phones)
			if("fax".equals(extractSubStructure(phone, "TYPE").getValue()))
				destinationNode.addChildValue("FAX", phone.getValue());
		final List<GedcomNode> urls = contact.getChildrenWithTag("URL");
		for(final GedcomNode url : urls)
			destinationNode.addChildValue("WWW", url.getValue());
	}

	private void notesFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final List<GedcomNode> notes = parent.getChildrenWithTag("NOTE");
		for(final GedcomNode note : notes)
			destinationNode.addChild(GedcomNode.create("NOTE")
				.withID(note.getID()));
	}

}
