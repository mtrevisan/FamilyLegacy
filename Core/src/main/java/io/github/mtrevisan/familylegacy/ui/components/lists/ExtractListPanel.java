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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.api.LinkRole;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.ExtractHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;


/**
 * Panel for the extracts a source citation references ({@code extract*: Xref<ExtractRecord>}).
 * <p>
 * The extracts are records of the source cited: a new extract is created for that source, an existing extract is chosen among the
 * extracts of that source, and removing an extract from the list removes only the reference (the extract stays with its source, and
 * other citations may reference it).
 */
public class ExtractListPanel extends AbstractListPanel<FLEFRecord>{

	private final String path;
	/** Tells the id of the source currently cited by the citation being edited. */
	private final Supplier<String> citedSource;

	/** The ids of the extracts created from this panel, to discard if the citation is not saved. */
	private final Set<String> createdIds = new LinkedHashSet<>();


	/**
	 * @param path	The path of the references in the citation.
	 * @param parent	The parent window.
	 * @param panelTitle	The border title.
	 * @param model	The model.
	 * @param citedSource	Tells the id of the source currently cited (it may change while the citation is edited).
	 */
	public ExtractListPanel(final String path, final Window parent, final String panelTitle, final FLEFModel model,
			final Supplier<String> citedSource){
		super(parent, panelTitle, model);

		this.path = path;
		this.citedSource = citedSource;


		initComponents();
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		BindingsHelper.installBehavior(list,
			this::editItem, null,
			this::createNewItem, this::removeItem,
			builder -> {
				builder.item(I18N.t("popupmenu.create_new"), this::createNewItem);
				builder.item(I18N.t("popupmenu.add_existing"), this::addItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit"), this::editItem);
				builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplayText(final FLEFRecord record){
		return ExtractHandler.getInstance().getDisplayText(record, model);
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final FLEFRecord source = citedSourceRecord();
		if(source == null)
			return null;

		final RecordDialog dialog = ExtractHandler.getInstance().createNewDialog(parent, model);
		dialog.link(LinkRole.SOURCE, source);
		dialog.setVisible(true);
		if(!dialog.isSaved())
			return null;

		final FLEFRecord extract = dialog.getRecord();
		createdIds.add(extract.getId());
		return extract;
	}

	/** Chooses one of the extracts of the source cited that the citation does not reference yet. */
	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord source = citedSourceRecord();
		if(source == null)
			return null;

		final List<FLEFRecord> listed = getItems();
		final List<Choice> choices = new ArrayList<>();
		for(final FLEFRecord extract : ExtractReader.extractsOfSource(source.getId(), model))
			if(!listed.contains(extract))
				choices.add(new Choice(extract, getDisplayText(extract) + " [" + extract.getId() + "]"));
		if(choices.isEmpty()){
			JOptionPane.showMessageDialog(parent,
				I18N.tf("dialog.extract.none_of_source", source.getId()),
				I18N.t("dialog.source_citation.extract.title"),
				JOptionPane.INFORMATION_MESSAGE);

			return null;
		}

		final Object selected = JOptionPane.showInputDialog(parent,
			I18N.tf("dialog.extract.choose_of_source", source.getId()),
			I18N.t("dialog.source_citation.extract.title"),
			JOptionPane.QUESTION_MESSAGE, null,
			choices.toArray(), choices.getFirst());
		return (selected instanceof final Choice choice? choice.extract(): null);
	}

	/** Edits the extract in place (it is a record: the change is seen by every citation that references it). */
	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		final RecordDialog dialog = ExtractHandler.getInstance().createEditDialog(parent, model, record);
		dialog.setVisible(true);
		return record;
	}

	private FLEFRecord citedSourceRecord(){
		final String sourceId = citedSource.get();
		final FLEFRecord source = (StringUtils.isNotEmpty(sourceId)? model.getRecordById(sourceId): null);
		if(source == null)
			JOptionPane.showMessageDialog(parent,
				I18N.t("dialog.extract.source_first"),
				I18N.t("dialog.source_citation.extract.title"),
				JOptionPane.INFORMATION_MESSAGE);
		return source;
	}


	/**
	 * @param sourceId	The id of the source cited.
	 * @return	The extracts in the list that do not belong to the source (they cannot be referenced by a citation of it).
	 */
	public List<FLEFRecord> extractsNotOf(final String sourceId){
		final List<FLEFRecord> others = new ArrayList<>(0);
		for(final FLEFRecord extract : getItems())
			if(!Objects.equals(sourceId, ExtractReader.extractSource(extract)))
				others.add(extract);
		return others;
	}

	/**
	 * Removes from the model the extracts created from this panel that no list references anymore: to call when the citation is not
	 * saved, since nothing else references them.
	 */
	public void discardCreated(){
		for(final String id : createdIds)
			model.removeRecord(id);
		createdIds.clear();
	}

	/** Forgets the extracts created from this panel: to call when the citation is saved. */
	public void keepCreated(){
		final List<FLEFRecord> items = getItems();
		//an extract created and then removed from the list is referenced by nothing: it is not kept
		for(final String id : createdIds){
			final FLEFRecord extract = model.getRecordById(id);
			if(extract != null && !items.contains(extract))
				model.removeRecord(id);
		}
		createdIds.clear();
	}

	public void load(final FLEFRecord citation){
		clear();

		if(citation == null || citation.isEmpty())
			return;

		setItems(ExtractReader.extractsOf(citation, model));
	}

	public void save(final FLEFRecord citation){
		FLEFRecordHelper.removeChildren(citation, path);
		for(final FLEFRecord extract : getItems())
			citation.addChild(FLEFRecord.createChildWithTagAndValue(path, extract.getId()));
	}


	/** An extract shown in the list to choose from. */
	private record Choice(FLEFRecord extract, String text){
		@Override
		public String toString(){
			return text;
		}
	}

}
