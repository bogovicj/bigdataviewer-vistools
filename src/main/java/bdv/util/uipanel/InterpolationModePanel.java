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

import java.awt.event.ItemListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import bdv.viewer.Interpolation;
import bdv.viewer.ViewerPanel;
import net.miginfocom.swing.MigLayout;

/**
 * This panel holds the {@link Interpolation} mode selection.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 */
public class InterpolationModePanel extends JPanel
{

	final JComboBox< Interpolation > interpolationModes;

	private static final long serialVersionUID = 1L;

	/**
	 * Showing the different {@link Interpolation} modes.
	 */
	public InterpolationModePanel()
	{
		interpolationModes = new JComboBox<>();
		for ( final Interpolation mode : Interpolation.values() )
		{
			interpolationModes.addItem( mode );
		}

		this.setBackground( Color.WHITE );
		this.setBorder( new TitledBorder( "Interpolation Mode" ) );
		this.setLayout( new MigLayout( "fillx", "[grow]", "" ) );
		this.add( interpolationModes, "growx" );
	}

	/**
	 * Add item listern to the component.
	 *
	 * @param itemListener
	 */
	public void addItemListener( final ItemListener itemListener )
	{
		interpolationModes.addItemListener( itemListener );
	}

	/**
	 *
	 * @return Selected interpolation mode.
	 */
	public Interpolation getInterpolationMode()
	{
		return ( Interpolation ) interpolationModes.getSelectedItem();
	}

	/**
	 *
	 * @param mode Set selected interpolation mode.
	 */
	public void setInterpolationModes( final Interpolation mode )
	{
		interpolationModes.setSelectedItem( mode );
	}
}
