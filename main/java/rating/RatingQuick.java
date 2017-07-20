package rating;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
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

TO DO
simpler zone hints without prefix "D_"
Make it executable JAR + plugin shade
Better logging output??

CHANGE LIST
write price as formula to see base price
Price override - non-blank cell is skipped
Support for all common tarifications
Options to set root dir - "excel.dir" in properties
Include in CSV empty cells when reading Excel rows
Duration set to 1 sec if not supplied
Logging with log4j
Zone functions refactored to a class
Support for SMS/MMS domestic, foreign SMS

NOT IMPLEMENTED
Formatting to highlight match with actual price, only for full match, no
rounding (let user to apply ROUND() func should they wish)
select cenik based on phone number
default duration - iterator that cycles over list of values e.g 1 30 60
customize IN rating explicitly - not worth, only few zones affected

--LESSONS LEARNT--
TDD se hodi i pro zdanlive mensi projekty. Tento projekt se zdal maly a jednoduchy, ale
s casem se stal komplexni a testovani zmen / oprava bugu se stala narocnejsi a pomalejsi bez
unit testu, ktere bych mohl poustet vsechny nebo izolovane pro overeni zmen.

Je dobre navyknout si na TDD jako standard i pro maly vyvoj. Obrovsky to urychli testovani a vyvoj
pro jakykoli netrivialni program.
 */

/**
 * Created by Tom on 1/21/2017.
 */
public class RatingQuick {
    public static final String delimiter = ";";
    private static String propertyFile =
            "D:\\projects\\excel-tools\\src\\main\\test files\\RatingQuick.properties";
    //    private String propertyFile = "RatingQuick.properties";
    private static String rootDir;
    private Properties properties;
    private Logger log = Logger.getLogger(RatingQuick.class.getName());
    private DataFormatter fmt = new DataFormatter();
    private String cenikSheetName = "Zaklad-Rating";
    private String ratingSheetName = "Data";
    private List<String> destToZone = new ArrayList<>();
    private String zonesSheetName = "Operator_Vzorky";
    private boolean debug;
    private boolean overrideCenikPrice = true;

    public RatingQuick() throws IOException {
        loadProperties(propertyFile);
        File f = new File(".");
        System.out.println(f.getAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        RatingQuick rating = new RatingQuick();
        rating.writePricesToTestCases();
    }

    public static List<String> getExcelSheetAsCsv(Path file, String sheetName)
            throws IOException {
        FileInputStream fis = new FileInputStream(file.toString());
        Workbook wb = new XSSFWorkbook(fis);
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        DataFormatter fmt = new DataFormatter();

        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) {
            throw new RatingException("Sheet not found: " + sheetName);
        }
        StringJoiner csv;
        List<String> csvSheet = new ArrayList<>();
        for (Row row : sheet) {
            csv = new StringJoiner(delimiter);
            for (int i = 0; i <= row.getLastCellNum(); i++) {
                csv.add(fmt.formatCellValue(
                        row.getCell(i, Row.RETURN_BLANK_AS_NULL), evaluator)
                        .trim()
                );
            }
            csvSheet.add(csv.toString());
        }
//        excel can be opened while I close it if I use file input stream
        wb.close();
        fis.close();
        return csvSheet;
    }

