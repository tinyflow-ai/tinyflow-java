package dev.tinyflow.core.knowledge;

import com.agentsflex.core.document.Document;

import java.util.List;

public interface Knowledge {

    List<Document> search(String keyword, int limit);

}
