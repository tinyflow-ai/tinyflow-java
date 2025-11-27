package dev.tinyflow.core.llm;

import java.util.ArrayList;
import java.util.List;

public class LlmManager {

    public List<LlmProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final LlmManager INSTANCE = new LlmManager();
    }

    private LlmManager() {
    }

    public static LlmManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(LlmProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(LlmProvider provider) {
        providers.remove(provider);
    }

    public Llm getChatModel(Object modelId) {
        for (LlmProvider provider : providers) {
            Llm llm = provider.getChatModel(modelId);
            if (llm != null) {
                return llm;
            }
        }
        return null;
    }
}
