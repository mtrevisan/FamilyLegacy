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

import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNodeBuilder;
import io.github.mtrevisan.familylegacy.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TransformerHelper{

	//tag, or tag{value}, tag[index], or tag{value}[index], or tag#id, or tag#id{value}
	private static final String PARAM_TAG = "tag";
	private static final String PARAM_VALUE = "value";
	private static final String PARAM_INDEX = "index";
	private static final String PARAM_ID = "id";
	private static final String PARAM_XREF = "xref";
	private static final String PATH_COMPONENT = "[^#@{}\\[\\])]+";
	private static final String PATH_COMPONENT_OPTIONAL = "[^#{}\\[\\])]*";
	private static final String PATH_COMPONENT_TAG = "(?<" + PARAM_TAG + ">" + PATH_COMPONENT + ")";
	/** Value is between curly brackets (`{value}`). */
	private static final String PATH_COMPONENT_VALUE = "\\{(?<" + PARAM_VALUE + ">" + PATH_COMPONENT + ")\\}";
	/** Index is between square brackets (`[index]`). */
	private static final String PATH_COMPONENT_INDEX = "\\[(?<" + PARAM_INDEX + ">" + PATH_COMPONENT_OPTIONAL + ")\\]";
	/** ID is preceded by sharp (`#id`). */
	private static final String PATH_COMPONENT_ID = "#(?<" + PARAM_ID + ">" + PATH_COMPONENT + ")";
	/** XRef is preceded by at-sign (`@id`). */
	private static final String PATH_COMPONENT_XREF = "@(?<" + PARAM_XREF + ">" + PATH_COMPONENT + ")";
	private static final Pattern PATH_COMPONENTS = RegexHelper.pattern(
		PATH_COMPONENT_TAG + "?(?:" + PATH_COMPONENT_ID + ")?(?:" + PATH_COMPONENT_XREF + ")?(?:" + PATH_COMPONENT_VALUE + ")?(?:"
			+ PATH_COMPONENT_INDEX + ")?"
	);


	private final Protocol protocol;


	public TransformerHelper(final Protocol protocol){
		this.protocol = protocol;
	}


	public GedcomNode createEmpty(){
		return GedcomNodeBuilder.createEmpty(protocol);
	}

	public GedcomNode create(final String tag){
		return GedcomNodeBuilder.create(protocol, tag);
	}

	public GedcomNode createWithID(final String tag, final String id){
		return GedcomNodeBuilder.createWithID(protocol, tag, id);
	}

	public GedcomNode createWithValue(final String tag, final String value){
		return GedcomNodeBuilder.createWithValue(protocol, tag, value);
	}

	public GedcomNode createWithIDValue(final String tag, final String id, final String value){
		return GedcomNodeBuilder.createWithIDValue(protocol, tag, id, value);
	}

	public GedcomNode createWithReference(final String tag, final String xref){
		return GedcomNodeBuilder.createWithReference(protocol, tag, xref);
	}


	/**
	 * @param origin	Origin node from which to start the traversal.
	 * @param path	The path to follow from the origin in the form `tag#id@xref{value}[index]` or `(tag1|tag2)#id@xref{value}[index]` and separated by dots.
	 * @return	The final node.
	 */
	public GedcomNode traverse(final GedcomNode origin, final String path){
		final GedcomNode node = (GedcomNode)traverseInner(origin, path);
		return (node != null? node: createEmpty());
	}

	/**
	 * @param origin	Origin node from which to start the traversal.
	 * @param path	The path to follow from the origin in the form `tag#id@xref{value}[]` or `(tag1|tag2)#id@xref{value}[]` and separated by dots.
	 * 	<p>The void array MUST BE last in the sequence.</p>
	 * @return	The final node list.
	 */
	@SuppressWarnings("unchecked")
	public List<GedcomNode> traverseAsList(final GedcomNode origin, String path){
		final boolean hasCloseParenthesis = (path.charAt(path.length() - 1) == ']');
		final boolean hasOpenParenthesis = (path.charAt(path.length() - 2) == '[');
		if(hasCloseParenthesis && !hasOpenParenthesis)
			throw new IllegalArgumentException("The array indication `[]` must be last in the path, was " + path);
		else if(!hasCloseParenthesis && !hasOpenParenthesis)
			path += "[]";
		final List<GedcomNode> nodes = (List<GedcomNode>)traverseInner(origin, path);
		return (nodes != null? nodes: Collections.emptyList());
	}

	private Object traverseInner(final GedcomNode origin, final String path){
		Object pointer = origin;
		if(origin != null){
			final String[] components = StringUtils.split(path, '.');
			for(final String component : components){
				if(pointer instanceof List)
					throw new IllegalArgumentException("Only the last step of the path can produce an array, was " + path);

				final Matcher m = RegexHelper.matcher(component, PATH_COMPONENTS);
				if(m.find()){
					final List<GedcomNode> nodes = new ArrayList<>(((GedcomNode)pointer).getChildren());
					removeNodeIfTag(nodes, m.group(PARAM_TAG));
					removeNodeIf(nodes, m.group(PARAM_VALUE), GedcomNode::getValue);
					removeNodeIf(nodes,  m.group(PARAM_ID), GedcomNode::getID);
					removeNodeIf(nodes, m.group(PARAM_XREF), GedcomNode::getXRef);

					final String index = m.group(PARAM_INDEX);
					if(index == null){
						final int size = nodes.size();
						if(size > 1)
							throw new IllegalArgumentException("More than one node is selected from path " + path);
						else if(size == 1)
							pointer = nodes.get(0);
						else{
							pointer = null;
							break;
						}
					}
					else if(index.isEmpty())
						pointer = nodes;
					else
						pointer = nodes.get(Integer.parseInt(index));
				}
				else
					throw new IllegalArgumentException("Illegal path " + path);
			}
		}
		return pointer;
	}

	private void removeNodeIfTag(final List<GedcomNode> nodes, final String tag){
		if(tag != null){
			final String[] tags = (tag.charAt(0) == '(' && tag.charAt(tag.length() - 1) == ')'?
				StringUtils.split(tag.substring(1, tag.length() - 1), '|'): new String[]{tag});
			Arrays.sort(tags);

			final Iterator<GedcomNode> itr = nodes.iterator();
			while(itr.hasNext())
				if(Arrays.binarySearch(tags, itr.next().getTag()) < 0)
					itr.remove();
		}
	}

	private void removeNodeIf(final List<GedcomNode> nodes, final String value, final Function<GedcomNode, String> extractor){
		if(value != null){
			final Iterator<GedcomNode> itr = nodes.iterator();
			while(itr.hasNext()){
				final GedcomNode node = itr.next();
				if(!value.equals(extractor.apply(node)))
					itr.remove();
			}
		}
	}


	public void transferValue(final GedcomNode origin, final String originPath, final GedcomNode destination, final String destinationPath){
		final GedcomNode originNode = traverse(origin, originPath);
		if(!originNode.isEmpty())
			traverseAndCreate(destination, destinationPath)
				.withValue(originNode.getValue());
	}

	private GedcomNode traverseAndCreate(final GedcomNode origin, final String path){
		GedcomNode pointer = origin;
		if(origin != null){
			final String[] components = StringUtils.split(path, '.');
			for(final String component : components){
				final Matcher m = RegexHelper.matcher(component, PATH_COMPONENTS);
				if(m.find()){
					final List<GedcomNode> nodes = new ArrayList<>(pointer.getChildren());
					final String tag = m.group(PARAM_TAG);
					removeNodeIf(nodes, tag, GedcomNode::getTag);
					removeNodeIf(nodes, m.group(PARAM_VALUE), GedcomNode::getValue);
					removeNodeIf(nodes,  m.group(PARAM_ID), GedcomNode::getID);
					removeNodeIf(nodes, m.group(PARAM_XREF), GedcomNode::getXRef);

					//create node if needed
					if(nodes.isEmpty())
						nodes.add(create(tag));

					final String index = m.group(PARAM_INDEX);
					if(index == null){
						final int size = nodes.size();
						if(size > 1)
							throw new IllegalArgumentException("More than one node is selected from path " + path);
						else if(size == 1)
							pointer = nodes.get(0);
						else{
							pointer = null;
							break;
						}
					}
					else if(index.isEmpty())
						throw new IllegalArgumentException("Cannot create an array from path " + path);
					else
						pointer = nodes.get(Integer.parseInt(index));
				}
				else
					throw new IllegalArgumentException("Illegal path " + path);
			}
		}
		return pointer;
	}

}
