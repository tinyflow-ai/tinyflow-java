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
package dev.tinyflow.core.node;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.DataType;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.filestoreage.FileStorage;
import dev.tinyflow.core.filestoreage.FileStorageManager;
import dev.tinyflow.core.util.OkHttpClientUtil;
import dev.tinyflow.core.util.StringUtil;
import dev.tinyflow.core.util.TextTemplate;
import okhttp3.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpNode extends BaseNode {

    private String url;
    private String method;

    private List<Parameter> headers;

    private String bodyType;
    private List<Parameter> formData;
    private List<Parameter> formUrlencoded;
    private String bodyJson;
    private String rawBody;

    public static String mapToQueryString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (String key : map.keySet()) {
            if (StringUtil.noText(key)) {
                continue;
            }
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }
            stringBuilder.append(key.trim());
            stringBuilder.append("=");
            Object value = map.get(key);
            stringBuilder.append(value == null ? "" : urlEncode(value.toString().trim()));
        }
        return stringBuilder.toString();
    }

    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Parameter> getHeaders() {
        return headers;
    }

    public void setHeaders(List<Parameter> headers) {
        this.headers = headers;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public List<Parameter> getFormData() {
        return formData;
    }

    public void setFormData(List<Parameter> formData) {
        this.formData = formData;
    }

    public List<Parameter> getFormUrlencoded() {
        return formUrlencoded;
    }

    public void setFormUrlencoded(List<Parameter> formUrlencoded) {
        this.formUrlencoded = formUrlencoded;
    }

    public String getBodyJson() {
        return bodyJson;
    }

    public void setBodyJson(String bodyJson) {
        this.bodyJson = bodyJson;
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        int maxRetry = 5;
        long retryInterval = 2000L;

        int attempt = 0;
        Throwable lastError = null;

        while (attempt < maxRetry) {
            attempt++;

            try {
                return doExecute(chain);
            } catch (Throwable ex) {

                lastError = ex;

                // 判断是否需要重试
                if (!shouldRetry(ex)) {
                    throw wrapAsRuntime(ex, attempt);
                }

                try {
                    long waitMs = Math.min(
                            retryInterval * (1L << (attempt - 1)),
                            10_000L // 最大 10 秒
                    );
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("HTTP retry interrupted", ie);
                }
            }
        }

        // 理论上不会走到这里
        throw wrapAsRuntime(lastError, attempt);
    }


    protected boolean shouldRetry(Throwable ex) {
        if (ex instanceof HttpServerErrorException) {
            int code = ((HttpServerErrorException) ex).getStatusCode();
            return code == 503 || code == 504; // 只对特定 5xx 重试
        }

        // 1. IO 异常（超时、连接失败、Socket 问题）
        if (ex instanceof IOException) {
            return true;
        }

        // 2. 包装过的异常
        Throwable cause = ex.getCause();
        return cause instanceof IOException;
    }

    private RuntimeException wrapAsRuntime(Throwable ex, int attempt) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        return new RuntimeException(
                String.format("HttpNode[%s] failed after %d attempt(s)", getName(), attempt),
                ex
        );
    }


    public Map<String, Object> doExecute(Chain chain) throws IOException {

        Map<String, Object> formatParameters = getFormatParameters(chain);
        String newUrl = TextTemplate.of(url).formatToString(formatParameters);

        Request.Builder reqBuilder = new Request.Builder().url(newUrl);

        Map<String, Object> headersMap = chain.getState().resolveParameters(this, headers, formatParameters);
        headersMap.forEach((s, o) -> reqBuilder.addHeader(s, String.valueOf(o)));

        if (StringUtil.noText(method) || "GET".equalsIgnoreCase(method)) {
            reqBuilder.method("GET", null);
        } else {
            reqBuilder.method(method.toUpperCase(), getRequestBody(chain, formatParameters));
        }

        OkHttpClient okHttpClient = OkHttpClientUtil.buildDefaultClient();
        try (Response response = okHttpClient.newCall(reqBuilder.build()).execute()) {

            // 服务器异常
            if (response.code() >= 500 && response.code() < 600) {
                throw new HttpServerErrorException(response.code(), response.message());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", response.code());

            Map<String, String> responseHeaders = new HashMap<>();
            Headers headers = response.headers();
            for (String name : headers.names()) {
                responseHeaders.put(name, response.header(name));
            }
            result.put("headers", responseHeaders);

            ResponseBody body = response.body();
            if (body == null) {
                result.put("body", null);
                return result;
            }

            DataType bodyDataType = null;
            List<Parameter> outputDefs = getOutputDefs();
            if (outputDefs != null) {
                for (Parameter outputDef : outputDefs) {
                    if ("body".equalsIgnoreCase(outputDef.getName())) {
                        bodyDataType = outputDef.getDataType();
                        break;
                    }
                }
            }

            if (bodyDataType == null) {
                result.put("body", body.string());
            } else if (bodyDataType == DataType.Object || bodyDataType.getValue().startsWith("Array")) {
                result.put("body", JSON.parse(body.string()));
            } else if (bodyDataType == DataType.File) {
                try (InputStream stream = body.byteStream()) {
                    FileStorage fileStorage = FileStorageManager.getInstance().getFileStorage();
                    String fileUrl = fileStorage.saveFile(stream, responseHeaders, this, chain);
                    result.put("body", fileUrl);
                }
            } else {
                result.put("body", body.string());
            }
            return result;
        }
    }

    private RequestBody getRequestBody(Chain chain, Map<String, Object> formatArgs) {
        if ("json".equals(bodyType)) {
            String bodyJsonString = TextTemplate.of(bodyJson).formatToString(formatArgs, true);
            JSONObject jsonObject = JSON.parseObject(bodyJsonString);
            return RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));
        }

        if ("x-www-form-urlencoded".equals(bodyType)) {
            Map<String, Object> formUrlencodedMap = chain.getState().resolveParameters(this, formUrlencoded);
            String bodyString = mapToQueryString(formUrlencodedMap);
            return RequestBody.create(bodyString, MediaType.parse("application/x-www-form-urlencoded"));
        }

        if ("form-data".equals(bodyType)) {
            Map<String, Object> formDataMap = chain.getState().resolveParameters(this, formData, formatArgs);

            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            formDataMap.forEach((s, o) -> {
//                if (o instanceof File) {
//                    File f = (File) o;
//                    RequestBody body = RequestBody.create(f, MediaType.parse("application/octet-stream"));
//                    builder.addFormDataPart(s, f.getName(), body);
//                } else if (o instanceof InputStream) {
//                    RequestBody body = new HttpClient.InputStreamRequestBody(MediaType.parse("application/octet-stream"), (InputStream) o);
//                    builder.addFormDataPart(s, s, body);
//                } else if (o instanceof byte[]) {
//                    builder.addFormDataPart(s, s, RequestBody.create((byte[]) o));
//                } else {
//                    builder.addFormDataPart(s, String.valueOf(o));
//                }
                builder.addFormDataPart(s, String.valueOf(o));
            });

            return builder.build();
        }

        if ("raw".equals(bodyType)) {
            String rawBodyString = TextTemplate.of(rawBody).formatToString(formatArgs);
            return RequestBody.create(rawBodyString, null);
        }
        //none
        return RequestBody.create("", null);
    }

    public static class HttpServerErrorException extends IOException {
        private final int statusCode;

        public HttpServerErrorException(int statusCode, String message) {
            super("HTTP " + statusCode + ": " + message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }


    @Override
    public String toString() {
        return "HttpNode{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", headers=" + headers +
                ", bodyType='" + bodyType + '\'' +
                ", formData=" + formData +
                ", formUrlencoded=" + formUrlencoded +
                ", bodyJson='" + bodyJson + '\'' +
                ", rawBody='" + rawBody + '\'' +
                ", parameters=" + parameters +
                ", outputDefs=" + outputDefs +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", condition=" + condition +
                ", validator=" + validator +
                ", loopEnable=" + loopEnable +
                ", loopIntervalMs=" + loopIntervalMs +
                ", loopBreakCondition=" + loopBreakCondition +
                ", maxLoopCount=" + maxLoopCount +
                ", retryEnable=" + retryEnable +
                ", resetRetryCountAfterNormal=" + resetRetryCountAfterNormal +
                ", maxRetryCount=" + maxRetryCount +
                ", retryIntervalMs=" + retryIntervalMs +
                ", computeCostExpr='" + computeCostExpr + '\'' +
                '}';
    }
}
