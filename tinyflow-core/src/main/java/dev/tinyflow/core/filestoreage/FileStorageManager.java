package dev.tinyflow.core.filestoreage;

import java.util.ArrayList;
import java.util.List;

public class FileStorageManager {

    public List<FileStorageProvider> providers = new ArrayList<>();

    private static class ManagerHolder {
        private static final FileStorageManager INSTANCE = new FileStorageManager();
    }

    private FileStorageManager() {
    }

    public static FileStorageManager getInstance() {
        return ManagerHolder.INSTANCE;
    }

    public void registerProvider(FileStorageProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(FileStorageProvider provider) {
        providers.remove(provider);
    }

    public FileStorage getFileStorage() {
        for (FileStorageProvider provider : providers) {
            FileStorage fileStorage = provider.getFileStorage();
            if (fileStorage != null) {
                return fileStorage;
            }
        }
        return null;
    }
}
