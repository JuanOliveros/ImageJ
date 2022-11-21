package ij.plugin;
import ij.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.*;

/** This plugin implements most of the Edit/Options/Colors command. */
public class Colors implements PlugIn, ItemListener {

	private final static String[] colorsArray = {"red","green","blue","magenta","cyan","yellow","orange","black","white","gray","lightgray","darkgray","pink"};
	private Choice fchoice, bchoice, schoice;
	private Color fc2, bc2, sc2;

 	public void run(String arg) {
		showDialog();
	}

	/** The Edit>Options>Colors dialog */
	private void showDialog() {
		Color fc =Toolbar.getForegroundColor();
		String fname = getColorName(fc, "black");
		Color bc =Toolbar.getBackgroundColor();
		String bname = getColorName(bc, "white");
		Color sc =Roi.getColor();
		String sname = getColorName(sc, "yellow");
		GenericDialog gd = new GenericDialog("Colors");
		gd.addChoice("Foreground:", colorsArray, fname);
		gd.addChoice("Background:", colorsArray, bname);
		gd.addChoice("Selection:", colorsArray, sname);
		Vector choices = gd.getChoices();
		if (choices!=null) {
			fchoice = (Choice)choices.elementAt(0);
			bchoice = (Choice)choices.elementAt(1);
			schoice = (Choice)choices.elementAt(2);
			fchoice.addItemListener(this);
			bchoice.addItemListener(this);
			schoice.addItemListener(this);
		}

		gd.showDialog();
		if (gd.wasCanceled()) {
			if (fc2!=fc) Toolbar.setForegroundColor(fc);
			if (bc2!=bc) Toolbar.setBackgroundColor(bc);
			if (sc2!=sc) {
				Roi.setColor(sc);
				ImagePlus imp = WindowManager.getCurrentImage();
				if (imp!=null && imp.getRoi()!=null) imp.draw();
			}
			return;
		}
		fname = gd.getNextChoice();
		bname = gd.getNextChoice();
		sname = gd.getNextChoice();
		fc2 = getColor(fname, Color.black);
		bc2 = getColor(bname, Color.white);
		sc2 = getColor(sname, Color.yellow);
		if (fc2!=fc) Toolbar.setForegroundColor(fc2);
		if (bc2!=bc) Toolbar.setBackgroundColor(bc2);
		if (sc2!=sc) {
			Roi.setColor(sc2);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null) imp.draw();
			Toolbar tb = Toolbar.getInstance();
			if (tb!=null) tb.repaint();
		}
	}

	/** For named colors, returns the name, or 'defaultName' if not a named color.
	 *  If 'defaultName' is non-null and starts with an uppercase character,
	 *  the returned name is capitalized (first character uppercase).
	 *  Use colorToString or colorToString2 to get a String representation (hexadecimal)
	 *  also for unnamed colors.*/
	public static String getColorName(Color c, String defaultName) {
		if (c==null) return defaultName;
		boolean useCapitalizedName = defaultName!=null && defaultName.length()>0 && Character.isUpperCase(defaultName.charAt(0));
		return getColorName(c, defaultName, useCapitalizedName);
	}

	/** For named colors, returns the name, or 'defaultName' if not a named color.
	 *  'color' must not be null. */
	private static String getColorName(Color c, String defaultName, boolean useCapitalizedName) {
		String colorName;
		if (c.equals(Color.red))            colorName = colorsArray[0];
		else if (c.equals(Color.green))     colorName = colorsArray[1];
		else if (c.equals(Color.blue))      colorName = colorsArray[2];
		else if (c.equals(Color.magenta))   colorName = colorsArray[3];
		else if (c.equals(Color.cyan))      colorName = colorsArray[4];
		else if (c.equals(Color.yellow))    colorName = colorsArray[5];
		else if (c.equals(Color.orange))    colorName = colorsArray[6];
		else if (c.equals(Color.black))     colorName = colorsArray[7];
		else if (c.equals(Color.white))     colorName = colorsArray[8];
		else if (c.equals(Color.gray))      colorName = colorsArray[9];
		else if (c.equals(Color.lightGray)) colorName = colorsArray[10];
		else if (c.equals(Color.darkGray))  colorName = colorsArray[11];
		else if (c.equals(Color.pink))      colorName = colorsArray[12];
		else colorName = defaultName;
		return useCapitalizedName ? capitalize(colorName)  : colorName.toLowerCase();
	}

	private static String capitalize(String str){
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/** For named colors, converts the name String to the corresponding color.
	 *  Returns 'defaultColor' if the color has no name.
	 *  Use 'decode' to also decode hex color names like "#ffff00" */
	public static Color getColor(String name, Color defaultColor)  {
		if (name==null || name.length()<2)
			return defaultColor;
		name = name.toLowerCase(Locale.US);
		Color c = defaultColor;
		Class colorClass = Color.class;
		try {
			Field field = colorClass.getDeclaredField(name);
			return (Color) field.get(c);
		}catch (Exception ex){
			return c;
		}
	}

	/** Converts a String with the color name or the hexadecimal representation
	 *  of a color with 6 or 8 hex digits to a Color.
	 *  With 8 hex digits, the first two digits are the alpha.
	 *  With 6 hex digits, the color is opaque (alpha = hex ff).
	 *  A hex String may be preceded by '#' such as "#80ff00".
	 *  When the string does not include a valid color name or hex code,
	 *  returns Color.GRAY. */
	public static Color decode(String hexColor) {
		return decode(hexColor, Color.gray);
	}

	/** Converts a String with the color name or the hexadecimal representation
	 *  of a color with 6 or 8 hex digits to a Color.
	 *  With 8 hex digits, the first two digits are the alpha.
	 *  With 6 hex digits, the color is opaque (alpha = hex ff).
	 *  A hex String may be preceded by "#" such as "#80ff00" or "0x".
	 *  When the string does not include a valid color name or hex code,
	 *  returns 'defaultColor'. */
	public static Color decode(String hexColor, Color defaultColor) {
		if (hexColor==null || hexColor.length()<2)
			return defaultColor;
		Color color = getColor(hexColor, null);  //for named colors
		if (color==null) {
			if (hexColor.startsWith("#"))
				hexColor = hexColor.substring(1);
			else if (hexColor.startsWith("0x"))
				hexColor = hexColor.substring(2);
			int len = hexColor.length();
			if (!(len==6 || len==8))
				return defaultColor;
			boolean hasAlpha = len==8;
			try {
				int rgba = (int)Long.parseLong(hexColor, 16);
				color = new Color(rgba, hasAlpha);
			} catch (NumberFormatException e) {
				return defaultColor;
			}
		}
		return color;
	}

	public static int getRed(String hexColor) {
		return decode(hexColor, Color.black).getRed();
	}

	public static int getGreen(String hexColor) {
		return decode(hexColor, Color.black).getGreen();
	}

	public static int getBlue(String hexColor) {
		return decode(hexColor, Color.black).getBlue();
	}

	/** Converts a Color into a lowercase string ("red", "green", "#aa55ff", etc.).
	 *  If <code>color</code> is <code>null</code>, returns the String "none". */
	public static String colorToString(Color color) {
		if (color == null) return "none";
		String str = getColorName(color, null, false);
		if (str == null)
			str = "#"+getHexString(color);
		return str;
	}

	/** Converts a Color into a string ("Red", "Green", #aa55ff, etc.).
	 *  If <code>color</code> is <code>null</code>, returns the String "None". */
	public static String colorToString2(Color color) {
		if (color == null) return "None";
		String str = getColorName(color, null, true);
		if (str == null)
			str = "#"+getHexString(color);
		return str;
	}

	/** Returns the 6-digit hex string such as "aa55ff" for opaque colors or
	 *  or 8-digit like "80aa55ff" for other colors (the first two hex digits are alpha).
	 *  'color' must not be null. */
	private static String getHexString(Color color) {
		int rgb = color.getRGB();
		boolean isOpaque = (rgb & 0xff000000) == 0xff000000;
		if (isOpaque)
			rgb &= 0x00ffffff;  //don't show alpha for opaque colors
		String format = isOpaque? "%06x" : "%08x";
		return String.format(format, rgb);
	}

	/** Returns an opaque color with the specified red, green, and blue values.
	 *  Values is outside the 0-255 range are replaced by the nearest
	 *  valid number (0 or 255) */
	public static Color toColor(int red, int green, int blue) {
	    if (red<0) red=0; if (green<0) green=0; if (blue<0) blue=0; 
	    if (red>255) red=255; if (green>255) green=255; if (blue>255) blue=255;  
		return  new Color(red, green, blue);
	}

	/** Callback listener for Choice modifications in the dialog */
	public void itemStateChanged(ItemEvent e) {
		Choice choice = (Choice)e.getSource();
		String item = choice.getSelectedItem();
		Color color = getColor(item, Color.black);
		if (choice==fchoice)
			Toolbar.setForegroundColor(color);
		else if (choice==bchoice)
			Toolbar.setBackgroundColor(color);
		else if (choice==schoice) {
			Roi.setColor(color);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null && imp.getRoi()!=null) imp.draw();
			Toolbar.getInstance().repaint();
		}
	}

	/** Returns an array of the color Strings in the argument(s) and the 13
	 *  predefined color names "Red", "Green", ... "Pink".
	 *  The Strings arguments must be either "None" or hex codes starting with "#".
	 *  Any null arguments are ignored. */
	public static String[] getColors(String... moreColors) {
		ArrayList names = new ArrayList();
		for (String arg: moreColors) {
			if (arg!=null && arg.length()>0 && (!Character.isLetter(arg.charAt(0))||arg.equals("None")))
				names.add(arg);
		}
		for (String arg: colorsArray)
			names.add(capitalize(arg));
		return (String[])names.toArray(new String[names.size()]);
	}

	public static String[] getColorsArray() {
		return colorsArray;
	}
}
