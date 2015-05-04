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
import ij.gui.ImageCanvas;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.util.ThreadUtil;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class for projecting the extracted nuclear signal onto the surface morphology of a tissue.
 * 
 * This class is called by the Atlas Toolkit menu command "2. Project to segment Label".
 * 
 */
public class ProjectToLabel_ implements PlugIn, ActionListener, AdjustmentListener {
    
    // GUI components:
	Button browseLabelButton;
	Button browseInputButton;

 	// Parameters
	ImagePlus labelFile = null;
	ImagePlus inputFile = null;
	String labelFilename = "";
	String labelDir = "";
	String inputFilename = "";
	String inputDir = "";
	int labelChannel = 1;
 	double blockSize = 12;
 	int sampleRadius = 3;
 	ImageCanvas labelPreview;
 	ImageCanvas inputPreview;
 	LUT[] lut;
 	Scrollbar sb;
 	GD gd;
 	Boolean savePreCrop = false;
        int normalisationMode = 0;

 	private class GD extends GenericDialog {

 		boolean enableOK = false;

		public GD(String name) {

			super(name);
		}

		public void show() {

			if (!enableOK) {
						
				getButtons()[0].setEnabled(false);
			}
			super.show();
		}

		public void enableOK(boolean enableOK) {

			this.enableOK = enableOK;

			if (getButtons()[0] != null) {

				getButtons()[0].setEnabled(false);
			}
		}

		public void textValueChanged(TextEvent e) {

		 	super.textValueChanged(e);

		 	// Get changes
		 	java.util.Vector<java.awt.TextField> tf = getStringFields();
		 	java.util.Vector<java.awt.TextField> nf = getNumericFields();
		 	java.util.Vector<java.awt.Scrollbar> sl = getSliders();

		 	if (tf.get(0).getText().length() == 0 ||
		 	    tf.get(1).getText().length() == 0) {

		 	    	gd.enableOK(false);
		 	}

		 	// Update selected channel
		 	int chan = sl.get(0).getValue();
		 	if (chan >= 0 && chan <= 255) {
					 		
		 		labelChannel = chan;
		 	}

		 	// Apply current LUT to 
		 	if (labelFile != null) {
					 		
		 		labelFile.getProcessor().setLut( lut[labelChannel] );
				labelFile.updateImage();
				labelPreview.update( labelPreview.getGraphics() );
		 	}
		}
	}
	
	public void adjustmentValueChanged(AdjustmentEvent e) {
		
		int val = sb.getValue();
		labelFile.setSlice(val);
		labelFile.updateImage();
		labelPreview.update( labelPreview.getGraphics() );
		inputFile.setSlice(val);
		inputFile.updateImage();
		inputPreview.update( inputPreview.getGraphics() );
	}

	public void actionPerformed (ActionEvent e) {

		if (e.getSource() == browseLabelButton) {

			// Browse for a Label file
			OpenDialog od = new OpenDialog("Select Label file...", "");
			labelFilename = od.getDirectory() + od.getFileName();
			Opener op = new Opener();
			labelFile = op.openImage(labelFilename);
			gd.dispose();
	 	
	 	} else if (e.getSource() == browseInputButton) {

			// Browse for Input file
			OpenDialog od = new OpenDialog("Select Input file...", "");
			inputFilename = od.getDirectory() + od.getFileName();
			Opener op = new Opener();
			inputFile = op.openImage(inputFilename);
			gd.dispose();
		 }
	 }

	 private void initializeLUTs () {
	 		
 		// Set up LUTs for label preview
 		lut = new LUT[256];
 		for (int i = 0; i < 256; i++) {

 			byte[] table = new byte[256];
 			for (int j = 0; j < 256; j++) {

 				table[j] = 0;//-128;//Byte.MIN_VALUE;
 			}
 			table[i] = (byte)255;//127;//Byte.MAX_VALUE;
			lut[i] = new LUT(8, 256, table, table, table);
	 	}
	 }

