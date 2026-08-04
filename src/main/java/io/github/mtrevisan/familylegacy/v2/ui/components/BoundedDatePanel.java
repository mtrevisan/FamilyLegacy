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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Panel for BOUNDED date (uncertainty interval).
 * <p>
 * Structure:
 * <pre>
 * struct BoundedDate {
 *   not_before?: QualifiedDate
 *   not_after?: QualifiedDate
 *   // At least one of NOT_BEFORE or NOT_AFTER is required. An omitted bound represents an open-ended limit.
 *   require one_of(not_before, not_after)
 * }
 * </pre>
 */
public class BoundedDatePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 1351303209761075021L;


	private static final String TAG_NOT_BEFORE = "NOT_BEFORE";
	private static final String TAG_NOT_AFTER = "NOT_AFTER";
	private static final String TAG_BOUNDED = "BOUNDED";

	private final SingleDatePanel notBeforePanel;
	private final SingleDatePanel notAfterPanel;


	public BoundedDatePanel(final Dialog parent, final FLEFModel model){
		this.notBeforePanel = new SingleDatePanel(parent, model);
		this.notAfterPanel = new SingleDatePanel(parent, model);

		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx,top", "[grow,fill][grow,fill]"));
		setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

		final JPanel beforePanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		beforePanel.setBorder(new TitledBorder("Not Before"));
		beforePanel.add(notBeforePanel, "growx");
		add(beforePanel, "growx");

		final JPanel afterPanel = new JPanel(new MigLayout("fillx", "[right]rel[grow]"));
		afterPanel.setBorder(new TitledBorder("Not After"));
		afterPanel.add(notAfterPanel, "growx");
		add(afterPanel, "growx");
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final FLEFRecord notBefore = FLEFRecordHelper.findChild(record, TAG_NOT_BEFORE);
		if(notBefore != null)
			notBeforePanel.load(notBefore);

		final FLEFRecord notAfter = FLEFRecordHelper.findChild(record, TAG_NOT_AFTER);
		if(notAfter != null)
			notAfterPanel.load(notAfter);
	}

	public FLEFRecord save(){
		final FLEFRecord record = FLEFRecord.createEmpty();

		if(notBeforePanel.hasData()){
			final FLEFRecord notBefore = notBeforePanel.save();
			if(notBefore != null && notBefore.hasData()){
				notBefore.setTag(TAG_NOT_BEFORE);
				record.addChild(notBefore);
			}
		}

		if(notAfterPanel.hasData()){
			final FLEFRecord notAfter = notAfterPanel.save();
			if(notAfter != null && notAfter.hasData()){
				notAfter.setTag(TAG_NOT_AFTER);
				record.addChild(notAfter);
			}
		}

		return (record.hasData()? record.setTag(TAG_BOUNDED): FLEFRecord.createEmpty());
	}

	public void clear(){
		notBeforePanel.clear();
		notAfterPanel.clear();
	}

	public boolean hasData(){
		return (notBeforePanel.hasData() || notAfterPanel.hasData());
	}

	public boolean validateData(){
		if(!hasData()){
			JOptionPane.showMessageDialog(this,
				"At least one of Not Before or Not After is required for BOUNDED date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(notBeforePanel.hasData() && !notBeforePanel.validateData())
			return false;

		return (!notAfterPanel.hasData() || notAfterPanel.validateData());
	}

}
