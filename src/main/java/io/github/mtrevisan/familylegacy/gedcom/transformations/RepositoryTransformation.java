package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.Protocol;

import java.util.List;


public class RepositoryTransformation implements Transformation<Gedcom, Flef>{

	private final Transformer transformerTo = new Transformer(Protocol.FLEF);
	private final Transformer transformerFrom = new Transformer(Protocol.GEDCOM);


	@Override
	public void to(final Gedcom origin, final Flef destination){
		final List<GedcomNode> repositories = origin.getRepositories();
		for(final GedcomNode repository : repositories)
			repositoryTo(repository, destination);
	}

	private void repositoryTo(final GedcomNode repository, final Flef destination){
		final GedcomNode destinationRepository = transformerTo.create("REPOSITORY")
			.withID(repository.getID());
		final String name = transformerTo.extractSubStructure(repository, "NAME")
			.getValue();
		destinationRepository.addChildValue("NAME", name);
		transformerTo.addressStructureTo(repository, destinationRepository, destination);
		contactStructureTo(repository, destinationRepository);
		transformerTo.noteTo(repository, destinationRepository, destination);

		destination.addRepository(destinationRepository);
	}

	private void contactStructureTo(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode destinationContact = transformerTo.create("CONTACT");
		final List<GedcomNode> phones = parent.getChildrenWithTag("PHON");
		for(final GedcomNode phone : phones)
			destinationContact.addChildValue("PHONE", phone.getValue());
		final List<GedcomNode> emails = parent.getChildrenWithTag("EMAIL");
		for(final GedcomNode email : emails)
			destinationContact.addChildValue("EMAIL", email.getValue());
		final List<GedcomNode> faxes = parent.getChildrenWithTag("FAX");
		for(final GedcomNode fax : faxes)
			destinationContact.addChild(transformerTo.create("PHONE")
				.withValue(fax.getValue())
				.addChildValue("TYPE", "fax"));
		final List<GedcomNode> urls = parent.getChildrenWithTag("WWW");
		for(final GedcomNode url : urls)
			destinationContact.addChildValue("URL", url.getValue());
		destinationNode.addChild(destinationContact);
	}


	@Override
	public void from(final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final List<GedcomNode> repositories = origin.getRepositories();
		for(final GedcomNode repository : repositories)
			repositoryFrom(repository, origin, destination);
	}

	private void repositoryFrom(final GedcomNode repository, final Flef origin, final Gedcom destination) throws GedcomGrammarParseException{
		final String name = transformerFrom.extractSubStructure(repository, "NAME")
			.getValue();
		final GedcomNode destinationRepository = transformerFrom.create("REPO")
			.withID(repository.getID())
			.addChildValue("NAME", name);
		transformerFrom.addressStructureFrom(repository, destinationRepository, origin);
		contactStructureFrom(repository, destinationRepository);
		transformerFrom.noteFrom(repository, destinationRepository);

		destination.addRepository(destinationRepository);
	}

	private void contactStructureFrom(final GedcomNode parent, final GedcomNode destinationNode){
		final GedcomNode contact = transformerFrom.extractSubStructure(parent, "CONTACT");
		final List<GedcomNode> phones = contact.getChildrenWithTag("PHONE");
		for(final GedcomNode phone : phones)
			if(!"FAX".equals(transformerFrom.extractSubStructure(phone, "TYPE").getValue()))
				destinationNode.addChildValue("PHONE", phone.getValue());
		final List<GedcomNode> emails = contact.getChildrenWithTag("EMAIL");
		for(final GedcomNode email : emails)
			destinationNode.addChildValue("EMAIL", email.getValue());
		for(final GedcomNode phone : phones)
			if("fax".equals(transformerFrom.extractSubStructure(phone, "TYPE").getValue()))
				destinationNode.addChildValue("FAX", phone.getValue());
		final List<GedcomNode> urls = contact.getChildrenWithTag("URL");
		for(final GedcomNode url : urls)
			destinationNode.addChildValue("WWW", url.getValue());
	}

}
