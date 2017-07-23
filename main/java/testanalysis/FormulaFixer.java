package testanalysis;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Tom on 7/15/2017.
 */
/*
Helper for creation of test analysis

Pain points
- cell references in formula does not increment by 1 but rather the row difference
between cells, this mandates boring time-consuming copy-paste. Filtering
affected rows shows the same unwanted behaviour

Plan
Detect header rows
Grab each cell
Check for formula, if found, find reference, replace with incremented ref

Locating header basically outlines section (header, steps) so the same formula
can be applied to steps as well.

Features
create new sheet with fixed formulas
filter cells on custom column

TODO
test out scenarios when grabbing id from row
low - dump fixed rows in columns space separated for easy copy-paste?
change references in selected? non-header rows
 */
public class FormulaFixer {
    private final DataFormatter fmt = new DataFormatter();
    String file = "D:\\projects\\excel-tools\\src\\main\\java\\testanalysis" +
            "\\TA.xlsx";
    Pattern cellRef = Pattern.compile("($?[A-Z])([0-9])+");
    Integer startingHeader = 1; //-1 to grab first valid value
    String ignoreFormula = ("(VLOOKUP|INDIRECT).*");
    String filterOnColumn = "B";
    private Workbook wb;
    private String sourceSheet = "Test Scripts ";

    public static void main(String[] args) throws IOException {
        FormulaFixer fixer = new FormulaFixer();
        fixer.work();
    }

    public void work() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        wb = new XSSFWorkbook(fis);
        Sheet sheetOrig = wb.getSheet(sourceSheet);
        if (sheetOrig == null) {
            System.out.println("Error: Sheet does not exist <" +
                    sourceSheet + ">");
            return;
        }
        Sheet sheetCopy = wb.cloneSheet(wb.getSheetIndex(sheetOrig));

        Matcher mFormula;
        Integer headerId = -1;
        String formula;
        for (int i = 0; i < 30; i++) {
            Row row = sheetCopy.getRow(i);
//        for (Row row: sheetCopy) {
            if (!isHeader(row))
                continue;
            System.out.println("[Row " + row.getRowNum() + 1);
            for (Cell cell : row) {
                if (!isTarget(cell))
                    continue;
                mFormula = cellRef.matcher(cell.getCellFormula());
                if (mFormula.find()) {
//                    id can set user or grab first valid
                    if (headerId == -1)
                        headerId = startingHeader == -1 ?
                                Integer.parseInt(mFormula.group(2)) :
                                startingHeader;
//                    System.out.println(mFormula.group());
                    formula = mFormula.replaceAll("$1" + headerId);
                    cell.setCellFormula(formula);
                    System.out.println("[formula " + formula);
                }
            }
            headerId++;
        }
        fis.close();

        FileOutputStream fos = new FileOutputStream(file);
        wb.write(fos);
        wb.close();
        fos.close();
    }

    void changeSheetName(Sheet sheet) {
        wb.setSheetName(wb.getSheetIndex(sheet), "New sheet");
    }

    boolean isHeader(Row row) {
        return !getCellValue(row.getCell(0)).equals("")
                && row.getRowNum() != 0;
    }

    String getCellValue(Cell cell) {
        return fmt.formatCellValue(cell);
    }

    boolean isTarget(Cell cell) {
        String colName = CellReference.convertNumToColString(
                cell.getColumnIndex());
        boolean colFilter = filterOnColumn.equals("") ||
                colName.equals(filterOnColumn);
        return colFilter &&
                cell.getCellType() == Cell.CELL_TYPE_FORMULA &&
                !getCellValue(cell).matches(ignoreFormula);
    }
}
