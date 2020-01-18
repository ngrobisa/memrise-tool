package net.grobisa.memrise_tool.api;

import java.util.ArrayList;
import java.util.List;

public class Learnable {

    private String id;
    private String item;
    private String definition;
    private String pluralAndInflected;
    private String partOfSpeech;
    private List<String> audios = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getPluralAndInflected() {
        return pluralAndInflected;
    }

    public void setPluralAndInflected(String pluralAndInflected) {
        this.pluralAndInflected = pluralAndInflected;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }
    
    public List<String> getAudios() {
        return audios;
    }

    @Override
    public String toString() {
        return "Learnable{" +
                "item='" + item + '\'' +
                ", definition='" + definition + '\'' +
                ", pluralAndInflected='" + pluralAndInflected + '\'' +
                ", partOfSpeech='" + partOfSpeech + '\'' +
                ", audios=" + audios +
                '}';
    }
}
