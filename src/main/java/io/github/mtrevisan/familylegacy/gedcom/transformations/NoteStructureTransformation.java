package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;


public class NoteStructureTransformation implements Transformation{

	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		if(node.getID() == null){
			//create a note in the root:
//			node.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()));
			root.addChild(GedcomNode.create("NOTE")
				.withID(node.getID())
				.withValue(node.extractValueConcatenated()));
			node.removeValue();
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		node.withValueConcatenated(node.getValue());
	}

}
