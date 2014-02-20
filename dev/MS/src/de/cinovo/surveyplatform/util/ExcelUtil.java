/**
 * 
 */
package de.cinovo.surveyplatform.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;


/**
 * Copyright 2012 Cinovo AG<br><br>
 * @author yschubert
 *
 */
public class ExcelUtil {
	
	/**
	 * 
	 */
	private static final XSSFColor COLOR_EXCEL_BLUE = new XSSFColor(new java.awt.Color(79, 129, 189));
	
	public static class CellPointer {
		
		private Sheet sheet;
		
		private int currentRow = 0;
		private int currentColumn = 0;
		
		
		public CellPointer(final Sheet sheet) {
			this.sheet = sheet;
		}
		
		public Cell moveTo(final int row, final int column) {
			Row r = sheet.getRow(row);
			if (r == null) {
				r = sheet.createRow(row);
			}
			Cell c = r.getCell(column);
			if (c == null) {
				c = r.createCell(column);
			}
			currentRow = row;
			currentColumn = column;
			return c;
		}
		
		public Cell currentCell() {
			return moveTo(currentRow, currentColumn);
		}
		
		public Row currentRow() {
			Row r = sheet.getRow(currentRow);
			if (r == null) {
				r = currentCell().getRow();
			}
			return r;
		}
		
		public Cell nextColumn() {
			return moveTo(currentRow, currentColumn + 1);
		}
		
		public Cell nextRow() {
			return moveTo(currentRow + 1, 0);
		}
		
		public Cell jumpColumn(final int delta) {
			return moveTo(currentRow, currentColumn + delta);
		}
		
		public Cell jumpRow(final int delta) {
			return moveTo(currentRow + delta, 0);
		}
		
		public Cell jumpLastRow() {
			return moveTo(sheet.getLastRowNum() + 1, 0);
		}
	}
	
	
	public static CellStyle createBlueStyle(final Cell cell) {
		Workbook workbook = cell.getSheet().getWorkbook();
		
		Font font = workbook.createFont();
		font.setColor(IndexedColors.WHITE.getIndex());
		
		CellStyle blueBgStyle = workbook.createCellStyle();
		((XSSFCellStyle) blueBgStyle).setFillForegroundColor(COLOR_EXCEL_BLUE);
		blueBgStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		blueBgStyle.setAlignment(CellStyle.ALIGN_CENTER);
		blueBgStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		blueBgStyle.setFont(font);
		return blueBgStyle;
	}
	
	public static CellStyle createPercentStyle(final Cell cell) {
		Workbook workbook = cell.getSheet().getWorkbook();
		CellStyle style = workbook.createCellStyle();
		style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
		cell.setCellStyle(style);
		return style;
	}
	
	// public static String createSaveSheetTitle(final String unsafeTitle) {
	//
	// String safeTitle = unsafeTitle == null ? "" : (unsafeTitle.trim());
	//
	// // Rules for Excel Sheet Names:
	// // Name must not exceed 31 characters.
	// // Name must not contain any of the following characters: // : \ / ? * [
	// // or ]
	// // Make sure you did not leave the name blank.
	// if (safeTitle.isEmpty()) {
	// safeTitle = "Sheet 1";
	// }
	// safeTitle = safeTitle.replaceAll("[\\;\\:\\\\\\/\\?\\*\\[\\]]", "");
	// if (safeTitle.length() > 30) {
	// safeTitle = safeTitle.substring(0, 30);
	// }
	// return safeTitle;
	//
	// }
	
	// public static void main(final String[] args) {
	// System.out.println(createSaveSheetTitle("  hallo\\was:geht;so*ab[fdsfdffdsdf] "));
	// System.out.println(createSaveSheetTitle("hallo\\was:gijoijoijoijoij e asoijdasoijsd ht;so*ab[fdsfdffdsdf] "));
	// System.out.println(createSaveSheetTitle(" "));
	// System.out.println(createSaveSheetTitle(""));
	// System.out.println(createSaveSheetTitle(null));
	// }
}
