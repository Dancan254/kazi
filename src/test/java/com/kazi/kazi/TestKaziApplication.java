package com.kazi.kazi;

import org.springframework.boot.SpringApplication;

public class TestKaziApplication {

	public static void main(String[] args) {
		SpringApplication.from(KaziApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
