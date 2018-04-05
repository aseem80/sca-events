package io.nordstrom.org.scaevents.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Created by bmwi on 4/4/18.
 */
public class OrgKafkaAppTestUtil {

    public static Path tempTestFilePath() throws IOException{
        String tmpDirectory = System.getProperty("java.io.tmpdir");
        Path path = Paths.get(tmpDirectory, "sca_Payload.json");
        return path;
    }

}
