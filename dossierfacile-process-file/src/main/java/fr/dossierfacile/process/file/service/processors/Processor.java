package fr.dossierfacile.process.file.service.processors;

import fr.dossierfacile.common.entity.File;

public interface Processor {
    File process(File dfFile);
}
