package bot.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bot.constant.MiscConstant;

public class ApiAction {
	private static final Logger logger = LogManager.getLogger(ApiAction.class);

    public int readLastBlockNo() throws IOException {
        int lastBlockNo = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(MiscConstant.BLOCK_NO_FILE))) {
            lastBlockNo = Integer.parseInt(br.readLine());
        }
        return lastBlockNo;
    }

    public void writeLastBlockNo(int lastBlockNo) throws IOException {
        if (!Files.exists(Paths.get(MiscConstant.BLOCK_NO_FILE), LinkOption.NOFOLLOW_LINKS)) {
            Files.createFile(Paths.get(MiscConstant.BLOCK_NO_FILE));
        }
        String oldContent = Files.readString(Path.of(MiscConstant.BLOCK_NO_FILE), Charset.defaultCharset());
        FileWriter fw = new FileWriter(MiscConstant.BLOCK_NO_FILE, false); // the true will append the new data
        fw.write(lastBlockNo + System.getProperty("line.separator") + oldContent); // appends the string to the file
        fw.close();
    }
}
