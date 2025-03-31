package com.iptracer.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpInfo {
    private String status;
    private String message;
    private String country;
    private String regionName;
    private String city;
    private String zip;
    private double lat;
    private double lon;
    private String timezone;
    private String isp;
    private String org;
    private String as;
    private String query;
    private boolean mobile;
    private boolean proxy;
    private boolean hosting;

}
