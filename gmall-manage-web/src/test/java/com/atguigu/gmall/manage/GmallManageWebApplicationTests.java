package com.atguigu.gmall.manage;

import com.atguigu.gmall.util.RedisUtil;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.awt.*;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {


    @Test
    public void contextLoads() throws IOException, MyException {

//        //prizhi fdfs的全局连接地址
//       String track = GmallManageWebApplicationTests.class.getResource("/tracker.conf").getPath();//huode 配置晚间路径
//
//        ClientGlobal.init(track);
//
//        TrackerClient trackerClient = new TrackerClient();
//        TrackerServer trackerServer = trackerClient.getTrackerServer();
//        StorageClient storageClient = new StorageClient(trackerServer,null);
//        String[] uploadinfs = storageClient.upload_appender_file("c:/Users/Administrator/Desktop/msg1.jpg","jpg",null);
//
//        for (String uploadinf : uploadinfs){
//
//
//            System.out.println(uploadinf);
//        }

    }

}
