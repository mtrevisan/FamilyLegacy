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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections;


/**
 * Static content used by the Help menu: URLs, version, and the embedded
 * HTML documents for "Help Contents" and "User Guide".
 * <p>
 * Keeping the content in a dedicated class serves two purposes: it keeps
 * {@link ApplicationMenuBar} focused on menu wiring, and it gives a
 * single place to update when the documentation moves or when the
 * embedded HTML is revised.
 */
final class HelpContent{

	/** Base URL of the online documentation. */
	static final String DOCUMENTATION_URL = "https://github.com/mtrevisan/family-legacy/wiki";
	/** URL of the issue tracker, with a prefilled "new issue" form. */
	static final String ISSUE_TRACKER_URL = "https://github.com/mtrevisan/family-legacy/issues/new";
	/** URL of the release page. */
	static final String RELEASES_URL = "https://github.com/mtrevisan/family-legacy/releases";
	/** Version shown in the About dialog and in "Check for Updates". */
	static final String APPLICATION_VERSION = "2.0.0 (development)";

	/**
	 * Short embedded help shown by "Help Contents". The content is
	 * intentionally compact: it lists the main features and points to
	 * the online documentation for detail.
	 */
	static final String HELP_CONTENTS_HTML = """
    <html>
    <head>
    <style>
      body { font-family: SansSerif; font-size: 12px; margin: 12px; }
      h1 { font-size: 18px; margin-bottom: 6px; }
      h2 { font-size: 14px; margin-top: 14px; margin-bottom: 4px; }
      p, li { margin: 4px 0; }
      code { font-family: Monospaced; background: #f0f0f0; padding: 1px 3px; }
    </style>
    </head>
    <body>
    <h1>Family Legacy — Help</h1>
    <p>Family Legacy is a genealogical data manager built on the
    <b>Family LEgacy Format</b> (FLEF) protocol, a modern alternative to
    GEDCOM that keeps source assertions, researcher conclusions, and
    hypotheses separated and auditable.</p>

    <h2>Projections</h2>
    <p>Three views are available and can be cycled with
    <code>Ctrl+E</code>:</p>
    <ul>
      <li><b>Ancestor tree</b> (<code>Ctrl+1</code>) — a classical
      pedigree layout with one box per individual and the couples drawn
      side by side.</li>
      <li><b>Sugiyama graph</b> (<code>Ctrl+2</code>) — the same
      pedigree, but rendered as a layered DAG. Each individual appears
      only once, so endogamy and pedigree collapse are visible as
      converging edges rather than duplicated nodes.</li>
      <li><b>Ego network</b> (<code>Ctrl+3</code>) — the relations of a
      focal individual or group, shown as a graph.</li>
    </ul>

    <h2>Navigation</h2>
    <p>Click on a name to set the selected individual as the new root.
    Use <code>Ctrl+Left</code> and <code>Ctrl+Right</code> to walk back
    and forward through the navigation history. <code>Ctrl+J</code>
    opens a search dialog to jump to any individual by name.</p>

    <h2>Editing</h2>
    <p>Double-click a box to open its record editor, or press
    <code>F2</code> to edit the current selection. The editor exposes
    every field of the record, including its sources, notes, and audit
    trail.</p>

    <h2>Bookmarks</h2>
    <p>Use the <b>Bookmarks</b> menu to save the current view — the
    active projection plus the current root — and restore it later.
    Bookmarks persist between sessions.</p>

    <h2>Further reading</h2>
    <p>For a complete reference, see the
    <a href="https://github.com/mtrevisan/family-legacy/wiki">online
    documentation</a>.</p>
    </body>
    </html>
    """;

	/**
	 * Short embedded user guide shown by "User Guide…". It walks the user
	 * through the typical workflow: open a file, navigate, edit, cite.
	 */
	static final String USER_GUIDE_HTML = """
    <html>
    <head>
    <style>
      body { font-family: SansSerif; font-size: 12px; margin: 12px; }
      h1 { font-size: 18px; margin-bottom: 6px; }
      h2 { font-size: 14px; margin-top: 14px; margin-bottom: 4px; }
      p, li { margin: 4px 0; }
      code { font-family: Monospaced; background: #f0f0f0; padding: 1px 3px; }
      ol { margin-left: 20px; }
    </style>
    </head>
    <body>
    <h1>Family Legacy — User Guide</h1>

    <h2>1. Opening a file</h2>
    <ol>
      <li>From the <b>File</b> menu, choose <b>Open File…</b>.</li>
      <li>Select a <code>.flef</code> file. The file is parsed and
      validated against the FLEF grammar; any warning or error is shown
      in a dialog before the model is displayed.</li>
    </ol>

    <h2>2. Navigating the pedigree</h2>
    <ol>
      <li>Click on any name to make that individual the new root.</li>
      <li>Use <code>Ctrl+Left</code> to go back and
      <code>Ctrl+Right</code> to go forward in the navigation history.</li>
      <li>Use the arrow keys to move the visual selection and
      <code>Enter</code> to confirm it.</li>
      <li>Press <code>Ctrl+E</code> to cycle through the three
      projections (tree, Sugiyama graph, ego network).</li>
    </ol>

    <h2>3. Reading the record set of an individual</h2>
    <p>The right-hand pane shows the complete FLEF record set of the
    selected individual: every event they participated in, every
    attribute, every relationship, and every citation. Records are
    grouped by type and can be expanded individually.</p>

    <h2>4. Editing a record</h2>
    <ol>
      <li>Press <code>F2</code>, or double-click a box in the tree, to
      open the record editor.</li>
      <li>Edit the fields you want to change. Required fields are
      marked with an asterisk.</li>
      <li>Click <b>Save</b>. The record is validated against the FLEF
      grammar and any constraint declared in the protocol before being
      written back to the model.</li>
    </ol>

    <h2>5. Citing a source</h2>
    <ol>
      <li>In the record editor, open the <b>Sources</b> tab.</li>
      <li>Click <b>Add Citation</b> and choose an existing source, or
      create a new one.</li>
      <li>Fill in the locator (page, line, entry) and, if available,
      add an extract with the verbatim or summarized text.</li>
      <li>Set the <b>Evidence Qualifiers</b> to declare whether the
      source is original or derived, and whether the information is
      primary or secondary.</li>
    </ol>

    <h2>6. Research workflow</h2>
    <p>Family Legacy separates <b>assertions</b> (what a source says),
    <b>hypotheses</b> (possible interpretations), and <b>conclusions</b>
    (the researcher's assessment). Use the <b>Research</b> menu to
    create research questions, log activities, and record conclusions
    that resolve conflicting evidence.</p>

    <h2>Getting help</h2>
    <p>Press <code>F1</code> at any time to reopen the Help Contents.
    The full documentation is available at
    <a href="https://github.com/mtrevisan/family-legacy/wiki">
    https://github.com/mtrevisan/family-legacy/wiki</a>.</p>
    </body>
    </html>
    """;


	private HelpContent(){
	}

}
