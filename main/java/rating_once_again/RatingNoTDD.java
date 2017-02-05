package rating_once_again;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
INTERESTING QUESTIONS
How to match for czech alphabet characters? How to deal with non-ASCII chars
in terms of reading and writing to a file?
 */

/**
 * Created by Tom on 1/21/2017.
 */
public class RatingNoTDD {
    private final String cenikPtrn = "(Cenik|Ceník).+";
    private final String delimiter = ";";
    private final List<RatedZone> cenik = new ArrayList<>();
    private final Map<String, String> hintToZone = new HashMap<>();
    private Path sourceFolder = Paths
            .get("D:\\projects\\excel-tools\\src\\main\\resources");
    private DataFormatter fmt = new DataFormatter();
    private String cenikSheetName = "Zaklad-Rating";
    private String ratingPtrn = "Rating.+";
    private String ratingSheetName = "Data";
    private String zoneHints = "zoneHints.csv";
    private List<String> destToZone = new ArrayList<>();
    private String zonesSheetName = "Operator_Vzorky";

    public static void main(String[] args) throws Exception {
        RatingNoTDD rating = new RatingNoTDD();
        rating.run();
    }

    private void run() throws Exception {
        loadCenikFile();
        writePriceToTestCases();
    }

    private void writePriceToTestCases() throws IOException {
        loadZoneHints();
        Path path = findFileByPtrn(ratingPtrn);
        FileInputStream fis = new FileInputStream(path.toString());
        Workbook wb = new XSSFWorkbook(fis);

        Sheet testCases = wb.getSheet(ratingSheetName);
        Map<String, Integer> testFields = new HashMap<>();
        testFields.put("destination", 3);
        testFields.put("duration", 6);
        testFields.put("price", 7);
        testFields.put("test description", 1);

        String zone, dest, descr;
        for (Row row : testCases) {
//            filter valid test cases
            descr = fmt.formatCellValue(
                    row.getCell(testFields.get("test description")))
                    .toLowerCase();
            if (descr.contains("no") || descr.contains("test description"))
                continue;

            dest = fmt.formatCellValue(
                    row.getCell(testFields.get("destination")));
            zone = getZoneForDest(dest);
            if (zone == null) {
                System.out.println("Warning: zone not found for " + dest);
                continue;
            }
            zone = normalizeZoneName(zone);
            RatedZone ratedZone = getRatedZone(zone);
            if (ratedZone == null) {
                System.out.println("Warning: rating not found for " + zone);
                continue;
            }

            Cell price = row.createCell(testFields.get("price"));
            price.setCellValue("bob");

//            System.out.println("[zone " + dest + "->" + zone);
//            System.out.println("[rating " + getRatedZone(zone));

//            NEXT - calc price, save to wb
        }

        FileOutputStream fos = new FileOutputStream(path.toString());
        wb.write(fos);
        fos.close();
    }

    private String normalizeZoneName(String zone) {
        String[] items = zone.split("_");
        String zoneId = items[1];
        if (zoneId.length() < 2) {
            items[1] = String.format("0%s", zoneId);
        }
        zone = items[0] + "_" + items[1].toUpperCase();
        return zone.replace("D_", "DOMESTIC_").replace("F_", "FOREIGN_");
    }

    private RatedZone getRatedZone(String zone) {
        for (RatedZone rated : cenik) {
//            System.out.println("[zz " + rated.getZone() + ", " + zone);
            if (rated.getZone().contains(zone)) {
                return rated;
            }
        }
        return null;
    }

    private String getZoneForDest(String destination) throws IOException {
        Pattern foreignZoneRE = Pattern.compile(".+_([0-9]{1,2})$");
        Matcher isForeignZone = foreignZoneRE.matcher(destination);
//        foreign zone has zone number in the name, unlike other zones
        if (isForeignZone.find()) {
            return "F_" + isForeignZone.group(1);
        }
        if (destToZone.isEmpty()) {
            destToZone = getExcelSheetAsCsv(
                    findFileByPtrn(ratingPtrn), zonesSheetName);
        }
        Pattern zoneHint = Pattern.compile(
                destination + ".+((D|F)_[0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher hintMatch;
        for (String row : destToZone) {
            hintMatch = zoneHint.matcher(row);
            if (hintMatch.find()) {
                return hintMatch.group(1);
            }
        }
        return null;
    }

    private void loadZoneHints() throws IOException {
        Path path = findFileByPtrn(zoneHints);
        List<String> csvHints = Files.readAllLines(path);
        if (csvHints.isEmpty()) {
            throw new FileNotFoundException("File " + zoneHints + " not found");
        }
        String[] tokens;
        for (String line : csvHints) {
            tokens = line.split(delimiter);
            hintToZone.put(tokens[0], tokens[1].trim());
        }
        System.out.println(hintToZone);
    }

    private void loadCenikFile() throws IOException {
        Path path = findFileByPtrn(cenikPtrn);
        List<String> csv = getExcelSheetAsCsv(path, cenikSheetName);
        Pattern itemsPtrn = Pattern.compile(
                "([a-z]+_[0-9]{2}).+?([0-9]+\\+[0-9]+).+?([0-9.,]+) ?" +
                        "(kč|kc)",
                Pattern.CASE_INSENSITIVE);

        for (String csvRow : csv) {
            Matcher match = itemsPtrn.matcher(csvRow);
            if (match.find()) {
//                System.out.printf("[parse %s,%s,%s\n",
//                        match.group(1), match.group(2), match.group(3));
                cenik.add(
                        new RatedZone(match.group(1), match.group(2), match.group(3))
                );
            }
        }
        cenik.forEach((rate) -> System.out.println("[cenik " + rate));
    }

    private List<String> getExcelSheetAsCsv(Path file, String sheetName)
            throws IOException {
        FileInputStream fis = new FileInputStream(file.toString());
        Workbook wb = new XSSFWorkbook(fis);
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            throw new RatingException("Sheet not found: " + sheetName);
        }
        StringJoiner csv;
        List<String> csvSheet = new ArrayList<>();
        for (Row row : sheet) {
            csv = new StringJoiner(delimiter);
            for (Cell cell : row) {
                csv.add(fmt.formatCellValue(cell));
            }
//            System.out.println("[csv " + csv.toString());
            csvSheet.add(csv.toString());
        }
//        excel can be opened while I close it if I use file input stream
        wb.close();
        fis.close();
        return csvSheet;
    }

    private Path findFileByPtrn(String pattern) throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream
                (sourceFolder)) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                if (fileName.matches(pattern)) {
                    return file;
                }
            }
        }
        throw new RatingException("File not found: " + pattern);
    }


}
