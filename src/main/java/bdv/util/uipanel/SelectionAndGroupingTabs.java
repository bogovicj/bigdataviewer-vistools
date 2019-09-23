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

import bdv.tools.brightness.ColorIcon;
import bdv.util.PlaceHolderSource;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;

import org.scijava.listeners.Listeners;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.VisibilityAndGrouping.Event;
import bdv.viewer.state.SourceGroup;

/**
 * The tabbed pane with all BDV-UI components.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 * @author Tobias Pietzsch
 */
public class SelectionAndGroupingTabs extends JTabbedPane implements BdvHandle.SourceChangeListener
{
	private static final long serialVersionUID = 1L;

	/**
	 * The UI foreground color.
	 */
	private static final Color FOREGROUND_COLOR = Color.darkGray;

	/**
	 * The UI background color.
	 */
	private static final Color BACKGROUND_COLOR = Color.white;

	/**
	 * Item to add new groups.
	 */
	private final SourceGroup NEW_GROUP = new SourceGroup( "<New Group>" );

	private final SourceGroup ALL_GROUP = new SourceGroup( "All" );

	/**
	 * Combobox displaying all current sources.
	 */
	private JComboBox< Source< ? > > sourcesComboBox;

	/**
	 * Combobox displaying all groups with an option to create new groups.
	 */
	private JComboBox< SourceGroup > groupsComboBox;

	private final SourceIndexHelper sourceIndexHelper;

	private final SourceNameBimap sourceNameBimap = new SourceNameBimap();

	private final Map< Source< ? >, ConverterSetup > sourceToConverterSetup = new HashMap<>();

	/**
	 * Label representing the visibility state of the source.
	 */
	private JCheckBox sourceVisibilityCheckbox;

	/**
	 * Single source mode checkbox.
	 */
	private JCheckBox singleSourceModeCheckbox;

	/**
	 * Label representing the visibility state of the group.
	 */
	private JCheckBox groupVisibilityCheckbox;

	/**
	 * Single groupp mode checkbox.
	 */
	private JCheckBox singleGroupModeCheckbox;

	/**
	 * Remove the selected group button.
	 */
	private JButton removeGroup;

	/**
	 * Splitpane holding the selected sources and remaining (not selected)
	 * sources of a group.
	 */
	private JSplitPane selection;

	/**
	 * Sources which are part of the selected group.
	 */
	private JPanel selectedSources;

	/**
	 * Sources which are not part of the selected group.
	 */
	private JPanel remainingSources;

	/**
	 * Activity state of manual transformation.
	 */
	private boolean manualTransformationActive;

	/**
	 * The information panel, showing information about the selected source.
	 */
	private InformationPanel informationPanel;

	/**
	 * The min-max range slider-component.
	 */
	private IntensitySlider intensitySlider;

	/**
	 * Bdv visiblity and grouping.
	 */
	private final VisibilityAndGrouping visGro;

	/**
	 * Bdv viewer panel.
	 */
	private final ViewerPanel viewerPanel;

	/**
	 * Bdv setup assignments.
	 */
	private final SetupAssignments setupAssignments;

	/**
	 * Subscribers to selection changes.
	 */
	private final Listeners.List< SelectionChangeListener > selectionChangeListeners = new Listeners.SynchronizedList<>();


	/**
	 * This class holds the selection and grouping tab of the big data viewer
	 * UI.
	 */
	public SelectionAndGroupingTabs( final ViewerPanel vp, final VisibilityAndGrouping visGro,
			final ManualTransformationEditor manualTE, final SetupAssignments sa )
	{

		this.visGro = visGro;
		this.viewerPanel = vp;
		this.sourceIndexHelper = new SourceIndexHelper( visGro, vp );
		this.setupAssignments = sa;

		removeAllGroups();

		viewerPanel.addGroup( ALL_GROUP );
		sourceIndexHelper.setGroupActive( ALL_GROUP, true );
		sourceIndexHelper.setCurrentGroup( ALL_GROUP );

		setupTabbedPane();
		addListeners( manualTE );
	}

