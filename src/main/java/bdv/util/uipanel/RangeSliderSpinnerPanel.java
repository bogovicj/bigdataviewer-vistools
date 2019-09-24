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
 * @author Tobias Pietzsch
 */
public class RangeSliderSpinnerPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	/**
	 * The range slider.
	 */
	private final RangeSlider rs;

	/**
	 * Range slider number of steps.
	 */
	private static final int sliderLength = 10000;

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
	 * The minimum spinner.
	 */
	private final JSpinner currentMinSpinner;

	/**
	 * The maximum spinner.
	 */
	private final JSpinner currentMaxSpinner;

	private Range range;

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
	public RangeSliderSpinnerPanel()
	{
		setupPanel();

		currentMinSpinner = new JSpinner( new SpinnerNumberModel( 0.0, 0.0, 1.0, 1.0 ) );
		setupMinSpinner();

		currentMaxSpinner = new JSpinner( new SpinnerNumberModel( 1.0, 0.0, 1.0, 1.0 ) );
		setupMaxSpinner();

		rs = new RangeSlider( 0, sliderLength );
		setupRangeSlider();

		lowerValue.subscribe( d -> setDisplayRange() );
		upperValue.subscribe( d -> setDisplayRange() );

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
			lowerValue.setValue( lowerBound.getValue() );
			upperValue.setValue( upperBound.getValue() );
			lowerBound.updateListeners();
			upperBound.updateListeners();
			lowerValue.updateListeners();
			upperValue.updateListeners();
			range.setUpperBound( upperValue.getValue() );
			range.setLowerBound( lowerValue.getValue() );
			rs.setValue( 0 );
			rs.setUpperValue( sliderLength );
		} );
	}

	private void setupRangeSlider()
	{
		rs.setBackground( Color.WHITE );
		rs.setPreferredSize( new Dimension( 50, rs.getPreferredSize().height ) );
		rs.setValue( 0 );
		rs.setUpperValue( sliderLength);
		rs.setMinorTickSpacing( 1 );

		upperValue.subscribe( d -> {
			if ( d != posToValue( rs.getUpperValue() ) )
				setRangeSlider();
		} );

		lowerValue.subscribe( d -> {
			if ( d != posToValue( rs.getValue() ) )
				setRangeSlider();
		} );

		rs.addChangeListener( e -> {
			upperValue.setValue( posToValue( rs.getUpperValue() ) );
			lowerValue.setValue( posToValue( rs.getValue() ) );
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
			upperValue.setValue( ( double ) currentMaxSpinner.getModel().getValue() );
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
			lowerValue.setValue( ( double ) currentMinSpinner.getModel().getValue() );
			lowerValue.updateListeners();
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
							range.setUpperBound( upperBound.getValue() );
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
							range.setLowerBound( lowerBound.getValue() );
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
		range.setValueRange( min, max );
	}

	/**
	 * Set the knobs of the range-slider.
	 */
	private void setRangeSlider()
	{
		final int upper = valueToPos( upperValue.getValue() );
		final int lower = valueToPos( lowerValue.getValue() );
		rs.setUpperValue( upper );
		rs.setValue( lower );
	}

	/**
	 * Convert range-slider position to value.
	 *
	 * @param pos
	 *            of range-slider
	 */
	private double posToValue( final int pos )
	{
		final double dmin = lowerBound.getValue();
		final double dmax = upperBound.getValue();
		return ( pos * ( dmax - dmin ) / sliderLength ) + dmin;
	}

	/**
	 * Convert value to range-slider position.
	 */
	private int valueToPos( final double value )
	{
		final double dmin = lowerBound.getValue();
		final double dmax = upperBound.getValue();
		return ( int ) Math.round( ( value - dmin ) * sliderLength / ( dmax - dmin ) );
	}







	public interface Range
	{
		double getLowerBound();

		void setLowerBound( final double value );

		double getLowerValue();

		void setLowerValue( final double value );

		double getUpperBound();

		void setUpperBound( final double value );

		double getUpperValue();

		void setUpperValue( final double value );

		void setValueRange( double lowerValue, double upperValue );
	}

	public synchronized void setRange( final Range range )
	{
		currentMaxSpinner.removeChangeListener( maxSpinnerCL );
		currentMinSpinner.removeChangeListener( minSpinnerCL );

		if ( range != null ) // TODO: what to do when a null range is added
		{
			this.range = range;

			lowerBound.setValue( range.getLowerBound() );
			upperBound.setValue( range.getUpperBound() );
			lowerValue.setValue( range.getLowerValue() );
			upperValue.setValue( range.getUpperValue() );

			( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).setMinimum( range.getLowerBound() );
			( ( SpinnerNumberModel ) currentMinSpinner.getModel() ).setMaximum( range.getUpperBound() );
			( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).setMinimum( range.getLowerBound() );
			( ( SpinnerNumberModel ) currentMaxSpinner.getModel() ).setMaximum( range.getUpperBound() );

			lowerValue.updateListeners();
			upperValue.updateListeners();

			rs.revalidate();
			rs.repaint();

			currentMaxSpinner.addChangeListener( maxSpinnerCL );
			currentMinSpinner.addChangeListener( minSpinnerCL );
		}
	}
}
