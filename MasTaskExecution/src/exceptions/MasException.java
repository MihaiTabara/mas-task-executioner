package exceptions;

/**
 * @author mtabara
 * Own class of Exception to define several messages
 */
public class MasException extends Exception {

	private static final long serialVersionUID = -7667850500626170717L;

	/**
	 * @param Return the indicated message
	 */
	public MasException(String message) {
		super(message);
	}

}
