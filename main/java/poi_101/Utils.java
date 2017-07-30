package poi_101;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

/**
 * Created by Tom on 7/30/2017.
 */
public class Utils {
    //    Apache POI does not provide copy row method
    static void copyRowsOnlyFormula(List<Row> rows, Sheet blankSheet) {
        Integer i = 0;
        Integer j;
        for (Row rowFrom : rows) {
            Row rowTo = blankSheet.createRow(i++);
            j = 0;
            for (Cell cell : rowFrom) {
//                Might expand to support all type of cells
                if (cell.getCellType() == Cell.CELL_TYPE_FORMULA)
                    rowTo.createCell(j++).setCellFormula(cell.getCellFormula());
            }
        }
    }

    static void setSheetName(String name, Sheet sheet) {
        Workbook wb = sheet.getWorkbook();
        wb.setSheetName(wb.getSheetIndex(sheet), name);

    }
}
