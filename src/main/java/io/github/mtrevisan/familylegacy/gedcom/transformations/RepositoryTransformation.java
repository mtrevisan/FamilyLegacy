/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;


public class RepositoryTransformation extends Transformation<Gedcom, Flef>{

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
	public void from(final Flef origin, final Gedcom destination){
		final List<GedcomNode> repositories = origin.getRepositories();
		for(final GedcomNode repository : repositories)
			repositoryFrom(repository, origin, destination);
	}

	private void repositoryFrom(final GedcomNode repository, final Flef origin, final Gedcom destination){
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
