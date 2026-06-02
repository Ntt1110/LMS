package com.example.LMS.Config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName != null ? cloudName.trim() : "", // ✅ Thêm .trim() để tự bóc khoảng trắng thừa
                "api_key", apiKey != null ? apiKey.trim() : "",
                "api_secret", apiSecret != null ? apiSecret.trim() : ""
        ));
    }
}