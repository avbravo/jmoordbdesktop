/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.interfaces;

/**
 *
 * @author avbravo
 */
public interface FindInterface<T> {
    public T findById(T t);
    
}
