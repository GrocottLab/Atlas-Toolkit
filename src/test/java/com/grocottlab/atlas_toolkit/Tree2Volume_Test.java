package com.grocottlab.atlas_toolkit;

import com.grocottlab.atlas_toolkit.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;
import ij.process.ByteProcessor;
import ij3d.Content;
import ij3d.Image3DUniverse;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Random;
import java.util.Stack;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreeModel;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RowModel;

public class Tree2Volume_Test implements PlugIn {

	String[][] arry_mapping;
	int[] bins = new int[3];
	ClusterTreeModel clusterTree;
	String cluster_filepath;
	String tree_filepath;
	// Create a universe and show it
	Image3DUniverse univ;
	boolean animating = false;

	public void run(String arg) {

		// Get cdt file name & path
		OpenDialog od = new OpenDialog("Select a Clustered Data Table file (*.cdt)...", "");
		if (od.getFileName() == null) return;
		cluster_filepath = od.getDirectory() + od.getFileName();

		// Get tree file name & path
		od = new OpenDialog("Select a tree file (*.atr)...", "");
		if (od.getFileName() == null) return;
		tree_filepath = od.getDirectory() + od.getFileName();

		// Parse bin size
		parseBinSize(cluster_filepath);

		// Construct the ClusterTreeModel from the given .atr file
		clusterTree = new ClusterTreeModel(tree_filepath);

		// Start-up the user interface
		Tree2VolumeUI UI = new Tree2VolumeUI();
	}

	// This class constructs the user interface
	private class Tree2VolumeUI extends PlugInFrame {

		public Tree2VolumeUI() {

			// Setup frame
			super("Tree2Volume - ATLAS Toolkit");
			setSize(1024, 768);

			// Construct the Outline control for navigating clusterTree
			NodeRowModel nodeRowModel = new NodeRowModel();
			OutlineModel model = DefaultOutlineModel.createOutlineModel(clusterTree, nodeRowModel, true, " ");
			Outline outline = new Outline(); // Control for displaying the heirarchical tree, complete with display options for each node
			outline.setRootVisible(true);
			outline.setModel(model);
			outline.setDefaultRenderer(Color.class, new ColorRenderer(true));
			outline.setDefaultEditor(Color.class, new ColorEditor() );
			//outline.getTableHeader().setVisible(false); // Table header is not needed so make invisible
			TableColumn column = outline.getColumnModel().getColumn(2);
			column.setMaxWidth(30);
			column.setResizable(false);
			column = outline.getColumnModel().getColumn(1);
			column.setMaxWidth(30);
			column.setResizable(false);
			outline.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);///AUTO_RESIZE_OFF);outline.setRootVisible(false);
			outline.setShowGrid(true);
			outline.setGridColor(new Color(225,225,225) );
			outline.setRowSelectionAllowed(false);
			//outline.setMinimumSize( new Dimension(300, 300) );

			//univ.getCanvas().setPreferredSize(new Dimension(500, 500) );
			univ = new Image3DUniverse();
			univ.getCanvas().setMinimumSize(new Dimension(300, 300) );
			//univ.show();
			//JScrollPane pane3DView = new JScrollPane();
			//pane3DView.add( univ.getCanvas() );
			JCheckBox animateCheckBox = new JCheckBox("Animate", false);
			animateCheckBox.addActionListener(new ActionListener() {
																	@Override
																	public void actionPerformed(ActionEvent e) {
																		if (!animating) {
																			animating = true;
																			univ.startAnimation();
																		} else {
																			animating = false;
																			univ.pauseAnimation();
																		}
																	}
																	});
			
			JButton movieButon = new JButton("Record Movie");
			movieButon.addActionListener(new ActionListener()	{	@Override
																	public void actionPerformed(ActionEvent e) {
																		ImagePlus movie = univ.record360();
																		movie.show();
																	}
																});
			
			FlowLayout flow = new FlowLayout();
			JPanel controlPanel = new JPanel(flow);
			controlPanel.add(animateCheckBox);
			controlPanel.add(movieButon);
			BorderLayout layout = new BorderLayout();
			JPanel panel3d = new JPanel(layout);
			panel3d.add(univ.getCanvas(), BorderLayout.CENTER);
			panel3d.add(controlPanel, BorderLayout.PAGE_END);

			// Add the image as a volume rendering
			//Content c = univ.addMesh(imp);

