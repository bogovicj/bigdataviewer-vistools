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

import bdv.tools.transformation.ManualTransformActiveListener;
import bdv.tools.transformation.TransformedSource;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import bdv.BigDataViewer;
import net.miginfocom.swing.MigLayout;

/**
 * Offering the different transformation option of the {@link BigDataViewer}.
 *
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 */
public class TransformationPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final TransformationModel model;

	/**
	 * Panel holding the controls of the viewer and individual transformation.
	 *
	 * @param model
	 * 		holding the source-rotations and handles individual vs viewer transforms.
	 */
	public TransformationPanel( final TransformationModel model )
	{
		this.model = model;

		setupPanel();

		final JCheckBox translation = new JCheckBox( "Allow Translation", true );
		setupTranslationCheckBox( translation );

		final JCheckBox rotation = new JCheckBox( "Allow Rotation", true );
		setupRotationCheckBox( rotation );

		JButton reset = new JButton( "Reset Viewer Transformation" );
		JCheckBox individualTransformation = new JCheckBox( "Manipulate Initial Transformation" );
		setupResetButton( reset, individualTransformation );

		setupManualTransformationCheckBox( reset, individualTransformation );
		model.manualTransformationEditorActive( active -> individualTransformation.setSelected( active ) );

		translation.doClick();
		rotation.doClick();

		this.add( translation, "wrap" );
		this.add( rotation, "wrap" );
		this.add( individualTransformation, "growx, wrap" );
		this.add( reset );
	}

	/**
	 * Initialize transformation panel with color, title, and layout manager
	 */
	private void setupPanel()
	{
		this.setBackground( Color.white );
		this.setBorder( new TitledBorder( "Transformation" ) );
		this.setLayout( new MigLayout( "fillx", "", "" ) );
	}

	/**
	 * Initialize and configure translation check box.
	 *
	 * @param translation
	 */
	private void setupTranslationCheckBox( final JCheckBox translation )
	{
		translation.setBackground( Color.WHITE );
		translation.addActionListener( e -> {
			if ( e.getSource() == translation )
			{
				model.enableTranslation( translation.isSelected() );
			}
		} );
	}

	/**
	 * Initialize and configure rotation check box.
	 *
	 * @param rotation
	 */
	private void setupRotationCheckBox( final JCheckBox rotation )
	{
		rotation.setBackground( Color.WHITE );
		rotation.addActionListener( e -> {
			if ( e.getSource() == rotation )
			{
				model.enableRotation( rotation.isSelected() );
			}
		} );
	}

	/**
	 * Initialize and configure manual transformation checkbox
	 */
	private void setupManualTransformationCheckBox( final JButton reset, final JCheckBox individualTransformation )
	{
		individualTransformation.setToolTipText( "Only possible if all active sources are shown." );
		individualTransformation.setBackground( Color.white );
		individualTransformation.addActionListener( ev -> {
			if ( ev.getSource() == individualTransformation )
			{
				final boolean selected = individualTransformation.isSelected();
				model.manualTransformActiveChanged( selected );
				reset.setText( selected ? "Reset to Initial Transformation" : "Reset Viewer Transformation" );
			}
		} );
	}

	/**
	 * Initialize and configure reset button.
	 *
	 * @param reset
	 */
	private void setupResetButton( final JButton reset, final JCheckBox individualTransformation )
	{
		reset.setBackground( Color.WHITE );
		reset.addActionListener( e -> {
			if ( e.getSource() == reset )
			{
				model.reset( individualTransformation.isSelected() );
			}
		} );
	}
}
