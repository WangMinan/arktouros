package edu.npu.arktouros.util.elasticsearch.pool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import edu.npu.arktouros.config.LocalDateTimeDeserializer;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author : [wangminan]
 * @description : Elasticsearch连接池工厂
 */
@Slf4j
public class ElasticsearchClientPoolFactory implements PooledObjectFactory<ElasticsearchClient> {

    private final String serverUrl;

    private final String username;

    private final String password;

    private final String ca;

    private final int maxTotal;

    private final int maxWait;

    private final int minIdle;

    private final ObjectMapper mapper;

    public ElasticsearchClientPoolFactory() {
        this.serverUrl = PropertiesProvider.getProperty("elasticsearch.serverUrl");
        this.username = PropertiesProvider.getProperty("elasticsearch.username");
        this.password = PropertiesProvider.getProperty("elasticsearch.password");
        this.ca = PropertiesProvider.getProperty("elasticsearch.ca");
        this.maxTotal = Integer.parseInt(PropertiesProvider.getProperty("elasticsearch.pool.maxTotal"));
        this.maxWait = Integer.parseInt(PropertiesProvider.getProperty("elasticsearch.pool.maxWait"));
        this.minIdle = Integer.parseInt(PropertiesProvider.getProperty("elasticsearch.pool.minIdle"));
        Jackson2ObjectMapperBuilder builder = getJackson2ObjectMapperBuilder();
        this.mapper = builder.build();
    }

    private static Jackson2ObjectMapperBuilder getJackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilderCustomizer customizer =
                jacksonObjectMapperBuilder -> {
                    jacksonObjectMapperBuilder
                            .serializerByType(Long.TYPE, ToStringSerializer.instance);
                    jacksonObjectMapperBuilder
                            .serializerByType(Long.class, ToStringSerializer.instance);
                    jacksonObjectMapperBuilder
                            .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer());
                };
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        return builder;
    }

    private final GenericObjectPoolConfig<ElasticsearchClient> poolConfig =
            new GenericObjectPoolConfig<>();

    public GenericObjectPoolConfig<ElasticsearchClient> getPoolConfig() {
        log.info("Getting pool config.");
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));
        poolConfig.setJmxEnabled(false);
        // 当对象池耗尽时，是否等待获取对象
        poolConfig.setBlockWhenExhausted(true);
        // 创建对象时是否进行对象有效性检查
        poolConfig.setTestOnCreate(true);
        // 借出对象时是否进行对象有效性检查
        poolConfig.setTestOnBorrow(true);
        // 归还对象时是否进行对象有效性检查
        poolConfig.setTestOnReturn(true);
        // 空闲时是否进行对象有效性检查
        poolConfig.setTestWhileIdle(true);

        return poolConfig;
    }

    @Override
    public void activateObject(PooledObject<ElasticsearchClient> pooledObject) throws Exception {
        log.debug("Activate object:{}", pooledObject.getCreateInstant());
    }

    @Override
    public void destroyObject(PooledObject<ElasticsearchClient> pooledObject) throws Exception {
        log.debug("Destroy object:{}", pooledObject.getCreateInstant());
        ElasticsearchClient elasticsearchClient = pooledObject.getObject();
        elasticsearchClient.shutdown();
    }

    @Override
    public PooledObject<ElasticsearchClient> makeObject() throws Exception {
        log.debug("Make object");
        validateInputs();

        RestClientBuilder builder = RestClient.builder(HttpHost.create(serverUrl));
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        if (StringUtils.isNotEmpty(ca)) {
            SSLContext sslContext = createSSLContext();
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setSSLContext(sslContext));
        } else {
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider));
        }

        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient,
                new JacksonJsonpMapper(mapper));
        log.debug("Elasticsearch client created.");
        ElasticsearchClient elasticsearchClient = new ElasticsearchClient(transport);
        return new DefaultPooledObject<>(elasticsearchClient);
    }

    @Override
    public void passivateObject(PooledObject<ElasticsearchClient> pooledObject) throws Exception {
        log.debug("Passivate object:{}", pooledObject.getCreateInstant());
    }

    @Override
    public boolean validateObject(PooledObject<ElasticsearchClient> pooledObject) {
        return true;
    }

    private void validateInputs() {
        if (StringUtils.isEmpty(serverUrl)) {
            throw new IllegalArgumentException("serverUrl must be set");
        } else if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Username and password must be set");
        }
    }

    private SSLContext createSSLContext() throws Exception {
        log.debug("Ssl config detected. Loading ca certificate from {}", ca);
        Path caCertificatePath = Paths.get(ca);
        Certificate trustedCa;
        if (ca.endsWith("crt")) {
            log.info("Ca certificate is a file");
            try (InputStream is = Files.newInputStream(caCertificatePath)) {
                trustedCa = CertificateFactory.getInstance("X.509").generateCertificate(is);
            }
        } else if (ca.startsWith("-----BEGIN CERTIFICATE-----")){
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
