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
Pain point aka reason
- cell references in formula does not increment by 1 but rather the row difference
between cells, this mandates boring and time-consuming copy-paste. Filtering
affected rows and dragging first row down shows the same unwanted behaviour.

This class will filter header rows and change formula cell references to
ascending number sequence.

TODO
test out scenarios when grabbing id from row
change references in selected? non-header rows
cloning takes too long suddenly, why? - faster impl
no - copy out only headers (copy-paste will mess up cel ref indexes)
 */
public class FormulaFixer {
    private final DataFormatter fmt = new DataFormatter();
    String file = "D:\\projects\\excel-tools\\src\\main\\java\\testanalysis" +
            "\\TA.xlsx";
    Integer defaultHeaderId = -1; //-1 to grab first valid value
    String ignoreFormula = ("(VLOOKUP|INDIRECT).*");
    String filterOnColumn = ""; //leave empty to select any
    Pattern cellRef = Pattern.compile("(?<col>[A-Z])([0-9]+)");
    private String sourceSheet = "Test Scripts ";
    private Integer startFromLine = 46;
    private String replaceExpr = "${col}%s";
    private Workbook wb;

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
        System.out.println("Cloning sheet ...");
        Sheet sheetCopy = wb.cloneSheet(wb.getSheetIndex(sheetOrig));

        Matcher mFormula;
        Integer headerId = -1;
        String formula;
        for (Row row : sheetCopy) {
            if (!isHeader(row))
                continue;
            System.out.println("[Row " + (row.getRowNum() + 1));
            for (Cell cell : row) {
                if (!isTarget(cell))
                    continue;
                mFormula = cellRef.matcher(cell.getCellFormula());
                if (mFormula.find()) {
//                    id can set user or grab first valid
                    if (headerId == -1)
                        headerId = defaultHeaderId == -1 ?
                                Integer.parseInt(mFormula.group(2)) :
                                defaultHeaderId;
                    formula = mFormula.replaceAll(
                            String.format(replaceExpr, headerId)
                    );
                    System.out.println("[formula " + formula);
                    cell.setCellFormula(formula);
                }
            }
            headerId++;
        }
        fis.close();
        saveExcel();
    }

    void saveExcel() throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        wb.write(fos);
        fos.close();
        wb.close();
    }

    boolean isHeader(Row row) {
        return !getCellValue(row.getCell(0)).equals("")
                && row.getRowNum() != 0
                && row.getRowNum() >= startFromLine - 1;
    }

    String getCellValue(Cell cell) {
        return fmt.formatCellValue(cell);
    }

    boolean isTarget(Cell cell) {
        String colName = CellReference.convertNumToColString(
                cell.getColumnIndex());
        boolean colFilter = filterOnColumn.equals("") ||
                colName.equals(filterOnColumn);
        return colFilter
                && cell.getCellType() == Cell.CELL_TYPE_FORMULA
                && !getCellValue(cell).matches(ignoreFormula);
    }
}
