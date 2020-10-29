package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;


final class TransformationHelper{

	private static final Collection<String> ADDRESS_TAGS = new HashSet<>(Arrays.asList("ADDR", "CONT", "ADR1", "ADR2", "ADR3"));


	private TransformationHelper(){}

//	public static void addNode(final GedcomNode node, final GedcomNode context, final String... tags){
//		final GedcomNode currentContext = extractSubStructure(context, tags);
//
//		if(!currentContext.isEmpty())
//			currentContext.addChild(node);
//	}

	public static GedcomNode moveTag(final String value, final GedcomNode context, final String... tags){
		final GedcomNode currentContext = extractSubStructure(context, tags);

		if(!currentContext.isEmpty())
			currentContext.withTag(value);
		return currentContext;
	}

	public static void deleteTag(final GedcomNode context, final String... tags){
		final String lastTag = tags[tags.length - 1];
		final String[] firstTags = ArrayUtils.remove(tags, tags.length - 1);
		final GedcomNode currentContext = extractSubStructure(context, firstTags);

		if(!currentContext.isEmpty())
			currentContext.getChildren()
				.removeIf(gedcomNode -> lastTag.equals(gedcomNode.getTag()));
		if(currentContext.getChildren().isEmpty())
			currentContext.removeChildren();
	}

	public static void transferValues(final GedcomNode context, final String tag, final GedcomNode destination, final String destinationTag){
		final List<GedcomNode> componentContext = context.getChildrenWithTag(tag);
		for(final GedcomNode child : componentContext){
			destination.addChild(GedcomNode.create(destinationTag)
				.withValue(child.getValue()));

			context.removeChild(child);
		}
	}


	/** NOTE: remember to set xref! */
	public static GedcomNode extractPlaceStructure(final GedcomNode context, final String... tags){
		final GedcomNode parentContext = extractSubStructure(context, tags);
		final GedcomNode placeContext = extractSubStructure(parentContext, "ADDR");

		GedcomNode place = GedcomNode.createEmpty();
		if(!placeContext.isEmpty()){
			final StringJoiner street = new StringJoiner(" - ");
			String value = placeContext.getValue();
			final Iterator<GedcomNode> itr = placeContext.getChildren().iterator();
			while(itr.hasNext()){
				final GedcomNode child = itr.next();
				if(ADDRESS_TAGS.contains(child.getTag())){
					final String component = child.getValue();
					if(component != null && ! component.isEmpty())
						street.add(component);

					itr.remove();
				}
			}
			if(street.length() > 0)
				value = street.toString();

			place = GedcomNode.create("PLACE");
			if(value != null && !value.isEmpty())
				place.addChild(GedcomNode.create("STREET")
					.withValue(value));
			transferValues(placeContext, "CITY", place, "CITY");
			transferValues(placeContext, "STAE", place, "STATE");
			transferValues(placeContext, "POST", place, "POSTAL_CODE");
			transferValues(placeContext, "CTRY", place, "COUNTRY");

			transferValues(parentContext, "PHON", place, "PHONE");
			transferValues(parentContext, "FAX", place, "FAX");
			transferValues(parentContext, "EMAIL", place, "EMAIL");
			transferValues(parentContext, "WWW", place, "WWW");

			parentContext.removeChild(placeContext);
		}
		return place;
	}

	/** NOTE: remember to set xref! */
	public static GedcomNode extractNote(final GedcomNode context){
		return (context.isEmpty() || context.getID() != null? context:
			GedcomNode.create("NOTE").withValue(context.getValueConcatenated()));
	}

