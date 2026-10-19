package com.mantix;

import org.springframework.boot.SpringApplication;

import com.mantix.MantixBackendApplication;

public class TestMantixBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(MantixBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
