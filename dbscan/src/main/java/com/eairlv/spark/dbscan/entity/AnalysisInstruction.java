package com.eairlv.spark.dbscan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisInstruction<T> {

    private String code;

    private Integer way;

    private T data;
}
