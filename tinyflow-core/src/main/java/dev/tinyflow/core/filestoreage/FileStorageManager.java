/**
 * Copyright (c) 2025-2026, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
