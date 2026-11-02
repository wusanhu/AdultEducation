package com.adultedu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuNode {
    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String path;
    private String component;
    private String icon;
    private String perms;
    private Integer sort;
    private Integer hidden;
    private Integer mobileVisible;
    private List<MenuNode> children = new ArrayList<>();
}
