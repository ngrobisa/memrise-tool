package net.grobisa.memrise_tool.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.grobisa.memrise_tool.exception.MemriseAuthenticationException;
import net.grobisa.memrise_tool.exception.MemriseConnectionException;
import net.grobisa.memrise_tool.exception.MemriseToolException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class MemriseApi {

    private static final String BASE_URL = "https://www.memrise.com";

    private static final int MAX_BULK_ADD_SIZE = 500;

    @Autowired
    private RestTemplate rest;

    private HttpHeaders reqHeaders;

    private String csrfToken;


    public MemriseApi() {
        setUpHeaders();
    }

    private void setUpHeaders() {
        reqHeaders = new HttpHeaders();
        reqHeaders.add("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:61.0) Gecko/20100101 Firefox/61.0");
        reqHeaders.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }


    public String authenticate(String username, String password) {
        log.info("Attempting to log in...");
        ResponseEntity<String> response = rest.exchange("https://www.memrise.com/login/", HttpMethod.GET, null, String.class);
        setCsrfTokenCookieFromResponse(response);

        Document doc = Jsoup.parse(response.getBody());
        Element e = doc.selectFirst("input[name=csrfmiddlewaretoken]");
        String csrfMiddlewareToken = e.attr("value");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("csrfmiddlewaretoken", csrfMiddlewareToken);
        formData.add("username", username);
        formData.add("password", password);
        formData.add("next", "");

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Referer", "https://www.memrise.com/login/");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        response = rest.exchange("https://www.memrise.com/login/", HttpMethod.POST, request, String.class);
        String sessionCookie = getCookie("sessionid_2", response.getHeaders().get("Set-Cookie"));

        if (!response.getStatusCode().equals(HttpStatus.FOUND) || sessionCookie == null) {
            throw new MemriseToolException("Login failed. Check your username and password.");
        }

        log.info("Login successful.");
        String sessionId = extractValueFromCookie(sessionCookie);
        authenticate(sessionId);

        return sessionId;
    }


    private void setCsrfTokenCookieFromResponse(ResponseEntity response) {
        String csrfCookie = getCookie("csrftoken", response.getHeaders().get("Set-Cookie"));
        this.csrfToken = extractValueFromCookie(csrfCookie);
        if (reqHeaders.get("Cookie") != null) {
            reqHeaders.remove("Cookie").stream()
                    .filter(v -> !v.startsWith("csrftoken="))
                    .forEach(e -> reqHeaders.add("Cookie", e));
        }

        reqHeaders.add("Cookie", csrfCookie);
    }

    private String getCookie(String cookieName, List<String> cookies) {
        return cookies == null ? null :
                cookies.stream()
                        .filter(c -> c.startsWith(cookieName + "="))
                        .findFirst()
                        .get();
    }

    private String extractValueFromCookie(String cookie) {
        int beginIdx = cookie.indexOf('=') + 1;
        int endIdx = cookie.indexOf(';');

        return cookie.substring(beginIdx, endIdx);
    }


    public void authenticate(String sessionToken) {
        log.info("Authenticating using session token...");
        reqHeaders = new HttpHeaders();
        reqHeaders.set("Cookie", "sessionid_2=" + sessionToken + "; Secure");

        String url = "https://www.memrise.com/home/";
        reqHeaders.set("Referer", url);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, new HttpEntity(null, reqHeaders), String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new MemriseConnectionException();
        }
        if (response.getBody().contains("id_password")) {
            throw new MemriseAuthenticationException();
        }

        setCsrfTokenCookieFromResponse(response);
        log.info("Authentication successful.");
    }


    public List<Level> getLevels(Integer courseId) {
        String url = BASE_URL + String.format("/course/%d/", courseId);
        HttpEntity requestEntity = new HttpEntity(null, reqHeaders);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new MemriseConnectionException();
        }

        String html = response.getBody();
        System.out.println(html);
        Document doc = Jsoup.parse(html);
        return doc.select("a[href].level").stream()
                .map(e -> Level.builder()
                        .urlPath(e.attr("href"))
                        .index(Integer.valueOf(e.select("div.level-index").text()))
                        .title(e.select("div.level-title").text())
                        .courseId(courseId)
                        .build())
                .collect(toList());
    }


    public Integer getLevelId(Level level) {
        String url = BASE_URL + level.getUrlPath();
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, new HttpEntity(null, reqHeaders), String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new MemriseConnectionException();
        }

        Document doc = Jsoup.parse(response.getBody());
        String levelIdString = doc.select("body[data-level-id]").attr("data-level-id");
        Integer levelId = null;

        try {
            levelId = Integer.parseInt(levelIdString);
        } catch (NumberFormatException e) {
            log.error("Failed to obtain level id for level#{}", level.getIndex());
        }

        return levelId;
    }

    private void createLevel() {

        // TODO: implement
    }


    public List<Learnable> getLevelLearnables(int courseId, int levelIndex) {
        String url = String.format("https://www.memrise.com/ajax/session/?course_id=%s&level_index=%d&session_slug=preview", courseId, levelIndex);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, new HttpEntity(null, reqHeaders), String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new MemriseToolException("Failed to process Memrise response.");
        }
        JsonNode screens = root.get("screens");

        List<Learnable> learnables = new ArrayList<>();
        screens.fields().forEachRemaining(screenField -> {
            String learnableId = screenField.getKey();
            Learnable learnable = unmarshallLearnable(screenField.getValue());
            learnable.setId(learnableId);
            learnables.add(learnable);
        });

        return learnables;
    }

    private Learnable unmarshallLearnable(JsonNode screen) {
        Learnable learnable = new Learnable();

        JsonNode info = screen.get("1");
        learnable.setItem(info.get("item").get("value").textValue());
        learnable.setDefinition(info.get("definition").get("value").textValue());

        if (!info.get("attributes").isEmpty()) {
            info.get("attributes").forEach(attribute -> {
                if ("part of speech".equals(attribute.get("label").textValue().toLowerCase())) {
                    String value = attribute.get("value").textValue();
                    if (value != null) {
                        learnable.setPartOfSpeech(value);
                    }
                }
            });
        }

        if (!info.get("hidden_info").isEmpty()) {
            info.get("hidden_info").forEach(attribute -> {
                if ("plural and inflected forms".equals(attribute.get("label").textValue().toLowerCase())) {
                    String value = attribute.get("value").textValue();
                    if (value != null) {
                        learnable.setPluralAndInflected(value);
                    }
                }
            });
        }

        if (!info.get("audio").isEmpty()) {
            info.get("audio").get("value").forEach(audio -> {
                String value = audio.get("normal").textValue();
                if (value != null) {
                    learnable.getAudios().add(value);
                }
            });
        }

        return learnable;
    }

    public void addLearnables(Integer levelId, List<Learnable> learnables) {
        List<List<Learnable>> batches = Lists.partition(learnables, MAX_BULK_ADD_SIZE);
        int i = 1;
        for (List<Learnable> batch : batches) {
            System.out.println("Adding batch " + i++ + " ...");
            addLearnableBatch(levelId, batch);
        }
    }


    private void addLearnableBatch(Integer levelId, List<Learnable> batch) {
        StringBuilder bulk = new StringBuilder();
        for (int i = 0, n = batch.size(); i < n; i++) {
            Learnable learnable = batch.get(i);

            bulk.append(learnable.getItem());
            bulk.append('\t');
            bulk.append(learnable.getDefinition());
            bulk.append('\t');
            if (learnable.getPluralAndInflected() != null) {
                bulk.append(learnable.getPluralAndInflected());
            }
            bulk.append('\t');
            if (learnable.getPartOfSpeech() != null) {
                bulk.append(learnable.getPartOfSpeech());
            }
            bulk.append('\n');
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("level_id", String.valueOf(levelId));
        params.add("word_delimiter", "tab");
        params.add("data", bulk.toString());

        String url = "https://www.memrise.com/ajax/level/add_things_in_bulk/";
        HttpHeaders headers = createHeaders();
        headers.set("x-csrftoken", csrfToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> reqEntity = new HttpEntity<>(params, headers);
        rest.exchange(url, HttpMethod.POST, reqEntity, String.class);
    }


    private HttpHeaders createHeaders() {
        return new HttpHeaders(new LinkedMultiValueMap(reqHeaders));
    }

}
