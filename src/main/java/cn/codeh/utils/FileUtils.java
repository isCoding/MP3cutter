package cn.codeh.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class FileUtils {

    /**
     * 检测是否有文件， 如果有则删除。
     */
    public static void checkAndDelFile(File file) {
        if (file.exists()) {
            file.delete();//删除文件
        }
    }

    public static void closeRandomAccessFiles(List<RandomAccessFile> randomAccessFiles) {
        randomAccessFiles.forEach(randomAccessFile -> {
            try {
                if (randomAccessFile != null)
                    randomAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
