package ltweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UteExpress2Application {

	public static void main(String[] args) {
		SpringApplication.run(UteExpress2Application.class, args);
	}

}