	 public void showDialog() {

	 	buildDialog();
	 	
	 	while(true) {
			
			if ( gd.wasCanceled() ) {

				return;
				
			} else if ( gd.wasOKed() ) {

				break;
			} else {
				
				buildDialog();
			}
		}

		blockSize = (int)gd.getNextNumber();
		sampleRadius = (int)gd.getNextNumber();
                String radio = (String)gd.getNextRadioButton();
                if (radio == "Background") {
                    normalisationMode = 0;
                } else if (radio == "Peak") {
                    normalisationMode = 1;
                } else if (radio == "None") {
                    normalisationMode = 3;
                }
	 }

	 private void buildDialog () {

	 	// Create panel containing a "Browse..." button to browse for a label file
		browseLabelButton = new Button();
		browseLabelButton.setLabel("Browse...");
		browseLabelButton.addActionListener(this);
		Panel browseLabelPanel = new Panel();
		browseLabelPanel.add(browseLabelButton);

		// Create panel containing a "Browse..." button to browse for an input file
		browseInputButton = new Button();
		browseInputButton.setLabel("Browse...");
		browseInputButton.addActionListener(this);
		Panel browseInputPanel = new Panel();
		browseInputPanel.add(browseInputButton);

		// Create dialog
		gd = new GD("Project to Anatomy - ATLAS Toolkit");
		gd.addPanel(browseLabelPanel);
		gd.addStringField("Label file name:", labelFilename, 2^31 - 1);
		gd.addPanel(browseInputPanel);
		gd.addStringField("Input file name:", inputFilename, 2^31 - 1);
		if (inputFile != null) {
			
			gd.addNumericField("Block Size (" + inputFile.getCalibration().getXUnit() + "):", blockSize, 2);
		} else {

			gd.addNumericField("Block Size (pixels):", blockSize, 2);
		}
		gd.addNumericField("Sample Radius (blocks):", sampleRadius, 0);
                String[] norm = {"Background", "Peak", "None"};
		gd.addRadioButtonGroup("Normalisation:", norm, 3, 1, "Background");
                gd.addSlider("Label# (0-255):", 0, 255, 1);
		
		java.util.Vector<java.awt.TextField> tf = gd.getStringFields();
		tf.get(0).setEditable(false);
		tf.get(1).setEditable(false);

	 	// Create a panel for showing the currently selected label channel
		if (labelFile != null && inputFile != null) {
			
			// Get dimensions
			int[] lfd = labelFile.getDimensions();
			int[] ifd = inputFile.getDimensions();

			// Abort if labelFile and inputFile have different dimensions
			if (lfd[0] != ifd[0] ||
			    lfd[1] != ifd[1] ||
			    lfd[3] != ifd[3]) {
	
				// Show error message on exit
				IJ.error("Error: Incompatible Dimensions", "The images \"Label file\" and \"Input file\" have different dimensions.");

				gd.enableOK(false);
					
			} else {

				// Apply current LUT to labelFile
				labelFile.getProcessor().setLut( lut[labelChannel] );

				// Set up layout
				GridBagLayout gridbag = new GridBagLayout();
				GridBagConstraints c = new GridBagConstraints();
				c.gridwidth = 1;
				c.gridheight = 1;
				c.anchor = GridBagConstraints.LINE_START;
				
				// Set up the grid panel
				Panel gridPanel = new Panel();
				gridPanel.setLayout(gridbag);

				// Set up the preview canvas
				Label label1 = new Label("Preview Label...");
				c.gridx = 0;
				c.gridy = 0;
				gridbag.setConstraints(label1, c);
				gridPanel.add(label1);
				
				Label label2 = new Label("Preview Input...");
				c.gridx = 1;
				c.gridy = 0;
				gridbag.setConstraints(label2, c);
				gridPanel.add(label2);
			
				labelPreview = new ImageCanvas(labelFile) {

					public Dimension getPreferredSize() {
						return new Dimension(150, 150);
					}
					public Dimension getMinimumSize() {
						return new Dimension(150, 150);
					}
					public Dimension getMaximumSize() {
						return new Dimension(150, 150);
					}
				};
				labelPreview.disablePopupMenu(true);
				int maxD;
				if (lfd[1] > lfd[0]) {
					maxD = lfd[1];
				} else {
					maxD = lfd[0];
				}
				labelPreview.setMagnification( (double) 150 / maxD);
				labelFile.setSlice( (int)(labelFile.getNSlices() / 2) );
				c.gridx = 0;
				c.gridy = 1;
				gridbag.setConstraints(labelPreview, c);
				gridPanel.add(labelPreview);

				inputPreview = new ImageCanvas(inputFile) {

					public Dimension getPreferredSize() {
						return new Dimension(150, 150);
					}
					public Dimension getMinimumSize() {
						return new Dimension(150, 150);
					}
					public Dimension getMaximumSize() {
						return new Dimension(150, 150);
					}
				};
				inputPreview.disablePopupMenu(true);
				if (lfd[1] > ifd[0]) {
					maxD = ifd[1];
				} else {
					maxD = ifd[0];
				}
				inputPreview.setMagnification( (double) 150 / maxD);
				inputFile.setSlice( (int)(inputFile.getNSlices() / 2) );
				c.gridx = 1;
				c.gridy = 1;
				gridbag.setConstraints(inputPreview, c);
				gridPanel.add(inputPreview);
			
				sb = new Scrollbar( Scrollbar.HORIZONTAL, (int)(labelFile.getNSlices() / 2), 1, 1, (labelFile.getNSlices() + 1) );
				sb.addAdjustmentListener(this);
				c.gridx = 0;
				c.gridy = 2;
				c.gridwidth = 2;
				c.fill = GridBagConstraints.HORIZONTAL;
				gridbag.setConstraints(sb, c);
				gridPanel.add(sb);
				gd.addPanel(gridPanel);

				gd.enableOK(true);
			}
		}

		// Show dialog
		gd.showDialog();
	 }
	
