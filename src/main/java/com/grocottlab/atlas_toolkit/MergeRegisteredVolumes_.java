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
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.plugin.Resizer;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;

/**
 * A class for combining image stacks following registration by ApplyLabelRegistration.
 * 
 * The run method does the following:
 * 
 *  - Scales each image stack to match the dimensions of the smallest stack.
 *  - Generates a new image stack containing the mean average of the image stacks.
 *  - Generates a new image stack containing the standard deviation of the image stacks.
 * 
 * This class is called by the Atlas Toolkit menu command "5. Merge Registered Volumes".
 * 
 */
public class MergeRegisteredVolumes_ implements PlugIn {
    
    @Override
	public void run(String arg) {

		// Get input filenames - allow selection of multiple files
		String[] input_filepath = new String[255];
		int input_count = 0;
		boolean cancelled = false;
		OpenDialog od;
		for (int i = 0; (i < input_filepath.length) && !cancelled; i++) {

			od = new OpenDialog("Select Volume #" + (i+1) + "...", "");
			if (od.getFileName() != null) {

				input_filepath[i] = od.getDirectory() + od.getFileName();
				input_count++;
			} else {
				cancelled = true; //i = input_filename.length;
			}
		}
		
		// Open the list of input files
		Opener op = new Opener();
		ImagePlus[] input_image = new ImagePlus[input_count];
		for (int i = 0; i < input_count; i++) {

			IJ.log("Opening Volume #" + (i+1) + ": " + input_filepath[i]);
			input_image[i] = op.openImage(input_filepath[i]);
		}
		
		// Find biggest input dimensions
		int[][] input_dimensions = new int[input_count][];
		int biggest_x = 0;
		int biggest_y = 0;
		int biggest_z = 0;
		for (int i = 0; i < input_count; i++) {

			input_dimensions[i] = input_image[i].getDimensions();
			IJ.log("Volume #" + (i+1) + " dimensions: " + input_dimensions[i][0] + " x " + input_dimensions[i][1] + " x " + input_dimensions[i][3]);
			if (i == 0) {
				biggest_x = input_dimensions[i][0];
				biggest_y = input_dimensions[i][1];
				biggest_z = input_dimensions[i][3];
			} else {

				if (input_dimensions[i][0] > biggest_x) biggest_x = input_dimensions[i][0];
				if (input_dimensions[i][1] > biggest_y) biggest_y = input_dimensions[i][1];
				if (input_dimensions[i][3] > biggest_z) biggest_z = input_dimensions[i][3];
			}
		}
		IJ.log("Biggest dimensions = " + biggest_x + " x " + biggest_y + " x " + biggest_z);

		// Scale inputs to smallest dimensions
		Resizer rs = new Resizer();
		for (int i = 0; i < input_count; i++) {

			if (input_dimensions[i][0] < biggest_x || input_dimensions[i][1] < biggest_y) {

				IJ.log("Resizing Volume #" + (i+1));
				StackProcessor sp = new StackProcessor( input_image[i].getStack() );
				input_image[i].setStack( sp.resize(biggest_x, biggest_y, true) );
			}

			if (input_dimensions[i][3] < biggest_z) {

				IJ.log("Z Scaling Volume #" + (i+1));
				input_image[i] = rs.zScale(input_image[i], biggest_z, ImageProcessor.BILINEAR);
			}
		}
/*
		// Determine min/max value for each input volume
		IJ.log("Normalising each input volume to its peak signal level");
		float[] max_value = new float[input_count];
		float[] min_value = new float[input_count];
		for (int i = 0; i < input_count; i++) {

			max_value[i] = 0.0f;
			min_value[i] = Float.MAX_VALUE;

			for (int z = 0; z < biggest_z; z++) {

				input_image[i].setSlice(z+1);

				for (int y = 0; y < biggest_y; y++) {
					for (int x = 0; x < biggest_x; x++) {

						float current_value = input_image[i].getProcessor().getPixelValue(x, y);
						if ( (current_value > 0.0f) && (current_value > max_value[i]) ) max_value[i] = current_value;
						if ( (current_value > 0.0f) && (current_value < min_value[i]) ) min_value[i] = current_value;
					}
				}
			}
			IJ.log("Image #" + i + "; min == " + min_value[i] + "; max == " + max_value[i]);
		}

		// Normalise each value to min value
		for (int i = 0; i < input_count; i++) {

			for (int z = 0; z < biggest_z; z++) {

				input_image[i].setSlice(z+1);
				input_image[i].getProcessor().multiply(1.0 / min_value[i]);
			}
		}
*/
		ImageStack mean_stack = new ImageStack(biggest_x, biggest_y);
		ImageStack sd_stack = new ImageStack(biggest_x, biggest_y);

		// Calculate statistics
		for (int z = 0; z < biggest_z; z++) {

			FloatProcessor mean_ip = new FloatProcessor(biggest_x, biggest_y);
			FloatProcessor sd_ip = new FloatProcessor(biggest_x, biggest_y);
			for (int i = 0; i < input_count; i++) {
				
				input_image[i].setSlice(z+1);
			}
			for (int y = 0; y < biggest_y; y++) {
				for (int x = 0; x < biggest_x; x++) {

					// Calculate mean
					float sum = 0.0f;
					for (int i = 0; i < input_count; i++) {

						sum = sum + input_image[i].getProcessor().getPixelValue(x, y);
					}
					double mean = sum / input_count;
					mean_ip.putPixelValue(x, y, mean);

					// Calculate standard deviation
					double temp = 0.0;
					for (int i = 0; i < input_count; i++) {

						temp += (mean - input_image[i].getProcessor().getPixelValue(x, y)) * (mean - input_image[i].getProcessor().getPixelValue(x, y));
					}
					double variance = temp / input_count;
					double standard_deviation = Math.sqrt(variance);
					sd_ip.putPixelValue(x, y, standard_deviation);
				}
			}
			mean_stack.addSlice(mean_ip);
			sd_stack.addSlice(sd_ip);
		}

		//for (int i = 0; i < input_count; i++) {

		//	input_image[i].show();
		//}

		ImagePlus mean_image = new ImagePlus("Mean Volume", mean_stack);
		mean_image.show();
		ImagePlus sd_image = new ImagePlus("Standard Deviation Volume", sd_stack);
		sd_image.show();
	}
}
