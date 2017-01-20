/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb;

/**
 *
 * @author avbravo
 */
public class JmoordbException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JmoordbException() {
	}

	public JmoordbException(String msg) {
		super(msg);
	}

	public JmoordbException(Throwable t) {
		super(t);
	}

	public JmoordbException(String msg, Throwable t) {
		super(msg, t);
	}

}