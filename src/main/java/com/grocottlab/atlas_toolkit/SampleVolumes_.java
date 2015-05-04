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
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

/**
 * This class is used to format registered datasets ready for hierarchical clustering by the software Cluster 3.0.
 * 
 * It prompts the user to open a series of registered image stacks, samples the data, and saves it as a "results.txt" 
 * file.
 * 
 * This class is called by the Atlas Toolkit menu command "6. Sample Volumes for Clustering".
 * 
 */
public class SampleVolumes_ implements PlugIn {
    // Extracts data from an image volume.
    // Divides volume into sample bins.
    // Calculates mean non-zero signal within each bin.
    // Writes binned values and bin coordinates to a results table.
    
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
        
        // Global Variables
        double bin_size_x = 18.0, bin_size_y = 18.0, bin_size_z = 18.0;
        int bin_count_x, bin_count_y, bin_count_z;
        String size_string = new String();
        
        // Create a results table
        ResultsTable rt = new ResultsTable();
        rt.showRowNumbers(false);
        
        // Iterate through input files
        Opener op = new Opener();
        ImagePlus[] input_stack = new ImagePlus[input_count];
        int[] input_dimensions;
        for (int input = 0; input < input_count; input++) {
            
            // Open "input_stack" and get dimensions
            input_stack[input] = op.openImage(input_filepath[input]);
            if (input_stack[0] == null) return;
            if (input_stack[input] == null)  break;
            input_dimensions = input_stack[0].getDimensions(); // Returns the dimensions of this image (width, height, nChannels, nSlices, nFrames) as a 5 element int array
            if (input == 0) {
                
                // Get bin size using input_stack[0] dimensions
                Calibration cal = input_stack[0].getCalibration();
                String units = cal.getUnits();
                GenericDialog gd = new GenericDialog("Bin Size");
                gd.addNumericField("x (" + units + "):", bin_size_x, 2);
                gd.addNumericField("y (" + units + "):", bin_size_y, 2);
                gd.addNumericField("z (" + units + "):", bin_size_z, 2);
                gd.showDialog();
                if (gd.wasCanceled()) return;
                bin_size_x = (int)gd.getNextNumber();
                bin_size_y = (int)gd.getNextNumber();
                bin_size_z = (int)gd.getNextNumber();
                //IJ.log("bin_size_x = " + bin_size_x + "; bin_size_y = " + bin_size_y + "; bin_size_z = " + bin_size_z);
                
                // If necessary, convert bin size to pixels
                if (units != "pixels") {
                    
                    double x_cal = cal.getX(1.0);
                    bin_size_x = bin_size_x / x_cal;
                    double y_cal = cal.getY(1.0);
                    bin_size_y = bin_size_y / y_cal;
                    double z_cal = cal.getZ(1.0);
                    bin_size_z = bin_size_z / z_cal;
                }
                //IJ.log("bin_size_x = " + bin_size_x + "; bin_size_y = " + bin_size_y + "; bin_size_z = " + bin_size_z);
                
                // Caluclate the number of bins in x, y, z
                bin_count_x = input_dimensions[0] / (int)bin_size_x;
                bin_count_y = input_dimensions[1] / (int)bin_size_y;
                bin_count_z = input_dimensions[3] / (int)bin_size_z;
                
                //IJ.log("bin_count_x = " + bin_count_x + "; bin_count_y = " + bin_count_y + "; bin_count_z = " + bin_count_z);
                
                // Format size string
                int bin_count[] = new int[3];
                bin_count[0] = 1 + input_dimensions[0] / (int)bin_size_x;
                bin_count[1] = 1 + input_dimensions[1] / (int)bin_size_y;
                bin_count[2] = 1 + input_dimensions[3] / (int)bin_size_z;
                for (int i = 0; i < 3; i++) {
                    
                    if (bin_count[i] < 10) {
                        size_string = size_string + "000000" + bin_count[i];
                    } else if (bin_count[i] < 100) {
                        size_string = size_string + "00000" + bin_count[i];
                    } else if (bin_count[i] < 1000) {
                        size_string = size_string + "0000" + bin_count[i];
                    } else if (bin_count[i] < 10000) {
                        size_string = size_string + "000" + bin_count[i];
                    } else if (bin_count[i] < 100000) {
                        size_string = size_string + "00" + bin_count[i];
                    } else if (bin_count[i] < 1000000) {
                        size_string = size_string + "0" + bin_count[i];
                    }
                    if (i < 2) size_string = size_string + "x";
                }
            }
            
            // So far so good. Add a row to the results table
            rt.incrementCounter();
            rt.addValue(size_string, input_stack[input].getTitle());
            
            int current_bin[] = new int[3];
            for (int i = 0; i < 3; i++) current_bin[i] = -1;
            
            // Iterate through bins
            for (int origin_z = 0; origin_z < input_dimensions[3]; origin_z += bin_size_z) {
                
                current_bin[0] = -1;
                current_bin[1] = -1;
                current_bin[2]++;
                
                for (int origin_y = 0; origin_y < input_dimensions[1]; origin_y += bin_size_y) {
                    
                    current_bin[0] = -1;
                    current_bin[1]++;
                    
                    for (int origin_x = 0; origin_x < input_dimensions[0]; origin_x += bin_size_x) {
                        
                        current_bin[0]++;
                        
                        // Update progress
                        IJ.showStatus("Sampling Volume #" + (input+1) + "...");
                        IJ.showProgress( (origin_z * input_dimensions[1] * input_dimensions[0]) + (origin_y * input_dimensions[0]) + origin_x, input_dimensions[0] * input_dimensions[1] * input_dimensions[3] );
                        
                        // Confirm present bin size
                        int current_bin_size_x = (int)bin_size_x;
                        if ( (input_dimensions[0] - origin_x) < bin_size_x) current_bin_size_x = (input_dimensions[0] - origin_x);
                        int current_bin_size_y = (int)bin_size_y;
                        if ( (input_dimensions[1] - origin_y) < bin_size_y) current_bin_size_y = (input_dimensions[1] - origin_y);
                        int current_bin_size_z = (int)bin_size_z;
                        if ( (input_dimensions[3] - origin_z) < bin_size_z) current_bin_size_z = (input_dimensions[3] - origin_z);
                        
                        // Calculate x, y, z coordinates of present bin
                        double x_coord = origin_x / bin_size_x;
                        double y_coord = origin_y / bin_size_y;
                        double z_coord = origin_z / bin_size_z;
                        
                        double pixel_value_accumulator = 0.0;
                        int pixels_counted = 0;
                        
                        // Iterate through pixels of present bin
                        for (int z = origin_z; z < origin_z + current_bin_size_z; z++) {
                            
                            input_stack[input].setSlice(z + 1);
                            for (int y = origin_y; y < origin_y + current_bin_size_y; y++) {
                                
                                for (int x = origin_x; x < origin_x + current_bin_size_x; x++) {
                                    
                                    // Calculating mean positive non-zero pixel value...
                                    double pixel_value = (double)input_stack[input].getProcessor().getPixelValue(x, y);
                                    if (pixel_value > 0.0) {
                                        
                                        pixel_value_accumulator += pixel_value;
                                        pixels_counted++;
                                    }
                                }
                            }
                        }
                        
                        // Calculate mean positive non-zero pixel value
                        if (pixels_counted > 0) {
                            
                            double mean_value = pixel_value_accumulator / pixels_counted;
                            
                            // Only record non-zero values
                            // Format bin coordinates
                            String coord_string = new String();
                            for (int i = 0; i < 3; i++) {
                                
                                if (current_bin[i] < 10) {
                                    coord_string = coord_string + "000000" + current_bin[i];
                                } else if (current_bin[i] < 100) {
                                    coord_string = coord_string + "00000" + current_bin[i];
                                } else if (current_bin[i] < 1000) {
                                    coord_string = coord_string + "0000" + current_bin[i];
                                } else if (current_bin[i] < 10000) {
                                    coord_string = coord_string + "000" + current_bin[i];
                                } else if (current_bin[i] < 100000) {
                                    coord_string = coord_string + "00" + current_bin[i];
                                } else if (current_bin[i] < 1000000) {
                                    coord_string = coord_string + "0" + current_bin[i];
                                }
                                if (i < 2) coord_string = coord_string + "x";
                            }
                            
                            // Write result to table
                            rt.addValue(coord_string, mean_value);
                        }
                    }
                }
            }
        }
        IJ.showStatus("Done!");
        IJ.showProgress(0, 0);
        rt.show("Results");
    }
}
