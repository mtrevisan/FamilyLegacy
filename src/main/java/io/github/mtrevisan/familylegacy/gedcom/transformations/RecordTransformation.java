package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;


public class RecordTransformation implements Transformation{

	private static final Transformation FAMILY_RECORD_TRANSFORMATION = new FamilyRecordTransformation();
	private static final Transformation INDIVIDUAL_RECORD_TRANSFORMATION = new IndividualRecordTransformation();
	private static final Transformation MULTIMEDIA_RECORD_TRANSFORMATION = new MultimediaRecordTransformation();
	private static final Transformation NOTE_RECORD_TRANSFORMATION = new NoteRecordTransformation();
	private static final Transformation REPOSITORY_RECORD_TRANSFORMATION = new RepositoryRecordTransformation();
	private static final Transformation SOURCE_RECORD_TRANSFORMATION = new SourceRecordTransformation();
	private static final Transformation SUBMITTER_RECORD_TRANSFORMATION = new SubmitterRecordTransformation();


	@Override
	public void to(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren()){
			final String tag = child.getTag();
			if("FAM".equals(tag))
				FAMILY_RECORD_TRANSFORMATION.to(child, root);
			else if("INDI".equals(tag))
				INDIVIDUAL_RECORD_TRANSFORMATION.to(child, root);
			else if("OBJE".equals(tag))
				MULTIMEDIA_RECORD_TRANSFORMATION.to(child, root);
			else if("NOTE".equals(tag))
				NOTE_RECORD_TRANSFORMATION.to(child, root);
			else if("REPO".equals(tag))
				REPOSITORY_RECORD_TRANSFORMATION.to(child, root);
			else if("SOUR".equals(tag))
				SOURCE_RECORD_TRANSFORMATION.to(child, root);
			else if("SUBM".equals(tag))
				SUBMITTER_RECORD_TRANSFORMATION.to(child, root);
		}
	}

	@Override
	public void from(final GedcomNode node, final GedcomNode root){
		for(final GedcomNode child : node.getChildren()){
			final String tag = child.getTag();
			if("INDIVIDUAL".equals(tag))
				INDIVIDUAL_RECORD_TRANSFORMATION.from(child, root);
			else if("FAMILY".equals(tag))
				FAMILY_RECORD_TRANSFORMATION.from(child, root);
			else if("DOCUMENT".equals(tag))
				MULTIMEDIA_RECORD_TRANSFORMATION.from(child, root);
			else if("NOTE".equals(tag))
				NOTE_RECORD_TRANSFORMATION.from(child, root);
			else if("REPOSITORY".equals(tag))
				REPOSITORY_RECORD_TRANSFORMATION.from(child, root);
			else if("SOURCE".equals(tag))
				SOURCE_RECORD_TRANSFORMATION.from(child, root);
			else if("SUBMITTER".equals(tag))
				SUBMITTER_RECORD_TRANSFORMATION.from(child, root);
		}
	}

}
