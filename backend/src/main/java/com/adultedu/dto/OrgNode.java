package com.adultedu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrgNode {
    private Long id;
    private Long parentId;
    private String orgName;
    private Integer orgType;
    private String orgCode;
    private Long leaderUserId;
    private String leaderName;
    private Integer sort;
    private Integer status;
    private Long userCount;
    private List<OrgNode> children = new ArrayList<>();
}
