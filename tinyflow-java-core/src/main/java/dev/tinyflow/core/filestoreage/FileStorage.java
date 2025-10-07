package dev.tinyflow.core.filestoreage;

import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;

import java.io.InputStream;
import java.util.Map;

public interface FileStorage {

    String saveFile(InputStream stream, Map<String, String> headers, BaseNode node, Chain chain);

}
