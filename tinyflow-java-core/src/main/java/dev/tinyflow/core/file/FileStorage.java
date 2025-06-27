package dev.tinyflow.core.file;

import java.io.InputStream;
import java.util.Map;

public interface FileStorage {

    String saveFile(InputStream stream, Map<String, String> headers);

}
