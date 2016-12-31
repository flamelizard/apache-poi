package RatingUpdate;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Created by Tom on 12/30/2016.
 */
/*
Locate Cenik spreadsheet
Open Rating and Cenik
Cycle over rows (call def) in Rating
    Determine zone for each call
    Lookup in Cenik
    Capture price
Write prices to Rating spreadhseet

Improvements
- close or not the files only to be read
- logging

Specification / Purpose
- for each row in rating.xls file, fill in price with and without DPH from
tariff's Cenik
- this way, I spend less time copy/paste data between Excels

 */
public class ChargeCalls {
    private final DataFormatter cellFmt = new DataFormatter();
    private Path ratingFile;
    private Path cenikFile;
    private Path sourceDir = Paths.get
            ("D:\\projects\\excel-tools\\src\\main\\resources");
    private String cenikSheetName = "Zaklad-Rating";
    private String cenikRowPtrn = "(fixní hlas|sms),.+";
    private int[] cenikColumnIds = new int[]{2, 4, 10};


    public static void main(String[] args) throws Exception {
        ChargeCalls charger = new ChargeCalls();
        charger.begin();
    }

    public void begin() throws Exception {
        ratingFile = locateFile("[Rr]ating.+");
        cenikFile = locateFile("[Cc]en.k.+");
        rating();
    }

//    void extractTextFromFormula() throws IOException {
//        HSSFWorkbook wb = new HSSFWorkbook(Files.newInputStream(locateFile
//                ("extract_formula.xls")));
//        ExcelExtractor extractor = new org.apache.poi.hssf.extractor
//                .ExcelExtractor(wb);
//        extractor.setFormulasNotResults(false);
//        System.out.println(extractor.getText());
//    }

    public void rating() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook(ratingFile.toString());
        String sheetName = "Data";
        Sheet dataSheet = wb.getSheet(sheetName);
        if (dataSheet == null) {
            throw new SourceDataException(sheetName);
        }

//        System.out.println(findColumnByName("destination", dataSheet));
//        findColumnByName("tim", dataSheet);
        extractZonesCost();

//        wb.close();

    }

    private int findColumnByName(String name, Sheet sheet)
            throws SourceDataException {
        List<String> header = new ArrayList<>();
        for (Cell cell : sheet.getRow(0)) {
            header.add(cellFmt.formatCellValue(cell));
        }
//        System.out.println(header);
        int columnId = 1;
        for (String column : header) {
            if (column.equalsIgnoreCase(name)) {
                return columnId;
            }
            columnId++;
        }
        throw new SourceDataException("Data column [" + name + "] not found");
    }

    public Path locateFile(String wildcard) throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(sourceDir)) {
            for (Path path : files) {
//                System.out.println(path);
                String fileName = path.getFileName().toString();
                if (fileName.matches(wildcard)) {
                    return path;
                }
            }
        }
        throw new FileNotFoundException(wildcard);
    }

    private void extractZonesCost() throws IOException {
        XSSFWorkbook wb = new XSSFWorkbook(cenikFile.toString());
//        wb.setForceFormulaRecalculation(true);
        Sheet sheet = wb.getSheet(cenikSheetName);
        if (sheet == null)
            throw new SourceDataException(
                    "Could not find sheet: " + cenikSheetName);

        List<ZoneCost> costs = new ArrayList<>();
        StringJoiner csv;
        FormulaEvaluator evaluator = wb.getCreationHelper()
                .createFormulaEvaluator();

        for (Row row : sheet) {
            csv = new StringJoiner(",");
            for (Cell cell : row) {
                csv.add(cellFmt.formatCellValue(cell, evaluator));
            }
            if (csv.toString().matches(cenikRowPtrn)) {
                System.out.println("[match " + csv.toString());
                costs.add(new ZoneCost(csv.toString()));
            }
        }
        System.out.println("[cost " + costs);
    }

//    List<String> getRowsAsCsv(Sheet sheet) {
//        List<String> rows = new ArrayList<>();
//        StringJoiner csv;
//
//        for (Row row: sheet) {
//            csv = new StringJoiner(",");
//            for (Cell cell: row) {
//                csv.add(cellFmt.formatCellValue(cell, evaluator));
//            }
//        }
//    }

    /*
    DataFormatter grabs formula from a cell.
    Either grab manually or pass FormulaEvaluator to formatter.
     */
    void getValueFromFormula(Cell cell) {
        if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {

            switch (cell.getCachedFormulaResultType()) {
                case Cell.CELL_TYPE_NUMERIC:
                    System.out.println(cell.getNumericCellValue());
                    break;
                case Cell.CELL_TYPE_STRING:
                    System.out.println(cell.getStringCellValue());
                    break;
            }
        }
    }
}
