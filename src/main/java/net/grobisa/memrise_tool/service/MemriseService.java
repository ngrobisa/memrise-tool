package net.grobisa.memrise_tool.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.grobisa.memrise_tool.api.Learnable;
import net.grobisa.memrise_tool.api.Level;
import net.grobisa.memrise_tool.api.MemriseApi;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class MemriseService {

    private final MemriseApi memriseApi;


    // TODO: finish
    public void copyCourse(Integer sourceCourseId, Integer targetCourseId) {
        List<Level> sourceCourseLevels = memriseApi.getLevels(sourceCourseId);
        List<Level> targetCourseLevels = memriseApi.getLevels(targetCourseId);
        log.info("Found {} levels for course {}", sourceCourseLevels.size(), sourceCourseId);
//        for (Level level: levels) {
//            copyCourseLevel(sourceCourseId, level, targetCourseId);
//            break;
//        }
        int i = 1;
        Level level = sourceCourseLevels.get(i);

        // target: [0]

        if (i >= targetCourseLevels.size()) {
            // create target level
        }

        copyCourseLevel(sourceCourseLevels.get(1),
                targetCourseLevels.size() >= 1? targetCourseLevels.get(1): null);

    }


    private void createTargetLevelIfNecessary(List<Level> targetLevels, int levelIndex) {




    }

    private void copyCourseLevel(Level sourceLevel, Level targetLevel) {

//        log.info("Copying level#{} ({})", level.getIndex(), level.getTitle());
//        List<Learnable> targetLevelLearnables = memriseApi.getLevelLearnables(targetCourseId, level.getIndex());
//
//        if (!targetLevelLearnables.isEmpty()) {
//            log.warn("Destination course already has contents on level#{}. Skipping this level.", level.getIndex());
//        }
//
//        List<Learnable> sourceLevelLearnables = memriseApi.getLevelLearnables(sourceCourseId, level.getIndex());

//        memriseApi.addLearnables();


//        memriseApi.addLearnables(levelId, learnables);
    }
}
