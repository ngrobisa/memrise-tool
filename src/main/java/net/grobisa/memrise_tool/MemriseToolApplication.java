package net.grobisa.memrise_tool;

import net.grobisa.memrise_tool.api.Learnable;
import net.grobisa.memrise_tool.api.MemriseApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class MemriseToolApplication implements CommandLineRunner {

	private static final String BEGEGNUNGEN_COURSE_ID = "1703475";
	private static final String BEGEGNUNGEN_CUSTOM_COURSE_ID = "11965886";
	private static final String DUMMY_COURSE_ID = "5556127";

	@Autowired
	private MemriseApi memriseApi;

	public static void main(String[] args) {
		SpringApplication.run(MemriseToolApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		/* get a token (lasts ~2 months) from the set-cookie/sessionid_2 response header on the response
		 	to POST /login */
		memriseApi.authenticate("u7a2r61ykg2nnkq3b554a93b7ztjlpphp");

		// kapitel 6
		List<Learnable> learnables = memriseApi.getLevelContents(BEGEGNUNGEN_COURSE_ID, 6);
		System.out.println("Learnables: " + learnables.size());
		System.out.println(learnables);

		/* get the level id from the level source code. Look for 'data-level-id' */
		memriseApi.addLearnables("11948770", learnables);
    }
}