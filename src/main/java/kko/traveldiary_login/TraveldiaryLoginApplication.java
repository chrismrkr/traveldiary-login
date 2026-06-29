package kko.traveldiary_login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TraveldiaryLoginApplication {

	public static void main(String[] args) {
		SpringApplication.run(TraveldiaryLoginApplication.class, args);
	}

}
