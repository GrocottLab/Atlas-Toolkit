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

import java.awt.*;
import javax.swing.*;

/**
 * Provides the user with progress information about the registration process.
 * 
 * Called by the LabelRegistration3D and StackRegister classes.
 * 
 */
public class ProgressWindow extends JFrame {
    
    int stackCount;
    int iterationCount;
    JProgressBar iterationProgress;
    JProgressBar planeProgress;
    JLabel[][] regProgress;
    JProgressBar[] progressBar;
    JPanel progressBarPanel;
    
    public ProgressWindow(int stackCount, int iterationCount) {
        
        this.stackCount = stackCount;
        this.iterationCount = iterationCount;
        buildWindow();
    }
    
    private void buildWindow() {
        
        this.setTitle("Label Registration 3D - ATLAS Toolkit");
        this.setSize(640, 480);
        this.setResizable(false);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        BorderLayout border = new BorderLayout();//20, 20);
        this.setLayout(border);//new BoxLayout(this, BoxLayout.PAGE_AXIS));//grid1);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        //this.setUndecorated(true);
        //iterationProgress = new JProgressBar(0, iterationCount);
        //planeProgress = new JProgressBar(0, 3);
        
        
        progressBar = new JProgressBar[2];
        for (int barIndex = 0; barIndex < 2; barIndex++ ) {
            progressBar[barIndex] = new JProgressBar(0, 100);
        }
        int padding = 20;
        GridLayout grid1 = new GridLayout(2, 1, padding, padding);
        progressBarPanel = new JPanel(grid1);
        progressBarPanel.setBorder( BorderFactory.createEmptyBorder(padding, padding, 0, padding) );
        progressBarPanel.add(progressBar[0]);
        progressBarPanel.add(progressBar[1]);
        
        // Define header row and column
        JLabel[] headerRow = new JLabel[stackCount+1];
        JLabel[] headerCol = new JLabel[stackCount];
        headerRow[0] = new JLabel("vs", JLabel.CENTER);
        for (int i = 0; i < stackCount; i++) {
            
            headerRow[i+1] = new JLabel(Integer.toString(i+1), JLabel.CENTER);
            //headerRow[i+1].setBorder(BorderFactory.createLineBorder(new Color(128, 128, 128) ) );
            headerCol[i] = new JLabel(Integer.toString(i+1), JLabel.CENTER);
            //headerCol[i].setBorder(BorderFactory.createLineBorder(new Color(128, 128, 128) ) );
        }
        // Define remaining table contents
        regProgress = new JLabel[stackCount][stackCount];
        for (int i = 0; i < stackCount; i++) {
            
            for (int j = 0; j < stackCount; j++) {
                
                regProgress[i][j] = new JLabel();
                regProgress[i][j].setOpaque(true);
                regProgress[i][j].setBackground(new Color(128, 128, 128) );//progressBarPanel.getBackground() );//setBackground(Color.getHSBColor( (2.0f / 360.0f), (60.0f / 100.0f), (75.0f / 100.0f) ));
                //regProgress[i][j].setBorder(BorderFactory.createLineBorder(new Color(128, 128, 128) ) );//Color.darkGray) );
            }
        }
        // Add header row, column and table contents to a panel
        GridLayout grid2 = new GridLayout(stackCount+1, stackCount+1, 2, 2);
        JPanel panel = new JPanel(grid2);
        panel.setBorder( BorderFactory.createEmptyBorder(padding, padding, padding, padding) );
        for (int i = 0; i < stackCount + 1; i++) {
            
            for (int j = 0; j < stackCount + 1; j++) {
                
                if (j == 0) {
                    
                    // If this is the first row, populate with headerRow
                    panel.add(headerRow[i]);
                    
                } else if (i == 0) {
                    
                    // If this is the first colum, populate with headerCol
                    panel.add(headerCol[j-1]); // j-1 because the zeroth j is the header row
                    
                } else {
                    
                    // Otherwise, populate grid with regProgress
                    panel.add(regProgress[i-1][j-1]);
                }
            }
        }
        
        this.add(progressBarPanel, BorderLayout.PAGE_START);//progressBar[0], BorderLayout.PAGE_START);//iterationProgress);
        //this.add(progressBar[1], BorderLayout.PAGE_START);//planeProgress);
        progressBar[0].setVisible(false);
        progressBar[1].setVisible(false);
        this.add(panel, BorderLayout.CENTER);
        this.setVisible(true);
    }
    
    public void showProgress(int barIndex, int progress, int max) {
        
        progressBar[barIndex].setMaximum(max);
        progressBar[barIndex].setValue(progress);
        progressBar[barIndex].setVisible(true);
    }
    
    public void hideProgress(int barIndex) {
        
        progressBar[barIndex].setVisible(false);
    }
    
    public void showStatus(int barIndex, String status) {
        
        progressBar[barIndex].setStringPainted(true);
        progressBar[barIndex].setString(status);
    }
    
    public void setProgress(int iStack, int jStack) {
        
        // Set every label to red
        for (int i = 0; i < stackCount; i++) {
            
            for (int j = 0; j < stackCount; j++) {
                
                //regProgress[i][j].setOpaque(true);
                regProgress[i][j].setBackground(Color.getHSBColor( (2.0f / 360.0f), (60.0f / 100.0f), (75.0f / 100.0f) ));
            }
        }
        
        // Set every label on the current row (== iStack), up to the current column (<= jStack) to green
        for (int j = 0; j <= jStack; j++) {
            
            regProgress[iStack][j].setBackground(Color.getHSBColor( (80.0f / 360.0f), (52.0f / 100.0f), (73.0f / 100.0f) ));
        }
        
        // Set every label on every previous row (< iStack) to green
        for (int i = 0; i < iStack; i++) {
            
            for (int j = 0; j < stackCount; j++) {
                
                regProgress[i][j].setBackground(Color.getHSBColor( (80.0f / 360.0f), (52.0f / 100.0f), (73.0f / 100.0f) ));
            }
        }
    }
    
    public void clearProgress() {
        
        // Set every label to grey
        for (int i = 0; i < stackCount; i++) {
            
            for (int j = 0; j < stackCount; j++) {
                
                //regProgress[i][j].setOpaque(false);//Background(Color.getHSBColor( (2.0f / 360.0f), (60.0f / 100.0f), (75.0f / 100.0f) ));
                regProgress[i][j].setBackground(new Color(128, 128, 128) );//progressBarPanel.getBackground() );//Color.getHSBColor( (2.0f / 360.0f), (60.0f / 100.0f), (75.0f / 100.0f) ));
            }
        }
    }
}
