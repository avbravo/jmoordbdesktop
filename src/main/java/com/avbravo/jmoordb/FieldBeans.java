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
}
