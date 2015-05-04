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
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.plugin.Resizer;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import java.util.Date;

/**
 * A class for registering multiple 3D image stacks in group-wise fashion.
 * 
 * This class is called by the Atlas Toolkit menu command "3. Label Registration 3D".
 * 
 */
public class LabelRegistration3D_ implements PlugIn {
    
    int image_count;
    String[] input_filename;
    static ProgressWindow progressWindow;
 
	@Override
	public void run(String arg) {

		// Get number of source images
		/*GenericDialog gd = new GenericDialog("Number of images");
		int image_count = 2;
		gd.addNumericField("Number of images to register: ", image_count, 0);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		image_count = (int)gd.getNextNumber();*/

		// Get input filenames - allow selection of multiple files
		String[] input_filepath = new String[255];
                input_filename = new String[255];
		image_count = 0;
		boolean cancelled = false;
		OpenDialog od;
		for (int i = 0; (i < input_filepath.length) && !cancelled; i++) {

			od = new OpenDialog("Select Volume #" + (i+1) + "... - ATLAS toolkit", "");
			if (od.getFileName() != null) {

				input_filepath[i] = od.getDirectory() + od.getFileName();
                                input_filename[i] = od.getFileName();
				image_count++;
			} else {
				cancelled = true; //i = input_filename.length;
			}
		}

		// Determine number of iterations to run, first orthogonal plane
		GenericDialog gd = new GenericDialog("Label Registration 3D - ATLAS toolkit");
		int label_choice = 1;
                gd.addNumericField("Select Channel (0-255):", label_choice, 0);
                int iteration_count = 1;
                gd.addNumericField("Number of iterations: ", iteration_count, 0);
		int first_plane = 2;
                String[] choice_of_planes = { "XY", "YZ", "ZX" };
		gd.addChoice("First Orthogonal Plane: ", choice_of_planes, "YZ");
		gd.showDialog();
		if (gd.wasCanceled()) return;
                label_choice = (int)gd.getNextNumber();
		iteration_count = (int)gd.getNextNumber();
		String chosen_plane = gd.getNextChoice();

		// Load the input images
		ImagePlus[] source_images = new ImagePlus[image_count];
		Opener op = new Opener();
		for (int i = 0; i < image_count; i++) {

			source_images[i] = op.openImage(input_filepath[i]);
			source_images[i].setTitle("source_image #" + (i+1));
		}

		// Get dimensions of source images
		int[][] source_dimensions = new int[image_count][3];
		for (int image = 0; image < image_count; image++) {
			for (int i = 0; i < 3; i++) {

				source_dimensions[image][0] = source_images[image].getDimensions()[0];	// Store the x dimension
				source_dimensions[image][1] = source_images[image].getDimensions()[1];	// Store the y dimension
				source_dimensions[image][2] = source_images[image].getDimensions()[3];	// Store the z dimension
			}
		}

		// Isolate the chosen labels (set label pixels to 255, all other pixels to zero
		IJ.log("Analyzing labels...");
		for (int image = 0; image < image_count; image++) {

			for (int z = 0; z < source_dimensions[image][2]; z++) {

				float z_progress = z / source_dimensions[image][2];
				//IJ.showStatus("z_progress = " + z_progress);
				IJ.showStatus("Analyzing labels...");
				IJ.showProgress( (int)(100.0 * (image + z_progress) ), image_count * 100);
				source_images[image].setSlice(z+1);
				for (int y = 0; y < source_dimensions[image][1]; y++) {
					for (int x = 0; x < source_dimensions[image][0]; x++) {

						if (source_images[image].getProcessor().getPixel(x, y) == label_choice) {

							source_images[image].getProcessor().set(x, y, 255);
						} else {
							source_images[image].getProcessor().set(x, y, 0);
						}
					}
				}
			}
		}
		IJ.showStatus("Analyzing labels...done!");
		IJ.log("...done!");
		
		// Get start_time
		final long start_time = new Date().getTime();

		// Create an array of SerialOrthogonalTransforms for storing Coeffs
		OrthogonalTransform[][][] ot = new OrthogonalTransform[iteration_count][3][image_count];

		// Define arrays for holding resliced_images and registered_images
		ImagePlus[] registered_images = new ImagePlus[image_count];
		final ImagePlus[] resliced_images = new ImagePlus[image_count];

		// Establish number of threads to use when reslicing images
		final int thread_count;
		final int max_cores = Runtime.getRuntime().availableProcessors();
		if (image_count > max_cores) {

			thread_count = (int)(max_cores); // * 0.75); // / 2;//- 1;
			
		} else {

			thread_count = image_count;
		}
		IJ.log("image_count = " + image_count);
		IJ.log("thread_count = " + thread_count);
		//int images_done = 0;
		final Thread[] threads = new Thread[thread_count];

		// Advance to first plane
		if (chosen_plane == "ZX" ) {
			//source_images = 
			advance_plane(source_images, 1);
		} else if (chosen_plane == "XY") {
			//source_images = 
			advance_plane(source_images, 2);
		}
                
                // Show the progress window
                progressWindow = new ProgressWindow(image_count, iteration_count);

		// Iterate through iterations
		for (int iteration = 0; iteration < iteration_count; iteration++) {
			
			// Iterate through orthognal planes
			IJ.showStatus("Begin...");
                        int regPlane = 0;
			for (OrthogonalTransformSequence.OrthogonalPlane orthoplane : OrthogonalTransformSequence.OrthogonalPlane.values()) {

				// Update progress
                                regPlane++;
                                //progressWindow.setPlane(regPlane);
                                //progressWindow.setIteration(iteration);
                                progressWindow.showProgress(0, ( (iteration * 3) + regPlane), (iteration_count * 3) );
                                progressWindow.showStatus(0, "Iteration " + (iteration+1) + " of " + iteration_count + "; Plane " + orthoplane);
                                // Iterate through source_images to reslice
				for (int src = 0; src < image_count; src++) {

					int threads_to_use = thread_count;
					if ( (image_count - src) < thread_count ) {
						threads_to_use = (image_count - src);
					}
					// Iterate through threads
					for (int i = 0; i < threads_to_use; i++) {

						//images_done++;
						//IJ.log("images_done = " + images_done);
						final int current_image = src;
						final int current_iteration = iteration;
						final OrthogonalTransformSequence.OrthogonalPlane current_plane = orthoplane;
						final int max_images = image_count;
						final ImagePlus previous_image = registered_images[current_image];
						final ImagePlus temp_source = source_images[current_image];

						// Setup current thread
						threads[i] = new Thread() {

							{ setPriority(Thread.NORM_PRIORITY); }

							public void run() {

								// Reslice current image
								IJ.log("Reslicing image " + (current_image+1) + " of " + max_images);
								if (current_iteration == 0) {
									switch (current_plane) {
										case YZ:
											if (temp_source == null) IJ.log("temp_source == null");
											resliced_images[current_image] = reslice(temp_source);//source_images[current_image]);
											if (resliced_images[current_image] == null) IJ.log("resliced_images["+current_image+"] == null");
											//temp_source.close();//source_images[current_image].close();
											break;
										default:
											// This is not the first plane, so reslice previously registered_images
											resliced_images[current_image] = reslice(previous_image);
											break;
									}
								} else {
									// This is not the first plane, so reslice previously registered_images
									resliced_images[current_image] = reslice(previous_image);
								}
								if (resliced_images[current_image] == null) IJ.log("resliced_images["+current_image+"] == null");
							}
						};

						// Increment current source image
						src++;
					}
					src--;

					// Start threads and wait until finished
					try {
						for (int i = 0; i < threads_to_use; i++) {
							threads[i].start();
						}
						for (int i = 0; i < threads_to_use; i++) {
							threads[i].join();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				// Register source_images
				IJ.log("\n-----\nOrthogonal Plane " + orthoplane + "\n-----\n");
				if (resliced_images[0] == null) IJ.log("resliced_images[0] == null");
				StackRegister sr = new StackRegister(resliced_images, progressWindow);
				registered_images = sr.getTransformedImages();

				// Store tranforms for the current orthoplane
				switch (orthoplane) {
					case XY: ot[iteration][0] = sr.getOrthogonalTransforms();
						 break;
					case YZ: ot[iteration][1] = sr.getOrthogonalTransforms();
						 break;
					case ZX: ot[iteration][2] = sr.getOrthogonalTransforms();
						 break;
					default: ot[0][0] = sr.getOrthogonalTransforms();
						 break;
				}
			}
		}
		

		// Advance to first plane
		if (chosen_plane == "ZX" ) {
			//registered_images = 
			advance_plane(registered_images, 2);
		} else if (chosen_plane == "XY") {
			//registered_images = 
			advance_plane(registered_images, 1);
		}

		// Get end_time
		final long end_time = new Date().getTime();
		long elapsed_time = end_time - start_time;
		int min_elapsed = (int)((elapsed_time / 1000) / 60);
		IJ.log("Registered " + image_count + " images in " + min_elapsed + " minutes!");
                progressWindow.setVisible(false);

		// Save coeffs to user-specified folder
		DirectoryChooser dc = new DirectoryChooser("Select folder to save serial orthogonal transforms... - ATLAS toolkit");
		OrthogonalTransformSequence[] ots = new OrthogonalTransformSequence[image_count];
		for (int image = 0; image < image_count; image++) {

			OrthogonalTransform[][] temp_ot = new OrthogonalTransform[iteration_count][3];
			for (int iteration = 0; iteration < iteration_count; iteration++) {
				
				for (int plane = 0; plane < 3; plane++) {

					temp_ot[iteration][plane] = ot[iteration][plane][image];
					ots[image] = new OrthogonalTransformSequence(temp_ot, source_dimensions[image]);
				}
			}
			//ots[image].saveToFile(dc.getDirectory() + "/orthogonaltransform" + image + ".ots");
                        ots[image].saveToFile(dc.getDirectory() + "/" + input_filename[image] + ".ots");
			
			// Save & show registered_images
			//IJ.save(registered_images[image], dc.getDirectory() + "/label" + image + ".tif");
                        IJ.save(registered_images[image], dc.getDirectory() + "/" +  input_filename[image]);
			registered_images[image].show();
		}

		// Calculate intersection of registered images, save & show
		ImagePlus intersection = getIntersection(registered_images);

		intersection.setTitle("consensus");
		intersection.show();
		IJ.save(intersection, dc.getDirectory() + "/consensus.tif");
		//intersection.show();
	}

	private void advance_plane(ImagePlus[] image_array, int plane_count) {

		for (int i = 0; i < plane_count; i++) {
			for (int image = 0; image < image_array.length; image++) {
				if (image_array[image] == null) IJ.log("image_array[" + image + "] == null");
				ImagePlus temp = reslice(image_array[image]);
				image_array[image] = temp;
				IJ.showStatus("Advancing plane...");
                                if (progressWindow != null) progressWindow.showStatus(1, "Advancing plane...");
				IJ.showProgress( (i * image_array.length) + image, plane_count * image_array.length);
                                if (progressWindow != null) progressWindow.showProgress(1, (i * image_array.length) + image, plane_count * image_array.length);
				if (image_array[image] == null) {
					IJ.log("image_array[" + image + "] == null");
				} else {
					IJ.log("image_array[" + image + "] != null");
				}
			}
		}
		//return image_array;
	}

	static public ImagePlus reslice(ImagePlus input_image) {
		
		/*
		 * Reslices the supplied image plus and returns it. After performing 3 times on the same ImagePlus, it will return to the original orientation.
		 */

		// Get dimensions of input_image
		int[] input_dimensions = input_image.getDimensions();

		// Define a stack to contain the resliced output: y becomes x; z becomes y; x becomes z
		ImageStack output_stack = ImageStack.create(input_dimensions[1], input_dimensions[3], input_dimensions[0], input_image.getBitDepth());

		// Show status
		IJ.showStatus("Reslicing image stack...");
                if (progressWindow != null) progressWindow.showStatus(1, "Reslicing image stack...");

		// Iterate through voxels of input_image
		for (int z = 0; z < input_dimensions[3]; z++) {

			// Show progress
			IJ.showProgress(z, input_dimensions[3]);
                        if (progressWindow != null) progressWindow.showProgress(1, z, input_dimensions[3]);

			for (int y = 0; y < input_dimensions[1]; y++) {

				for (int x = 0; x < input_dimensions[0]; x++) {

					// Copy voxel value from input_image to output stack
					output_stack.setVoxel(y, z, x, input_image.getStack().getVoxel(x, y, z) );
				}
			}
		}
		
		// Put output_stack into an ImagePlus
		ImagePlus output_image = new ImagePlus("Reslice of " + input_image.getTitle(), output_stack);

		// Adjust calibration of output_image
		output_image.getCalibration().pixelWidth  = input_image.getCalibration().pixelHeight;
		output_image.getCalibration().pixelHeight = input_image.getCalibration().pixelDepth;
		output_image.getCalibration().pixelDepth  = input_image.getCalibration().pixelWidth;

		return output_image;
	}

	static public ImagePlus getIntersection(ImagePlus[] input_images) {

		// Returns the intersection of every image in input_images by performing an AND operaton on successive pairs
		// To succeed, all input_images must be scaled to the same dimensions

		// Check that input_images contains more than one image
		if (input_images.length < 2) {

			return null;
		}

		IJ.log("Getting intersection of registered stacks:");

		ImageCalculator ic = new ImageCalculator();
		Resizer rs = new Resizer();

		// Get biggest dimensions
		int biggest_x = 0, biggest_y = 0, biggest_z = 0;
		for (int i = 0; i < input_images.length; i++) {

			int dimensions[] = input_images[i].getDimensions();
			if (dimensions[0] > biggest_x) biggest_x = dimensions[0];
			if (dimensions[1] > biggest_y) biggest_y = dimensions[1];
			if (dimensions[3] > biggest_z) biggest_z = dimensions[3];
		}
		IJ.log(" -Found biggest dimensions");

		// Iterate through input_images
		for (int i = 0; i < input_images.length; i++) {

			// Scale current input_image to the largest dimensions found
			StackProcessor sp = new StackProcessor(input_images[i].getImageStack());
			input_images[i].setStack(sp.resize(biggest_x, biggest_y, true));	// Resize in x & y
			input_images[i] = rs.zScale(input_images[i], biggest_z, ImageProcessor.BILINEAR);
			IJ.log(" -Resized image #" + (i+1) );

			// Iterate through slices of input_images[i]
			//ImageStack stack = input_images[i].getStack();
			/*for (int slice = 0; slice < biggest_y; slice ++) {

				// Muliply the current slice by 1/255, i.e. divide by 255
				//ImageProcessor ip = stack.getProcessor(slice + 1);
				input_images[i].setSlice(slice+1);
				ImageProcessor ip = input_images[i].getProcessor();
				ip.multiply(1.00/255.00);
				input_images[i].show();
			}*/

			// Multiply input_images[0] by input_images[i], i.e. intersecting voxels will be mulitplied by 1 and non-intersecting by 0
			//if (i > 0) {
				
			//	input_images[0] = ic.run("Multiply stack", input_images[0], input_images[i]);
			//	IJ.log("Intersected images #1 and #" + (i+1) );
			//}
		}

		if (input_images[0] == null) {
			IJ.log("input_images[0] == null");
		} else {
			IJ.log("input_images[0] != null");
		}

		for (int i = 1; i < input_images.length; i++) {

			ImagePlus temp_image = ic.run("AND stack create", input_images[0], input_images[i]);
			IJ.log(" -Intersected images #1 and #" + (i+1) );
			if (temp_image == null) {
				IJ.log("temp_image == null");
			} else {
				IJ.log("temp_image != null");
			}
			input_images[0] = temp_image;
			//temp_image.show();
		}

		return input_images[0];
	}
}
