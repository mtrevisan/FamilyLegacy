/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import io.github.mtrevisan.familylegacy.flef.ui.tree.GenealogyNavigation;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JPanel;


public class GenealogyNavigationPanel extends JPanel{

	private JButton backButton;
	private JButton forwardButton;

	private GenealogyNavigation genealogyNavigation;

	private final TreeNavigationListenerInterface treeNavigationListener;


	public static GenealogyNavigationPanel create(final TreeNavigationListenerInterface treeNavigationListener){
		return new GenealogyNavigationPanel(treeNavigationListener);
	}


	private GenealogyNavigationPanel(final TreeNavigationListenerInterface treeNavigationListener){
		this.treeNavigationListener = treeNavigationListener;


		initComponents();

		initLayout();
	}

	private void initComponents(){
		backButton = new JButton("back");
		forwardButton = new JButton("forward");

		genealogyNavigation = new GenealogyNavigation();


		backButton.setEnabled(false);
		backButton.addActionListener(evt -> {
			genealogyNavigation.goBack();

			updateDisplay();
		});
		forwardButton.setEnabled(false);
		forwardButton.addActionListener(evt -> {
			genealogyNavigation.goForward();

			updateDisplay();
		});
	}

	private void initLayout(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));

		add(backButton, "cell 0 0,align center");
		add(forwardButton, "cell 0 1,align center");
	}

	private void updateDisplay(){
		final Integer lastPersonID = genealogyNavigation.getLastPersonID();
		final Integer lastUnionID = genealogyNavigation.getLastUnionID();

		backButton.setEnabled(genealogyNavigation.canGoBack());
		forwardButton.setEnabled(genealogyNavigation.canGoForward());

		if(treeNavigationListener != null)
			treeNavigationListener.updateTree(lastPersonID, lastUnionID);
	}


	public void navigateToPerson(final int personID){
		genealogyNavigation.navigateToPerson(personID);
	}

	public void navigateToUnion(final int unionID){
		genealogyNavigation.navigateToUnion(unionID);
	}

	public void navigateTo(final int personID, final int unionID){
		genealogyNavigation.navigateTo(personID, unionID);
	}

}
