import com.opencsv.CSVWriter;
import org.json.JSONArray;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

public class Main {
    public static List<String> date = new ArrayList<>();
    public static List<BigDecimal> value = new ArrayList<>();
    public static List<BigDecimal> difference = new ArrayList<>();

    public static void main(String[] args) {
        //day start: 2004-01-01
        Calendar calendar = new GregorianCalendar(2004, 1, 0); //first month in year - 0, last - 11
        //day end: TODAY
        Calendar today = Calendar.getInstance();
        while(calendar.before(today)) {
            getInfoFromJSON(createURL(calendar.getTime().getYear()+1900, calendar.getTime().getMonth()+1));
            //getYear()  returns value like: (this year - 1900)
            //getMonth() returns value like: (this month - 1)
            calendar.add(Calendar.MONTH, 1);
        }
        writeDataForCustomSeparatorCSV("./result.csv");
    }

    //get string value of Json by URL
    public static String getJSON(String url) {
        HttpsURLConnection con = null;
        try {
            URL u = new URL(url);
            con = (HttpsURLConnection) u.openConnection();

            con.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();


        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void getInfoFromJSON(String url) {
        String str = Objects.requireNonNull(getJSON(url));
        JSONArray jsonArray = new JSONArray(str);
        JSONArray filteredJSONArray = new JSONArray();

        //filter jsonArray
        IntStream.range(0, jsonArray.length())
                .filter(index -> jsonArray.getJSONObject(index).get("id_api").equals("RES_OffReserveAssets"))
                .forEach(index -> filteredJSONArray.put(jsonArray.get(index)));

        if (date.size() == 0) {
            date.add(filteredJSONArray.getJSONObject(0).getString("dt"));
            value.add(filteredJSONArray.getJSONObject(0).getBigDecimal("value"));
            difference.add(new BigDecimal(0)); //first value = 0
        } else {
            date.add(filteredJSONArray.getJSONObject(0).getString("dt"));
            value.add(filteredJSONArray.getJSONObject(0).getBigDecimal("value"));
            difference.add(filteredJSONArray.getJSONObject(0).getBigDecimal("value").subtract(value.get(value.size() - 2)));
        }
    }

    public static String createURL(int year, int month) {
        String begin = "https://bank.gov.ua/NBUStatService/v1/statdirectory/res?date=";
        String date;
        if (month < 10)
            date = year + "0" + month;
        else date = year + "" + month;
        String end = "&json";
        return begin + date + end;
    }

    public static void writeDataForCustomSeparatorCSV(String filePath) {
        File file = new File(filePath);
        try {
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile, ',',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            List<String[]> data = new ArrayList<>();
            data.add(new String[]{"Month", "Value", "Difference"});

            while (date.size() > 0) {
                data.add(new String[]{
                        formatDate(date.get(0)), value.get(0).toString(), difference.get(0).toString()
                });
                date.remove(0);
                value.remove(0);
                difference.remove(0);
            }
            writer.writeAll(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //output format: YYYY-MM-DD   (2021-05-01)
    public static String formatDate(String date) {
        String year = date.substring(0, 4);
        String month = date.substring(4, 6);
        String day = "01";
        return year + "-" + month + "-" + day;
    }
}