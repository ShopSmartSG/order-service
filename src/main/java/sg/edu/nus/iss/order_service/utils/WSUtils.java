package sg.edu.nus.iss.order_service.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import sg.edu.nus.iss.order_service.exception.CustomerResponseErrorHandler;
import sg.edu.nus.iss.order_service.model.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class WSUtils extends Constants{
    private static final Logger log = LoggerFactory.getLogger(WSUtils.class);
    private final ObjectMapper mapper = Json.mapper();

    private final RestTemplateBuilder restTemplateBuilder;

    @Autowired
    public WSUtils(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public RestTemplate restTemplateSync(long connectTimeout, long readTimeout) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().set("Transfer-Encoding", "chunked");
                    return execution.execute(request, body);
                })
                .errorHandler(new CustomerResponseErrorHandler())
                .build();
    }

    public Response makeWSCallObject(String url, JsonNode data, Map<String, String> headers, HttpMethod method,
                                                        long connectTimeout, long readTimeout) {
        Response resp = new Response();
        ObjectNode responseData = mapper.createObjectNode();
        log.info("ObjectWS :: Handling request for url: {}", url);
        RestTemplate restTemplate = restTemplateSync(connectTimeout, readTimeout);

        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));

        HttpEntity<JsonNode> request;
        if (data != null && !data.isNull() && !data.isEmpty()) {
            request = new HttpEntity<>(data, httpHeaders);
        } else {
            request = new HttpEntity<>(httpHeaders);
        }
        log.info("ObjectWS :: Making:: rest {} url call for {}, with request data: {}", method, url, data);
        try{
            ResponseEntity<?> response = restTemplate.exchange(url, method, request, Object.class);
            if(response.getBody()!=null){
                if(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED
                        || response.getStatusCode() == HttpStatus.ACCEPTED){
                    log.info("ObjectWS :: Success:: rest {} url call for {} gave status : {}", method, url, response.getStatusCode());
                    resp.setStatus(SUCCESS);
                }else{
                    log.error("ObjectWS :: Failed:: rest {} url call for {} with status code: {} and error {}", method, url,
                            response.getStatusCode(), response.getBody());
                    resp.setStatus(FAILURE);
                    resp.setMessage(response.getStatusCode().toString());
                }
                if (response.getHeaders().getContentType() != null &&
                        response.getHeaders().getContentType().includes(MediaType.TEXT_PLAIN)) {
                    try {
                        responseData.put(MESSAGE, response.getBody().toString());
                        resp.setData(responseData);
                    } catch (Exception e) {
                        log.error("ObjectWS :: Failed:: to parse text/plain response body for the url {} with error: ", url, e);
                        responseData.put(MESSAGE, "Failed to resolve url ".concat(url).concat(" with response: ").concat(String.valueOf(resp)));
                        resp.setData(responseData);
                    }
                    return resp;
                } else if (response.getBody() instanceof JsonNode || response.getBody() instanceof ArrayNode
                        || response.getBody() instanceof ObjectNode) {
                    resp.setData((JsonNode) response.getBody());
                    return resp;
                } else if (response.getBody() instanceof ArrayList || response.getBody() instanceof Map) {
                    resp.setData(mapper.convertValue(response.getBody(), JsonNode.class));
                    return resp;
                } else if (response.getBody() instanceof String) {
                    try {
                        responseData.set(MESSAGE, mapper.readTree((String) response.getBody()));
                        resp.setData(responseData);
                        return resp;
                    } catch (Exception e) {
                        log.error("ObjectWS :: Failed:: to parse response body for the url {} with error: ", url, e);
                        responseData.put(MESSAGE, "Failed to resolve url ".concat(url).concat(" with response: ").concat(String.valueOf(resp)));
                        resp.setData(responseData);
                        return resp;
                    }
                } else {
                    log.error("ObjectWS :: Exception:: Unexpected response body type: {} for url {}", response.getBody().getClass(), url);
                    responseData.put(MESSAGE, "Exception occurred due to unidentified body type for url ".concat(url)
                            .concat(" with response: ").concat(String.valueOf(resp)));
                    resp.setData(responseData);
                    return resp;
                }
            } else {
                responseData.put(MESSAGE, "No response body found for url ".concat(url).concat(" with response : ").concat(String.valueOf(resp)));
                resp.setData(responseData);
                return resp;
            }
        }catch(Exception ex){
            log.error("ObjectWS :: Failed:: rest {} url call for {} with error: {}", method, url, ex);
            resp.setStatus(FAILURE);
            resp.setMessage(ex.getMessage());
            return resp;
        }
    }

    public Response makeWSCallString(String url, JsonNode data, Map<String, String> headers, HttpMethod method,
                                                        long connectTimeout, long readTimeout) {
        Response resp = new Response();
        ObjectNode responseData = mapper.createObjectNode();
        log.info("StringWS :: Handling request for url: {}", url);
        RestTemplate restTemplate = restTemplateSync(connectTimeout, readTimeout);

        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));

        HttpEntity<JsonNode> request;
        if (data != null && !data.isNull() && !data.isEmpty()) {
            request = new HttpEntity<>(data, httpHeaders);
        } else {
            request = new HttpEntity<>(httpHeaders);
        }
        log.info("StringWS :: Making:: rest {} url call for {}, with request data: {}", method, url, data);
        ResponseEntity<String> response = restTemplate.exchange(url, method, request, String.class);
        try{
            if(response.getBody()!=null){
                if(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED
                        || response.getStatusCode() == HttpStatus.ACCEPTED){
                    log.info("StringWS ::Success:: rest {} url call for {} gave status : {}", method, url, response.getStatusCode());
                    resp.setStatus(SUCCESS);
                }else{
                    log.error("StringWS ::Failed:: rest {} url call for {} with status code: {} and error {}", method, url,
                            response.getStatusCode(), response.getBody());
                    resp.setStatus(FAILURE);
                    resp.setMessage(response.getStatusCode().toString());
                }
                if (response.getHeaders().getContentType() != null &&
                        response.getHeaders().getContentType().includes(MediaType.TEXT_PLAIN)) {
                    try {
                        log.debug("StringWS ::Success :: rest {} url call for {} gave status : {} when matched with plain test", method, url, response.getStatusCode());
                        responseData.put(MESSAGE, response.getBody());
                        resp.setData(responseData);
                    } catch (Exception e) {
                        log.error("StringWS :: Failed:: to parse text/plain response body for the url {} with error: ", url, e);
                        responseData.put(MESSAGE, "Failed to resolve url ".concat(url).concat(" with response: ").concat(String.valueOf(resp)));
                        resp.setData(responseData);
                    }
                    return resp;
                } else if (response.getBody() instanceof String) {
                    try {
                        log.debug("StringWS ::Success :: rest {} url call for {} gave status : {} when instanceof string.", method, url, response.getStatusCode());
                        responseData.put(MESSAGE, response.getBody());
                        resp.setData(responseData);
                        return resp;
                    } catch (Exception e) {
                        log.error("StringWS :: Failed:: to parse response body for the url {} with error: ", url, e);
                        responseData.put(MESSAGE, "Failed to resolve url ".concat(url).concat(" with response: ").concat(String.valueOf(resp)));
                        resp.setData(responseData);
                        return resp;
                    }
                } else {
                    log.error("StringWS :: Exception:: Unexpected response body type: {} for url {}", response.getBody().getClass(), url);
                    responseData.put(MESSAGE, "Exception occurred due to unidentified body type for url ".concat(url)
                            .concat(" with response: ").concat(response.getBody().toString()));
                    resp.setData(responseData);
                    return resp;
                }
            } else {
                responseData.put(MESSAGE, "No response body found for url ".concat(url).concat(" with response: ").concat(response.toString()));
                resp.setData(responseData);
                return resp;
            }
        }catch(Exception ex){
            log.error("StringWS :: Failed:: rest {} url call for {} with error: {}", method, url, ex);
            resp.setStatus(FAILURE);
            resp.setMessage(ex.getMessage());
            return resp;
        }
    }
}
