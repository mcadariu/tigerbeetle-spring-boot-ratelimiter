package com.example.tigerbeetle_ratelimiter.config;

import com.tigerbeetle.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

import static com.tigerbeetle.UInt128.asBytes;

@Configuration
public class TigerBeetleConfiguration {
    @Value("${tigerbeetle.clusterID:0}")
    private BigInteger clusterID;

    @Value("${tigerbeetle_0:3001}")
    private String[] replicaAddress;

    @Bean
    Client tigerBeetleClient() {
        return new Client(asBytes(clusterID), replicaAddress);
    }
}
