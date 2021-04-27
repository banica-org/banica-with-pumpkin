package com.market.banica.aurora;


import com.market.banica.common.util.ApplicationDirectoryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

@SpringBootTest(classes = AuroraApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testAuroraIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuroraApplicationTest {

    private static String channelsBackupUrl;
    private static String publishersBackupUrl;


    @Test
    void contextLoads() {
    }

    @BeforeAll
    public static void getFilePath(@Value("${aurora.channels.file.name}") String channels, @Value("${aurora.channels.publishers}") String publishersFileName) {
        channelsBackupUrl = channels;
        publishersBackupUrl = publishersFileName;

    }

    @AfterAll
    public static void cleanUp() throws IOException {
        ApplicationDirectoryUtil.getConfigFile(channelsBackupUrl).delete();
        ApplicationDirectoryUtil.getConfigFile(publishersBackupUrl).delete();
    }
}
