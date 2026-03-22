package dev.settlement.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "settlement.van-upload")
public class VanUploadProperties {

    // CSV 정산 파일 임시 저장
    private String tempDir = System.getProperty("java.io.tmpdir") + "/van-settlement";

}
