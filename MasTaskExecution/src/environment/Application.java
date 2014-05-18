package environment;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Mihai Tabara
 * Class to initialize the system
 */
public class Application {

	/**
	 * The place to get the tests from.
	 */
	protected static final String	TEST_SUITE	= "input/";	
	
	/**
	 * Name for the file containing the initial state of the system
	 */
	protected static final String	SI			= TEST_SUITE + "system.txt";
	
	/**
	 * @param args
	 *			- unused
	 * @throws FileNotFoundException
	 * 			- if input file not found
	 * @throws IOException
	 * 			- if input file is corrupted
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		MasTaskEnvironment env = null;
		try (InputStream input = new FileInputStream(SI))
		{
			env = new MasTaskEnvironment(input);
		}
		
		System.out.println(env.toString());
	}

}

