/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author avbravo
 */
@Getter
@Setter
public class FieldBeans {
    private String name;
    private String type;
    private Boolean isKey;
    private Boolean isEmbedded;
    private Boolean isReferenced;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsKey() {
        return isKey;
    }

    public void setIsKey(Boolean isKey) {
        this.isKey = isKey;
    }

    public Boolean getIsEmbedded() {
        return isEmbedded;
    }

    public void setIsEmbedded(Boolean isEmbedded) {
        this.isEmbedded = isEmbedded;
    }

    public Boolean getIsReferenced() {
        return isReferenced;
    }

    public void setIsReferenced(Boolean isReferenced) {
        this.isReferenced = isReferenced;
    }
    
    
    
    
}
