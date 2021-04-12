package org.petrinator.exception;

public class BehaviorException extends Exception {

	private static final long serialVersionUID = -7574500084315352706L;

	public BehaviorException() {
		super("Bad Behavior format");
	}

	public BehaviorException(String message) {
		super(message);
	}



}
