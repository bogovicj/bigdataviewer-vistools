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
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import bdv.tools.brightness.SetupAssignments;
import bdv.util.uipanel.rangeslider.RangeSlider;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;

/**
 * A panel holding a two-knob range slider with a lower- and upper-value
 * spinner.
 *
 * The bounds can be dynamically changed by either entering smaller/larger
 * values into the spinner or resizing the range-slider to the current positions
 * with a resize-button.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 */
public class RangeSliderSpinnerPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	/**
	 * Upper bound of the range slider.
	 */
	private static final int RS_UPPER_BOUND = 1000;

	/**
	 * Setup assignments of the viewer.
	 */
	private final SetupAssignments setupAssignments;

	/**
	 * Bdv viewer panel.
	 */
	private final ViewerPanel viewerPanel;

	/**
	 * The range slider.
	 */
	private final RangeSlider rs;

	/**
	 * Range slider number of steps.
	 */
	final double numberOfSteps = 1001.0;

	/**
	 * Display range upper bound.
	 */
	private final ListenableDouble upperBound = new ListenableDouble( 1 );

	/**
	 * Display range lower bound.
	 */
	private final ListenableDouble lowerBound = new ListenableDouble( 0 );

	/**
	 * Display range upper value. The currently selected upper value.
	 */
	private final ListenableDouble upperValue = new ListenableDouble( 1 );

	/**
	 * Display range lower value. The currently selected lower value.
	 */
	private final ListenableDouble lowerValue = new ListenableDouble( 0 );

	/**
	 * Store the lower bound for every source.
	 */
	private final HashMap< Source< ? >, Double > SOURCE_lowerBoundLookup = new HashMap<>();

	/**
	 * Store the upper bound for every source.
	 */
	private final HashMap< Source< ? >, Double > SOURCE_upperBoundLookup = new HashMap<>();

	/**
	 * The minimum spinner.
	 */
	private final JSpinner currentMinSpinner;

	/**
	 * The maximum spinner.
	 */
	private final JSpinner currentMaxSpinner;

	/**
	 * Index of the currently selected source.
	 */
	private int SOURCE_currentSourceIdx; // TODO: private field SOURCE_currentSourceIdx is never assigned

	private ChangeListener maxSpinnerCL;

	private ChangeListener minSpinnerCL;

	private class ListenableDouble
	{
		private double d;

		private final List< DoubleListener > listeners;

		public ListenableDouble( final double init )
		{
			this.d = init;
			listeners = new ArrayList<>();
		}

		public void setValue( final double val )
		{
			this.d = val;
		}

		public double getValue()
		{
			return this.d;
		}

		public void subscribe( final DoubleListener l )
		{
			listeners.add( l );
		}

		public void updateListeners()
		{
			for ( final DoubleListener l : listeners )
			{
				l.doubleChanged( this.d );
			}
		}
	}

	private interface DoubleListener
	{
		void doubleChanged( final double d );
	}

	/**
	 * A range slider panel with two knobs and min/max spinners.
	 */
	public RangeSliderSpinnerPanel( final SetupAssignments setupAssignments, final ViewerPanel vp )
	{
		setupPanel();

		this.setupAssignments = setupAssignments;

		this.viewerPanel = vp;

		currentMinSpinner = new JSpinner( new SpinnerNumberModel( 0.0, 0.0, 1.0, 1.0 ) );
		setupMinSpinner();

		currentMaxSpinner = new JSpinner( new SpinnerNumberModel( 1.0, 0.0, 1.0, 1.0 ) );
		setupMaxSpinner();

		rs = new RangeSlider( 0, RS_UPPER_BOUND );
		setupRangeSlider();

		lowerValue.subscribe( new DoubleListener()
		{

			@Override
			public void doubleChanged( final double d )
			{
				setDisplayRange();
			}
		} );

		upperValue.subscribe( new DoubleListener()
		{

			@Override
			public void doubleChanged( final double d )
			{
				setDisplayRange();
			}
		} );

		final JButton shrinkRange = new JButton( "><" );
		setupShrinkRangeButton( shrinkRange );

		this.add( currentMinSpinner );
		this.add( rs, "growx" );
		this.add( currentMaxSpinner );
		this.add( shrinkRange );
	}

	private void setupPanel()
	{
		this.setLayout( new MigLayout( "fillx, hidemode 3", "[][grow][][]", "" ) );
		this.setBorder( new TitledBorder( new LineBorder( Color.lightGray ), "Display Range" ) );
		this.setBackground( Color.WHITE );
	}

	private void setupShrinkRangeButton( final JButton shrinkRange )
	{
		shrinkRange.setBackground( Color.white );
		shrinkRange.setForeground( Color.darkGray );
		shrinkRange.setBorder( null );
		shrinkRange.setMargin( new Insets( 0, 2, 0, 2 ) );
		shrinkRange.addActionListener( e -> {
			lowerBound.setValue( ( double ) ( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).getValue() );
			upperBound.setValue( ( double ) ( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).getValue() );
			upperValue.setValue( upperBound.getValue() );
			lowerValue.setValue( lowerBound.getValue() );
			lowerBound.updateListeners();
			upperBound.updateListeners();
			lowerValue.updateListeners();
			upperValue.updateListeners();
			SOURCE_upperBoundLookup.put( viewerPanel.getState().getSources().get( SOURCE_currentSourceIdx ).getSpimSource(), upperValue.getValue() );
			SOURCE_lowerBoundLookup.put( viewerPanel.getState().getSources().get( SOURCE_currentSourceIdx ).getSpimSource(), lowerValue.getValue() );
			rs.setValue( 0 );
			rs.setUpperValue( RS_UPPER_BOUND );
		} );
	}

	private void setupRangeSlider()
	{
		rs.setBackground( Color.WHITE );
		rs.setPreferredSize( new Dimension( 50, rs.getPreferredSize().height ) );
		rs.setValue( 0 );
		rs.setUpperValue( RS_UPPER_BOUND );
		rs.setMinorTickSpacing( 1 );

		upperValue.subscribe( d -> {
			if ( d != posToLowerValue( rs.getUpperValue() ) )
				setRangeSlider();
		} );

		lowerValue.subscribe( d -> {
			if ( d != posToLowerValue( rs.getValue() ) )
				setRangeSlider();
		} );

		rs.addChangeListener( e -> {
			upperValue.setValue( posToUpperValue( rs.getUpperValue() ) );
			lowerValue.setValue( posToLowerValue( rs.getValue() ) );
			lowerValue.updateListeners();
			upperValue.updateListeners();
		} );
	}

	private void setupMaxSpinner()
	{
		currentMaxSpinner.setPreferredSize( new Dimension( 65, currentMaxSpinner.getPreferredSize().height ) );

		upperValue.subscribe( d -> {
			if ( d != ( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).getNumber().doubleValue() )
				currentMaxSpinner.setValue( d );
		} );

		lowerBound.subscribe( d -> {
			if ( 0 != ( double ) ( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).getMinimum() )
				( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).setMinimum( d );
		} );

		upperBound.subscribe( d -> {
			if ( 0 != ( double ) ( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).getMaximum() )
				( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).setMaximum( d );
		} );

		maxSpinnerCL = e -> {
			upperValue.setValue( ( double ) ( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).getValue() );
			upperValue.updateListeners();
		};
		currentMaxSpinner.addChangeListener( maxSpinnerCL );
		currentMaxSpinner.setEditor( new UpperBoundNumberEditor( currentMaxSpinner ) );
	}

	private void setupMinSpinner()
	{
		currentMinSpinner.setPreferredSize( new Dimension( 65, currentMinSpinner.getPreferredSize().height ) );

		lowerValue.subscribe( d -> {
			if ( d != ( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).getNumber().doubleValue() )
				currentMinSpinner.setValue( d );
		} );

		lowerBound.subscribe( d -> {
			if ( 0 != ( double ) ( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).getMinimum() )
				( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).setMinimum( d );
		} );

		upperBound.subscribe( d -> {
			if ( 0 != ( double ) ( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).getMaximum() )
				( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).setMaximum( d );
		} );

		minSpinnerCL = e -> {
			if ( e.getSource() == currentMinSpinner )
			{
				final double value = ( double ) ( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).getValue();
				lowerValue.setValue( value );
				lowerValue.updateListeners();
			}
		};
		currentMinSpinner.addChangeListener( minSpinnerCL );
		currentMinSpinner.setEditor( new LowerBoundNumberEditor( currentMinSpinner ) );
	}

	class UpperBoundNumberEditor extends JSpinner.NumberEditor implements KeyListener
	{
		private static final long serialVersionUID = 1L;

		private final JFormattedTextField textField;

		public UpperBoundNumberEditor( final JSpinner spinner )
		{
			super( spinner );
			textField = getTextField();
			textField.addKeyListener( this );
		}

		@Override
		public void keyTyped( final KeyEvent e )
		{}

		@Override
		public void keyPressed( final KeyEvent e )
		{
			final String text = textField.getText();
			if ( !text.isEmpty() )
			{
				try
				{
					if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					{
						final double tmp = NumberFormat.getNumberInstance().parse( text ).doubleValue();
						if ( tmp > upperBound.getValue() )
						{
							upperBound.setValue( tmp );
							SOURCE_upperBoundLookup.put( viewerPanel.getState().getSources().get( SOURCE_currentSourceIdx ).getSpimSource(), upperBound.getValue() );
							upperValue.setValue( tmp );
							upperBound.updateListeners();
							upperValue.updateListeners();
						}
						else
						{
							upperValue.setValue( tmp );
							upperValue.updateListeners();
						}
					}
				}
				catch ( final ParseException e1 )
				{
					textField.setText( Double.toString( upperBound.getValue() ) );
				}
			}
		}

		@Override
		public void keyReleased( final KeyEvent e )
		{}
	}

	class LowerBoundNumberEditor extends JSpinner.NumberEditor implements KeyListener
	{
		private static final long serialVersionUID = 1L;

		private final JFormattedTextField textField;

		public LowerBoundNumberEditor( final JSpinner spinner )
		{
			super( spinner );
			textField = getTextField();
			textField.addKeyListener( this );
		}

		@Override
		public void keyTyped( final KeyEvent e )
		{}

		@Override
		public void keyPressed( final KeyEvent e )
		{
			final String text = textField.getText();
			if ( !text.isEmpty() )
			{
				try
				{
					if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					{
						final double tmp = NumberFormat.getNumberInstance().parse( text ).doubleValue();
						if ( tmp < lowerBound.getValue() )
						{
							lowerBound.setValue( tmp );
							SOURCE_lowerBoundLookup.put( viewerPanel.getState().getSources().get( SOURCE_currentSourceIdx ).getSpimSource(), lowerBound.getValue() );
							lowerValue.setValue( tmp );
							lowerBound.updateListeners();
							lowerValue.updateListeners();
						}
						else
						{
							lowerValue.setValue( tmp );
							lowerValue.updateListeners();
						}
					}
				}
				catch ( final ParseException e1 )
				{
					textField.setText( Double.toString( lowerBound.getValue() ) );
				}
			}
		}

		@Override
		public void keyReleased( final KeyEvent e )
		{}
	}

	/**
	 * Set display range in setup-assignments.
	 *
	 */
	private void setDisplayRange()
	{
		final double min = lowerValue.getValue();
		final double max = upperValue.getValue();
		setupAssignments.getConverterSetups().get( SOURCE_currentSourceIdx ).setDisplayRange( min, max );
	}

	/**
	 * Convert range-slider position to upper-value.
	 *
	 * @param pos
	 *            of range-slider
	 * @return value
	 */
	private double posToUpperValue( final int pos )
	{
		final double frac = pos / 1000d;
		final double val = Math.abs( upperBound.getValue() - lowerBound.getValue() ) * frac + lowerBound.getValue();
		return val;
	}

	/**
	 * Convert range-slider position to lower-value.
	 *
	 * @param pos
	 *            of range-slider
	 * @return value
	 */
	// TODO: is identical to posToUpperValue
	private double posToLowerValue( final int pos )
	{
		final double frac = pos / 1000d;
		final double val = Math.abs( upperBound.getValue() - lowerBound.getValue() ) * frac + lowerBound.getValue();
		return val;
	}

	public synchronized void setSource( final Source< ? > src )
	{
		currentMaxSpinner.removeChangeListener( maxSpinnerCL );
		currentMinSpinner.removeChangeListener( minSpinnerCL );
		if ( SOURCE_lowerBoundLookup.containsKey( src ) )
		{
			lowerBound.setValue( SOURCE_lowerBoundLookup.get( src ) );
			upperBound.setValue( SOURCE_upperBoundLookup.get( src ) );

			final double displayRangeMin = setupAssignments.getConverterSetups().get( SOURCE_currentSourceIdx ).getDisplayRangeMin();
			final double displayRangeMax = setupAssignments.getConverterSetups().get( SOURCE_currentSourceIdx ).getDisplayRangeMax();
			lowerValue.setValue( displayRangeMin );
			upperValue.setValue( displayRangeMax );

			( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).setMinimum( lowerBound.getValue() );
			( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).setMaximum( upperBound.getValue() );
			( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).setMinimum( lowerBound.getValue() );
			( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).setMaximum( upperBound.getValue() );

			lowerValue.updateListeners();
			upperValue.updateListeners();

			rs.revalidate();
			rs.repaint();

			currentMaxSpinner.addChangeListener( maxSpinnerCL );
			currentMinSpinner.addChangeListener( minSpinnerCL );
		}
	}

	public void addSource( final Source< ? > src )
	{
		// TODO
		final double displayRangeMin = setupAssignments.getConverterSetups().get( 0 ).getDisplayRangeMin();
		final double displayRangeMax = setupAssignments.getConverterSetups().get( 0 ).getDisplayRangeMax();

		SOURCE_lowerBoundLookup.put( src, displayRangeMin );
		SOURCE_upperBoundLookup.put( src, displayRangeMax );
	}

	public synchronized void removeSource( final Source< ? > source )
	{
		SOURCE_lowerBoundLookup.remove( source );
		SOURCE_upperBoundLookup.remove( source );
	}

	/**
	 * Set the knobs of the range-slider.
	 */
	private void setRangeSlider()
	{
		final double range = upperBound.getValue() - lowerBound.getValue();
		final int upperVal = ( int ) ( ( ( upperValue.getValue() - lowerBound.getValue() ) / range ) * numberOfSteps );
		final int lowerVal = ( int ) ( ( ( lowerValue.getValue() - lowerBound.getValue() ) / range ) * numberOfSteps );
		rs.setUpperValue( upperVal );
		rs.setValue( lowerVal );
	}
}
