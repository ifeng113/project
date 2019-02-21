package com.eairlv.sanner.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlAndMethod {

    private String url;

    private String method;

    private String interceptType;
}
