package com.eairlv.spark.dbscan.middleware;

import com.alibaba.fastjson.JSON;
import com.eairlv.spark.dbscan.config.MQConfig;
import com.eairlv.spark.dbscan.entity.AnalysisInstruction;
import com.eairlv.spark.dbscan.entity.DBConnection;
import com.eairlv.spark.dbscan.entity.Location;
import com.eairlv.spark.dbscan.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MQService {

    @Autowired
    AmqpTemplate amqpTemplate;

    @Autowired
    TransferService mysqlService;

    @RabbitListener(queues = MQConfig.DB_SCAN_INSTRUCTION)
    public void getAnalysisInstruction(Message message){
        AnalysisInstruction analysisInstruction = JSON.parseObject(new String(message.getBody()), AnalysisInstruction.class);
        String instruction;
        try {
            if (analysisInstruction.getWay().equals(1)){
                instruction = mysqlService.createInstructionByList(JSON.parseArray(JSON.toJSONString(analysisInstruction.getData()), Location.class), analysisInstruction.getCode());
            } else {
                instruction = mysqlService.createInstructionByDB(JSON.parseObject(JSON.toJSONString(analysisInstruction.getData()), DBConnection.class), analysisInstruction.getCode());
            }
        } catch (Exception e){
            log.error("mq instruction analysis fail");
            return;
        }
        log.info("instruction:{}", instruction);
        try {
           Runtime.getRuntime().exec(instruction);
//            Process process = Runtime.getRuntime().exec(instruction);
//            // 阻塞队列获取MQ新数据
//            process.waitFor();
//            BufferedReader bufIn = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
//            BufferedReader bufError = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
//            String line;
//            while ((line = bufIn.readLine()) != null) {
//                log.info(line);
//            }
//            while ((line = bufError.readLine()) != null) {
//                log.info(line);
//            }
//            bufIn.close();
//            bufError.close();
//            process.destroy();
        } catch (Exception e) {
            log.error("exec spark-submit fail");
        }
    }

    public void putResult(String code, List<List<Location>> data){
        amqpTemplate.convertAndSend(MQConfig.DB_SCAN_RESULT, JSON.toJSONString(Result.builder()
                .code(code)
                .data(data)
                .build()));
    }

}
