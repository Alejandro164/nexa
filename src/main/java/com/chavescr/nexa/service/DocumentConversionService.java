package com.chavescr.nexa.service;

import java.nio.file.Path;

public interface DocumentConversionService {

    Path convertirAPdf(Path archivoOriginal, Path rutaSalida);
}