			// Establish the tabbed pane that appears on right side of usier interface
			JTabbedPane tabbedPane = new JTabbedPane();
			tabbedPane.addTab("3D View", panel3d );//pane3DView);
			JScrollPane paneTreeView = new JScrollPane();
			tabbedPane.addTab("Tree View", paneTreeView);

			// Add components
			JScrollPane leftScrollPane = new JScrollPane(outline);
			leftScrollPane.setMinimumSize( new Dimension(250,480) );
			JScrollPane rightScrollPane = new JScrollPane();
			JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScrollPane, tabbedPane);
			//jsp.setResizeWeight(0);
			jsp.setOneTouchExpandable(true);
			add(jsp);
			
			setVisible(true);
		}
	}

	// This Class defines the row information for each tree node in outline (part of UI)
	private class NodeRowModel implements RowModel /*, ActionListener*/ {

		public Class getColumnClass(int column) {
			switch (column) {
				case 0 : return Boolean.class;
				case 1 : return Color.class;
				default : assert false;
			}
			return null;
		}
		
		public int getColumnCount() {
			return 2;
		}
		
		public String getColumnName(int column) {
			return column == 0 ? "" : "";
		}
		
		public Object getValueFor(Object node, int column) {
			ClusterNode c = (ClusterNode) node;
			switch (column) {
				case 0 : return c.isVisible();
				case 1 : return c.getColor();
				default : assert false;
			}
			return null;
		}
	
		public boolean isCellEditable(Object node, int column) {
			return true;
		}
	
		public void setValueFor(Object node, int column, Object value) {
			ClusterNode c = (ClusterNode) node;
			if (column == 0) {
				c.toggleVisible();
			} else if (column == 1) {
				c.setColor( (Color) value);
			}
		}
	}

	// Class representing a single cluster tree node, replaces the old Node class
	private class ClusterNode {

		private boolean isVisible = false;
		private Color color;
		
		private String name;
		private int thisIndex;
		private int parentIndex;
		private int[] childIndex = new int[2];
		private String[] childValue = new String[2];
		private double distance;
		private ImagePlus imp;
		Content content;

		public ClusterNode(String name, int index) {
			this.name = name;
			//Random rand = new Random();
			this.color = new Color(225, 225, 225);//.getHSBColor(rand.nextFloat(), (rand.nextInt(1000) + 2000)/1000f, 0.75f);
			childIndex[0] = -1;
			childIndex[1] = -1;
			thisIndex = index;
		}

		public ImagePlus getImagePlus() {

			if (imp == null) {

				// Reconstruct a volume and show it
				String[] results = clusterTree.getLeafValues(thisIndex);
				int[][] parsed_coord_list = parseCoordinates(cluster_filepath, results);
				imp = reconstructVolume(parsed_coord_list, bins, "NODE" + (thisIndex + 1) );
				
			} 
			return imp;
		}

		public String toString() {
			return this.name;
		}

		public void setVisible(boolean isVisible) {
			this.isVisible = isVisible;
		}

		public void setColor(Color newColor) {
			this.color = newColor;
			if (content != null) {
				univ.getContent(this.name).setColor( new javax.vecmath.Color3f(this.color) );
			}
		}

		public boolean isVisible() {
			return this.isVisible;
		}

		public void toggleVisible() {

			if (content == null) {

				Random rand = new Random();
				this.color = Color.getHSBColor(rand.nextFloat(), (rand.nextInt(1000) + 2000)/1000f, 0.75f);
				boolean[] channels = {true, true, true}; 
				imp = getImagePlus();
				content = univ.addMesh(	imp, 									// The ImagePlus
										new javax.vecmath.Color3f(this.color),	// Color
										this.name,								// Name
										50,										// Threshold
										channels,						// Which RGB channels to display
										1);										// Resampling factor
			}
			
			if ( isVisible() ) {
				content.setVisible(false);
				this.isVisible = false;
			} else {
				content.setVisible(true);
				this.isVisible = true;
			}
		}

		public Color getColor() {
			return color;
		}

		public int[] getChildren() {

			return childIndex;
		}

		// Methods carried over from old Node class
		public void setParentIndex(int index) {
			parentIndex = index;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}

		public void setLeftValue(String value) {
			childValue[0] = value;
		}

		public void setLeftIndex(int index) {
			childIndex[0] = index;
		}

		public void setRightValue(String value) {
			childValue[1] = value;
		}

		public void setRightIndex(int index) {
			childIndex[1] = index;
		}

		public int getParentIndex() {
			return parentIndex;
		}

		public double getDistance() {
			return distance;
		}
		
		public String getLeftValue() {
			return childValue[0];
		}

		public int getLeftIndex() {
			return childIndex[0];
		}

		public String getRightValue() {
			return childValue[1];
		}

		public int getRightIndex() {
			return childIndex[1];
		}

		public boolean leftIsLeaf() {
			//return false;
			if (getLeftIndex() == -1) {
				return true;
			} else {
				return false;
			}
		}

		public boolean rightIsLeaf() {
			if (getRightIndex() == -1) {
				return true;
			} else {
				return false;
			}
		}

		public int countChildren() {

			int childCount = 0;
			if (getLeftIndex() != -1) childCount++;
			if (getRightIndex() != -1) childCount++;
			return childCount;
		}
	}

	// Class for holding the cluster tree data, replaces the old TreeProcessor class
	private class ClusterTreeModel implements TreeModel {

		//private ClusterNode root;
		private ClusterNode[] node;

		// Constructor
		public ClusterTreeModel(String tree_filepath) {

			// Open tree file
			BufferedInputStream buffer;
			try {
				// Count the number nodes by counting the number of '/n' characters in the input .atr file
				buffer = new BufferedInputStream(new FileInputStream(tree_filepath));
				byte[] chunk = new byte[1024];
				int node_count = 0;
				int readChars = 0;
				boolean empty = true;
				while ((readChars = buffer.read(chunk)) != -1) {
					empty = false;
					for (int i = 0; i < readChars; ++i) {
						if (chunk[i] == '\n') ++node_count;
					}
				}
				buffer.close();

				// Make an array for storing nodes
				node = new ClusterNode[node_count];

				// Read nodes, line by line
				LineNumberReader reader  = new LineNumberReader(new FileReader(tree_filepath));
				String line = "";
				while ( (line = reader.readLine() ) != null ) {

					// Parse node index
					int index = 0;
					char c;
					String word = "";
					while ( (c = line.charAt(index)) != 'X' ) {

						word = word + c;
						index++;
					}
					int node_index = Integer.parseInt( word.substring(4, word.length() ) ) - 1;
				
					// Create a new node at the current index
					node[node_index] = new ClusterNode(word, node_index);
				
					// Parse left child
					word = "";
					index = index + 2;
					while ( (c = line.charAt(index)) != 'X' ) {

						word = word + c;
						index++;
					}
					if (word.startsWith("NODE") ) {
					
						// Child is node
						node[node_index].setLeftIndex( Integer.parseInt( word.substring(4, word.length() ) ) - 1 );
					
					} else {

						// Child is leaf
						node[node_index].setLeftValue(word);
					}

					// Parse right child
					word = "";
					index = index + 2;
					while ( (c = line.charAt(index)) != 'X' ) {

						word = word + c;
						index++;
					}
					if (word.startsWith("NODE") ) {
					
						// Child is node
						node[node_index].setRightIndex( Integer.parseInt( word.substring(4, word.length() ) ) - 1 );
					
					} else {

						// Child is leaf
						node[node_index].setRightValue(word);
					}

					// Parse distance
					index = index + 2;
					word = line.substring(index, line.length() );
					node[node_index].setDistance( Double.parseDouble( line.substring(index, line.length() ) ) );
				}
				reader.close();
			
			} catch(IOException e) {
			
				IJ.showMessage("Error", "Cannot find file!");
			}
		}

		// This method returns the number of nodes in the tree
		public int getNodeCount() {

			return node.length;
		}

		// This method traverses the tree to produce a list of leaves beneath the given node
		public String[] getLeafValues(int node_index) {

			String[] temp_results = new String[ bins[0] * bins[1] * bins[2] ];

			// Create a stack
			Stack<ClusterNode> node_stack = new Stack<ClusterNode>();

			// Push the current node onto the stack
			node_stack.push(node[node_index]);

			int leafCount = 0;

			// Iterate through the tree
			while ( ! node_stack.empty() ) {

				// Pop a node from the top of the stack
				ClusterNode current_node = node_stack.pop();

				// Check the node's right child
				if ( current_node.rightIsLeaf() ) {

					// Right child is a leaf, so print value to log
					temp_results[leafCount] = current_node.getRightValue();
					leafCount++;
				} else {

					// Right child is a node, so push it onto the stack
					node_stack.push( node[ current_node.getRightIndex() ] );
				}

				// Check the node's left child
				if ( current_node.leftIsLeaf() ) {

					// Left child is a leaf, so print value to log
					temp_results[leafCount] = current_node.getLeftValue();
					leafCount++;
				} else {

					// Left child is a node, so push it onto the stack
					node_stack.push( node[ current_node.getLeftIndex() ] );
				}
			}
			String[] results = new String[leafCount];
			for (int i = 0; i < leafCount; i++) {

				results[i] = temp_results[i];
			}
			return results;
		}

		//The following methods implement the TreeModel interface
		@Override
		public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
			//do nothing
		}

		@Override
		public Object getChild(Object parent, int index) {
			// Returns one of the parent node's two children
			ClusterNode c = (ClusterNode) parent;
			if (c.countChildren() == 2) {
				if (index == 0) {
					return node[c.getLeftIndex()];
				} else if (index == 1) {
					return node[c.getRightIndex()];
				}
			} else if (c.countChildren() == 1) {
				if (index == 0) {
					if (c.getLeftIndex() != -1) {
						return node[c.getLeftIndex()];
					} else if (c.getRightIndex() != -1) {
						return node[c.getRightIndex()];
					} else {
						return null;
					}
				}
			}
			return null;
		}

		@Override
		public int getChildCount(Object parent) {
			// Returns the number of nodes beneath the given node
			ClusterNode c = (ClusterNode) parent;
			return c.countChildren();
		}

		@Override
		public int getIndexOfChild(Object parent, Object child) {
			// Returns index of given child node in parent node
			ClusterNode par = (ClusterNode) parent;
			ClusterNode ch = (ClusterNode) child;
			
			int inChildIndexInParent = 0;
			if (par.countChildren() == 2) {
				if ( ch.equals( node[ par.getLeftIndex() ] ) ) {
					inChildIndexInParent = 0;
				} else if ( ch.equals( node[ par.getRightIndex() ] ) ) {
					inChildIndexInParent = 1;
				}
			} else if (par.countChildren() == 1) {
				inChildIndexInParent = 0;
			}
			return inChildIndexInParent;
		}

		@Override
		public Object getRoot() {
			// The root node is always the last in the .atr file and therefore also in the node[]
			return node[node.length - 1];
		}

		@Override
		public boolean isLeaf(Object node) {
			ClusterNode c = (ClusterNode) node;
			boolean isLeaf = false;
			if ( c.leftIsLeaf() && c.rightIsLeaf() ) {
				isLeaf = true;	// Both children are leaves, so for the purposes of this method the node is a leaf (i.e. there are no other nodes below it).
			}
			return false;
		}

		@Override
		public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
			//do nothing
		}

		@Override
		public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
			//do nothing
		}
	}

	// Handles color property rendering (part of UI)
	public class ColorRenderer extends JLabel implements TableCellRenderer {
		//...
		boolean isBordered = true;
		public ColorRenderer(boolean isBordered) {
			this.isBordered = isBordered;
			setOpaque(true); //MUST do this for background to show up.
			Dimension d = getMaximumSize();
			d.setSize(d.getHeight(), d.getHeight());
			setMaximumSize(d);
		}

		public Component getTableCellRendererComponent(JTable table, Object color, boolean isSelected, boolean hasFocus, int row, int column) {
			Color newColor = (Color)color;
			setBackground(newColor);
			if (isBordered) {
				if (isSelected) {
					//if (selectedBorder == null) {
						Border selectedBorder = BorderFactory.createMatteBorder(2,5,2,5,table.getSelectionBackground());
					//}
					//selectedBorder is a solid border in the color table.getSelectionBackground().
					setBorder(selectedBorder);
				} else {
					//if (unselectedBorder == null) {
						Border unselectedBorder = BorderFactory.createMatteBorder(2,5,2,5,table.getBackground());
					//}
					//unselectedBorder is a solid border in the color table.getBackground().
					setBorder(unselectedBorder);
				}
			}
			//setToolTipText
			setToolTipText("RGB value: " + newColor.getRed() + ", " + newColor.getGreen() + ", " + newColor.getBlue());
			return this;
		}
    }

    // Handles color property editing (part of UI)
    public class ColorEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
		Color currentColor;
		JButton button;
		JColorChooser colorChooser;
		JDialog dialog;
		protected static final String EDIT = "edit";

		public ColorEditor() {
			button = new JButton();
			button.setActionCommand(EDIT);
			button.addActionListener(this);
			button.setBorderPainted(false);

			//Set up the dialog that the button brings up.
			colorChooser = new JColorChooser();
			int panelCount = colorChooser.getChooserPanels().length;
			for(int n = 0; n < panelCount; n++){
				AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
				String displayName=panels[n].getDisplayName();
				//if (displayName != "HSV") {
					colorChooser.removeChooserPanel(panels[n]);
					panelCount--;
					n = 0;
				//}
			}
			dialog = JColorChooser.createDialog(button,
												"Pick a Color",
												true,  //modal
												colorChooser,
												this,  //OK button handler
												null); //no CANCEL button handler
		}

		public void actionPerformed(ActionEvent e) {
			if (EDIT.equals(e.getActionCommand())) {
				//The user has clicked the cell, so
				//bring up the dialog.
				button.setBackground(currentColor);
				colorChooser.setColor(currentColor);
				dialog.setVisible(true);

				fireEditingStopped(); //Make the renderer reappear.

			} else { //User pressed dialog's "OK" button.
				currentColor = colorChooser.getColor();
			}
		}

		//Implement the one CellEditor method that AbstractCellEditor doesn't.
		public Object getCellEditorValue() {
			return currentColor;
		}

		//Implement the one method defined by TableCellEditor.
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			currentColor = (Color)value;
			return button;
		}
	}

	// The following methods and classes were taken from the old Tree_to_Volume
	private ImagePlus reconstructVolume(int[][] coord_list, int[] bins, String title) {

		// Create an ImagePlus
		ImageStack is = new ImageStack(bins[0], bins[1], bins[2] );
		for (int i = 0; i < bins[2]; i++) {

			is.setProcessor(new ByteProcessor(bins[0], bins[1]), i + 1 );
		}
		ImagePlus ip = new ImagePlus(title, is);

		// Populate ImagePlus with white pixels according to the coord_list
		for (int i = 0; i < coord_list.length; i++) {

			ip.setSlice(coord_list[i][2]);
			ip.getProcessor().set(coord_list[i][0], coord_list[i][1], 255);
		}

		return ip;
	}

	// This methods directly sets the values in the global int[] bins which keeps track of the number of bins in x, y and z.
	private void parseBinSize(String cdt_filepath) {

		try {
				// Count the number of tabs on the first line of the given .cdt file
				LineNumberReader reader  = new LineNumberReader(new FileReader(cdt_filepath));
				String line = reader.readLine();
				int tab_count = 0;
				int first_tab = 0;
				int index = 0;
				while ( index < line.length() ) {

					if (line.charAt(index) == '\t') {			// Have found a tab
						
						tab_count++;							// Increment the number of tabs counted
						if (tab_count == 1) first_tab = index;	// Set a pointer to the first tab. ??? The first_tab == 1 here ensures that the preceeding GID/t in .gtr files is skipped over. Should be changed to first_tab == 0 for .atr files that have no preceeding GID/t
					}
					index++;
				}
				reader.close();
				int arry_count = tab_count - 4;

				// Parse the number of x, y, z bins
				// Parse x bins
				index = first_tab+1; // Should be changed to index = 0 for .atr files that have no preceeding GID/t
				char c;
				String word = "";
				while ( ( c = line.charAt(index) ) != 'x' ) {

					word = word + c;
					index++;
				}
				bins[0] = Integer.parseInt(word); // Set the number of x bins

				// Parse y bins
				index++;
				word = "";
				while ( ( c = line.charAt(index) ) != 'x' ) {

					word = word + c;
					index++;
				}
				bins[1] = Integer.parseInt(word); // Set the number of y bins

				// Parse z bins
				index++;
				word = "";
				while ( ( c = line.charAt(index) ) != '\t' ) {

					word = word + c;
					index++;
				}
				bins[2] = Integer.parseInt(word); // Set the number of z bins

				// Print x, y, z bins
				reader.close();

			} catch(IOException e) {
			
				IJ.showMessage("Error", "Cannot find file!");
			}
	}

	private int[][] parseCoordinates(String cdt_filepath, String[] bin_labels) {

		int tab_count = 0;
		int arry_count = 0;
		int[][] parsed_coord_list = new int[bin_labels.length][3];

		if (arry_mapping == null) {

			// Mapping has not been created
			// Count the number of arrays/coords that need to be mapped

			// Open cdt file
			BufferedInputStream buffer;
			try {

				// Count the number of tabs on the first line
				LineNumberReader reader  = new LineNumberReader(new FileReader(cdt_filepath));
				String line = reader.readLine();
				tab_count = 0;
				int first_tab = 0;
				int index = 0;
				while ( index < line.length() ) {

					if (line.charAt(index) == '\t') {
						
						tab_count++;
						if (tab_count == 1) first_tab = index;
					}
					index++;
				}
				reader.close();
				arry_count = tab_count - 4;

				// Parse the number of x, y, z bins

				// Parse x bins
				index = first_tab+1;
				char c;
				String word = "";
				while ( ( c = line.charAt(index) ) != 'x' ) {

					word = word + c;
					index++;
				}
				bins[0] = Integer.parseInt(word);

				// Parse y bins
				index++;
				word = "";
				while ( ( c = line.charAt(index) ) != 'x' ) {

					word = word + c;
					index++;
				}
				bins[1] = Integer.parseInt(word);

				// Parse z bins
				index++;
				word = "";
				while ( ( c = line.charAt(index) ) != '\t' ) {

					word = word + c;
					index++;
				}
				bins[2] = Integer.parseInt(word);
				
				// Create an array for mapping arry:coord
				arry_mapping = new String[arry_count][2];	// 0 = coord, 1 = arry

				// Re-open .cdt file and read first line which contains the coord data
				reader  = new LineNumberReader(new FileReader(cdt_filepath));
				line = reader.readLine();

				// Skip over the first 4 tabs to find index of first coord within the file
				tab_count = 0;
				index = 0;
				while ( tab_count < 4 ) {

					if (line.charAt(index) == '\t') tab_count++;
					index++;
				}
				index--;

				// Iterate through coords
				int current_arry = 0;
				while ( (current_arry < arry_mapping.length) && ( index < line.length() ) ) {

					// Get the next word and add to arry_mapping
					word = "";
					index = index + 1;
					while ( (current_arry < arry_mapping.length) && ( index < line.length() ) && ( (c = line.charAt(index)) != '\t' ) ) {

						word = word + c;
						index++;
					}
					arry_mapping[current_arry][0] = word;
					current_arry++;
				}

				// Read the second line which contains the arry data
				line = reader.readLine();

				// Skip over the first 4 tabs to find index of first arry
				tab_count = 0;
				index = 0;
				while ( tab_count < 4 ) {

					if (line.charAt(index) == '\t') tab_count++;
					index++;
				}
				index--;

				// Iterate through arrys
				current_arry = 0;
				while ( (current_arry < arry_mapping.length) && ( index < line.length() ) ) {

					// Get the next word and add to arry_mapping
					word = "";
					index = index + 1;
					while (  (current_arry < arry_mapping.length) && ( index < line.length() ) && ( (c = line.charAt(index)) != 'X' ) ) {

						word = word + c;
						index++;
					}
					arry_mapping[current_arry][1] = word;
					current_arry++;
					index++;	// This increment is needed to push index past the tab that follows the 'X'
				}
				reader.close();

			} catch(IOException e) {
			
				IJ.showMessage("Error", "Cannot find file!");
			}
		}

		// Now that we have a mapping, find each ARRY in arry_mapping to determine it's coord, then parse the coord to get x, y, z coordinates
		// Build a list of x, y, z coordinates and return it
		
		// Iterate through bin_labels
		for (int i = 0; i < bin_labels.length; i++) {

			// For each bin_label iterate through arry_mapping
			for (int j = 0; j < arry_mapping.length; j++) {

				// Check if current bin_label matches current arry_mapping
				if ( bin_labels[i].equals(arry_mapping[j][1]) ) {

					// Found a match!
					// Parse coord to detemine x, y, z coordinates...

					// Parse x coordinate
					int index = 0;
					char c;
					String word = "";
					while (( index < arry_mapping[j][0].length() ) && ( ( c = arry_mapping[j][0].charAt(index) ) != 'x') ) {

						word = word + c;
						index++;
					}
					parsed_coord_list[i][0] = Integer.parseInt(word);

					// Parse y coordinate
					index++;
					word = "";
					while (( index < arry_mapping[j][0].length() ) && ( ( c = arry_mapping[j][0].charAt(index) ) != 'x') ) {

						word = word + c;
						index++;
					}
					parsed_coord_list[i][1] = Integer.parseInt(word);

					// Parse z coordinate
					index++;
					word = "";
					while ( ( index < arry_mapping[j][0].length() ) && ( ( c = arry_mapping[j][0].charAt(index) ) != 'x') ) {

						word = word + c;
						index++;
					}
					parsed_coord_list[i][2] = Integer.parseInt(word);
				}
			}
		}
		return parsed_coord_list;
		
	}
}
