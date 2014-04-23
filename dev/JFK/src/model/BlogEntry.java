package model;

import java.util.Date;
import java.util.List;

public class BlogEntry {
	private BlogType blogType;
	private String headLine;
	private String text;
	
	// TODO posted gut genug? oder beides merken? Sonstige?
	private Date posted;
	private Date lastModified;
	
	private List<Comment> commentList;
	
	// TODO smart Cut-Off des Textes?
	// oder einstellbare Zeichenlänge der Vorschau?
	private int previewLength;
	
	private String imagePath;
	
	// TODO - was ist "Filed in Column"??
	// ---
	
	// TODO - Author? Zitate?
	private String author;
}
