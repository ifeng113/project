package com.eairlv.spark.dbscan.middleware;

import com.eairlv.spark.dbscan.entity.DBConnection;
import com.eairlv.spark.dbscan.entity.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TransferService {

    @Value("${spark.docker_exec}")
    private String docker_exec;

    @Value("${spark.docker_program_path}")
    private String docker_program_path;

    @Value("${spark.host_data_path}")
    private String host_data_path;

    @Value("${spark.docker_data_path}")
    private String docker_data_path;

    @Value("${spark.docker_result_path}")
    private String docker_result_path;

    @Value("${spark.callback}")
    private String callback;

    @Value("${spark.eps}")
    private Integer eps;

    @Value("${spark.mps}")
    private Integer mps;

    public String createInstructionByList(List<Location> locations, String code){
        String fileHostPath = null;
        String fileDockerPath = null;
        FileWriter fw = null;
        try {
            String uuid = UUID.randomUUID().toString();
            fileHostPath = host_data_path + uuid + ".txt";
            fileDockerPath = docker_data_path + uuid + ".txt";
            log.info(fileHostPath);
            File file = new File(fileHostPath);
            file.createNewFile();
            fw = new FileWriter(fileHostPath, true);
            for (Location location : locations) {
                fw.write(location.getLongitude() + "," + location.getLatitude() + "\n");
            }
            // 完成后关闭
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("list create instruction error");
        } finally {
            try{
                if(fw!=null) fw.close();
            }catch(IOException ignored){}
        }
        return fileHostPath != null ? createInstruction(fileDockerPath, code) : null;
    }

    public String createInstructionByDB(DBConnection dbConnection, String code){
        Connection conn = null;
        Statement stmt = null;
        String fileHostPath = null;
        String fileDockerPath = null;
        FileWriter fw = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String dbUrl = "jdbc:mysql://" + dbConnection.getHost() + ":" + dbConnection.getPort()
                    + "/" + dbConnection.getDb();
            DriverManager.setLoginTimeout(5);
            conn = DriverManager.getConnection(dbUrl, dbConnection.getUser(), dbConnection.getPassword());
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(dbConnection.getSql());

            // 写入文件
            String uuid = UUID.randomUUID().toString();
            fileHostPath = host_data_path + uuid + ".txt";
            fileDockerPath = docker_data_path + uuid + ".txt";
            File file = new File(fileHostPath);
            log.info(fileHostPath);
            file.createNewFile();
            fw = new FileWriter(fileHostPath, true);
            while(rs.next()){
                Integer longitude = rs.getInt("longitude");
                Integer latitude = rs.getInt("latitude");
                fw.write(longitude + "," + latitude + "\n");
            }
            // 完成后关闭
            fw.close();
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
            log.error("db create instruction error");
        } finally{
            try{
                if(stmt!=null) stmt.close();
            } catch(SQLException ignored){}
            try{
                if(conn!=null) conn.close();
            }catch(SQLException ignored){}
            try{
                if(fw!=null) fw.close();
            }catch(IOException ignored){}
        }
        return fileHostPath != null ? createInstruction(fileDockerPath, code) : null;
    }

    private String createInstruction(String fileDockerPath, String code){
        return docker_exec + " spark-submit --master local[*] " + docker_program_path + " file:" + fileDockerPath
                + " " + code + " " + eps + " " + mps + " " + callback + " " + docker_result_path;
    }

}
