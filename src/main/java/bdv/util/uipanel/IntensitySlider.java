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

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;

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
public class IntensitySlider
{
	private final Map< Source< ? >, SourceRange > sourceToRange = new HashMap<>();

	public RangeSliderSpinnerPanel getPanel()
	{
		return panel;
	}

	private class SourceRange implements RangeSliderSpinnerPanel.Range
	{
		private final ConverterSetup converterSetup;

		private double lowerBound;

		private double upperBound;

		public SourceRange( final ConverterSetup converterSetup )
		{
			this.converterSetup = converterSetup;
			lowerBound = converterSetup.getDisplayRangeMin();
			upperBound = converterSetup.getDisplayRangeMax();
		}

		@Override
		public double getLowerBound()
		{
			return lowerBound;
		}

		@Override
		public void setLowerBound( final double value )
		{
			lowerBound = value;
		}

		@Override
		public double getLowerValue()
		{
			return converterSetup.getDisplayRangeMin();
		}

		@Override
		public void setLowerValue( final double value )
		{
			converterSetup.setDisplayRange( value, getUpperValue() );
		}

		@Override
		public double getUpperBound()
		{
			return upperBound;
		}

		@Override
		public void setUpperBound( final double value )
		{
			upperBound = value;
		}

		@Override
		public double getUpperValue()
		{
			return converterSetup.getDisplayRangeMax();
		}

		@Override
		public void setUpperValue( final double value )
		{
			converterSetup.setDisplayRange( getLowerValue(), value );
		}

		@Override
		public void setValueRange( final double lowerValue, final double upperValue )
		{
			converterSetup.setDisplayRange( lowerValue, upperValue );
		}
	}

	private final RangeSliderSpinnerPanel panel;

	public IntensitySlider()
	{
		this.panel = new RangeSliderSpinnerPanel();
	}

	public synchronized void setSource( final Source< ? > source )
	{
		panel.setRange( sourceToRange.get( source ) );
	}

	public synchronized void addSource( final Source< ? > source, final ConverterSetup converterSetup )
	{
		sourceToRange.put( source, new SourceRange( converterSetup ) );
	}

	public synchronized void removeSource( final Source< ? > source )
	{
		sourceToRange.remove( source );
	}

	public void setEnabled( final boolean enabled )
	{
		panel.setEnabled( enabled );
	}

}
