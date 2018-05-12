package cn.codeh.cutter;

import cn.codeh.utils.FileUtils;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Mp3Cutter {

    //缓存大小
    private static final int BUFFER_SIZE = 1024 * 1024;

    /**
     * 根据时间和源文件生成MP3文件 （源文件mp3 比特率为cbr恒定比特率）
     */
    public static void generateMp3ByTimeAndCBR(MP3AudioHeader header, String targetFileStr, long beginTime, long endTime, File sourceFile) {
        /*
            先获取音乐数据开始的字节位置
            然后获取截取开始时间的字节位置(相对于音乐数据)
            相加获得截取开始时间相对于整个文件的字节位置
            截取时间差(ms) * 每毫秒字节数获取截取结束时间字节位置
         */
        //获取音轨时长
        int trackLengthMs = header.getTrackLength() * 1000;
        long bitRateKbps = header.getBitRateAsNumber();
        //1KByte/s=8Kbps, bitRate *1024L / 8L / 1000L 转换为 bps 每毫秒
        //计算出截取开始的字节位置
        long beginBitRateBpm = convertKbpsToBpm(bitRateKbps) * beginTime;
        //返回音乐数据的第一个字节
        //Returns the byte position of the first MP3 Frame
        //This is the first byte of music data and not the ID3 Tag Frame
        long firstFrameByte = header.getMp3StartByte();
        //获取开始时间所在文件的字节位置
        long beginByte = firstFrameByte + beginBitRateBpm;
        //计算出结束字节位置
        long endByte = beginByte + convertKbpsToBpm(bitRateKbps) * (endTime - beginTime);
        if (endTime > trackLengthMs) {
            endByte = sourceFile.length() - 1L;
        }
        generateTargetMp3File(targetFileStr, beginByte, endByte, firstFrameByte, sourceFile);
    }

    /**
     * 生成目标mp3文件
     */
    private static void generateTargetMp3File(String targetFileStr, long beginByte, long endByte, long firstFrameByte, File sourceFile) {
        File file = new File(targetFileStr);
        //如果存在则删除
        FileUtils.checkAndDelFile(file);
        RandomAccessFile targetMp3File = null;
        RandomAccessFile sourceFileR = null;
        try {
            targetMp3File = new RandomAccessFile(targetFileStr, "rw");
            sourceFileR = new RandomAccessFile(sourceFile, "rw");
            //write mp3 header info
            writeSourceToTargetFileWithBuffer(targetMp3File, sourceFileR, firstFrameByte, 0);
            //write mp3 frame info
            int size = (int) (endByte - beginByte);
            writeSourceToTargetFileWithBuffer(targetMp3File, sourceFileR, size, beginByte);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtils.closeRandomAccessFiles(Arrays.asList(sourceFileR, targetMp3File));
        }
    }

    /**
     * 根据文件和大小以缓存的方式将源文件写入目标文件
     *
     * @param targetFile 写入的新文件
     * @param sourceFile 读取数据的文件
     * @param totalSize  需要读取并写入数据的总长度
     * @param offset     读取文件的偏移量
     */
    private static void writeSourceToTargetFileWithBuffer(RandomAccessFile targetFile, RandomAccessFile sourceFile, long totalSize, long offset) throws Exception {
        //缓存大小，每次写入指定数据防止内存泄漏
        int buffersize = BUFFER_SIZE;
        long count = totalSize / buffersize;
        if (count <= 1) {
            //文件总长度小于小于缓存大小情况
            writeSourceToTargetFile(targetFile, sourceFile, new byte[(int) totalSize], offset);
        } else {
            // 写入count后剩下的size
            long remainSize = totalSize % buffersize;
            byte data[] = new byte[buffersize];
            //读入文件时seek的偏移量
            for (int i = 0; i < count; i++) {
                writeSourceToTargetFile(targetFile, sourceFile, data, offset);
                offset += BUFFER_SIZE;
            }
            if (remainSize > 0) {
                writeSourceToTargetFile(targetFile, sourceFile, new byte[(int) remainSize], offset);
            }
        }
    }

    /**
     * 根据源文件和大小写入目标文件
     *
     * @param targetFile 输出的文件
     * @param sourceFile 读取的文件
     * @param data       输入输出的缓存数据
     * @param offset     读入文件时seek的偏移值
     */
    private static void writeSourceToTargetFile(RandomAccessFile targetFile, RandomAccessFile sourceFile, byte data[], long offset) throws Exception {
        sourceFile.seek(offset);
        sourceFile.read(data);
        long fileLength = targetFile.length();
        // 将写文件指针移到文件尾。
        targetFile.seek(fileLength);
        targetFile.write(data);
    }

    /**
     * kbps 每秒千字节 转换到 bpm  每毫秒字节数
     */
    private static long convertKbpsToBpm(long bitRate) {
        return bitRate * 1024L / 8L / 1000L;
    }

}
