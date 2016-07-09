package org.graylog.aws.tools;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class StateFile {

    private static final Logger LOG = LoggerFactory.getLogger(StateFile.class);

    public static final String PREFIX = "graylog_aws";

    private final String filename;
    private final String tmpDir;
    private File file;

    public StateFile(String filename) {
        this.filename = PREFIX + "_" + filename;

        this.tmpDir = System.getProperty("java.io.tmpdir");
        this.file = new File(tmpDir, filename + ".tmp");
    }

    public void writeValue(String value) throws IOException {
        ensureFile();

        Files.write(value, this.file, Charsets.UTF_8);
    }

    public String readValue() throws IOException {
        ensureFile();

        return Files.readFirstLine(this.file, Charsets.UTF_8);
    }

    public void ensureFile() throws IOException {
        if(!file.exists()) {
            LOG.debug("StateFile [{}] does not exist. Creating it.", file);
            if(!file.createNewFile()) {
                throw new IOException("Could not create new state file.");
            }
        }
    }

}
