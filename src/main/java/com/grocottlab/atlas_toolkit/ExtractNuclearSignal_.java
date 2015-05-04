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

import fiji.threshold.Auto_Local_Threshold;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.macro.Interpreter;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.RankFilters;
import ij.process.StackConverter;

/**
 * A class for extracting a normalized nuclear fluorescence signal.
 * 
 * This class is called by the Atlas Toolkit menu command "1. Extract Nuclear Signal".
 * 
 */
public class ExtractNuclearSignal_ implements PlugIn {
    
    // Parameters
	boolean doBackgroundSubtraction = false;
	String[] threshold_filtering = {"Niblack > Min > Outliers",
					"Mean > Outliers > Min"};
	String regime = threshold_filtering[0];

	static final int BRIGHT_OUTLIERS = 0, DARK_OUTLIERS = 1;
	
	@Override
	public void run(String arg) {

		// Get names of the two input files
		OpenDialog od;
		String reference_path = "";
		String target_path = "";

		// Get reference_filename
		od = new OpenDialog("Select Reference channel...", "");
		if (od.getFileName() != "") {
			
			reference_path = od.getDirectory() + od.getFileName();
			//IJ.log("Reference channel: " + reference_path);
			
		} else {
			
			return;
		}

		// Get target_filename
		od = new OpenDialog("Select target channel...", "");
		if (od.getFileName() != "") {
			
			target_path = od.getDirectory() + od.getFileName();
			//IJ.log("Target channel: " + target_path);
			
		} else {
			
			return;
		}

		// Get parameters
		GenericDialog gd = new GenericDialog("Parameters");
		//gd.addCheckbox("Subtract Background?", boolean true);
		String[] options1 = {"No", "Yes"};
		gd.addRadioButtonGroup("Subtract Background?", options1, 1, 2, "No");
		gd.addRadioButtonGroup("Threshold/Filter Regime:", threshold_filtering, 2, 1, regime);
		gd.showDialog();
		if (gd.getNextRadioButton() == "No") doBackgroundSubtraction = false;
		regime = gd.getNextRadioButton();
		
		// Derive mask from reference channel
		Opener op = new Opener();
		ImagePlus mask = op.openImage(reference_path);
		StackConverter sc = new StackConverter(mask);
		sc.convertToGray8();
		Auto_Local_Threshold alt = new Auto_Local_Threshold();
		RankFilters rf = new RankFilters();
		Interpreter macro = new Interpreter();
		int stacksize = mask.getStackSize();
		for (int i = 1; i <= stacksize; i++) {

			IJ.showStatus("Thresholding: " + i + "/" + stacksize);
			IJ.showProgress(i, stacksize);

			mask.setSlice(i);

			if (regime == threshold_filtering[0]) {

				Object[] obj = alt.exec(mask, "Niblack", 15, 0, 0, true);
				rf.rank(mask.getProcessor(),  0.0, RankFilters.MIN);
				rf.rank(mask.getProcessor(),  2.0, RankFilters.OUTLIERS, BRIGHT_OUTLIERS, 50.0f);
				rf.rank(mask.getProcessor(),  2.0, RankFilters.OUTLIERS, BRIGHT_OUTLIERS,   50.0f);
				
			} else if (regime == threshold_filtering[1]) {
				
				//Object[] obj = alt.exec(mask, "MidGrey", 15, 0, 0, true);//"Niblack", 15, 0, 0, true);//"Mean", 15, 0, 0, true);
				Object[] obj = alt.exec(mask, "Mean", 15, 0, 0, true);
				//rf.rank(mask.getProcessor(),  0.0, RankFilters.MIN);
				rf.rank(mask.getProcessor(), 10.0, RankFilters.OUTLIERS, BRIGHT_OUTLIERS, 50.0f);
				rf.rank(mask.getProcessor(),  2.0, RankFilters.OUTLIERS, DARK_OUTLIERS, 50.0f);
				//rf.rank(mask.getProcessor(),  2.0, RankFilters.OUTLIERS, BRIGHT_OUTLIERS,   50.0f);
				rf.rank(mask.getProcessor(),  0.0, RankFilters.MIN);
			}
		}
		sc = new StackConverter(mask);
		sc.convertToGray16();
		for (int i = 0; i <= stacksize; i++) {

			mask.setSlice(i);
			mask.getProcessor().multiply(1.0/255);
		}

		// Open target and reference channels
		ImagePlus target = op.openImage(target_path);
		ImagePlus reference = op.openImage(reference_path);

		// Do Background Subtraction
		if (doBackgroundSubtraction) {
	
			BackgroundSubtracter bs = new BackgroundSubtracter();
			for (int i = 0; i <= stacksize; i++) {

				IJ.showStatus("Subtracting Background: " + i + "/" + stacksize);
				IJ.showProgress(i, stacksize);
			
				target.setSlice(i);
				reference.setSlice(i);
				bs.rollingBallBackground(target.getProcessor(), 50.0, false, false, false, false, true);
				bs.rollingBallBackground(reference.getProcessor(), 50.0, false, false, false, false, true);
			}
		}

		// Apply Mask
		IJ.showStatus("Calculating...");
		ImageCalculator ic = new ImageCalculator();
		ic.run("Multiply stack", target, mask);
		ic.run("Multiply stack", reference, mask);
		ImagePlus output = ic.run("Divide stack 32 create", target, reference);
		if (doBackgroundSubtraction) {
			
			output.setTitle( target.getTitle() + "_norm_BS");
		} else {

			output.setTitle( target.getTitle() + "_norm");
		}
		output.show();
		mask.setTitle( reference.getTitle() + "_mask");
		mask.show();
	}
}
