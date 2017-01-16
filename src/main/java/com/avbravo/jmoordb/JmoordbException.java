package com.avbravo.jmoordb;

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
