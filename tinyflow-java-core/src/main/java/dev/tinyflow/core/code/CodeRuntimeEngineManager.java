package dev.tinyflow.core.code;

import dev.tinyflow.core.code.impl.JavascriptRuntimeEngine;

import java.util.ArrayList;
import java.util.List;

public class CodeRuntimeEngineManager {

    public List<CodeRuntimeEngineProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final CodeRuntimeEngineManager INSTANCE = new CodeRuntimeEngineManager();
    }

    private CodeRuntimeEngineManager() {
        JavascriptRuntimeEngine javascriptRuntimeEngine = new JavascriptRuntimeEngine();
        providers.add(engineId -> {
            if ("js".equals(engineId) || "javascript".equals(engineId)) {
                return javascriptRuntimeEngine;
            }
            return null;
        });
    }

    public static CodeRuntimeEngineManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(CodeRuntimeEngineProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(CodeRuntimeEngineProvider provider) {
        providers.remove(provider);
    }

    public CodeRuntimeEngine getCodeRuntimeEngine(Object engineId) {
        for (CodeRuntimeEngineProvider provider : providers) {
            CodeRuntimeEngine codeRuntimeEngine = provider.getCodeRuntimeEngine(engineId);
            if (codeRuntimeEngine != null) {
                return codeRuntimeEngine;
            }
        }
        return null;
    }
}
