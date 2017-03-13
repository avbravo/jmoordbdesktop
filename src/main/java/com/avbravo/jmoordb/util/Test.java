/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.util;

import java.util.logging.Logger;

/**
 *
 * @author avbravo
 */
public class Test {

    private static final Logger LOG = Logger.getLogger(Test.class.getName());
    
    
    public static void msg(String texto){
        LOG.warning(texto);
        System.out.println( texto);
    }
}
