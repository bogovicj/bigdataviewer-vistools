/*-
 * #%L
 * UI for BigDataViewer.
 * %%
 * Copyright (C) 2017 - 2018 Tim-Oliver Buchholz
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.util.uipanel;

import java.util.ArrayList;

import java.util.Collections;
import java.util.List;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import org.scijava.listeners.Listeners;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.VisibilityAndGrouping.Event;
import bdv.viewer.state.SourceGroup;

import static bdv.util.uipanel.ColorsAndIcons.BACKGROUND_COLOR;
import static bdv.util.uipanel.ColorsAndIcons.FOREGROUND_COLOR;

/**
 * The tabbed pane with all BDV-UI components.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 * @author Tobias Pietzsch
 */
public class SelectionAndGroupingTabs extends JTabbedPane implements BdvHandle.SourceChangeListener
{
	private static final long serialVersionUID = 1L;

	private final SourceIndexHelper sourceIndexHelper;

	private final SourceNameBimap sourceNameBimap = new SourceNameBimap();

	/**
	 * Bdv visiblity and grouping.
	 */
	private final VisibilityAndGrouping visGro;

	/**
	 * Bdv viewer panel.
	 */
	private final ViewerPanel viewerPanel;

	private SourceSettingsPanel sourceControlTab;

	private GroupSettingsPanel groupControlTab;

	private final SourceGroup ALL_GROUP = new SourceGroup( "All" );

	/**
	 * This class holds the selection and grouping tab of the big data viewer
	 * UI.
	 */
	public SelectionAndGroupingTabs( final ViewerPanel vp, final VisibilityAndGrouping visGro,
			final ManualTransformationEditor manualTE )
	{
		this.visGro = visGro;
		this.viewerPanel = vp;
		this.sourceIndexHelper = new SourceIndexHelper( visGro, vp );

		// TODO: this shouldn't happen here. (and maybe not at all).
		removeAllGroups();
		viewerPanel.addGroup( ALL_GROUP );
		sourceIndexHelper.setGroupActive( ALL_GROUP, true );
		sourceIndexHelper.setCurrentGroup( ALL_GROUP );

		setupTabbedPane();
		addListeners( manualTE );
	}

	private void removeAllGroups()
	{
		final List< SourceGroup > sourceGroups = visGro.getSourceGroups();
		new ArrayList<>( sourceGroups ).forEach( viewerPanel::removeGroup );
	}

	/**
	 * Add tabs source and group to tabbed pane.
	 *
	 * Also notify the bdv handle of tab switches.
	 */
	private void setupTabbedPane()
	{
		UIManager.put( "TabbedPane.contentAreaColor", BACKGROUND_COLOR );
		this.setUI( new CustomTabbedPaneUI() );

		this.setBackground( BACKGROUND_COLOR );
		this.setForeground( FOREGROUND_COLOR );

		sourceControlTab = new SourceSettingsPanel( visGro, sourceIndexHelper, sourceNameBimap );
		groupControlTab = new GroupSettingsPanel( viewerPanel, visGro, sourceIndexHelper, sourceNameBimap, Collections.singleton( ALL_GROUP ) );
		this.addTab( "Source Control", sourceControlTab );
		this.addTab( "Group Control", groupControlTab );
		this.addChangeListener( e -> visGro.setGroupingEnabled( getSelectedComponent() == groupControlTab ) );
	}

	/**
	 * Link the components to the BDV handle components to keep the state of bdv
	 * and UI consistent.
	 *
	 * @param manualTransformationEditor
	 */
	private void addListeners( final ManualTransformationEditor manualTransformationEditor )
	{
		manualTransformationEditor.manualTransformActiveListeners().add( active -> {
			final boolean enabled = !active;
			this.setEnabled( enabled );
			sourceControlTab.setEnabled( enabled );
			groupControlTab.setEnabled( enabled );
		} );

		// -- for tab pane --
		visGro.addUpdateListener( e -> {
			if ( e.id == Event.DISPLAY_MODE_CHANGED )
				setSelectedComponent( visGro.isGroupingEnabled() ? groupControlTab : sourceControlTab );
		} );
	}

	/**
	 * Add information of new source to the UI.
	 *
	 * Put it into the corresponding group, set visibility and add it to the
	 * source selection.
	 */
	@Override
	public synchronized void sourceAdded( final Source< ? > source, final ConverterSetup converterSetup )
	{
		sourceNameBimap.add( source );
		sourceControlTab.sourceAdded( source, converterSetup );
		sourceIndexHelper.addSourceToGroup( source, ALL_GROUP );
	}

	/**
	 * Remove source.
	 */
	@Override
	public synchronized void sourceRemoved( final Source< ? > source )
	{
		sourceNameBimap.remove( source );
		sourceControlTab.sourceRemoved( source );
	}
}
