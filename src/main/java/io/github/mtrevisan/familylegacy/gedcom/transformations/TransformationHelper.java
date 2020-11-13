package io.github.mtrevisan.familylegacy.gedcom.transformations;

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;


final class TransformationHelper{

	private TransformationHelper(){}

	static String joinIfNotNull(final String separator, final String... components){
		final StringJoiner sj = new StringJoiner(separator);
		for(final String component : components)
			if(component != null)
				sj.add(component);
		return (sj.length() > 0? sj.toString(): null);
	}

	static GedcomNode extractSubStructure(final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(final String tag : tags){
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.size() != 1)
				return GedcomNode.createEmpty();

			current = childrenWithTag.get(0);
		}
		return current;
	}

	static GedcomNode moveTag(final String value, final GedcomNode context, final String... tags){
		final GedcomNode currentContext = extractSubStructure(context, tags);

		if(!currentContext.isEmpty())
			currentContext.withTag(value);
		return currentContext;
	}

	static List<GedcomNode> moveMultipleTag(final String value, final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(int i = 0; i < tags.length - 1; i ++){
			final String tag = tags[i];
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.size() != 1)
				return Collections.emptyList();

			current = childrenWithTag.get(0);
		}

		final List<GedcomNode> currentContexts = current.getChildrenWithTag(tags[tags.length - 1]);
		for(final GedcomNode currentContext : currentContexts)
			currentContext.withTag(value);
		return currentContexts;
	}

	static void deleteTag(final GedcomNode context, final String... tags){
		final String lastTag = tags[tags.length - 1];
		final String[] firstTags = ArrayUtils.remove(tags, tags.length - 1);
		final GedcomNode currentContext = extractSubStructure(context, firstTags);

		if(!currentContext.isEmpty())
			currentContext.getChildren()
				.removeIf(gedcomNode -> lastTag.equals(gedcomNode.getTag()));
		if(currentContext.getChildren().isEmpty())
			currentContext.removeChildren();
	}

	static List<GedcomNode> deleteMultipleTag(final GedcomNode context, final String... tags){
		GedcomNode current = context;
		for(int i = 0; i < tags.length - 1; i ++){
			final String tag = tags[i];
			final List<GedcomNode> childrenWithTag = current.getChildrenWithTag(tag);
			if(childrenWithTag.size() != 1)
				return Collections.emptyList();

			current = childrenWithTag.get(0);
		}

		final List<GedcomNode> currentContexts = current.getChildrenWithTag(tags[tags.length - 1]);
		for(final GedcomNode currentContext : currentContexts)
			current.removeChild(currentContext);
		if(current.getChildren().isEmpty())
			current.removeChildren();
		return currentContexts;
	}

	static void transferValues(final GedcomNode context, final String tag, final GedcomNode destination, final String destinationTag){
		final List<GedcomNode> componentContext = context.getChildrenWithTag(tag);
		for(final GedcomNode child : componentContext){
			destination.addChild(GedcomNode.create(destinationTag)
				.withValue(child.getValue()));

			context.removeChild(child);
		}
	}

}
