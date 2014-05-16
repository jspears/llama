package com.speajus.llama.file;

import java.nio.file.Path;

public interface TriggerStrategy {

	Path resolve(Path path);
}
