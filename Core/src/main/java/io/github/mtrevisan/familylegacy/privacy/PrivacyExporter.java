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
package io.github.mtrevisan.familylegacy.privacy;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Builds the copy of a model that can be distributed under a privacy policy.
 * <p>
 * Compared with the model:
 * <ul>
 *   <li>the records whose effective level is hidden by the policy are omitted;</li>
 *   <li>in the other records, the nodes that reference an omitted record, and the nested structures (for example a source
 *       citation) whose own {@code privacy} is hidden, are removed, together with the wrappers left empty;</li>
 *   <li>in the text, a link to {@code flef:confidential} is dropped, and a link to an omitted record is replaced by a
 *       placeholder, so that the name of the record is not leaked;</li>
 *   <li>the contacts of the submitter in the header are omitted unless their privacy is explicitly public.</li>
 * </ul>
 * The model is not modified.
 */
public final class PrivacyExporter{

	/**
	 * The outcome of the filtering.
	 *
	 * @param model	The model to distribute.
	 * @param omittedRecords	The number of records that have been omitted.
	 * @param omittedNodes	The number of nodes, inside the kept records, that have been removed.
	 */
	public record Result(FLEFModel model, int omittedRecords, int omittedNodes){}


	private static final String TAG_SUBMITTER = "submitter";
	private static final String TAG_CONTACT = "contact";
	private static final String LINK_PREFIX = "flef:";
	private static final String OMITTED = "[omitted]";
	private static final Pattern CONFIDENTIAL_LINK = Pattern.compile("\\[[^\\]]*\\]\\(flef:confidential\\)");
	private static final Pattern RECORD_LINK = Pattern.compile("\\[[^\\]]*\\]\\(flef:([^)\\s]+)\\)");


	private final PrivacyPolicy policy;
	private final PrivacyResolver resolver;
	private final Set<String> hiddenIds = new HashSet<>();
	private int omittedNodes;


	private PrivacyExporter(final FLEFModel model, final PrivacyPolicy policy){
		this.policy = policy;
		resolver = new PrivacyResolver(model);
	}


	/**
	 * Filters the model.
	 *
	 * @param model	The model.
	 * @param policy	The policy; with {@link PrivacyPolicy#SHOW_ALL} the model itself is returned.
	 * @return	The filtered copy.
	 */
	public static Result filter(final FLEFModel model, final PrivacyPolicy policy){
		if(policy == PrivacyPolicy.SHOW_ALL)
			return new Result(model, 0, 0);

		return new PrivacyExporter(model, policy).run(model);
	}


	private Result run(final FLEFModel model){
		final List<FLEFRecord> records = model.getRecords();
		for(final FLEFRecord record : records)
			if(policy.hides(resolver.effectiveLevel(record)))
				hiddenIds.add(record.getId());

		final FLEFModel result = new FLEFModel();

		final FLEFRecord header = FLEFRecord.createEmpty();
		model.getHeader()
			.deepCopyTo(header);
		omitSubmitterContacts(header);
		result.setHeader(header);

		for(final FLEFRecord record : records){
			if(hiddenIds.contains(record.getId()))
				continue;

			final FLEFRecord copy = FLEFRecord.createEmpty();
			record.deepCopyTo(copy);
			prune(copy);
			result.addRecord(copy);
		}
		return new Result(result, hiddenIds.size(), omittedNodes);
	}

	/** Contacts of the submitter are personal data: they are omitted unless they are explicitly public. */
	private void omitSubmitterContacts(final FLEFRecord header){
		for(final FLEFRecord submitter : FLEFRecordHelper.findChildren(header, TAG_SUBMITTER))
			for(final FLEFRecord contact : FLEFRecordHelper.findChildren(submitter, TAG_CONTACT)){
				final PrivacyResolver.Effective privacy = resolver.describe(contact);
				if(privacy.origin() != PrivacyResolver.Origin.DECLARED || privacy.level() != PrivacyLevel.PUBLIC){
					submitter.removeChild(contact);
					omittedNodes ++;
				}
			}
	}

	private void prune(final FLEFRecord node){
		for(final FLEFRecord child : new ArrayList<>(node.getChildren())){
			if(isOmitted(child)){
				node.removeChild(child);
				omittedNodes ++;
				continue;
			}

			if(StringUtils.contains(child.getValue(), LINK_PREFIX))
				child.setValue(redact(child.getValue()));

			final boolean hadChildren = child.hasChildren();
			prune(child);
			//a wrapper left empty by the removals carries nothing
			if(hadChildren && !child.hasChildren() && StringUtils.isEmpty(child.getValue())){
				node.removeChild(child);
				omittedNodes ++;
			}
		}
	}

	/** Whether the nested node references an omitted record or declares a privacy hidden by the policy. */
	private boolean isOmitted(final FLEFRecord child){
		return (hiddenIds.contains(child.getValue()) || policy.hides(resolver.describe(child).level()));
	}

	private String redact(final String text){
		final String withoutConfidential = CONFIDENTIAL_LINK.matcher(text)
			.replaceAll(StringUtils.EMPTY);

		final Matcher matcher = RECORD_LINK.matcher(withoutConfidential);
		final StringBuilder sb = new StringBuilder();
		while(matcher.find())
			matcher.appendReplacement(sb, Matcher.quoteReplacement(hiddenIds.contains(matcher.group(1))
				? OMITTED
				: matcher.group()));
		matcher.appendTail(sb);
		return sb.toString();
	}

}
