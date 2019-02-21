package com.eairlv.spark.dbscan.middleware;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eairlv.spark.dbscan.entity.Location;
import com.eairlv.spark.dbscan.config.MQConfig;
import com.eairlv.spark.dbscan.entity.AnalysisInstruction;
import com.eairlv.spark.dbscan.entity.DBConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/spark")
@Slf4j
public class SparkService {

    @Value("${spark.host_result_path}")
    private String host_result_path;

    @Autowired
    MQService mqService;

    @Autowired
    AmqpTemplate amqpTemplate;

    @PostMapping
    public String getSparkFileResult(@RequestBody String fileName){
        File file = new File(host_result_path + "/" +  fileName);
        return getSparkResult(txtToString(file));
    }

    private String getSparkResult(String result){
        JSONObject jsonObject = JSON.parseObject(result);
        String code = jsonObject.getString("code");
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        List<List<Location>> clusterList = new ArrayList<>();
        log.info("code:{} and clusterList size:{}", code, clusterList.size());
        for (int i = 0; i < jsonArray.size(); i++){
            JSONArray cluster = jsonArray.getJSONArray(i);
            List<Location> locationList = new ArrayList<>();
//            log.info("code:{} and cluster:{} size:{} ", code, i, cluster.size());
            for (int j = 0; j < cluster.size(); j++){
                String locationString = cluster.getString(j);
                String[] longitudeAndLatitude = locationString.split(",");
                locationList.add(Location.builder()
                        .longitude(Integer.parseInt(longitudeAndLatitude[0]))
                        .latitude(Integer.parseInt(longitudeAndLatitude[1]))
                        .build());
            }
            clusterList.add(locationList);
        }
        mqService.putResult(code, clusterList);
        log.info("code:{} mq send ok ", code);
        return code + "\n";
    }

    /**
     * 读取txt文件的内容
     * @param file 想要读取的文件对象
     * @return 返回文件内容
     */
    public static String txtToString(File file){
        StringBuilder result = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;
            while((s = br.readLine())!=null){
                result.append(System.lineSeparator()).append(s);
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return result.toString();
    }

    @GetMapping("/list")
    public String putListMessage(){
        List<Location> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(Location.builder()
                    .longitude(103000000 + i)
                    .latitude(30000000 + i)
                    .build());
        }
        amqpTemplate.convertAndSend(MQConfig.DB_SCAN_INSTRUCTION, JSON.toJSONString(AnalysisInstruction.builder()
                .code("lv")
                .way(1)
                .data(list)
                .build()));
        return "list\n";
    }

    @GetMapping("/db")
    public String putDBMessage(){
        amqpTemplate.convertAndSend(MQConfig.DB_SCAN_INSTRUCTION, JSON.toJSONString(AnalysisInstruction.builder()
                .code("lv")
                .way(2)
//                .data(DBConnection.builder()
//                        .host("10.50.40.145")
//                        .port(3306)
//                        .db("adasdb_v2")
//                        .user("root")
//                        .password("cdwk-3g-145")
//                        .sql("select longitude, latitude from alarm_history where id < 100 and id >= 90")
//                        .build())
                .data(DBConnection.builder()
                        .host("192.168.60.10")
                        .port(3306)
                        .db("adasdb_v2")
                        .user("root")
                        .password("DbSql#dodo888")
                        .sql("select longitude, latitude from alarm_history where id < 100 and id >= 90")
                        .build())
                .build()));
        return "db\n";
    }

    @PostMapping("/cmd")
    public String putCmd(@RequestBody String cmd){
        try {
            log.info("cmd:{}", cmd);
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            BufferedReader bufError = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            String line;
            while ((line = bufIn.readLine()) != null) {
                log.info(line);
            }
            while ((line = bufError.readLine()) != null) {
                log.error(line);
            }
            bufIn.close();
            bufError.close();
            process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "cmd\n";
    }
}
