package com.eairlv.sanner.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingUrl {

    private String url;

    private String method;

    private String className;

    private String methodName;

    private String packageName;

    private String interceptType;
}
