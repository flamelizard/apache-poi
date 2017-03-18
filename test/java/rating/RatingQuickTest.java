package rating;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by sedlakto on 21.2.2017.
 */
public class RatingQuickTest {
    RatingQuick rating;

    @Before
    public void init() throws Exception {
        rating = new RatingQuick();
        rating.loadProperties(
                "D:\\projects\\excel-tools\\src\\main\\test files\\unittest\\Prop_60+60.properties");
    }

    @Test
    public void verifyRating_60plus60() throws Exception {
        rating.writePricesToTestCases();
        validateCallPrices();
    }

    @Test
    public void validateCallPrices() throws IOException {
        List<String> csv = RatingQuick.getExcelSheetAsCsv(
                Paths.get(rating.getRatingFile().toString()),
                "Data");
        for (String testCase : csv) {
//            System.out.println(testCase);
            if (!testCase.matches("^[0-9]+;[^N].+")) {
                continue;
            }
            String[] cells = testCase.split(";");
            assertThat(testCase, cells[8], equalTo(cells[10]));
        }
    }

//    @Test
//    public void findingCenikFiles() throws IOException {
//        List<Path> files = RatingQuick.findFilesInRootDir("Cen.+");
//        System.out.println("[all cenik " + files);
//    }

    @Test
    public void checkPathBehaviour() {
        Path p = Paths.get("D:\\projects\\excel-tools\\src\\main\\test " +
                "files\\unittest\\Prop_60+60.properties");
        System.out.println("full " + p);
        System.out.println("is file " + Files.isRegularFile(p));
        System.out.println("parent " + p.getParent());
        System.out.println("root " + p.getRoot());
    }

}