//package com.vcolco.spark.dbscan;
//
//import com.alibaba.fastjson.JSON;
//import MQConfig;
//import AnalysisInstruction;
//import DBConnection;
//import Location;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = StartApplication.class)
//public class StartApplicationTests {
//
//    @Autowired
//    AmqpTemplate amqpTemplate;
//
//    @Test
//    public void test(){
//        System.out.println("test");
//    }
//
////    @Test
//    public void putListMessage(){
//        List<Location> list = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            list.add(Location.builder()
//                    .longitude(103000000 + i)
//                    .latitude(30000000 + i)
//                    .build());
//        }
//        amqpTemplate.convertAndSend(MQConfig.DB_SCAN_INSTRUCTION, JSON.toJSONString(AnalysisInstruction.builder()
//                .code("lv")
//                .way(1)
//                .data(list)
//                .build()));
//    }
//
////    @Test
//    public void putDBMessage(){
//        amqpTemplate.convertAndSend(MQConfig.DB_SCAN_INSTRUCTION, JSON.toJSONString(AnalysisInstruction.builder()
//                .code("lv")
//                .way(2)
//                .data(DBConnection.builder()
//                        .host("10.50.40.145")
//                        .port(3306)
//                        .db("adasdb_v2")
//                        .user("root")
//                        .password("cdwk-3g-145")
//                        .sql("select longitude, latitude from alarm_history where id < 100 and id >= 90")
//                        .build())
//                .build()));
//    }
//
//}
