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

import com.agentsflex.core.chain.Chain;
import com.agentsflex.core.chain.DataType;
import com.agentsflex.core.chain.Parameter;
import com.agentsflex.core.chain.node.BaseNode;
import com.agentsflex.core.llm.client.OkHttpClientUtil;
import com.agentsflex.core.util.StringUtil;
import com.alibaba.fastjson.JSON;
import okhttp3.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpNode extends BaseNode {

    private String url;
    private String method;

    private List<Parameter> headers;
    private List<Parameter> urlParameters;

    private String bodyDataType;
    private List<Parameter> fromData;
    private List<Parameter> fromUrlencoded;
    private String jsonBody;
    private String rawBody;

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

    public List<Parameter> getUrlParameters() {
        return urlParameters;
    }

    public void setUrlParameters(List<Parameter> urlParameters) {
        this.urlParameters = urlParameters;
    }

    public String getBodyDataType() {
        return bodyDataType;
    }

    public void setBodyDataType(String bodyDataType) {
        this.bodyDataType = bodyDataType;
    }

    public List<Parameter> getFromData() {
        return fromData;
    }

    public void setFromData(List<Parameter> fromData) {
        this.fromData = fromData;
    }

    public List<Parameter> getFromUrlencoded() {
        return fromUrlencoded;
    }

    public void setFromUrlencoded(List<Parameter> fromUrlencoded) {
        this.fromUrlencoded = fromUrlencoded;
    }

    public String getJsonBody() {
        return jsonBody;
    }

    public void setJsonBody(String jsonBody) {
        this.jsonBody = jsonBody;
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }


    @Override
    protected Map<String, Object> execute(Chain chain) {

        Map<String, Object> urlDataMap = getChainParameters(chain, urlParameters);
        String parametersString = mapToQueryString(urlDataMap);
        String newUrl = parametersString.isEmpty() ? url : url +
                (url.contains("?") ? "&" + parametersString : "?" + parametersString);

        Request.Builder reqBuilder = new Request.Builder()
                .url(newUrl);

        Map<String, Object> headersMap = getChainParameters(chain, headers);
        headersMap.forEach((s, o) -> reqBuilder.addHeader(s, String.valueOf(o)));

        if (StringUtil.noText(method) || "GET".equalsIgnoreCase(method)) {
            reqBuilder.method("GET", null);
        } else {
            reqBuilder.method(method.toUpperCase(), getRequestBody(chain));
        }


        OkHttpClient okHttpClient = OkHttpClientUtil.buildDefaultClient();
        try (Response response = okHttpClient.newCall(reqBuilder.build()).execute()) {

            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", response.code());

            Map<String, String> responseHeaders = new HashMap<>();
            for (String name : response.headers().names()) {
                responseHeaders.put(name, response.header(name));
            }
            result.put("headers", responseHeaders);

            ResponseBody body = response.body();
            String bodyString = null;
            if (body != null) bodyString = body.string();

            if (StringUtil.hasText(bodyString)) {
                DataType outDataType = null;
                List<Parameter> outputDefs = getOutputDefs();
                if (outputDefs != null) {
                    for (Parameter outputDef : outputDefs) {
                        if ("body".equalsIgnoreCase(outputDef.getName())) {
                            outDataType = outputDef.getDataType();
                            break;
                        }
                    }
                }

                if (outDataType != null && (outDataType == DataType.Object || outDataType
                        .getValue().startsWith("Array"))) {
                    result.put("body", JSON.parse(bodyString));
                } else {
                    result.put("body", bodyString);
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RequestBody getRequestBody(Chain chain) {
        if ("json".equals(bodyDataType)) {
            return RequestBody.create(jsonBody, MediaType.parse("application/json"));
        }

        if ("x-www-form-urlencoded".equals(bodyDataType)) {
            Map<String, Object> formUrlencodedMap = getChainParameters(chain, fromUrlencoded);
            String bodyString = mapToQueryString(formUrlencodedMap);
            return RequestBody.create(bodyString, MediaType.parse("application/x-www-form-urlencoded"));
        }

        if ("form-data".equals(bodyDataType)) {
            Map<String, Object> formDataMap = getChainParameters(chain, fromData);

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

        if ("raw".equals(bodyDataType)) {
            return RequestBody.create(rawBody, null);
        }
        //none
        return RequestBody.create("", null);
    }

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

    @Override
    public String toString() {
        return "HttpNode{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", headers=" + headers +
                ", parameters=" + urlParameters +
                ", bodyDataType='" + bodyDataType + '\'' +
                ", fromData=" + fromData +
                ", fromUrlencoded=" + fromUrlencoded +
                ", jsonBody='" + jsonBody + '\'' +
                ", rawBody='" + rawBody + '\'' +
                ", description='" + description + '\'' +
                ", parameter=" + urlParameters +
                ", outputDefs=" + outputDefs +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", async=" + async +
                ", inwardEdges=" + inwardEdges +
                ", outwardEdges=" + outwardEdges +
                ", condition=" + condition +
                ", memory=" + memory +
                ", nodeStatus=" + nodeStatus +
                '}';
    }
}
