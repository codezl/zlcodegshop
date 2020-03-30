package com.atguigu.gmall.manage.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {
    public static String uploadImage(MultipartFile multipartFile) {

        String imgUrl = "http://192.168.229.130";

        //shangchuanfuwuqi

        //prizhi fdfs的全局连接地址  反射得到路径
        String track = PmsUploadUtil.class.getResource("/tracker.conf").getPath();//huode 配置晚间路径

        try {
            ClientGlobal.init(track);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getTrackerServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StorageClient storageClient = new StorageClient(trackerServer,null);

        try {
            String originalFilename = multipartFile.getOriginalFilename();//获得文件后缀(有些文件有很多个.只需得到最后一个点的位置后的字符)
            int i = originalFilename.lastIndexOf(".");

            String extName = originalFilename.substring(i+1);
            byte[] bytes = multipartFile.getBytes();//huodeshangchuan 二进制对线
            String[] uploadinfos = storageClient.upload_appender_file(bytes,extName,null);



            for (String uploadinfo : uploadinfos){
                imgUrl += "/"+uploadinfo;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgUrl;
    }
}
