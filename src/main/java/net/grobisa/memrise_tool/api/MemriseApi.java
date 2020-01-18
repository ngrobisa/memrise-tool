package net.grobisa.memrise_tool.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
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
import java.util.List;

@Component
public class MemriseApi {

    private static final int MAX_BULK_ADD_SIZE = 50;

    private HttpHeaders reqHeaders;

    @Autowired
    private RestTemplate rest;




    public void authenticate(String sessionToken) {
        reqHeaders = new HttpHeaders();
        reqHeaders.add("Cookie", "sessionid_2=" + sessionToken);
    }


    public void getLevels() {
        HttpEntity requestEntity = new HttpEntity(null, reqHeaders);
        ResponseEntity<String> response = rest.exchange("https://www.memrise.com/course/1703475/begegnungen-a1/", HttpMethod.GET, requestEntity, String.class);
//        String html = rest.getForObject("https://www.memrise.com/course/1703475/begegnungen-a1/", String.class);
        String html = response.getBody();
        System.out.println(html);
        Document doc = Jsoup.parse(html);
        Elements elements = doc.getElementsByClass("level");
        elements.forEach(x -> System.out.println("Text: " + x.text()));
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
                    learnable.getAudios().add(value);
                }
            });
        }

        return learnable;
    }

    public void addLearnables(String levelId, List<Learnable> learnables) {
        List<List<Learnable>> batches = Lists.partition(learnables, MAX_BULK_ADD_SIZE);
        int i = 1;
        for (List<Learnable> batch: batches) {
            System.out.println("Adding batch " + i++ + " ...");
            addLearnableBatch(levelId, batch);
        }
    }

    private void addLearnableBatch(String levelId, List<Learnable> batch) {
        String url = "https://www.memrise.com/ajax/level/add_things_in_bulk/";
        reqHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        reqHeaders.set("Referer", "https://www.memrise.com/course/5561588/begegnungen-a1-custom/edit/");
        reqHeaders.set("x-csrftoken", "tuCiWdaGTdWxPGoNZ4PFiuhHheNq8Rf5mHCrWJy35WEJJz0s3KEvIg7xHJWRBwMH");

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

        HttpEntity<MultiValueMap<String, String>> reqEntity = new HttpEntity<>(params, reqHeaders);
        ResponseEntity<String> response = rest.exchange(url, HttpMethod.POST, reqEntity, String.class);

        System.out.println("status code: " + response.getStatusCode());
    }

}