	@Override
	public void run(String arg) {

		initializeLUTs();
		showDialog();
		
		int[] lfd = labelFile.getDimensions();
		int[] ifd = inputFile.getDimensions();
		if (lfd[0] != ifd[0] ||
		    lfd[1] != ifd[1] ||
		    lfd[3] != ifd[3]) {
	
			// Close input stack and anatomical label
			labelFile.close();
			inputFile.close();

			// Show error message on exit
			IJ.error("Error: Incompatible Dimensions", "The images \"input stack\" and \"anatomical label\" have different dimensions.");
			return;
    		}

		// Use label file to generate anatomical mask
		for (int z = 0; z < lfd[3]; z++) {

			labelFile.setSlice(z + 1);
			inputFile.setSlice(z + 1);
			ImageProcessor labelP = labelFile.getProcessor();
			ImageProcessor inputP = inputFile.getProcessor();
			IJ.showStatus("Applying label channel...");
			IJ.showProgress(z, lfd[3]);
			for (int x = 0; x < lfd[0]; x++) {
				for (int y = 0; y < lfd[1]; y++) {

					if (labelP.getPixel(x, y) != labelChannel) {

						inputP.putPixel(x, y, 0);
					}
				}
			}
		}
		IJ.showStatus("Applying label channel...done!");

		// Define target and sample boxel dimensions
		final int sampling_factor = sampleRadius;
		final int block_width = (int)Math.ceil(inputFile.getCalibration().getRawX(blockSize));
		final int block_height = (int)Math.ceil(inputFile.getCalibration().getRawY(blockSize));
		final int block_depth = (int)Math.ceil((double)block_width * (inputFile.getCalibration().getX(1) / inputFile.getCalibration().getZ(1)) );
		final int sample_width = sampleRadius * block_width;
		final int sample_height = sampleRadius * block_height;
		final int sample_depth = sampleRadius * block_depth;

		// Calculate the number of target boxels in inputFile
		final int blocks_wide = (int)Math.floor(ifd[0] / block_width);
		final int blocks_high = (int)Math.floor(ifd[1] / block_height);
		final int blocks_deep = 1 + (int)Math.floor(ifd[3] / block_depth);	// boxels_depth is overestimated by 1 to avoid z-cropping due to rounding down
		final int depth_discrepency = (blocks_deep * block_depth) - ifd[3];	// the discrepency between boxels_deep and the actual z-depth - this difference is later added to the bottom of the input stack

		// Determine thread_count, max_boxel_depths_per_thread and min_boxel_depths_per_thread
		final int thread_count;
		final int max_block_depths_per_thread;
		final int max_cores = 4;//Runtime.getRuntime().availableProcessors();
		if (blocks_deep > max_cores) {

			thread_count = max_cores;
			max_block_depths_per_thread = 1 + blocks_deep / max_cores;
			
		} else {

			thread_count = blocks_deep;
			max_block_depths_per_thread = 1;
		}
		final int min_block_depths_per_thread = blocks_deep - (max_block_depths_per_thread * (thread_count - 1));

		// Calculate how much padding is required
		final int x_padding = (sample_width  - block_width);
		final int y_padding = (sample_height - block_height);
		final int z_padding = (sample_depth  - block_depth);

		// Create series of input & output pixel arrays to partition the computation - include edge padding
		IJ.showStatus("Grabbing memory...");
		final float[][][][] input = new float[thread_count][(max_block_depths_per_thread * block_depth) + z_padding][ifd[0] + x_padding][ifd[1] + y_padding];// + depth_discrepency][ifd[0] + x_padding][ifd[1] + y_padding];
		IJ.showStatus("Grabbing memory...still...");
		final float[][][][] output = new float[thread_count][ max_block_depths_per_thread ][ blocks_wide ][ blocks_high ];
		IJ.showStatus("Grabbing memory...still...done!!!");
		final Thread[] thread = new Thread[thread_count];

		IJ.log(" input x:" + ifd[0] + " y:" + ifd[1] + "z:" + ifd[3]);
		IJ.log("output x:" + (blocks_wide * block_width) + " y:" + (blocks_high * block_height) + "z:" + (blocks_deep * block_depth) );

		// Setup progress tracking
		final int blocks_total = blocks_wide * blocks_high * blocks_deep;
		final AtomicInteger blocks_done = new AtomicInteger(1);
		final long start_time = new Date().getTime();

		// Partition data into input
		ImageProcessor ip = inputFile.getProcessor();
		for (int i = 0; i < thread_count; i++) {

			// Calculate current_blocks_deep
			final int current_blocks_deep;
			if (i < (thread_count - 1)) {

				current_blocks_deep = max_block_depths_per_thread;
				
			} else {

				current_blocks_deep = min_block_depths_per_thread;
			}
			
			// Calculate start and stop slices for current thread
			final int z_start = 1 + (block_depth * (max_block_depths_per_thread * i));
			final int z_depth = (z_padding + (block_depth * current_blocks_deep));
			final int z_stop = (z_start + z_depth) - 1;
			
			// Load data for current thread
			for (int z = z_start; z < z_stop; z++) {

				IJ.showStatus("Partitioning data...");
				IJ.showProgress(z, z_stop);
				
				// Set the inputFile z slice
				if (z > (0.5 * z_padding) && z <= (0.5 * z_padding) + ifd[3]) {
					
					inputFile.setSlice( (int)(z - (0.5 * z_padding) + 1) );
					ip = inputFile.getProcessor();
				}

				for (int x = 0; x < input[0][0].length; x++) {
					for (int y = 0; y < input[0][0][0].length; y++) {

						// If current coordinate is within the edge padding...
						if (z <= (0.5 * z_padding) || z > (0.5 * z_padding) + ifd[3] ||
						    z <= (0.5 * z_padding) || z > (0.5 * z_padding) + ifd[3] ||
						    z <= (0.5 * z_padding) || z > (0.5 * z_padding) + ifd[3]) {

						    	// Set pixel value to zero
						    	input[i][z-z_start][x][y] = 0;
						} else {

							// Otherwise, copy data from inputFile
							input[i][z-z_start][x][y] = ip.getPixelValue(x, y);
						}
					}
				}
			}

			// Set up current thread
			final int current_thread = i;
			thread[i] = new Thread() {

				{ setPriority(Thread.NORM_PRIORITY); }

				public void run() {

					// Each thread processes a different substack
					// Iterate through target blocks
					for (int z = 0; z < current_blocks_deep; z++) {

						for (int x = 0; x < blocks_wide; x++) {

							for (int y = 0; y < blocks_high; y++) {
							
								// Find the origin of the current input sample
								int origin_x = x * block_width;
								int origin_y = y * block_height;
								int origin_z = z * block_depth;

								// Define variables for calculating average non-zero voxel value
								double pixel_accumulator = 0.0;
								int nonzero_pixel_counter = 0;

								// Iterate through pixels of current block's sample volume
								for (int sample_z = origin_z; (sample_z < (origin_z + sample_depth)) && (sample_z < input[current_thread].length); sample_z++) {
								
									for (int sample_x = origin_x; sample_x < (origin_x + sample_width); sample_x++) {

										for (int sample_y = origin_y; sample_y < (origin_y + sample_height); sample_y++) {

											// Get the current_pixel_value
											double current_pixel_value = (double)input[current_thread][sample_z][sample_x][sample_y];

											// If pixel value is not zero
											if (current_pixel_value != 0.0) {

												// Add pixel value to pixel_accumulator
												pixel_accumulator = pixel_accumulator + current_pixel_value;

												// Increment nonzero_voxel_counter
												nonzero_pixel_counter++;
											}
										}
									}
								}

								// Calculate average_pixel_value for sample block
								double average_pixel_value;
								
								if (nonzero_pixel_counter != 0) {
									
									average_pixel_value = pixel_accumulator / (double)nonzero_pixel_counter;

								} else {

									average_pixel_value = 0.0;
								}

								// Write average_pixel_value to output...
								output[current_thread][z][x][y] = (float)average_pixel_value;
							}

							// Report progress
							long time_elapsed = new Date().getTime() - start_time;
							double time_per_block = (double)time_elapsed / (double)blocks_done.addAndGet( blocks_high ); // * current_blocks_deep );
							int blocks_remaining = blocks_total - blocks_done.get();
							long minutes_elapsed = (long) ( time_elapsed / 1000 ) / 60;
							long minutes_remaining = (long) ( ( (time_per_block * blocks_remaining) / 1000 ) / 60 ) ;
							IJ.showStatus("Processing block " + blocks_done + " of " + blocks_total + "; <" + (minutes_remaining+1) + " min remaining.");
							IJ.showProgress(blocks_done.get(), blocks_total);
						}
					}

					IJ.log("Thread " + (current_thread+1) + " complete.");
				}
			};
		}

		IJ.showStatus("Partitioning data...");

		// Start threads and wait until finished
		ThreadUtil.startAndJoin(thread);
		int first = 0;
		int last = 1;
		IJ.log("Threads joined: merge results...");
                
                // Keep track of max/min values as we go along
                float maxValue = 0.0f;
                float minValue = Float.MAX_VALUE;

		// Write output back to inputFile...
		// Iterate through partitions
		for (int i = 0; i < thread_count; i++) {

			// Iterate through target blocks
			for (int z = 0; z < max_block_depths_per_thread; z++) {

				IJ.showStatus("Writing output...");
				IJ.showProgress(z, max_block_depths_per_thread);
			
				for (int x = 0; x < blocks_wide; x++) {

					for (int y = 0; y < blocks_high; y++) {
                                            
                                            // Record max/min values for normalisation
                                            if (output[i][z][x][y] > maxValue) maxValue = output[i][z][x][y];
                                            if ( (output[i][z][x][y] < minValue) && (output[i][z][x][y] > 0.0f) ) {
                                                minValue = output[i][z][x][y];
                                                //IJ.log("minValue == " + minValue + "; x == " + (x*block_width) + "; y == " + (y*block_height) + ";z == " + ((z + (i * max_block_depths_per_thread) ) * block_depth) );
                                            }

						// Find the origin of the current input sample
						int origin_x = x * block_width;
						int origin_y = y * block_height;
						int origin_z = (z + (i * max_block_depths_per_thread) ) * block_depth;
								
						// Iterate through slices of inputFile
						for (int input_z = origin_z; (input_z < (origin_z + block_depth)) && (input_z < inputFile.getStack().getSize()); input_z++) {

							// Get ImageProcessor of current stack slice
							inputFile.setSlice(input_z);
							ip = inputFile.getProcessor();

							// Fill target rectangle of current output slice with average_voxel_value
                                                        ip.setValue( output[i][z][x][y] );
							ip.setRoi(origin_x, origin_y, block_width, block_height);
							ip.fill();
						}
					}
				}
			}
		}
		IJ.showStatus("Writing output...done!");
                
                // Perform normalisation if required...
                if ( (normalisationMode == 0) || (normalisationMode == 1) ) {
                    
                    IJ.showStatus("Normalising output...");
                    // Find min and max values...
                    /*float maxValue = 0.0f;
                    float minValue = Float.MAX_VALUE;
                    for (int z = 0; z < inputFile.getDimensions()[3]; z++) {
                        inputFile.setSlice(z+1);
                        IJ.showProgress(z, inputFile.getDimensions()[3]);
                        for (int x = 0; x < inputFile.getDimensions()[0]; x++) {
                            for (int y = 0; y < inputFile.getDimensions()[1]; y++) {
                                
                                float currentValue = inputFile.getProcessor().getPixelValue(x, y);
                                if ( (currentValue > 0.0f) && (currentValue > maxValue) ) maxValue = currentValue;
                                if ( (currentValue > 0.0f) && (currentValue < minValue) ) minValue = currentValue;
                            }
                        }
                    }*/
                    // Normalise...
                    for (int z = 0; z < inputFile.getDimensions()[3]; z++) {
                        inputFile.setSlice(z+1);
                        IJ.showProgress(z, inputFile.getDimensions()[3]);
                        if (normalisationMode == 0) {
                            // Normalise to background...
                            inputFile.getProcessor().multiply(1.0 / minValue);
                        } else if (normalisationMode == 1) {
                            // Normalise to peak...
                            inputFile.getProcessor().multiply(1.0 / maxValue);
                        }
                    }
                    IJ.showStatus("Normalising output...done!");
                }

		// Show pre-cropped output if required
		if (savePreCrop) {

			ImagePlus preCropIP = inputFile.duplicate();
			preCropIP.setSlice(1);
			preCropIP.show();
		}

		// Use label file to generate anatomical mask
		for (int z = 0; z < lfd[3]; z++) {

			labelFile.setSlice(z + 1);
			inputFile.setSlice(z + 1);
			ImageProcessor labelP = labelFile.getProcessor();
			ImageProcessor inputP = inputFile.getProcessor();
			IJ.showStatus("Applying label channel...");
			IJ.showProgress(z, lfd[3]);
			for (int x = 0; x < lfd[0]; x++) {
				for (int y = 0; y < lfd[1]; y++) {

					if (labelP.getPixel(x, y) != labelChannel) {

						inputP.putPixel(x, y, 0);
					}
				}
			}
		}
		IJ.showStatus("Applying label channel...done!");

		
		inputFile.setSlice(1);
		inputFile.show();
/*
		// Make output_substack a duplicate of first output_substack
		ImagePlus output_stack = output_substack[0].duplicate();
		output_substack[0].close();
		output_stack.setTitle("output_stack");

		// Iterate through remaining output_substacks and concatenate to output_stack
		for (int substacki = 1; substacki < output_substack.length; substacki++) {

			// Iterate through slices of current output_substack
			for (int slicei = 1; slicei <= output_substack[substacki].getStack().getSize(); slicei++) {

				// Add the current slice substack to end of output_stack
				output_stack.getStack().addSlice( output_substack[substacki].getStack().getProcessor(slicei) );
				output_substack[substacki].close();
			}
		}

		// Crop output_stack to match depth of anatomical_label, thereby removing depth_discrepency added earlier
		output_stack = sm.makeSubstack(output_stack, 1 + "-" + label_dimensions[3]);

		// Save "output_stack"
		IJ.save(output_stack, output_folder + "output_stack.tif");

		// Apply "anatomical_label" to "output_stack" to isolate anatomy of interest
		output_stack = ic.run("Multiply 32-bit stack", output_stack, anatomical_label);
		output_stack.setTitle("output_stack_cropped");
		anatomical_label.close();
		//anatomical_label.show();
		//output_stack.show();

		// Save output_stack_cropped
		IJ.save(output_stack, output_folder + "output_stack_cropped.tif");
		output_stack.show();
*/
	}
}
