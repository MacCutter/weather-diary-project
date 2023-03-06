package zerobase.weather.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey;

    private final DiaryRepository diaryRepository;  // DiaryService 안에서 diaryRepository를 쓸수있게 해줌
    private final DateWeatherRepository dateWeatherRepository;  // DiaryService 안에서 dateWeatherRepository를 쓸수있게 해줌

    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void saveWeatherDate() {
        logger.info("오늘의 날씨데이터 가져오기");
        dateWeatherRepository.save(getWeatherFromApi());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        logger.info("started to create diary");
        // 날씨 데이터 가져오기 (API or DB에서 가져오기)
        DateWeather dateWeather = getDateWeather(date);

        // 파싱된 데이터+일기 값을 DB 넣기
        Diary nowDiary = new Diary();   // Diary class에 @NoArgsConstructor 어노테이션때문에 사용가(Argument 없는 생성자 생성가능)
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);
        logger.info("end to create diary");
    }

    private DateWeather getWeatherFromApi() {
        // OpenWeatherMap 에서 날씨데이터 가져오기
        String weatherData = getWeatherString();
        // 받아온 날씨 json 파싱하기
        Map<String, Object> parsedWeather = parseWeather(weatherData);  // parseWeather 에서만든 기능으로 weatherData를받아 parsedWeather에 삽입
        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parsedWeather.get("temp"));
        return dateWeather;
    }

    private DateWeather getDateWeather(LocalDate date){
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);
        if (dateWeatherListFromDB.size() == 0) {
            // DB에 없는경우 API에서 새로운 날씨정보 가져오기
            // 과거날씨호출은 유료이니, 현재날씨를 가져와서 일기를 쓰도록 설정
            return getWeatherFromApi();
        } else {
            return dateWeatherListFromDB.get(0);    // List이기때문에 get 사용
        }
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {
//        if(date.isAfter(LocalDate.ofYearDay(2100, 1)))
//            throw new InvalidDate();

        return diaryRepository.findAllByDate(date);
    }

    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    public void updateDiary(LocalDate date, String text) {
        Diary nowDiary = diaryRepository.getFirstByDate(date);  // DB에서 해당날짜의 첫번째 일기만 가져와서 nowDiary 에 넣어줌
        nowDiary.setText(text); // DB의 nowDiary에서 setText에 파라미터text를 넣어서 새로쓴 일기를 넣어 (getText는 가져오는거 주의)
        diaryRepository.save(nowDiary); // 새로썼으니 create때와 같은 save기능으로 일기 저장
    }

    public void deleteDiary(LocalDate date) {
        diaryRepository.deleteAllByDate(date);
    }

    private String getWeatherString() {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;
        try {

            URL url = new URL(apiUrl);  // 스트링형식의 apiUrl을 자바의 url 객체로 만들어줌
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();    // URL객체를 http 형식으로 바꿔줌
            connection.setRequestMethod("GET");     // 요청중에서 GET 방식을 선택할것이다
            int responseCode = connection.getResponseCode(); // 리스폰스값 ex) 200,400  받아올수 있는거
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream())); // 200(OK)인경우
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream())); // 그이외의 실패의 경
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            return response.toString();

        } catch (Exception e) {
            return "failed to get response";
        }

    }

    private Map<String, Object> parseWeather(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;

        try {
            jsonObject = (JSONObject) jsonParser.parse(jsonString); // 최초로 jsonString을 받아 파싱을하고 jsonObject형식으로 jsonObject에 넣어줌
        } catch (ParseException e) {   // 파싱할때 생긴 예외이기 때문에 ParseException
            throw new RuntimeException(e);  // 그냥 Exception을 뱉게 해준다.(예외를 어떻게 처리하는게 아니라 예외가 났다고 보고만 해줌)
        }

        Map<String, Object> resultMap = new HashMap<>();

        JSONObject mainData = (JSONObject) jsonObject.get("main");  // 상위데이터인 main을 가져와서 mainData에 넣어준다. (JSONObject) 형식변환 해줘야 인식됨.
        resultMap.put("temp", mainData.get("temp"));
        JSONArray weatherArray = (JSONArray) jsonObject.get("weather"); // weather 의 데이터 형식이 중괄호아닌 대괄호로 되어있어 Object를 Array로 풀어준다
        JSONObject weatherData = (JSONObject) weatherArray.get(0);  // Object -> Array로 풀어준것의 0번째값(이데이터 에서는)을 넣어줌
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));
        return resultMap;
    }
}
