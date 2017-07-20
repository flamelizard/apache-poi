package rating;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class PriceCalculator {
    private final Float priceVAT;
    private final String tarification;
    private String serviceType;
    private Logger log = Logger.getLogger(PriceCalculator.class.getName());

    public PriceCalculator(Float priceVAT, String tarification, String serviceType) {
        this.priceVAT = priceVAT;
        this.tarification = tarification;
        this.serviceType = serviceType;
        log.setLevel(Level.INFO);
    }

    public static String formatNumberEU(Float number) {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance(Locale.FRENCH);
        return fmt.format(number);
    }

    static Integer applySingleTarific(Integer tariff, Integer seconds) {
        Integer remainder = seconds % tariff;
        if (remainder == 0) {
            return seconds;
        }
        return seconds - remainder + tariff;
    }

    //    made static for unit testing
    static Integer applyTarification(String tarif, Integer seconds) {
        String[] tarifItems = tarif.split("\\+");
        Integer blockA = Integer.valueOf(tarifItems[0].trim());
        Integer blockB = Integer.valueOf(tarifItems[1].trim());

        if (seconds <= blockA) {
            return seconds == 0 ? 0 : blockA;
        }
        return blockA + applySingleTarific(blockB, seconds - blockA);
    }

    public Float getPriceVAT() {
        return priceVAT;
    }

    private Float roundAsRequired(Float number) {
        // force decimal separator to "." for internal operations
        DecimalFormat numFmt = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        numFmt.applyPattern(".###");
        numFmt.setRoundingMode(RoundingMode.HALF_UP);
        return Float.valueOf(numFmt.format(number));
    }

    @Deprecated
    public Float calculatePrice(Integer duration) {
        Float priceNoVAT = roundAsRequired(priceVAT / 1.21f);
        if (Zones.isMessagingService(serviceType))
            return priceNoVAT;
        Float pricePerSec = priceNoVAT / 60;
        Float price = pricePerSec * applyTarification(tarification, duration);
        log.debug(String.format("[calc price %s, %s, %s->%s, %s\n",
                price, pricePerSec, duration,
                applyTarification(tarification, duration),
                price));
        return roundAsRequired(price);
    }

    public String getCallPriceAsFormula(Integer duration) {
        Float priceNoVAT = roundAsRequired(priceVAT / 1.21f);
        if (Zones.isMessagingService(serviceType))
            return priceNoVAT.toString();
        return String.format("round((%f / 60) * %d, 3)",
                priceNoVAT, applyTarification(tarification, duration));
    }


}
