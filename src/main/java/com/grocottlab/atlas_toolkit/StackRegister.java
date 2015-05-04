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

import bunwarpj.MiscTools;
import bunwarpj.Param;
import bunwarpj.bUnwarpJ_;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;

/**
 * This class performs group-wise registration of multiple 3D (XYZ) image stacks in a single 2D 
 * (e.g XY, YZ, ZX) plane.
 * 
 * It is called by LabelRegistration3D for each orthogonal plane in each iteration. It provides
 * the user with progress feedback via a ProgressWindow object.
 * 
 */
public class StackRegister {
    
        /*
	 *  A class for registering multiple image stacks to one-another.
	 */ 

	private ImagePlus[] source_images;
	private OrthogonalTransform[] ot = null;
        private ProgressWindow progressWindow;

	// Set the registration_parameters
	private Param registration_parameters = new Param(2,	// Mode, 2 = mono
							  0,	// Sub-sample factor
							  2,	// Minimum (final) scale deformation, 2 = fine, 3 = very fine
							  0,	// Maximum (initial) scale deformation, 0 = very coarse
							  0.0,	// Divergence weight
							  0.0,	// Curl weight
							  0.0,	// Landmark weight
							  1.0,	// Image weight
							  10.0,	// Consistency weight
							  0.01	// Stop threshold
							  );
		 
	public StackRegister(ImagePlus[] source) {

		// Constructor
		source_images = source;
	}
        
        public StackRegister(ImagePlus[] source, ProgressWindow progressWindow) {

		// Constructor
		source_images = source;
                this.progressWindow = progressWindow;
	}

	public StackRegister(ImagePlus source) {

		// Constructor
		source_images = new ImagePlus[1];
		source_images[0] = source;
	}

	public ImagePlus getTransformedImage(OrthogonalTransform ot) {

		/*
		 * Use the supplied transformation_coefficients to transform the source images.
		 */

		this.ot = new OrthogonalTransform[1];
		this.ot[0] = ot;

		ImagePlus[] temp = getTransformedImages();

		return temp[0];
	}

	public ImagePlus[] getTransformedImages(OrthogonalTransform[] ot) {

		/*
		 * Use the supplied transformation_coefficients to transform the source images.
		 */

		this.ot = ot;

		return getTransformedImages();
	}

	public ImagePlus[] getTransformedImages() {

		/*
		 *  Register source images to one-another then return the transformed images.
		 */

		if (ot == null) {

			// Calculate transforms
			calculateTransforms();
		}

		// Create registered_images for storing registered images
		ImagePlus[] registered_images = new ImagePlus[source_images.length];
		
		// Transform source_image using mean coefficients
		for (int src = 0; src < source_images.length; src++) {

			// Update status
                        String status = "Image " + (src+1) + " of " + source_images.length + "; Applying transform...";
			IJ.showStatus(status);
                        if (progressWindow != null) progressWindow.showStatus(1, status);
			IJ.log("\n-----\nImage " + (src+1) + " of " + source_images.length + "; Applying transform...\n-----\n");

			// Create a temp_stack from source_image
			ImageStack temp_stack = source_images[src].createEmptyStack();
			ImagePlus current_imp;
			int stack_size = source_images[src].getStack().getSize();

			// Iterate through each slice
			for (int slice = 1; slice <= stack_size; slice++) {

				// Update progress every ten slices
                                if (slice % 10 == 0) {
                                    IJ.showProgress(slice, stack_size);
                                    if (progressWindow != null) progressWindow.showProgress(1, slice, stack_size);
                                }

				// Set the current stack slice
				//source_images[src].setSlice(slice);

				// Copy the current ImageProcessor into a new ImagePlus
				current_imp = new ImagePlus("Slice_" + slice, source_images[src].getStack().getProcessor(slice));

				// Transform the current slice
				MiscTools.applyTransformationToSourceMT(current_imp, current_imp, ot[src].getIntervals(), ot[src].getCoefficientsX(), ot[src].getCoefficientsY() );

				// Add ImageProcessor from current_imp to the end of temp_Stack
				if (source_images[src].getBitDepth() == 8) {
					
					temp_stack.addSlice(current_imp.getProcessor().convertToByte(false));
					
				} else {

					temp_stack.addSlice(current_imp.getProcessor());
				}
				
			}

			// Assign temp_stack to registered_image
			source_images[src] = new ImagePlus("transformed_source #" + (src+1), temp_stack);
		}

		// Return the registered images
		return source_images;
	}

	public OrthogonalTransform[] getOrthogonalTransforms() {

		/*
		 *  If mean coefficients is null, calculate and return, else just return.
		 */ 

		if (ot == null) {

			calculateTransforms();
				
		}

		return ot;
	}

	private void calculateTransforms() {

		/*
		 *  Calculate the average for each source image.
		 */

		 for (int i = 0; i < source_images.length; i++) {
		 	 if (source_images[i] == null) {

		 	 	IJ.log("source_images[" + (i+1) + "] == null!");
		 	 }
		 }

		// Generate average projections from source_images
		ImagePlus [] guide_projections = new ImagePlus[source_images.length];
		ZProjector zp = new ZProjector();
		for (int i = 0; i < source_images.length; i++) {

			IJ.log("Projecting source image " + (i + 1) + " of " + source_images.length);
			zp.setImage(source_images[i]);
			zp.setMethod(ZProjector.AVG_METHOD);//MAX_METHOD);
			zp.doProjection();
			guide_projections[i] = zp.getProjection();
		}

		OrthogonalTransform[] mean_ot = new OrthogonalTransform[source_images.length];

		// Iterate through guide_images, i.e. the 'source images' to be registered
		for (int src = 0; src < source_images.length; src++) {

			OrthogonalTransform[] temp_ot = new OrthogonalTransform[source_images.length];

			//int pair_counter = 0;

			// Update status
                        String status = "Image " + (src+1) + " of " + source_images.length + "; Calculating transforms...";
			IJ.showStatus(status);
                        if (progressWindow != null) progressWindow.showStatus(1, status);
			IJ.log("\n-----\nImage " + (src+1) + " of " + source_images.length + "; Calculating transforms...\n-----\n");

			// Iterate through all other guide_images, i.e. the targets we a registering to
			for (int tgt = 0; tgt < source_images.length; tgt++) {

				// Update progress
                                if (progressWindow != null) progressWindow.setProgress(src, tgt);
				IJ.showProgress(tgt, source_images.length);
                                if (progressWindow != null) progressWindow.showProgress(1, tgt, source_images.length);

				// Register the current src/tgt pair from guide_projections[]
				temp_ot[tgt] = new OrthogonalTransform(bUnwarpJ_.computeTransformationBatch(guide_projections[tgt],	// Target image plus
													    guide_projections[src],	// Souce image plus
													    null,			// Target Mask image processor
													    null,			// Source Mask image processor
													    registration_parameters   // Registration parameters
													    )
													    );
				//pair_counter++;
			}

			// Calculate average transformation
			mean_ot[src] = OrthogonalTransform.computeAverageTransform(temp_ot);
		}
                if (progressWindow != null) progressWindow.clearProgress();

		this.ot = mean_ot;
	}
}
