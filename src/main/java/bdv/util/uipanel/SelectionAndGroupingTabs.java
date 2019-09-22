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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;

import org.scijava.listeners.Listeners;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.ManualTransformActiveListener;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.util.BdvHandle;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.VisibilityAndGrouping.Event;
import bdv.viewer.VisibilityAndGrouping.UpdateListener;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.SourceState;

/**
 * The tabbed pane with all BDV-UI components.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
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
	private static final String NEW_GROUP = "<New Group>";

	/**
	 * Combobox displaying all current sources.
	 */
	private JComboBox< String > sourcesComboBox;

	/**
	 * Combobox displaying all groups with an option to create new groups.
	 */
	private JComboBox< String > groupsComboBox;

	/**
	 * Maps sources to/from unique names.
	 */
	static class SourceNameBimap // TODO move to separate file
	{
		private final Map< Source< ? >, String > sourceToName = new HashMap<>();
		private final Map< String, Source< ? > > nameToSource = new HashMap<>();

		public synchronized String add( final Source< ? > source )
		{
			if ( contains( source ) )
				throw new IllegalArgumentException();

			final String name = uniqueName( source.getName() );
			sourceToName.put( source, name );
			nameToSource.put( name, source );
			return name;
		}

		public synchronized String remove( final Source< ? > source )
		{
			final String name = sourceToName.remove( source );
			if ( name != null )
				nameToSource.remove( name );
			return name;
		}

		public synchronized boolean contains( final Source< ? > source )
		{
			return sourceToName.containsKey( source );
		}

		public synchronized String getName( final Source< ? > source )
		{
			return sourceToName.get( source );
		}

		public String getName( final SourceState< ? > source )
		{
			return getName( source.getSpimSource() );
		}

		public synchronized Source< ? > getSource( final String name )
		{
			return nameToSource.get( name );
		}

		private String uniqueName( final String name )
		{
			String prefix = "";
			int i = 0;
			while ( nameToSource.containsKey( prefix + name ) )
			{
				prefix = i + "_";
				i++;
			}
			return prefix + name;
		}
	}

	private final SourceNameBimap sourceNameBimap = new SourceNameBimap();

	/**
	 * Map holding the group names mapped to the {@link GroupProperties}.
	 */
	private final Map< SourceGroup, GroupProperties > groupLookup = new HashMap<>();

	/**
	 * The currently selected group.
	 */
	private int currentSelection;

	/**
	 * Eye icon normal size.
	 */
	private ImageIcon visibleIcon;

	/**
	 * Crossed eye icon normal size.
	 */
	private ImageIcon notVisibleIcon;

	/**
	 * Eye icon small.
	 */
	private ImageIcon visibleIconSmall;

	/**
	 * Crossed eye icon small.
	 */
	private ImageIcon notVisibleIconSmall;

	/**
	 * Label representing the visibility state of the source.
	 */
	private JLabel sourceVisibilityLabel;

	/**
	 * Only display selected source.
	 */
	private boolean singleSourceMode;

	/**
	 * Single source mode checkbox.
	 */
	private JCheckBox singleSourceModeCheckbox;

	/**
	 * Label representing the visibility state of the group.
	 */
	private JLabel groupVisibilityLabel;

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
	private RangeSliderSpinnerPanel intensitySlider;

	/**
	 * Group mode active.
	 */
	private boolean groupMode = false;

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
		this.setupAssignments = sa;
		initialize();

		final SourceGroup all = new SourceGroup( "All" );
		this.viewerPanel.addGroup( all );
		groupLookup.put( all, new GroupProperties( "All", true ) );
		while ( visGro.getSourceGroups().size() > 1 )
		{
			final SourceGroup g = visGro.getSourceGroups().get( 0 );
			viewerPanel.removeGroup( g );
		}

		visGro.setCurrentGroup( 1 );
		visGro.setGroupActive( 1, true );

		setupTabbedPane();
		addListeners( manualTE );
	}

	/**
	 * Initialize components.
	 */
	private void initialize()
	{
		visibleIcon = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "visible.png" ), "Visible" );
		notVisibleIcon = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "notVisible.png" ), "Not Visible" );

		visibleIconSmall = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "visible_small.png" ), "Visible" );
		notVisibleIconSmall = new ImageIcon( SelectionAndGroupingTabs.class.getResource( "notVisible_small.png" ),
				"Not Visible" );

		groupLookup.put( viewerPanel.getState().getSourceGroups().get( 0 ), new GroupProperties( "All", true ) );
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

		this.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				if ( getSelectedIndex() == 1 )
				{
					if ( singleSourceMode )
					{
						if ( viewerPanel.getState().getDisplayMode() != DisplayMode.GROUP )
							viewerPanel.setDisplayMode( DisplayMode.GROUP );
					}
					else
					{
						if ( viewerPanel.getState().getDisplayMode() != DisplayMode.FUSEDGROUP )
							viewerPanel.setDisplayMode( DisplayMode.FUSEDGROUP );
					}
					groupMode = true;
				}
				else
				{
					sourcesComboBox.setSelectedIndex( visGro.getCurrentSource() );
					if ( singleSourceMode )
					{
						if ( viewerPanel.getState().getDisplayMode() != DisplayMode.SINGLE )
							viewerPanel.setDisplayMode( DisplayMode.SINGLE );
					}
					else
					{
						if ( viewerPanel.getState().getDisplayMode() != DisplayMode.FUSED )
							viewerPanel.setDisplayMode( DisplayMode.FUSED );
					}
					groupMode = false;
				}
			}
		} );
	}

	/**
	 * Link the components to the BDV handle components to keep the state of bdv
	 * and UI consistent.
	 *
	 * @param manualTransformationEditor
	 */
	private void addListeners( final ManualTransformationEditor manualTransformationEditor )
	{

		manualTransformationEditor.addManualTransformActiveListener( new ManualTransformActiveListener()
		{

			@Override
			public void manualTransformActiveChanged( final boolean enabled )
			{
				setEnableSelectionAndGrouping( !enabled );
				manualTransformationActive = enabled;
			}
		} );

		visGro.addUpdateListener( new UpdateListener()
		{

			@Override
			public void visibilityChanged( final Event e )
			{
				SwingUtilities.invokeLater( () -> {
					if ( e.id == VisibilityAndGrouping.Event.CURRENT_SOURCE_CHANGED )
					{
						sourcesComboBox.setSelectedIndex( visGro.getCurrentSource() );
					}
					if ( e.id == VisibilityAndGrouping.Event.CURRENT_GROUP_CHANGED )
					{
						groupsComboBox.setSelectedIndex( visGro.getCurrentGroup() + 1 );
					}
					if ( e.id == VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED )
					{
						final DisplayMode mode = visGro.getDisplayMode();
						if ( mode.equals( DisplayMode.FUSEDGROUP ) )
						{
							singleGroupModeCheckbox.setSelected( false );
							singleSourceModeCheckbox.setSelected( false );
							singleSourceMode = false;

							setEnableVisibilityIcons( true );

							setSelectedIndex( 1 );
						}
						else if ( mode.equals( DisplayMode.FUSED ) )
						{
							singleGroupModeCheckbox.setSelected( false );
							singleSourceModeCheckbox.setSelected( false );
							singleSourceMode = false;

							setEnableVisibilityIcons( true );

							setSelectedIndex( 0 );
						}
						else if ( mode.equals( DisplayMode.GROUP ) )
						{
							singleGroupModeCheckbox.setSelected( true );
							singleSourceModeCheckbox.setSelected( true );
							singleSourceMode = true;

							setEnableVisibilityIcons( false );

							setSelectedIndex( 1 );
						}
						else
						{
							singleGroupModeCheckbox.setSelected( true );
							singleSourceModeCheckbox.setSelected( true );
							sourceVisibilityLabel.setEnabled( false );
							singleSourceMode = true;

							setEnableVisibilityIcons( false );

							setSelectedIndex( 0 );
						}
					}
				} );
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
	public synchronized void sourceAdded( final Source< ? > p )
	{
		final String uniqueName = sourceNameBimap.add( p );

		this.visGro.addSourceToGroup( getSourceIndex( p, viewerPanel ), 0 );
		groupLookup.get( getGroup( "All" ) ).addSource( p );

		sourcesComboBox.addItem( uniqueName );
		intensitySlider.addSource( p );

		sourcesComboBox.setSelectedIndex( getCurrentSourceIndex( viewerPanel ) );
		intensitySlider.setSource( getCurrentSource( viewerPanel ) );
		groupsComboBox.setSelectedIndex( getCurrentGroupIndex( viewerPanel ) + 1 );
		updateGroupSourceComponent();
	}

	/**
	 * Remove source.
	 */
	@Override
	public synchronized void sourceRemoved( final Source< ? > source )
	{
		intensitySlider.removeSource( source );
		for ( final GroupProperties group : groupLookup.values() )
		{
			group.getSources().remove( source );
		}
		sourcesComboBox.removeItem( source.getName() );
		sourceNameBimap.remove( source );
	}

	private void setEnableVisibilityIcons( final boolean active )
	{
		if ( !groupLookup.isEmpty() )
		{
			if ( !active )
			{
				groupVisibilityLabel.setEnabled( false );
				groupVisibilityLabel.setIcon( visibleIcon );
				sourceVisibilityLabel.setEnabled( false );
				sourceVisibilityLabel.setIcon( visibleIcon );
			}
			else
			{
				groupVisibilityLabel.setEnabled( true );
				if ( groupLookup.get( getGroup( ( String ) groupsComboBox.getSelectedItem() ) ).isVisible() )
				{
					groupVisibilityLabel.setIcon( visibleIcon );
				}
				else
				{
					groupVisibilityLabel.setIcon( notVisibleIcon );
				}
				sourceVisibilityLabel.setEnabled( true );

				final boolean visible = visGro.isSourceActive( getSourceIndex( sourceNameBimap.getSource( ( String ) sourcesComboBox.getSelectedItem() ), viewerPanel ) );
				if ( visible )
				{
					sourceVisibilityLabel.setIcon( visibleIcon );
				}
				else
				{
					sourceVisibilityLabel.setIcon( notVisibleIcon );
				}
			}
		}
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
		sourceVisibilityLabel = new JLabel( visibleIcon );
		sourceVisibilityLabel.setBackground( BACKGROUND_COLOR );
		sourceVisibilityLabel.setToolTipText( "Show source in fused mode." );
		sourceVisibilityLabel.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseReleased( final MouseEvent e )
			{
				if ( !singleSourceMode )
				{
					boolean sourceActiveState = visGro.isSourceActive( sourcesComboBox.getSelectedIndex() );
					sourceActiveState = !sourceActiveState;
					if ( sourceActiveState )
					{
						sourceVisibilityLabel.setIcon( visibleIcon );
					}
					else
					{
						sourceVisibilityLabel.setIcon( notVisibleIcon );
					}
					visGro.getSources().get( sourcesComboBox.getSelectedIndex() ).setActive( sourceActiveState );
					viewerPanel.requestRepaint();
					sourcesComboBox.repaint();
				}
			}
		} );

		// color choser component
		final JButton colorButton = new JButton();
		colorButton.setPreferredSize( new Dimension( 15, 15 ) );
		colorButton.setBackground( BACKGROUND_COLOR );
		colorButton.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( final ActionEvent ev )
			{
				Color newColor = null;
				final ConverterSetup setup = setupAssignments.getConverterSetups().get( sourcesComboBox.getSelectedIndex() );
				if ( ev.getSource() == colorButton )
				{
					newColor = JColorChooser.showDialog( null, "Select Source Color", getColor( setup ) );
					if ( newColor != null )
					{
						colorButton.setBackground( newColor );

						setColor( setup, newColor );
					}
				}
			}
		} );

		// add action listener to source combobox
		sourcesComboBox.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( final ItemEvent e )
			{
				if ( e.getStateChange() == ItemEvent.SELECTED )
				{
					sourcesComboBox.setToolTipText( ( String ) sourcesComboBox.getSelectedItem() );
					final Source< ? > p = sourceNameBimap.getSource( ( String ) sourcesComboBox.getSelectedItem() );
					notifySelectionChangeListeners( false );
					if ( p != null )
					{
						visGro.setCurrentSource( getSourceIndex( p, viewerPanel ) );
						intensitySlider.setSource( p );
						informationPanel.setType( p.getType().getClass().getSimpleName() );
						colorButton.setVisible( p.getType().getClass() != ARGBType.class );
						colorButton.setBackground( getColor( setupAssignments.getConverterSetups()
								.get( sourcesComboBox.getSelectedIndex() ) ) );
						if ( !singleSourceMode )
						{
							sourceVisibilityLabel.setEnabled( true );
							if ( visGro.isSourceActive( sourcesComboBox.getSelectedIndex() ) )
							{
								sourceVisibilityLabel.setIcon( visibleIcon );
							}
							else
							{
								sourceVisibilityLabel.setIcon( notVisibleIcon );
							}
						}
						else
						{
							sourceVisibilityLabel.setIcon( visibleIcon );
							sourceVisibilityLabel.setEnabled( false );
						}
					}
				}
			}
		} );

		p.add( colorButton );
		p.add( sourceVisibilityLabel, "wrap" );

		// add information panel
		informationPanel = new InformationPanel();
		p.add( informationPanel, "span, growx, wrap" );

		// single source mode checkbox to toggle between fused mode and single
		// source mode
		singleSourceModeCheckbox = new JCheckBox( "Single Source Mode" );
		singleSourceModeCheckbox.setBackground( BACKGROUND_COLOR );
		singleSourceModeCheckbox.setToolTipText( "Display only the selected source." );
		singleSourceModeCheckbox.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				singleSourceMode = singleSourceModeCheckbox.isSelected();
				if ( !singleSourceMode && !groupMode )
				{
					visGro.setDisplayMode( DisplayMode.FUSED );
				}
				else if ( !singleSourceMode && groupMode )
				{
					visGro.setDisplayMode( DisplayMode.FUSEDGROUP );
				}
				else if ( singleSourceMode && !groupMode )
				{
					visGro.setDisplayMode( DisplayMode.SINGLE );
				}
				else
				{
					visGro.setDisplayMode( DisplayMode.GROUP );
				}
			}
		} );

		// add range slider for intensity boundaries.
		intensitySlider = new RangeSliderSpinnerPanel( setupAssignments, viewerPanel );
		intensitySlider.setPreferredSize( new Dimension( 20, 20 ) );
		p.add( intensitySlider, "span, growx, wrap" );
		p.add( singleSourceModeCheckbox, "span, growx" );
		return p;
	}

	private void updateGroupSourceComponent()
	{
		selectedSources.removeAll();
		remainingSources.removeAll();

		viewerPanel.getState().getSources().forEach( new Consumer< SourceState< ? > >()
		{
			@Override
			public void accept( final SourceState< ? > sourceState )
			{
				if ( getCurrentGroup( viewerPanel ).getSourceIds().contains( getSourceIndex( sourceState.getSpimSource(), viewerPanel ) ) )
				{
					selectedSources.add( createEntry( sourceState ), "growx, wrap" );
				}
				else
				{
					remainingSources.add( createEntry( sourceState ), "growx, wrap" );
				}
				repaintComponents();
			}
		} );
	}

	private Component createEntry( final SourceState< ? > t )
	{
		final JLabel p = new JLabel( sourceNameBimap.getName( t ) );
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
					final SourceGroup group = getGroup( ( String ) groupsComboBox.getSelectedItem() );
					final GroupProperties groupProps = groupLookup.get( group );
					if ( groupProps.getSources().contains( t.getSpimSource() ) )
					{
						groupProps.removeSource( t.getSpimSource() );
						selectedSources.remove( p );
						remainingSources.add( p, "growx, wrap" );
						visGro.removeSourceFromGroup( getSourceIndex( t.getSpimSource(), viewerPanel ),
								getGroupIndex( ( String ) groupsComboBox.getSelectedItem(), viewerPanel ) );
					}
					else
					{
						groupProps.addSource( t.getSpimSource() );
						remainingSources.remove( p );
						selectedSources.add( p, "growx, wrap" );
						visGro.addSourceToGroup( getSourceIndex( t.getSpimSource(), viewerPanel ),
								getGroupIndex( ( String ) groupsComboBox.getSelectedItem(), viewerPanel ) );
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
		// entry which opens the add-group dialog
		groupsComboBox.addItem( NEW_GROUP );
		// the default group containing all entries
		groupsComboBox.addItem( "All" );

		// remove group button
		removeGroup = new JButton( "-" );
		removeGroup.setForeground( FOREGROUND_COLOR );
		removeGroup.setBackground( BACKGROUND_COLOR );
		removeGroup.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( e.getSource() == removeGroup )
				{
					final int selectedIndex = groupsComboBox.getSelectedIndex();
					final String selected = ( String ) groupsComboBox.getSelectedItem();
					SourceGroup toRemove = null;
					for ( int i = 0; i < visGro.getSourceGroups().size(); i++ )
					{
						if ( visGro.getSourceGroups().get( i ).getName().equals( selected ) )
						{
							toRemove = visGro.getSourceGroups().get( i );
						}
					}
					groupsComboBox.removeItemAt( selectedIndex );
					groupsComboBox.setSelectedIndex( 1 );
					if ( toRemove != null )
					{
						viewerPanel.removeGroup( toRemove );
						groupLookup.remove( selected );
					}
				}
			}
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

		// Action listener handling the current group and updating selected and
		// remaining sources.
		// Also handles new group creation.
		groupsComboBox.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( final ItemEvent e )
			{
				if ( e.getStateChange() == ItemEvent.SELECTED )
				{
					final Object selection = e.getItem();
					if ( selection != null && selection instanceof String )
					{
						final String s = ( String ) selection;

						if ( s.equals( NEW_GROUP ) )
						{
							final String newGroupName = JOptionPane.showInputDialog( p, "New Group Name:" );
							if ( newGroupName != null && !newGroupName.isEmpty() )
							{
								if ( groupLookup.containsKey( newGroupName ) )
								{
									JOptionPane.showMessageDialog( p, "This group already exists." );
									groupsComboBox.setSelectedItem( newGroupName );
								}
								else
								{

									groupsComboBox.addItem( newGroupName );
									final int idx = viewerPanel.getState().getSourceGroups().size();
									final SourceGroup group = new SourceGroup( newGroupName );
									viewerPanel.addGroup( group );
									groupLookup.put( group, new GroupProperties( newGroupName, true ) );
									visGro.setCurrentGroup( idx );
									groupsComboBox.setSelectedItem( newGroupName );
								}
							}
							else
							{
								groupsComboBox.setSelectedIndex( currentSelection );
							}
						}

						currentSelection = groupsComboBox.getSelectedIndex();
						if ( getSelectedIndex() == 1 )
						{
							visGro.setCurrentGroup( getGroupIndex( ( String ) groupsComboBox.getSelectedItem(), viewerPanel ) );
							if ( !singleSourceMode )
							{
								groupVisibilityLabel.setEnabled( true );
								if ( groupLookup.get( getGroup( ( String ) groupsComboBox.getSelectedItem() ) ).isVisible() )
								{
									groupVisibilityLabel.setIcon( visibleIcon );
								}
								else
								{
									groupVisibilityLabel.setIcon( notVisibleIcon );
								}
							}
							else
							{
								groupVisibilityLabel.setIcon( visibleIcon );
								groupVisibilityLabel.setEnabled( false );
							}
						}
						updateGroupSourceComponent();
						removeGroup.setEnabled( groupsComboBox.getSelectedIndex() > 1 );
					}
				}
			}

		} );
		groupsComboBox.setSelectedIndex( -1 );
		p.add( groupsComboBox, "growx" );

		// label displaying the visibility state of the current group (eye icon)
		groupVisibilityLabel = new JLabel( visibleIcon );
		groupVisibilityLabel.setBackground( BACKGROUND_COLOR );
		groupVisibilityLabel.setBorder( null );
		groupVisibilityLabel.setToolTipText( "Show group in fused-group mode." );
		groupVisibilityLabel.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseReleased( final MouseEvent e )
			{
				if ( !singleSourceMode )
				{
					final String selected = ( String ) groupsComboBox.getSelectedItem();
					boolean groupActiveState = groupLookup.get( getGroup( selected ) ).isVisible();
					if ( groupActiveState )
					{
						groupActiveState = !groupActiveState;
						groupVisibilityLabel.setIcon( notVisibleIcon );
					}
					else
					{
						groupActiveState = !groupActiveState;
						groupVisibilityLabel.setIcon( visibleIcon );
					}
					groupLookup.get( getGroup( selected ) ).setVisible( groupActiveState );
					visGro.setGroupActive( getGroupIndex( selected, viewerPanel ), groupActiveState );
				}
			}
		} );

		p.add( groupVisibilityLabel );

		p.add( removeGroup, "growx, wrap" );

		// checkbox to toggle between fused group mode and single group mode
		singleGroupModeCheckbox = new JCheckBox( "Single Group Mode" );
		singleGroupModeCheckbox.setBackground( BACKGROUND_COLOR );
		singleGroupModeCheckbox.setToolTipText( "Display only the currently selected group." );
		singleGroupModeCheckbox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				singleSourceModeCheckbox.setSelected( singleGroupModeCheckbox.isSelected() );
				singleSourceMode = singleSourceModeCheckbox.isSelected();
				if ( !singleSourceMode && !groupMode )
				{
					visGro.setDisplayMode( DisplayMode.FUSED );
				}
				else if ( !singleSourceMode && groupMode )
				{
					visGro.setDisplayMode( DisplayMode.FUSEDGROUP );
				}
				else if ( singleSourceMode && !groupMode )
				{
					visGro.setDisplayMode( DisplayMode.SINGLE );
				}
				else
				{
					visGro.setDisplayMode( DisplayMode.GROUP );
				}
			}
		} );
		p.add( selection, "span, growx, wrap" );

		p.add( singleGroupModeCheckbox, "span, growx" );

		return p;
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
	class SourceComboBoxRenderer extends JLabel implements ListCellRenderer< String >
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent( final JList< ? extends String > list, final String value, final int index,
				final boolean isSelected, final boolean cellHasFocus )
		{

			if ( value != null )
			{
				this.setText( value );
				this.setToolTipText( value );
				this.setIcon( visibleIconSmall );
				final boolean visible = visGro.isSourceActive( getSourceIndex( sourceNameBimap.getSource( value ), viewerPanel ) );
				if ( !singleSourceMode && !visible )
				{
					this.setIcon( notVisibleIconSmall );
				}
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
	class GroupComboBoxRenderer extends JLabel implements ListCellRenderer< String >
	{
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent( final JList< ? extends String > list, final String value, final int index,
				final boolean isSelected, final boolean cellHasFocus )
		{

			if ( value != null )
			{
				if ( !value.equals( NEW_GROUP ) )
				{
					this.setIcon( visibleIconSmall );
					if ( !singleSourceMode && !groupLookup.get( getGroup( value ) ).isVisible() )
					{
						this.setIcon( notVisibleIconSmall );
					}
				}
				else
				{
					this.setIcon( null );
				}
				this.setText( value );
				this.setToolTipText( value );
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

	private static Color getColor( final ConverterSetup setup )
	{
		if ( setup.supportsColor() )
		{
			final int value = setup.getColor().get();
			return new Color( value );
		}
		else
			return new Color( 0xFFBBBBBB );
	}

	private static void setColor( final ConverterSetup setup, final Color color )
	{
		setup.setColor( new ARGBType( color.getRGB() | 0xff000000 ) );
	}

	protected static Source< ? > getSource( final int i, final ViewerPanel viewerPanel )
	{
		return viewerPanel.getState().getSources().get( i ).getSpimSource();
	}

	protected static Source< ? > getCurrentSource( final ViewerPanel viewerPanel )
	{
		return getSource( viewerPanel.getState().getCurrentSource(), viewerPanel );
	}

	protected static int getCurrentSourceIndex( final ViewerPanel viewerPanel )
	{
		return viewerPanel.getState().getCurrentSource();
	}

	protected static int getCurrentGroupIndex( final ViewerPanel viewerPanel )
	{
		return viewerPanel.getState().getCurrentGroup();
	}

	protected Source< ? > getSource( final String name )
	{
		return viewerPanel.getState().getSources().get( getSourceIndex( sourceNameBimap.getSource( name ), viewerPanel ) ).getSpimSource();
	}

	protected static int getSourceIndex( final Source< ? > src, final ViewerPanel viewerPanel )
	{
		final List< SourceState< ? > > sources = viewerPanel.getState().getSources();
		for ( final SourceState< ? > s : sources )
		{
			if ( s.getSpimSource().equals( src ) )
			{ return sources.indexOf( s ); }
		}
		return -1;
	}

	protected SourceGroup getGroup( final String name )
	{
		for ( final SourceGroup sg : groupLookup.keySet() )
		{
			if ( groupLookup.get( sg ).getGroupName().equals( name ) )
			{ return sg; }
		}
		return null;
	}

	protected static SourceGroup getCurrentGroup( final ViewerPanel viewerPanel )
	{
		return viewerPanel.getState().getSourceGroups().get( viewerPanel.getState().getCurrentGroup() );
	}

	protected static int getGroupIndex( final String groupName, final ViewerPanel viewerPanel )
	{
		final List< String > groupNames = new ArrayList<>();
		viewerPanel.getState().getSourceGroups().forEach( g -> groupNames.add( g.getName() ) );
		return groupNames.indexOf( groupName );
	}

	public interface SelectionChangeListener
	{
		public void selectionChanged( final boolean isOverlay );
	}

	public Listeners< SelectionChangeListener > selectionChangeListeners()
	{
		return selectionChangeListeners;
	}

	private void notifySelectionChangeListeners( final boolean isOverlay )
	{
		selectionChangeListeners.list.forEach( l -> l.selectionChanged( isOverlay ) );
	}
}
