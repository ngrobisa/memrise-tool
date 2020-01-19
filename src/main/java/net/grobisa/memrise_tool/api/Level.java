package net.grobisa.memrise_tool.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Level {

    private String urlPath;
    private int index;
    private String title;
    private Integer courseId;

}
