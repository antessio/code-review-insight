package antessio.common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import antessio.common.exception.BadRequestException;
import antessio.common.exception.InternalServerErrorException;
import antessio.common.exception.NotFoundException;


public class HttpClient {

    public record WithHeaders<T>(T responseBody, Map<String, List<String>> headers) {

    }

    private final String uri;
    private final java.net.http.HttpClient httpClient;
    private final ObjectMapperJsonConverter jsonConverter;

    public HttpClient(String uri) {
        this.uri = uri;
        this.httpClient = java.net.http.HttpClient
                .newHttpClient();
        this.jsonConverter = new ObjectMapperJsonConverter();
    }

    public <RESPONSE> RESPONSE get(Class<RESPONSE> clz, String path, Map<String, Object> queryParameters) {
        return get(clz, path, queryParameters, Collections.emptyMap());
    }

    public <RESPONSE> WithHeaders<RESPONSE> getWithHeaders(Class<RESPONSE> clz, String path, Map<String, Object> queryParameters) {
        return getWithHeaders(clz, path, queryParameters, Collections.emptyMap());
    }

    public <RESPONSE> RESPONSE get(Class<RESPONSE> clz, String path, Map<String, Object> queryParameters, Map<String, List<String>> headers) {
        return call(
                this.httpClient,
                HttpRequest.newBuilder()
                           .uri(URI.create(
                                   String.format(
                                           "%s/%s?%s",
                                           uri,
                                           path,
                                           ClientUtils.getQueryParameters(queryParameters))))
                           .headers(flattenMap(headers))
                           .GET()
                           .build(),
                responseBodyStr -> jsonConverter.fromJson(responseBodyStr, clz));
    }

    public <RESPONSE> WithHeaders<RESPONSE> getWithHeaders(
            Class<RESPONSE> clz,
            String path,
            Map<String, Object> queryParameters,
            Map<String, List<String>> headers) {
        return callWithHeaders(
                this.httpClient,
                HttpRequest.newBuilder()
                           .uri(URI.create(
                                   String.format(
                                           "%s/%s?%s",
                                           uri,
                                           path,
                                           ClientUtils.getQueryParameters(queryParameters))))
                           .headers(flattenMap(headers))
                           .GET()
                           .build(),
                responseBodyStr -> jsonConverter.fromJson(responseBodyStr, clz));
    }


    public <REQUEST, RESPONSE> RESPONSE post(
            Class<RESPONSE> responseClass,
            String path,
            REQUEST request) {
        HttpRequest.BodyPublisher bodyRequest = HttpRequest.BodyPublishers.ofString(
                jsonConverter.toJson(request)
        );
        return call(this.httpClient, HttpRequest.newBuilder()
                                                .uri(URI.create(
                                                        String.format(
                                                                "%s/%s",
                                                                uri, path)))
                                                .header("Content-Type", "application/json")
                                                .POST(bodyRequest)
                                                .build(),
                    responseStr -> jsonConverter.fromJson(responseStr, responseClass));

    }

    private <T> T call(
            java.net.http.HttpClient httpClient,
            HttpRequest request,
            Function<String, T> converter) {
        try {
            HttpResponse<String> httpResponseStr = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponseStr.statusCode() < 200 || httpResponseStr.statusCode() >= 300) {
                throwException(httpResponseStr);
            }
            return converter.apply(httpResponseStr.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> WithHeaders<T> callWithHeaders(
            java.net.http.HttpClient httpClient,
            HttpRequest request,
            Function<String, T> converter) {
        try {
            HttpResponse<String> httpResponseStr = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponseStr.statusCode() < 200 || httpResponseStr.statusCode() >= 300) {
                throwException(httpResponseStr);
            }
            return new WithHeaders<>(converter.apply(httpResponseStr.body()), httpResponseStr.headers().map());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void throwException(HttpResponse<String> httpResponseStr) {
        switch (httpResponseStr.statusCode()) {
            case 400:
                throw new BadRequestException(httpResponseStr.body());
            case 404:
                throw new NotFoundException(httpResponseStr.body());
            case 500:
                throw new InternalServerErrorException(httpResponseStr.body());
            default:
                throw new RuntimeException("error %d body = %s".formatted(httpResponseStr.statusCode(), httpResponseStr.body()));
        }
    }

    private String[] flattenMap(Map<String, List<String>> m) {
        return m.entrySet().stream().flatMap(x -> x.getValue().stream().flatMap(y -> Stream.of(x.getKey(), y)))
                .toArray(String[]::new);
    }

}
