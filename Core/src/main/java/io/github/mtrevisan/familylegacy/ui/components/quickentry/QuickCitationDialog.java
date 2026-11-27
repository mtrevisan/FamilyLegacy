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
package io.github.mtrevisan.familylegacy.ui.components.quickentry;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quickentry.RecordRef;
import io.github.mtrevisan.familylegacy.quickentry.RepositoryLookup;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.HintTextField;
import io.github.mtrevisan.familylegacy.ui.helpers.PickMenu;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * The short way to cite a source that is not in the file yet: its title, where in it (the locator) and, if known, where it is kept. The
 * dialog only asks; the records are written by the caller (see {@code SourceQuickEntry}) once it is confirmed.
 * <p>
 * If the file has repositories named like the one typed the user is asked which one is meant (a name is never matched silently).
 */
public final class QuickCitationDialog extends JDialog{

	private static final int LOOKUP_LIMIT = 10;


	/**
	 * What the user asked for.
	 *
	 * @param title	The title of the source (not blank).
	 * @param locator	Where in the source (blank if not said).
	 * @param repository	Where the source is kept.
	 */
	public record Result(String title, String locator, RecordRef repository){}


	private final FLEFModel model;

	private final JTextField titleField = new JTextField(40);
	private final JTextField locatorField = new JTextField(40);
	private final HintTextField repositoryField = new HintTextField(40);

	/** The repository chosen with the button, and the text it put in the field (the choice holds while the text is the same). */
	private String pickedId;
	private String pickedText;
	private Result result;

	private LifeFactRow.Asker asker = (parent, message, title, options) -> {
		final Object choice = JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, options.toArray(),
			options.getFirst());
		return (choice instanceof String picked? options.indexOf(picked): -1);
	};
	private LifeFactRow.Reporter reporter = (parent, message) -> MessageDialogs.show(parent, message, I18N.t("validation.title"),
		JOptionPane.ERROR_MESSAGE);


	/**
	 * Asks the user for the source to cite.
	 *
	 * @param owner	The window that owns the dialog.
	 * @param model	The model (to look for the repositories).
	 * @return	What the user asked for, {@code null} if the dialog was cancelled.
	 */
	public static Result show(final Window owner, final FLEFModel model){
		final QuickCitationDialog dialog = new QuickCitationDialog(owner, model);
		dialog.setVisible(true);

		return dialog.result;
	}


	QuickCitationDialog(final Window owner, final FLEFModel model){
		super(owner, I18N.t("dialog.quick_citation.title"), ModalityType.APPLICATION_MODAL);

		this.model = model;

		titleField.setToolTipText(I18N.t("dialog.quick_citation.source_title.tooltip"));
		locatorField.setToolTipText(I18N.t("dialog.quick_citation.locator.tooltip"));
		repositoryField.setToolTipText(I18N.t("dialog.quick_citation.repository.tooltip"));
		//a repository of the file is picked with the right button of the mouse, as every reference
		PickMenu.install(repositoryField, this::pickRepository, null);

		setLayout(new MigLayout("ins 10,hidemode 3,fillx,gapy 5", "[right]rel[grow,fill]rel[]"));
		add(new JLabel(GUIHelper.requiredLabel("dialog.quick_citation.source_title")));
		add(titleField, "span 2,growx,wrap");
		add(new JLabel(I18N.l("dialog.quick_citation.locator")));
		add(locatorField, "span 2,growx,wrap");
		add(new JLabel(I18N.l("dialog.quick_citation.repository")));
		add(repositoryField, "span 2,growx,wrap");
		add(GUIHelper.createButtonPanel(this, this::onOk, this::dispose), "span 3,growx");

		pack();
		setLocationRelativeTo(owner);
	}


	/** @return	What the user asked for ({@code null} if the dialog was not confirmed). */
	Result result(){
		return result;
	}

	void setAsker(final LifeFactRow.Asker asker){
		this.asker = asker;
	}

	void setReporter(final LifeFactRow.Reporter reporter){
		this.reporter = reporter;
	}

	/** Confirms the dialog: checks the title and settles the repository, then closes. The user stays here if something is not right. */
	void onOk(){
		final String title = StringUtils.normalizeSpace(titleField.getText());
		if(title.isEmpty()){
			reporter.report(this, I18N.tf("validation.required", I18N.t("dialog.quick_citation.source_title")));
			titleField.requestFocusInWindow();

			return;
		}

		final RecordRef repository = decideRepository(StringUtils.normalizeSpace(repositoryField.getText()));
		if(repository == null)
			return;

		result = new Result(title, StringUtils.normalizeSpace(locatorField.getText()), repository);
		dispose();
	}

	/** @return	The repository meant by the name, {@code null} if the user did not decide. */
	private RecordRef decideRepository(final String name){
		if(name.isEmpty())
			return RecordRef.NONE;
		if(pickedId != null && name.equals(pickedText))
			return RecordRef.existing(pickedId);

		final List<FLEFRecord> candidates = RepositoryLookup.find(model, name, LOOKUP_LIMIT);
		if(candidates.isEmpty())
			return RecordRef.named(name);

		final RepositoryHandler handler = RepositoryHandler.getInstance();
		final List<String> options = new ArrayList<>(candidates.size() + 1);
		for(final FLEFRecord candidate : candidates)
			options.add(handler.getDisplayText(candidate, model));
		options.add(I18N.tf("dialog.quick_citation.repository.create", name));
		final int index = asker.choose(repositoryField, I18N.tf("dialog.quick_citation.repository.choose.message", name),
			I18N.t("dialog.quick_citation.repository.choose.title"), options);
		if(index < 0)
			return null;

		return (index < candidates.size()? RecordRef.existing(candidates.get(index).getId()): RecordRef.named(name));
	}

	private void pickRepository(){
		final FLEFRecord[] picked = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(this, model,
			(record, handler) -> picked[0] = record, RepositoryHandler.class);
		dialog.setVisible(true);

		if(picked[0] == null || !RepositoryHandler.isOfType(picked[0]))
			return;

		pickedId = picked[0].getId();
		pickedText = StringUtils.normalizeSpace(RepositoryHandler.getInstance().getDisplayText(picked[0], model));
		repositoryField.setText(pickedText);
	}

}
