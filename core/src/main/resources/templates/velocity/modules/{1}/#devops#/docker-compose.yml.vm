version: '3'
services:
  ${projectName}:
    image: ${projectName}:latest
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    restart: always