	/** NOTE: remember to set xref! */
	public static GedcomNode extractSourceCitation(final GedcomNode context, final GedcomNode root){
		GedcomNode source = GedcomNode.createEmpty();
		if(!context.isEmpty()){
			source.withTag("SOURCE");
			final String sourceID = context.getID();
			if(sourceID != null){
				source.addChild(extractSubStructure(context, "PAGE"));
				final GedcomNode sourceEvent = extractSubStructure(context, "EVEN");
				sourceEvent.withTag("EVENT");
				source.addChild(sourceEvent);
				source.addChild(extractSubStructure(context, "DATA", "DATE"));
				final String text = extractSubStructure(context, "DATA", "TEXT")
					.getValueConcatenated();
				if(text != null){
					final GedcomNode sourceNote = GedcomNode.create("NOTE")
						.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()))
						.withValue(text);
					root.addChild(sourceNote, 1);
					source.addChild(GedcomNode.create("NOTE")
						.withID(sourceNote.getID()));
				}
			}
			else{
				String text = context.getValueConcatenated();
				if(text != null){
					final GedcomNode sourceNote = GedcomNode.create("NOTE")
						.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()))
						.withValue(text);
					root.addChild(sourceNote, 1);
					source.addChild(GedcomNode.create("NOTE")
						.withID(sourceNote.getID()));
				}
				final List<GedcomNode> sourceTexts = context.getChildrenWithTag("TEXT");
				for(final GedcomNode sourceText : sourceTexts){
					text = sourceText.getValueConcatenated();
					if(text != null){
						final GedcomNode sourceNote = GedcomNode.create("NOTE")
							.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()))
							.withValue(text);
						root.addChild(sourceNote, 1);
						source.addChild(GedcomNode.create("NOTE")
							.withID(sourceNote.getID()));
					}
				}
			}
			final List<GedcomNode> sourceDocuments = context.getChildrenWithTag("OBJE");
			for(final GedcomNode sourceDocument : sourceDocuments){
				final GedcomNode n = extractDocument(sourceDocument);
				if(n.getID() == null){
					n.withID(Flef.getNextDocumentID(root.getChildrenWithTag("DOCUMENT").size()));
					root.addChild(n, 1);
					source.addChild(GedcomNode.create("DOCUMENT")
						.withID(n.getID()));
				}
				else
					source.addChild(n);
			}
			final List<GedcomNode> notes = context.getChildrenWithTag("NOTE");
			for(final GedcomNode note : notes){
				final GedcomNode n = extractNote(note);
				if(n.getID() == null){
					n.withID(Flef.getNextNoteID(root.getChildrenWithTag("NOTE").size()));
					root.addChild(n, 1);
					source.addChild(GedcomNode.create("NOTE")
						.withID(n.getID()));
				}
				else
					source.addChild(n);
			}
			source.addChild(moveTag("CERTAINTY", context, "QUAY"));
		}
		return source;
	}

	/** NOTE: remember to set xref! */
	public static GedcomNode extractDocument(final GedcomNode context){
		if(!context.isEmpty()){
			context.withTag("DOCUMENT");
			if(context.getID() == null){
				final List<GedcomNode> docFiles = context.getChildrenWithTag("FILE");
				for(final GedcomNode docFile : docFiles){
					final GedcomNode format = moveTag("FORMAT", docFile, "FORM");
					deleteTag(format, "MEDI");
				}
				moveTag("TITLE", context, "TITL");
			}
		}
		return context;
	}

	public static void mergeNote(final GedcomNode context, final String... keys){
		final GedcomNode currentContext = extractSubStructure(context, keys);

		if(!currentContext.isEmpty()){
			currentContext.withValue(currentContext.getValueConcatenated());
			deleteTag(currentContext, "CONC", "CONT");

			final StringBuilder sb = new StringBuilder(currentContext.getValue());
			final Iterator<GedcomNode> itr = currentContext.getChildren().iterator();
			while(itr.hasNext()){
				final GedcomNode child = itr.next();
				if("CONC".equals(child.getTag())){
					sb.append(child.getValue());
					itr.remove();
				}
				else if("CONT".equals(child.getTag())){
					sb.append("\\n");
					sb.append(child.getValue());
					itr.remove();
				}
			}
			context.withValue(sb.toString());
		}
	}

	public static void splitNote(final GedcomNode context, final String... keys){
		final GedcomNode currentContext = extractSubStructure(context, keys);

		if(!currentContext.isEmpty())
			currentContext.setValueConcatenated(currentContext.getValue());
	}


	public static GedcomNode extractSubStructure(final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(final String tag : tags){
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.size() != 1)
				return GedcomNode.createEmpty();

			current = childrenWithTag.get(0);
		}
		return current;
	}

}
