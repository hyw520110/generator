package org.hyw.tools.generator.web.service;

import org.hyw.tools.generator.Generator;

@FunctionalInterface
public interface LockedGeneratorAction<T> {

	T apply(Generator generator) throws Exception;
}
