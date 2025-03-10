package com.notifysync.notifysync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class NotifySyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotifySyncApplication.class, args);
	}

}
