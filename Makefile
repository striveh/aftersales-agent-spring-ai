.PHONY: test package up eval local

test:
	mvn -B test

package:
	mvn -B package

up:
	docker compose up --build

local:
	mvn -pl aftersales-agent -am spring-boot:run

eval:
	mvn -B -pl aftersales-eval -am package -DskipTests
	java -jar aftersales-eval/target/aftersales-eval-0.1.0-exec.jar
