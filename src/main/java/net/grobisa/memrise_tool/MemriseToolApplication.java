package net.grobisa.memrise_tool;

import net.grobisa.memrise_tool.api.MemriseApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MemriseToolApplication implements CommandLineRunner {

    @Autowired
    private MemriseApi memriseApi;

    public static void main(String[] args) {
        SpringApplication.run(MemriseToolApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        /** when the token expires, request a new one */
//		String sessionToken = memriseApi.authenticate("<user>", "<pass>");
//		System.out.println("session token: " + sessionToken);

        /** If you have a session token, you can use it without having to provide user and password. */
//      memriseApi.authenticate("<session token>");

        /** Example: read words from Begegnungen course, chapter 6 */
//		List<Learnable> learnables = memriseApi.getLevelContents(BEGEGNUNGEN_COURSE_ID, 8);

        /** Add those words to the chapter of another course.
         * You can get the level id from the level HTML source code. Look for 'data-level-id' */
//		memriseApi.addLearnables("11948770", learnables);
    }
}
