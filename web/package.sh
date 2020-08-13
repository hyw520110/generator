#!/bin/sh

yarn install
yarn run build

rm -rf ./src/main/resources/static/*
mv ./dist/* ./src/main/resources/static/

mvn clean package