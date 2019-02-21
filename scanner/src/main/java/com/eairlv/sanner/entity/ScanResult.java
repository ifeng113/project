package com.eairlv.sanner.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ScanResult {

    private String microServiceName;

    private List<UrlAndMethod> urlAndMethods = new ArrayList<>();

    private String lock;
}
