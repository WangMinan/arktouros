package edu.npu.arktouros.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
// 仅当instance.active.sinker=elasticsearch时，才会加载此配置类
// prefix表示配置前缀 name表示配置名称 havingValue表示配置值
@ConditionalOnProperty(prefix = "instance.active",
        name = "sinker", havingValue = "elasticsearch")
public class ElasticSearchConfig {

    @Value("${elasticsearch.serverUrl}")
    private String serverUrl;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    @Value("${elasticsearch.caPos}")
    private String caPos;

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public ElasticsearchClient getElasticSearchClient() throws Exception {
        validateInputs();

        RestClientBuilder builder = RestClient.builder(HttpHost.create(serverUrl));
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        if (StringUtils.isNotEmpty(caPos)) {
            SSLContext sslContext = createSSLContext();
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setSSLContext(sslContext));
        } else {
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider));
        }

        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));

        return new ElasticsearchClient(transport);
    }

    private void validateInputs() {
        if (StringUtils.isEmpty(serverUrl)) {
            throw new IllegalArgumentException("serverUrl must be set");
        } else if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Username and password must be set");
        }
    }

    private SSLContext createSSLContext() throws Exception {
        Path caCertificatePath = Paths.get(caPos);
        Certificate trustedCa;
        try (InputStream is = Files.newInputStream(caCertificatePath)) {
            trustedCa = CertificateFactory.getInstance("X.509").generateCertificate(is);
        }

        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", trustedCa);

        return SSLContexts.custom().loadTrustMaterial(trustStore, null).build();
    }
}
