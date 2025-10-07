package dev.tinyflow.core.knowledge;


import java.util.ArrayList;
import java.util.List;

public class KnowledgeManager {

    public List<KnowledgeProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final KnowledgeManager INSTANCE = new KnowledgeManager();
    }

    private KnowledgeManager() {
    }

    public static KnowledgeManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(KnowledgeProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(KnowledgeProvider provider) {
        providers.remove(provider);
    }

    public Knowledge getKnowledge(Object knowledgeId) {
        for (KnowledgeProvider provider : providers) {
            Knowledge knowledge = provider.getKnowledge(knowledgeId);
            if (knowledge != null) {
                return knowledge;
            }
        }
        return null;
    }
}
