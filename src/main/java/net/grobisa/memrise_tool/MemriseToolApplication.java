package net.grobisa.memrise_tool;

import lombok.extern.slf4j.Slf4j;
import net.grobisa.memrise_tool.api.MemriseApi;
import net.grobisa.memrise_tool.exception.MemriseToolException;
import net.grobisa.memrise_tool.service.MemriseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@Slf4j
public class MemriseToolApplication implements CommandLineRunner {

    @Autowired
    private MemriseApi memriseApi;

    @Autowired
    private MemriseService memriseService;


    public static void main(String[] args) {
        new SpringApplicationBuilder(MemriseToolApplication.class)
                .logStartupInfo(false)
                .build()
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {

            /** when the token expires, request a new one */
//		String sessionToken = memriseApi.authenticate("<user>", "<pass>");
//            System.out.println("session token: " + sessionToken);

            /** If you have a session token, you can use it without having to provide user and password. */
//      memriseApi.authenticate("<session token>");

//            memriseApi.getLevels(1703475);

            /** Example: read words from Begegnungen course, chapter 6 */
//		List<Learnable> learnables = memriseApi.getLevelContents(BEGEGNUNGEN_COURSE_ID, 8);

            /** Add those words to the chapter of another course.
             * You can get the level id from the level HTML source code. Look for 'data-level-id' */
//		memriseApi.addLearnables("11948770", learnables);

            memriseService.copyCourse(1703475, 5556127);

        } catch (MemriseToolException e) {
            log.error("Error: {}", e.getMessage());
        }
    }
}
