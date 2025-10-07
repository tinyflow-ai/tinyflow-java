package dev.tinyflow.core.llm;

import java.util.ArrayList;
import java.util.List;

public class ChatModelManager {

    public List<ChatModelProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final ChatModelManager INSTANCE = new ChatModelManager();
    }

    private ChatModelManager() {
    }

    public static ChatModelManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(ChatModelProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(ChatModelProvider provider) {
        providers.remove(provider);
    }

    public ChatModel getChatModel(Object modelId) {
        for (ChatModelProvider provider : providers) {
            ChatModel chatModel = provider.getChatModel(modelId);
            if (chatModel != null) {
                return chatModel;
            }
        }
        return null;
    }
}
