package net.grobisa.memrise_tool.api;

import lombok.Data;

import java.util.List;


@Data
public class Learnable {

    private String id;
    private String item;
    private String definition;
    private String pluralAndInflected;
    private String partOfSpeech;
    private List<String> audios;

}
