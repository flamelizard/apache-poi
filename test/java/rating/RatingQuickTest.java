package rating;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.closeTo;
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

    void validateCallPrices() throws IOException {
        List<String> csv = RatingQuick.getExcelSheetAsCsv(
                Paths.get(rating.getRatingFile().toString()),
                "Data");
        for (String testCase : csv) {
//            System.out.println(testCase);
            if (!testCase.matches("^[0-9]+;[^N].+")) {
                continue;
            }
            String[] cells = testCase.split(";");
//            assertThat(testCase, cells[8], equalTo(cells[10]));
            assertThat(testCase, Double.parseDouble(cells[8]),
                    closeTo(Double.parseDouble(cells[10]), 0.002));
        }
    }

    @Ignore
    public void checkTarificationAlg() {
        System.out.println(PriceCalculator.applySingleTarific(60, 0));
        System.out.println(PriceCalculator.applySingleTarific(60, 30));
        System.out.println(PriceCalculator.applySingleTarific(60, 60));
        System.out.println(PriceCalculator.applySingleTarific(60, 90));
        System.out.println(PriceCalculator.applySingleTarific(60, 120));
        System.out.println(PriceCalculator.applySingleTarific(60, 125));

        System.out.println(PriceCalculator.applySingleTarific(1, 0));
        System.out.println(PriceCalculator.applySingleTarific(1, 30));
        System.out.println(PriceCalculator.applySingleTarific(1, 60));
        System.out.println(PriceCalculator.applySingleTarific(1, 90));
        System.out.println(PriceCalculator.applySingleTarific(1, 120));
        System.out.println(PriceCalculator.applySingleTarific(1, 125));

        System.out.println(PriceCalculator.applySingleTarific(120, 0));
        System.out.println(PriceCalculator.applySingleTarific(120, 30));
        System.out.println(PriceCalculator.applySingleTarific(120, 60));
        System.out.println(PriceCalculator.applySingleTarific(120, 90));
        System.out.println(PriceCalculator.applySingleTarific(120, 120));
        System.out.println(PriceCalculator.applySingleTarific(120, 125));

        Integer[] seconds = new Integer[]{
                0, 1, 30, 60, 61, 90, 91, 120, 121, 150, 250};
        String[] tarif = new String[]{
                "1+1", "60+60", "60+30", "60+1", "120+60"};
        for (String t : tarif) {
            System.out.println("Tarification = " + t);
            for (Integer s : seconds) {
                System.out.println(s + " => " + PriceCalculator.applyTarification(t, s));
            }
        }
    }


















}