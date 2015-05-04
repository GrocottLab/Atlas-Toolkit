package com.grocottlab.atlas_toolkit;

/**
 * Atlas Toolkit plugin for ImageJ and Fiji.
 * Copyright (C) 2015 Timothy Grocott 
 *
 * More information at http://www.grocottlab.com/software
 *
 * This file is part of Atlas Toolkit.
 * 
 * Atlas Toolkit is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * Atlas Toolkit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import java.util.Date;

/**
 * A class for applying an Orthogonal Transform Sequence to an image stack.
 * 
 * This class is called by the Atlas Toolkit menu command "4. Apply Label Registration".
 * 
 */
public class ApplyLabelRegistration_ implements PlugIn {
    
    @Override
	public void run(String arg) {

		// Load the input image
		OpenDialog od = new OpenDialog("Select source image... - ATLAS Toolkit", "");
		Opener op = new Opener();
		ImagePlus source_image = op.openImage(od.getDirectory(), od.getFileName());
		//ImagePlus input = source_image;
		//input.show();
                String name = source_image.getTitle();
		
		// Load orthogonal transform sequences
		od = new OpenDialog("Select orthogonal transform sequence... - ATLAS Toolkit", "");
                OrthogonalTransformSequence ots = OrthogonalTransformSequence.openOTS(od.getDirectory() + od.getFileName());
                
                // Get number of iterations in orthogonal transform sequence
                int iterationCount = ots.getIterations();
                
                // Ask user how many of those iterations should be applied to the input image
                GenericDialog gd = new GenericDialog("How many iterations should be applied? - ATLAS toolkit");
		int iterations = iterationCount;
                gd.addNumericField("Number of iterations: ", iterations, 0);
                gd.showDialog();
		if (gd.wasCanceled()) return;
                iterations = (int)gd.getNextNumber();
                if (iterations > iterationCount) {
                    iterations = iterationCount;
                } else if (iterations <=0) {
                    iterations = 1;
                }
		
		// Get start_time
		long start_time = new Date().getTime();

		// Define ImagePlus for holding resliced_images and registered_images
		ImagePlus resliced_image;// = new ImagePlus[1];

		// Determine the number of iterations included in sequence
                //int iteration_count = ots.getIterations();
                
		// Iterate through iterations
		IJ.showStatus("Begin...");
		for (int i = 0; i < iterations; i++) {
		
			IJ.log("iteration " + (i+1) + " of " + iterations);
			// Iterate through orthognal planes
			for (OrthogonalTransformSequence.OrthogonalPlane orthoplane : OrthogonalTransformSequence.OrthogonalPlane.values()) {

				IJ.log("orthoplane " + orthoplane);
				OrthogonalTransformSequence.OrthogonalPlane current_plane = orthoplane;

				// Reslice current source image
				IJ.log("Reslicing image.");
				resliced_image = LabelRegistration3D_.reslice(source_image);

				// Register source_image
				IJ.log("\n-----\nOrthogonal Plane " + orthoplane + "\n-----\n");
				StackRegister sr = new StackRegister(resliced_image);
				source_image = sr.getTransformedImage(ots.getOrthogonalTransform(orthoplane, i));
			}
		}
		
		// Get end_time
		final long end_time = new Date().getTime();
		long elapsed_time = end_time - start_time;
		int min_elapsed = (int)((elapsed_time / 1000) / 60);
		IJ.log("Transformed image in " + min_elapsed + " minutes!");

		// Show registered_images
                source_image.setTitle(name);
		source_image.show();
	}
}


