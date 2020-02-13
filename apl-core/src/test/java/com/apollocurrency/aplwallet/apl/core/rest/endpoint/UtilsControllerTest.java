/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.endpoint;

import com.apollocurrency.aplwallet.api.dto.QrDecodeDto;
import com.apollocurrency.aplwallet.api.dto.QrEncodeDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class UtilsControllerTest {

    private static ObjectMapper mapper = new ObjectMapper();
    private Dispatcher dispatcher;
    private static String encodeQrUri = "/utils/qrcode/encode";
    private static String decodeQrUri = "/utils/qrcode/decode";
//    private AnService service;

    @BeforeEach
    void setup(){
        dispatcher = MockDispatcherFactory.createDispatcher();
//        service = mock(AnService.class);
        UtilsController controller = new UtilsController();
        dispatcher.getRegistry().addSingletonResource(controller);
    }

    @Test
    void encodeQrCode_SUCCESS() throws URISyntaxException, IOException {
//        doReturn(data).when(service).method();
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "123456");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        QrEncodeDto qrCodeDto = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(qrCodeDto.qrCodeBase64);
        assertTrue(qrCodeDto.qrCodeBase64.length() > 0);
    }

    @Test
    void encodeQrCode_NO_QrCodeData() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "")
            .addFormHeader("width", "100").addFormHeader("height", "100");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<Error>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2003, error.getNewErrorCode());
    }

    @Test
    void encodeQrCode_incorrect_width() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "1234")
            .addFormHeader("width", "S_DF_123").addFormHeader("height", "100");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2011, error.getNewErrorCode());
    }

    @Test
    void encodeQrCode_incorrect_height() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "1234")
            .addFormHeader("width", "100").addFormHeader("height", "incorrect");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2011, error.getNewErrorCode());
    }

    @Test
    void encodeQrCode_height_outOfRange() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(encodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeData", "1234")
            .addFormHeader("width", "100").addFormHeader("height", "-1");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2011, error.getNewErrorCode());
    }

    @Test
    void decodeQrCode_SUCCESS() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.post(decodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeBase64", "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAVABUDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwCPw94evYdS0l30aNIETShFCvhyWK4guEltTO7zm3UYwk5JMhBDflb0bQXtdDe1jsb670KGW3ha3uLVp5Csd7bSzIBFEBNGC85/egSAl0EcZSXdYuNO8P61qOm7tK8/QJvs1taS3NteKYY2KwRqkz2hBjaPYVXzFAkmlbcW8uROM0HQ/Ec1xJqvhXTo7SC7uJYVury7+y+StwSlvO0aFUScJO4UR7vldCsQ3I0gBn+K/CPiG50bS4o9CkvdQjuLhp5dL8OzWiiIrD5at/o8W47llPQ4z15orY8S6Jp8llaabqHh6OK80q4ms5ruPXreyW4ISFwqtdR+bIiK6qgIIVNmHcHNFAGP4R8V6bc+L9Gji0m7j1C8fS9Olna+VogsE1t8yx+UCCfs69XONx61sXZhu9B8O3ttHJYaTdWWrXunabZuITaXVrGxSeSVADO42gKxCsAq5LneXKKAN/w5oVz44hTSLS+tLFNGsrOXzL3SLS/My3EKvHndGpV44ljiLZO8RqcLjBKKKAP/2Q==");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        QrDecodeDto qrCodeDto = mapper.readValue(respondJson, new TypeReference<>(){});
        assertNotNull(qrCodeDto.qrCodeData);
        assertTrue(qrCodeDto.qrCodeData.length() > 0);
        assertEquals("1", qrCodeDto.qrCodeData);
    }

    @Test
    void decodeQrCode_NO_QrCodeData() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(decodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeBase64", null);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2003, error.getNewErrorCode());
    }

    @Test
    void decodeQrCode_EMPTY_QrCodeData() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(decodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeBase64", "");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(2003, error.getNewErrorCode());
    }

    @Test
    void decodeQrCode_incorrectBase64_QrCodeData() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(decodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeBase64", "incorrect_base64");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(1000, error.getNewErrorCode());
    }

    @Test
    void decodeQrCode_incorrect_QrCodeData() throws Exception {
        MockHttpRequest request = MockHttpRequest.post(decodeQrUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            .addFormHeader("qrCodeBase64", "MQ=="); // correct base64, but incorrect QR
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(200, response.getStatus());
        String respondJson = response.getContentAsString();
        Error error = mapper.readValue(respondJson, new TypeReference<>(){});

        assertEquals(200, response.getStatus());
        assertNotNull(error.getErrorDescription());
        assertEquals(1000, error.getNewErrorCode());
    }

}