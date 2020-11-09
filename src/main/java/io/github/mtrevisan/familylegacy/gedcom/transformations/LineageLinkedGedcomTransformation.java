package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;

import java.util.List;

import static io.github.mtrevisan.familylegacy.gedcom.transformations.TransformationHelper.deleteMultipleTag;


public class LineageLinkedGedcomTransformation implements Transformation{

	private static final Transformation HEADER_TRANSFORMATION = new HeaderTransformation();
	private static final Transformation RECORD_TRANSFORMATION = new RecordTransformation();
	private static final Transformation END_OF_FILE_TRANSFORMATION = new EndOfFileTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		final List<GedcomNode> children = root.getChildren();
		final int lastElement = children.size() - 1;
		HEADER_TRANSFORMATION.to(children.get(0), root);
		for(int i = 1; i < lastElement; i ++)
			RECORD_TRANSFORMATION.to(children.get(i), root);
		END_OF_FILE_TRANSFORMATION.to(children.get(lastElement), root);
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		final List<GedcomNode> children = root.getChildren();
		final int size = children.size();
		HEADER_TRANSFORMATION.from(children.get(0), root);
		for(int i = 1; i < size - 1; i ++)
			RECORD_TRANSFORMATION.from(children.get(i), root);
		END_OF_FILE_TRANSFORMATION.from(children.get(size - 1), root);

		//remove PLACEs
		deleteMultipleTag(root, "PLACE");
	}

}
