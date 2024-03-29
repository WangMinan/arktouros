package edu.npu.arktouros.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * @author : [wangminan]
 * @description : ElasticSearch配置类
 */
@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.serverUrl}")
    private String serverUrl;

    @Value("${elasticsearch.apiKey}")
    private String apiKey;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    @Value("${elasticsearch.caPos}")
    private String caPos;

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public ElasticsearchClient getElasticSearchClient()
            throws NoSuchAlgorithmException, KeyManagementException,
            CertificateException, KeyStoreException, IOException {
        if (StringUtils.isEmpty(serverUrl)) {
            throw new IllegalArgumentException("serverUrl must be set");
        } else if (StringUtils.isEmpty(apiKey) && (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))) {
            throw new IllegalArgumentException("apiKey or username and password must be set");
        }
        RestClientBuilder builder = RestClient
                .builder(HttpHost.create(serverUrl));
        if (StringUtils.isNotEmpty(caPos)) {
            Path caCertificatePath = Paths.get(caPos);
            CertificateFactory factory =
                    CertificateFactory.getInstance("X.509");
            Certificate trustedCa;
            try (InputStream is = Files.newInputStream(caCertificatePath)) {
                trustedCa = factory.generateCertificate(is);
            }
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null);
            final SSLContext sslContext = sslContextBuilder.build();
            if (StringUtils.isNotEmpty(apiKey)) {
                builder.setDefaultHeaders(new Header[]{
                                new BasicHeader("Authorization", "ApiKey " + apiKey)
                        })
                        .setHttpClientConfigCallback(httpClientBuilder ->
                                httpClientBuilder.setSSLContext(sslContext));
            } else {
                final CredentialsProvider credentialsProvider =
                        new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                builder.setHttpClientConfigCallback(
                        httpClientBuilder ->
                                httpClientBuilder
                                        .setDefaultCredentialsProvider(credentialsProvider)
                                        .setSSLContext(sslContext));
            }
        } else {
            // ssl not enabled
            if (StringUtils.isNotEmpty(apiKey)) {
                builder.setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                });
            } else {
                final CredentialsProvider credentialsProvider =
                        new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                builder.setHttpClientConfigCallback(
                        httpClientBuilder ->
                                httpClientBuilder
                                        .setDefaultCredentialsProvider(credentialsProvider));
            }

        }
        RestClient restClient = builder.build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(objectMapper));

        // And create the API client
        return new ElasticsearchClient(transport);
    }
}
