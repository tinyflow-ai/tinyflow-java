package dev.tinyflow.core.searchengine;

import dev.tinyflow.core.searchengine.impl.BochaaiSearchEngineImpl;

import java.util.ArrayList;
import java.util.List;

public class SearchEngineManager {

    public List<SearchEngineProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final SearchEngineManager INSTANCE = new SearchEngineManager();
    }

    private SearchEngineManager() {
        BochaaiSearchEngineImpl bochaaiSearchEngine = new BochaaiSearchEngineImpl();
        providers.add(id -> {
            if ("bocha".equals(id) || "bochaai".equals(id)) {
                return bochaaiSearchEngine;
            }
            return null;
        });
    }

    public static SearchEngineManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(SearchEngineProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(SearchEngineProvider provider) {
        providers.remove(provider);
    }

    public SearchEngine geSearchEngine(Object searchEngineId) {
        for (SearchEngineProvider provider : providers) {
            SearchEngine searchEngine = provider.getSearchEngine(searchEngineId);
            if (searchEngine != null) {
                return searchEngine;
            }
        }
        return null;
    }
}