	private void removeAllGroups()
	{
		new ArrayList<>( visGro.getSourceGroups() ).forEach( viewerPanel::removeGroup );
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

		this.addTab( "Source Control", createSourceControl() );
		this.addTab( "Group Control", createGroupControl() );
		this.addChangeListener( e -> visGro.setGroupingEnabled( isGroupTabActive() ) );
	}

	/**
	 * Link the components to the BDV handle components to keep the state of bdv
	 * and UI consistent.
	 *
	 * @param manualTransformationEditor
	 */
	private void addListeners( final ManualTransformationEditor manualTransformationEditor )
	{
		manualTransformationEditor.manualTransformActiveListeners().add( enabled -> {
			setEnableSelectionAndGrouping( !enabled );
			manualTransformationActive = enabled;
		} );

		visGro.addUpdateListener( e -> {
			switch( e.id )
			{
			case Event.CURRENT_SOURCE_CHANGED:
				sourcesComboBox.setSelectedItem( sourceIndexHelper.getCurrentSource() );
				updateSourceVisibilityButton();
				break;
			case Event.CURRENT_GROUP_CHANGED:
				groupsComboBox.setSelectedItem( sourceIndexHelper.getCurrentGroup() );
				updateGroupVisibilityButton();
				break;
			case Event.SOURCE_ACTVITY_CHANGED:
				sourcesComboBox.repaint();
				updateSourceVisibilityButton();
				break;
			case Event.GROUP_ACTIVITY_CHANGED:
				groupsComboBox.repaint();
				updateGroupVisibilityButton();
				break;
			case Event.DISPLAY_MODE_CHANGED:
				singleGroupModeCheckbox.setSelected( !visGro.isFusedEnabled() );
				singleSourceModeCheckbox.setSelected( !visGro.isFusedEnabled() );
				setSelectedIndex( visGro.isGroupingEnabled() ? 1 : 0 );
				break;
			case Event.SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
				updateGroupSourceComponent();
				break;
			case Event.GROUP_NAME_CHANGED:
				groupsComboBox.repaint();
				break;
			case Event.VISIBILITY_CHANGED:
				// TODO?
				break;
			case Event.NUM_SOURCES_CHANGED:
				// TODO?
				break;
			case Event.NUM_GROUPS_CHANGED:
				// TODO?
				break;
			default:
			}
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
		sourceToConverterSetup.put( source, converterSetup );
		sourceNameBimap.add( source );

		intensitySlider.addSource( source, converterSetup );
		sourcesComboBox.addItem( source );

		sourceIndexHelper.addSourceToGroup( source, ALL_GROUP );
	}

	/**
	 * Remove source.
	 */
	@Override
	public synchronized void sourceRemoved( final Source< ? > source )
	{
		sourceToConverterSetup.remove( source );
		sourceNameBimap.remove( source );

		intensitySlider.removeSource( source );
		sourcesComboBox.removeItem( source );
	}

	private void updateGroupVisibilityButton()
	{
		groupVisibilityCheckbox.setSelected( sourceIndexHelper.getCurrentGroup().isActive() );
	}

	private void updateSourceVisibilityButton()
	{
		sourceVisibilityCheckbox.setSelected( sourceIndexHelper.isSourceActive( sourceIndexHelper.getCurrentSource() ) );
	}

	/**
	 * Toggle component enable.
	 *
	 * @param active
	 *            state
	 */
	private void setEnableSelectionAndGrouping( final boolean active )
	{
		sourcesComboBox.setEnabled( active );
		singleSourceModeCheckbox.setEnabled( active );
		groupsComboBox.setEnabled( active );
		singleGroupModeCheckbox.setEnabled( active );
		selectedSources.setEnabled( active );
		remainingSources.setEnabled( active );
		for ( final Component c : selectedSources.getComponents() )
		{
			if ( c instanceof JLabel )
				c.setEnabled( active );
		}
		for ( final Component c : remainingSources.getComponents() )
		{
			if ( c instanceof JLabel )
				c.setEnabled( active );
		}
		removeGroup.setEnabled( active );
		this.setEnabled( active );
	}

	/**
	 * Build the source control panel.
	 *
	 * @return the source contorl panel
	 */
	private Component createSourceControl()
	{
		final JPanel p = new JPanel( new MigLayout( "fillx", "[grow][][]", "[][]push[][]" ) );
		p.setBackground( BACKGROUND_COLOR );

		// source selection combobox
		sourcesComboBox = new JComboBox<>();
		sourcesComboBox.setMaximumSize( new Dimension( 270, 30 ) );
		sourcesComboBox.setRenderer( new SourceComboBoxRenderer() );
		sourcesComboBox.setBackground( BACKGROUND_COLOR );

		p.add( sourcesComboBox, "growx" );

		// source visibility icon (eye icon)
		sourceVisibilityCheckbox = new JCheckBox( VisibilityIcons.big( false ) );
		sourceVisibilityCheckbox.setSelectedIcon( VisibilityIcons.big( true ) );
		sourceVisibilityCheckbox.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		sourceVisibilityCheckbox.setToolTipText( "Show source in fused mode." );
		sourceVisibilityCheckbox.addActionListener( e -> {
			final Source< ? > source = sourceIndexHelper.getCurrentSource();
			final boolean active = !sourceIndexHelper.isSourceActive( source );
			visGro.setSourceActive( source, active );
		} );

		// color choser component
		final JButton colorButton = new JButton();
		colorButton.setPreferredSize( new Dimension( 15, 15 ) );
		colorButton.setIcon( colorIcon( BACKGROUND_COLOR ) );
		colorButton.addActionListener( e -> {
			Color newColor = null;
			final ConverterSetup setup = sourceToConverterSetup.get( sourcesComboBox.getSelectedItem() );
			newColor = JColorChooser.showDialog( null, "Select Source Color", getColor( setup ) );
			if ( newColor != null )
			{
				colorButton.setIcon( colorIcon( newColor ) );
				setColor( setup, newColor );
			}
		} );

		// add action listener to source combobox
		sourcesComboBox.addItemListener( e -> {
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				final Source< ? > source = ( Source< ? > ) sourcesComboBox.getSelectedItem();
				sourcesComboBox.setToolTipText( sourceNameBimap.getName( source ) );
				// TODO REMOVE
				notifySelectionChangeListeners( source instanceof PlaceHolderSource );
				if ( source != null )
				{
					visGro.setCurrentSource( source );
					intensitySlider.setSource( source );
					if (source instanceof PlaceHolderSource )
						informationPanel.setType( "N/A" );
					else
						informationPanel.setType( source.getType().getClass().getSimpleName() );

					final Color color = getColor( sourceToConverterSetup.get( source ) );
					colorButton.setIcon( colorIcon( color ) );
					colorButton.setEnabled( color != null );

					final boolean active = sourceIndexHelper.isSourceActive( source );
				}
			}
		} );

		p.add( colorButton );
		p.add( sourceVisibilityCheckbox, "wrap" );

		// add information panel
		informationPanel = new InformationPanel();
		p.add( informationPanel, "span, growx, wrap" );

		// single source mode checkbox to toggle between fused mode and single
		// source mode
		singleSourceModeCheckbox = new JCheckBox( "Single Source Mode" );
		singleSourceModeCheckbox.setBackground( BACKGROUND_COLOR );
		singleSourceModeCheckbox.setToolTipText( "Display only the selected source." );
		singleSourceModeCheckbox.addActionListener( e ->
				visGro.setFusedEnabled( !singleSourceModeCheckbox.isSelected() ) );

		// add range slider for intensity boundaries.
		intensitySlider = new IntensitySlider( setupAssignments, viewerPanel );
		final RangeSliderSpinnerPanel intensitySliderPanel = intensitySlider.getPanel();
		intensitySliderPanel.setPreferredSize( new Dimension( 20, 20 ) );
		p.add( intensitySliderPanel, "span, growx, wrap" );
		p.add( singleSourceModeCheckbox, "span, growx" );
		return p;
	}

	private void updateGroupSourceComponent()
	{
		selectedSources.removeAll();
		remainingSources.removeAll();

		viewerPanel.getState().getSources().forEach( sourceState -> {
			final Source< ? > source = sourceState.getSpimSource();
			if ( getCurrentGroup( viewerPanel ).getSourceIds().contains( sourceIndexHelper.getSourceIndex( source ) ) )
				selectedSources.add( createEntry( source ), "growx, wrap" );
			else
				remainingSources.add( createEntry( source ), "growx, wrap" );
			repaintComponents();
		} );
	}

	private Component createEntry( final Source< ? > source )
	{
		final JLabel p = new JLabel( sourceNameBimap.getName( source ) );
		p.setBackground( BACKGROUND_COLOR );
		p.setForeground( FOREGROUND_COLOR );
		p.setBorder( null );
		p.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseReleased( final MouseEvent e )
			{
				if ( !manualTransformationActive )
				{
					final SourceGroup group = ( SourceGroup ) groupsComboBox.getSelectedItem();

					if ( sourceIndexHelper.contains( group, source ) )
					{
						selectedSources.remove( p );
						remainingSources.add( p, "growx, wrap" );
						sourceIndexHelper.removeSourceFromGroup( source, group );
					}
					else
					{
						remainingSources.remove( p );
						selectedSources.add( p, "growx, wrap" );
						sourceIndexHelper.addSourceToGroup( source, group );
					}

					repaintComponents();
				}
			}
		} );
		return p;
	}

	/**
	 * Build the group control panel.
	 *
	 * @return the group control panel
	 */
	private Component createGroupControl()
	{
		final JPanel p = new JPanel( new MigLayout( "fillx", "[grow][][]", "" ) );
		p.setBackground( BACKGROUND_COLOR );

		groupsComboBox = new JComboBox<>();
		groupsComboBox.setMaximumSize( new Dimension( 269, 30 ) );
		groupsComboBox.setRenderer( new GroupComboBoxRenderer() );
		groupsComboBox.setBackground( BACKGROUND_COLOR );
		groupsComboBox.setForeground( FOREGROUND_COLOR );
		groupsComboBox.addItem( NEW_GROUP ); // entry which opens the add-group dialog
		groupsComboBox.addItem( ALL_GROUP ); // the default group containing all entries

		// remove group button
		removeGroup = new JButton( "-" );
		removeGroup.setForeground( FOREGROUND_COLOR );
		removeGroup.setBackground( BACKGROUND_COLOR );
		removeGroup.addActionListener( e -> {
			SourceGroup group = ( SourceGroup ) groupsComboBox.getSelectedItem();
			groupsComboBox.removeItem( group );
			viewerPanel.removeGroup( group );
		} );

		// panel which holds all sources which are part of the selected group
		selectedSources = new JPanel( new MigLayout( "fillx", "[grow]", "[]" ) );
		selectedSources.setBackground( BACKGROUND_COLOR );
		selectedSources.setBorder( null );

		// panel which holds all sources which are NOT part of the selected
		// group
		remainingSources = new JPanel( new MigLayout( "fillx", "[grow]", "[]" ) );
		remainingSources.setBackground( BACKGROUND_COLOR );
		remainingSources.setBorder( null );

		// the split pane holding selected and remaining sources
		selection = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		selection.setPreferredSize( new Dimension( selection.getPreferredSize().width, 150 ) );
		selection.setUI( new BasicSplitPaneUI()
		{
			@Override
			public BasicSplitPaneDivider createDefaultDivider()
			{
				return new BasicSplitPaneDivider( this )
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void paint( final Graphics g )
					{
						g.setColor( BACKGROUND_COLOR );
						g.fillRect( 0, 0, getSize().width, getSize().height );
						super.paint( g );
					}
				};
			}
		} );
		selection.setDividerLocation( 70 );
		selection.setBackground( BACKGROUND_COLOR );
		selection.setForeground( FOREGROUND_COLOR );
		selection.setBorder( null );
		final JScrollPane scrollPaneTop = new JScrollPane( selectedSources, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scrollPaneTop.getVerticalScrollBar().setUI( new WhiteScrollBarUI() );
		scrollPaneTop.getHorizontalScrollBar().setUI( new WhiteScrollBarUI() );
		selection.setTopComponent( scrollPaneTop );
		final JScrollPane scrollPaneBottom = new JScrollPane( remainingSources, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scrollPaneBottom.getVerticalScrollBar().setUI( new WhiteScrollBarUI() );
		scrollPaneBottom.getHorizontalScrollBar().setUI( new WhiteScrollBarUI() );
		selection.setBottomComponent( scrollPaneBottom );

		// label displaying the visibility state of the current group (eye icon)
		groupVisibilityCheckbox = new JCheckBox( VisibilityIcons.big( false ) );
		groupVisibilityCheckbox.setSelectedIcon( VisibilityIcons.big( true ) );
		groupVisibilityCheckbox.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		groupVisibilityCheckbox.setToolTipText( "Show group in fused-group mode." );
		groupVisibilityCheckbox.addActionListener( e -> {
			final SourceGroup group = sourceIndexHelper.getCurrentGroup();
			final boolean active = !group.isActive();
			sourceIndexHelper.setGroupActive( group, active );
		} );

		// Action listener handling the current group and updating selected and
		// remaining sources.
		// Also handles new group creation.
		groupsComboBox.addItemListener( e -> {
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				final SourceGroup selected = ( SourceGroup ) e.getItem();
				if ( selected.equals( NEW_GROUP ) )
				{
					final String newGroupName = JOptionPane.showInputDialog( p, "New Group Name:" );
					if ( newGroupName != null && !newGroupName.isEmpty() )
					{
						final Set< String > groupNames = visGro.getSourceGroups().stream().map( SourceGroup::getName ).collect( Collectors.toSet() );
						if ( groupNames.contains( newGroupName ) )
						{
							JOptionPane.showMessageDialog( p, "This group already exists." );
							groupsComboBox.setSelectedItem( newGroupName );
						}
						else
						{
							final SourceGroup newGroup = new SourceGroup( newGroupName );
							viewerPanel.addGroup( newGroup );
							sourceIndexHelper.setCurrentGroup( newGroup );
							groupsComboBox.addItem( newGroup );
							groupsComboBox.setSelectedItem( newGroup );
						}
					}
				}
				else
				{
					groupsComboBox.setSelectedItem( sourceIndexHelper.getCurrentGroup() );
				}

				sourceIndexHelper.setCurrentGroup( selected );
				updateGroupSourceComponent();
				removeGroup.setEnabled( !selected.equals( ALL_GROUP ) );
			}
		} );
		groupsComboBox.setSelectedItem( sourceIndexHelper.getCurrentGroup() );

		// checkbox to toggle between fused group mode and single group mode
		singleGroupModeCheckbox = new JCheckBox( "Single Group Mode" );
		singleGroupModeCheckbox.setBackground( BACKGROUND_COLOR );
		singleGroupModeCheckbox.setToolTipText( "Display only the currently selected group." );
		singleGroupModeCheckbox.addActionListener( e ->
				visGro.setFusedEnabled( !singleGroupModeCheckbox.isSelected() ) );

		p.add( groupsComboBox, "growx" );
		p.add( groupVisibilityCheckbox );
		p.add( removeGroup, "growx, wrap" );
		p.add( selection, "span, growx, wrap" );
		p.add( singleGroupModeCheckbox, "span, growx" );

		return p;
	}

	private boolean isGroupTabActive()
	{
		return getSelectedIndex() == 1;
	}

	private void repaintComponents()
	{
		selectedSources.revalidate();
		remainingSources.revalidate();
		selectedSources.repaint();
		remainingSources.repaint();
	}

	// A white look and feel for scroll bars.
	private final class WhiteScrollBarUI extends BasicScrollBarUI
	{
		@Override
		protected void configureScrollBarColors()
		{
			LookAndFeel.installColors( scrollbar, "ScrollBar.background", "ScrollBar.foreground" );
			thumbHighlightColor = BACKGROUND_COLOR;
			thumbLightShadowColor = BACKGROUND_COLOR;
			thumbDarkShadowColor = BACKGROUND_COLOR;
			thumbColor = Color.lightGray;
			trackColor = BACKGROUND_COLOR;
			trackHighlightColor = BACKGROUND_COLOR;
		}

		@Override
		protected JButton createDecreaseButton( final int orientation )
		{
			final BasicArrowButton button = new BasicArrowButton( orientation, BACKGROUND_COLOR, BACKGROUND_COLOR,
					Color.lightGray, BACKGROUND_COLOR );
			button.setBorder( new LineBorder( BACKGROUND_COLOR ) );
			button.setBackground( BACKGROUND_COLOR );
			return button;
		}

		@Override
		protected JButton createIncreaseButton( final int orientation )
		{
			final BasicArrowButton button = new BasicArrowButton( orientation, BACKGROUND_COLOR, BACKGROUND_COLOR,
					Color.lightGray, BACKGROUND_COLOR );
			button.setBorder( new LineBorder( BACKGROUND_COLOR ) );
			button.setBackground( BACKGROUND_COLOR );
			return button;
		}
	}

	// A combobox renderer displaying the visibility state of the sources.
	class SourceComboBoxRenderer extends JLabel implements ListCellRenderer< Source< ? > >
	{
		private static final long serialVersionUID = 1L;

		@Override

		public Component getListCellRendererComponent( final JList< ? extends Source< ? > > list, final Source< ? > value, final int index,
				final boolean isSelected, final boolean cellHasFocus )
		{

			if ( value != null )
			{
				final String uniqueName = sourceNameBimap.getName( value );
				this.setText( uniqueName );
				this.setToolTipText( uniqueName );
				final boolean visible = sourceIndexHelper.isSourceActive( value );
				this.setIcon( VisibilityIcons.small( visible ) );
			}
			else
			{
				this.setIcon( null );
			}

			if ( isSelected )
			{
				setForeground( Color.gray );
			}
			else
			{
				setForeground( FOREGROUND_COLOR );
			}

			return this;
		}
	}

	// A combobox renderer displaying the visibility state of the groups.
	class GroupComboBoxRenderer extends JLabel implements ListCellRenderer< SourceGroup >
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent( final JList< ? extends SourceGroup > list, final SourceGroup value, final int index,
				final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value != null )
			{
				if ( value.equals( NEW_GROUP ) )
					this.setIcon( null );
				else
					this.setIcon( VisibilityIcons.small( value.isActive() ) );
				this.setText( value.getName() );
				this.setToolTipText( value.getName() );
			}
			setForeground( isSelected ? Color.gray : FOREGROUND_COLOR );
			return this;
		}
	}

	protected static Source< ? > getSource( final int i, final ViewerPanel viewerPanel )
	{
		return viewerPanel.getState().getSources().get( i ).getSpimSource();
	}

	private static SourceGroup getCurrentGroup( final ViewerPanel viewerPanel )
	{
		return viewerPanel.getState().getSourceGroups().get( viewerPanel.getState().getCurrentGroup() );
	}

	// -- SelectionChangeListener --
	// TODO: SelectionChangeListener are notified when sourcesComboBox selection is changed.
	//       This is probably not needed, could listen to VisibilityAndGrouping instead.

	// TODO REMOVE
	public interface SelectionChangeListener
	{
		void selectionChanged( final boolean isOverlay );
	}

	// TODO REMOVE
	public Listeners< SelectionChangeListener > selectionChangeListeners()
	{
		return selectionChangeListeners;
	}

	// TODO REMOVE
	private void notifySelectionChangeListeners( final boolean isOverlay )
	{
		selectionChangeListeners.list.forEach( l -> l.selectionChanged( isOverlay ) );
	}

	// -- Colors --

	private static Color getColor( final ConverterSetup setup )
	{
		return setup.supportsColor() ? new Color( setup.getColor().get() ) : null;
	}

	private static void setColor( final ConverterSetup setup, final Color color )
	{
		setup.setColor( new ARGBType( color.getRGB() | 0xff000000 ) );
	}

	private ColorIcon colorIcon( Color color )
	{
		return new ColorIcon( color, 24, 16, 5, 5 );
	}
}
