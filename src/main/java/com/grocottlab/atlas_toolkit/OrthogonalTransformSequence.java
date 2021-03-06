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

import java.io.*;

/**
 * A class for storing a sequence of orthogonal 2D transformation coefficients generated by bUnwarpJ.
 * 
 * To transform a 3D (XYZ) image stack, a set of 2D transformation coefficients is applied in each
 * of three orthogonal planes (e.g. XY, YZ, ZX). Transformation can be initiated in any plane, 
 * and three orthogonal transformations are applied per iteration.
 * 
 * The class also provides methods for saving and re-opening the orthogonal transform sequence.
 * 
 */
public class OrthogonalTransformSequence implements java.io.Serializable {
    
    /**
     * Defines the three 2D orthogonal planes (XY, YZ, ZX) of a 3D (XYZ) image stack.
     */
    public enum OrthogonalPlane {YZ, ZX, XY}
	private OrthogonalTransform[][] ot;
	private int[] intervals;
	private int[] dimensions = new int[3]; // 0 = x, 1 = y, 2 = z. Allows for scaling of transforms in case labels were down-sampled.
	private int iteration_count = 1;

	/**
         * Takes a list of OrthogonalTransform objects - one for each orthogonal plane (= three), with three orthogonal planes per iteration.
         * 
         * @param ot A 2D array of OrthogonalTransform objects, each containing a set of 2D transformation coefficients generated by bUnwarpJ. <br> The first index refers to iteration and the second to orthogonal plane.
         * @param dimensions 
         */
        public OrthogonalTransformSequence (OrthogonalTransform[][] ot, int[] dimensions) {

		// Constructor takes a list of OrthogonalTransform objects - one for each orthogonal plane = 3, three orthogonal planes per iteration
		iteration_count = ot.length;
		this.ot = new OrthogonalTransform[iteration_count][3];
		this.dimensions = dimensions;
		for (int iteration = 0; iteration < iteration_count; iteration++) {
			for (OrthogonalTransformSequence.OrthogonalPlane orthoplane : OrthogonalTransformSequence.OrthogonalPlane.values()) {

				switch (orthoplane) {
					case XY: setOrthogonalTransform(ot[iteration][0], orthoplane, iteration);
						 break;
					case YZ: setOrthogonalTransform(ot[iteration][1], orthoplane, iteration);
						 break;
					case ZX: setOrthogonalTransform(ot[iteration][2], orthoplane, iteration);
						 break;
				}
			}
		}
	}

	/**
         * Generates an OrthogonalTransformSequence object from a previously saved ".ots" file.
         * 
         * @param path The absolute file path.
         * @return The re-opened OrthogonalTransformSequence object.
         */
        static public OrthogonalTransformSequence openOTS (String path) {

            OrthogonalTransformSequence ots = null;
            try {
                FileInputStream fileIn = new FileInputStream(path);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                ots = (OrthogonalTransformSequence) in.readObject();
                in.close();
                fileIn.close();
            
            } catch(IOException i) {
                
                i.printStackTrace();
            
            } catch(ClassNotFoundException c) {
                
                System.out.println("!!!!!Class not found!!!!!");
                c.printStackTrace();
            }
                return ots;
	}

	/**
         * Sets the transformation coefficients (as an OrthogonalTransform object) for the given orthogonal plane and iteration.
         * 
         * @param ot OrthogonalTransform object containing transformation coefficients for a single plane.
         * @param orthoplane Specifies the orthogonal plane for these coefficients as defined by the Enum OrthogonalTransformSequence.OrthogonalPlane.
         * @param iteration Specifies the iteration number.
         */
        public void setOrthogonalTransform (OrthogonalTransform ot, OrthogonalPlane orthoplane, int iteration) {

		switch (orthoplane) {
			case XY: this.ot[iteration][0] = ot;
				 break;
			case YZ: this.ot[iteration][1] = ot;
				 break;
			case ZX: this.ot[iteration][2] = ot;
				 break;
		}
	}

	/**
         * Gets the transformation coefficients (as an OrthogonalTransform object) for the given orthogonal plane and iteration.
         * @param orthoplane Specifies the orthogonal plane for these coefficients as defined by the Enum OrthogonalTransformSequence.OrthogonalPlane.
         * @param iteration Specifies the iteration number.
         * @return An OrthogonalTransform object containing the transformation coefficients.
         */
        public OrthogonalTransform getOrthogonalTransform (OrthogonalPlane orthoplane, int iteration) {

		OrthogonalTransform ot;
		switch (orthoplane) {
			case XY: ot = this.ot[iteration][0];
				 break;
			case YZ: ot = this.ot[iteration][1];
				 break;
			case ZX: ot = this.ot[iteration][2];
				 break;
			default: ot = this.ot[0][0];
				 //break;
		}
		return ot;
	}

	/**
         * Gets the X coefficients of the 2D (XY) transform for the specified orthogonal plane and iteration.
         * 
         * @param orthoplane Specifies the orthogonal plane for these coefficients as defined by the Enum OrthogonalTransformSequence.OrthogonalPlane.
         * @param iteration Specifies the iteration number.
         * @return A double[][] containing the X coefficients.
         */
        public double[][] getCoefficientsX (OrthogonalPlane orthoplane, int iteration) {

		double[][] x;
		switch (orthoplane) {
			case XY: x = ot[iteration][0].getCoefficientsX();
				 break;
			case YZ: x = ot[iteration][1].getCoefficientsX();
				 break;
			case ZX: x = ot[iteration][2].getCoefficientsX();
				 break;
			default: x = ot[0][0].getCoefficientsX();
		}
		return x;
	}

	/**
         * Gets the Y coefficients of the 2D (XY) transform for the specified orthogonal plane and iteration.
         * @param orthoplane Specifies the orthogonal plane for these coefficients as defined by the Enum OrthogonalTransformSequence.OrthogonalPlane.
         * @param iteration Specifies the iteration number.
         * @return A double[][] containing the Y coefficients.
         */
        public double[][] getCoefficientsY (OrthogonalPlane orthoplane, int iteration) {

		double[][] y;
		switch (orthoplane) {
			case XY: y = ot[iteration][0].getCoefficientsY();
				 break;
			case YZ: y = ot[iteration][1].getCoefficientsY();
				 break;
			case ZX: y = ot[iteration][2].getCoefficientsY();
				 break;
			default: y = ot[0][0].getCoefficientsY();
		}
		return y;
	}

	public int getIntervals (OrthogonalPlane orthoplane, int iteration) {

		int i;
		switch (orthoplane) {
			case XY: i = ot[iteration][0].getIntervals();
				 break;
			case YZ: i = ot[iteration][1].getIntervals();
				 break;
			case ZX: i = ot[iteration][2].getIntervals();
				 break;
			default: i = ot[0][0].getIntervals();
		}
		return i;
	}

	/**
         * Gets the number of iterations in this OrthogonalTransformSequence.
         * @return The number of iterations.
         */
        public int getIterations () {

            return iteration_count;
	}

	public int[] getDimensions () {

		return dimensions;
	}

	/**
         * Saves this OrthogonalTransformSequence object to an ".ots" file.
         * 
         * The ".ots" file can be re-opened using the static method openOTS().
         * 
         * @param path The absolute file path.
         */
        public void saveToFile(String path) {
		
		try {
			
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
			
		} catch(IOException i) {

			i.printStackTrace();
		}
	}
}
