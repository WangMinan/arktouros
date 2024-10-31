package edu.npu.arktouros.util.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
@Slf4j
public class HttpClientTest {

    static {
        PropertiesProvider.init();
    }

    public static void main(String[] args) throws Exception {
        String ca = PropertiesProvider.getProperty("elasticsearch.ca");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "HaoHao20021118"));
        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setSSLContext(createSSLContext(ca))
                .build();
        HttpResponse httpResponse = httpClient.execute(
                new HttpGet("https://38.147.172.149:9200"));
        InputStream contentStream = httpResponse.getEntity().getContent();
        // 流转字符串
        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[1024];
        int len;
        while ((len = contentStream.read(bytes)) != -1) {
            sb.append(new String(bytes, 0, len));
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(sb.toString());
        String versionNo = jsonNode.get("version").get("number").asText();
        log.info("Elasticsearch version: {}", versionNo);
    }

    protected static SSLContext createSSLContext(String ca) throws Exception {
        log.debug("Ssl config detected. Loading ca certificate from {}", ca);
        Certificate trustedCa;
        if (ca.contains(".crt")) {
            Path caCertificatePath = Paths.get(ca);
            log.info("Ca certificate is a file, trying to resolve");
            try (InputStream is = Files.newInputStream(caCertificatePath)) {
                trustedCa = CertificateFactory.getInstance("X.509").generateCertificate(is);
            }
        } else if (ca.trim().startsWith("-----BEGIN CERTIFICATE-----")){
            log.info("Ca certificate is a string");
            // 说明是一个字符串 需要新建临时文件
            Path tempFile = Files.createTempFile("ca", ".crt");
            Files.writeString(tempFile, ca);
            try (InputStream is = Files.newInputStream(tempFile)) {
                trustedCa = CertificateFactory.getInstance("X.509").generateCertificate(is);
            }
        } else {
            log.error("Unsupported ca certificate format");
            throw new IllegalArgumentException("Unsupported ca certificate format");
        }
        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", trustedCa);
        return SSLContexts.custom().loadTrustMaterial(trustStore, null).build();
    }
}
