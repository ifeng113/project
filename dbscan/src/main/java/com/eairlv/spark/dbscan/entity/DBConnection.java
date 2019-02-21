package com.eairlv.spark.dbscan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBConnection {

    private String host;

    private Integer port;

    private String user;

    private String password;

    private String db;

    private String sql;
}
