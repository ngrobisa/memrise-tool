package net.grobisa.memrise_tool.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class MemriseApi {

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

        ResponseEntity<String> response = rest.exchange("https://www.memrise.com/login/", HttpMethod.GET, null, String.class);
        setCsrfTokenCookieFromResponse(response);

        Document doc = Jsoup.parse(response.getBody());
        Element e = doc.selectFirst("input[name=csrfmiddlewaretoken]");
        String csrfMiddlewareToken = e.attr("value");

        reqHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        reqHeaders.set("Referer", "https://www.memrise.com/login/");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("csrfmiddlewaretoken", csrfMiddlewareToken);
        map.add("username", username);
        map.add("password", password);
        map.add("next", "");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, reqHeaders);
        response = rest.exchange("https://www.memrise.com/login/", HttpMethod.POST, entity, String.class);
        String sessionCookie = getCookie("sessionid_2", response.getHeaders().get("Set-Cookie"));
        String sessionId = extractValueFromCookie(sessionCookie);

        return sessionId;
    }


    private void setCsrfTokenCookieFromResponse(ResponseEntity response) {
        String csrfCookie = getCookie("csrftoken", response.getHeaders().get("Set-Cookie"));
        this.csrfToken = extractValueFromCookie(csrfCookie);
        reqHeaders.remove(
                reqHeaders.get("Cookie").stream()
                        .filter(v -> v.startsWith("csrftoken="))
                        .collect(toList())
        );
        reqHeaders.add("Cookie", csrfCookie);
    }

    private String getCookie(String cookieName, List<String> cookies) {
        return cookies.stream()
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
        reqHeaders = new HttpHeaders();
        reqHeaders.set("Cookie", "sessionid_2=" + sessionToken + "; Secure");

        String url = "https://www.memrise.com/home/";
        reqHeaders.set("Referer", url);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, new HttpEntity(null, reqHeaders), String.class);
        setCsrfTokenCookieFromResponse(response);
    }


    public List<Learnable> getLevelContents(String courseId, int levelIndex) throws JsonProcessingException {
        String url = String.format("https://www.memrise.com/ajax/session/?course_id=%s&level_index=%d&session_slug=preview", courseId, levelIndex);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, new HttpEntity(null, reqHeaders), String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.getBody());
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
                    learnable.setAudios(Arrays.asList(value));
                }
            });
        }

        return learnable;
    }

    public void addLearnables(String levelId, List<Learnable> learnables) {
        List<List<Learnable>> batches = Lists.partition(learnables, MAX_BULK_ADD_SIZE);
        int i = 1;
        for (List<Learnable> batch : batches) {
            System.out.println("Adding batch " + i++ + " ...");
            addLearnableBatch(levelId, batch);
        }
    }


    private void addLearnableBatch(String levelId, List<Learnable> batch) {
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
        params.add("level_id", levelId);
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