    public static Path findFileInRootDir(String pattern) throws IOException {
        Path path = Paths.get(rootDir);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
//                get first matching file
                if (fileName.matches(pattern)) {
                    return file;
                }
            }
        }
        throw new RatingException("File not found: " + pattern);
    }

    public static List<Path> findFilesInRootDir(String pattern) throws
            IOException {
        Pattern ptrn = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        List<Path> matchingFiles = new ArrayList<>();
        Path path = Paths.get(rootDir);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                if (ptrn.matcher(fileName).find())
                    matchingFiles.add(file);
            }
        }
        if (matchingFiles.isEmpty())
            throw new RatingException("File not found: " + pattern);
        return matchingFiles;
    }

    public Path getRatingFile() throws IOException {
        return findFileInRootDir(properties.getProperty("pattern.rating"));
    }

    public Path getCenikFile() throws IOException {
        return findFileInRootDir(properties.getProperty("pattern.cenik"));
    }

    public void writePricesToTestCases() throws IOException {
        log.info("Writing price to rating test cases ... ");
        mainProcess();
    }

    public void loadProperties(String propertyFile) throws IOException {
        properties = new Properties();
        File f = new File(propertyFile);
        log.debug("properties: " + f.getAbsolutePath() + " " + f.exists());
        try (FileInputStream fin = new FileInputStream(propertyFile)) {
            properties.load(fin);
        }
        debug = properties.getProperty("debug.mode").equals("1");
        rootDir = properties.getProperty("excel.dir");
        if (rootDir == null)
//            default root dir
            rootDir = Paths.get(propertyFile).getParent().toString();
    }

    private void mainProcess() throws IOException {
        Path ratingFile = getRatingFile();
        FileInputStream fis = new FileInputStream(ratingFile.toString());
        Workbook wb = new XSSFWorkbook(fis);
        Zones zones = new Zones(getCenikFile().toString(), cenikSheetName);
        FormulaEvaluator evaluator = wb.getCreationHelper()
                .createFormulaEvaluator();

        Sheet testCases = wb.getSheet(ratingSheetName);
        Map<String, Integer> testFields = new HashMap<>();
        testFields.put("destination", 3);
        testFields.put("duration", 6);
        testFields.put("expected price", 8);
        testFields.put("test description", 1);
        testFields.put("service type", 2);

        String zone, dest, descr, serviceType;
        Integer duration;
        RatedZone ratedZone;
        for (Row row : testCases) {
            List<String> cells = new ArrayList<>();
            for (int i = 0; i <= row.getLastCellNum(); i++) {
                cells.add(fmt.formatCellValue(row.getCell(i,
                        Row.RETURN_BLANK_AS_NULL)));
            }

            descr = cells.get(testFields.get("test description")).toUpperCase();
            dest = cells.get(testFields.get("destination")).toUpperCase();
            serviceType = cells.get(testFields.get("service type")).toUpperCase();
            if (descr.equals("NO") || dest.matches("^$|^DESTINATION.*")) {
                continue;
            }

            String _duration = cells.get(testFields.get("duration"));
            // default duration 1 sec
            duration = _duration.equals("") ? 1 : Integer.valueOf(_duration);

            zone = lookupZoneForDest(dest);
            if (zone == null) {
                log.warn("Zone not found for " + dest);
                zone = "GENERIC";
            }
            zone = Zones.normalize(zone);
            ratedZone = zones.getRatedZoneOrGeneric(zone, serviceType);

            PriceCalculator priceCalc = new PriceCalculator(
                    Float.valueOf(ratedZone.getPrice()), ratedZone.getTarification(), serviceType);
/*
Price override
Row.getCell() returns null for empty cell. For this case, Cell
Policy will make it create a cell with blank value.
*/
            Cell price = row.getCell(testFields.get("expected price"),
                    Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
//            Price override
            if (fmt.formatCellValue(price).equals("")) {
                price.setCellFormula(priceCalc.getCallPriceAsFormula(duration));
            }
            log.debug(String.format("[test_case %s,%s,%s",
                    row.getRowNum(), ratedZone,
                    fmt.formatCellValue(price, evaluator)));

/*
cannot use float value since it shows as very long number
conversion to string is possible but prohibits conditional formatting of the cell
DataFormat dataFmt = wb.createDataFormat();
CellStyle style = wb.createCellStyle();
style.setDataFormat(dataFmt.getFormat("#.###"));
price.setCellStyle(style);
*/
        }

        if (debug) {
            log.debug("[debug mode - no write");
            for (Row row : wb.getSheet(ratingSheetName)) {
                for (Cell cell : row) {
                    System.out.printf(
                            fmt.formatCellValue(cell) + ", ", evaluator);
                }
                System.out.println();
            }
            return;
        }
        FileOutputStream fos = new FileOutputStream(ratingFile.toString());
        wb.write(fos);
        fos.close();
    }

    private String lookupZoneForDest(String destination) throws IOException {
//        bug in RE - will not match on capital letters with diacritic
        destination = destination.toLowerCase();

        Pattern foreignZoneRE = Pattern.compile(".+_([0-9]{1,2})$");
        Matcher isForeignZone = foreignZoneRE.matcher(destination);
//        foreign zone has zone number in the name, unlike other zones
        if (isForeignZone.find()) {
            return "F_" + isForeignZone.group(1);
        }
        if (destToZone.isEmpty()) {
            Path ratingFile = findFileInRootDir(properties.getProperty("pattern.rating"));
            destToZone = getExcelSheetAsCsv(ratingFile, zonesSheetName);
        }
        Pattern zoneHint = Pattern.compile(
                destination + ".+((D|F)_[0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher hintMatch;
        log.debug("[zone_hint " + zoneHint.pattern());
        for (String row : destToZone) {
            hintMatch = zoneHint.matcher(row.toLowerCase());
            if (hintMatch.find()) {
                return hintMatch.group(1);
            }
        }
        return null;
    }
}
