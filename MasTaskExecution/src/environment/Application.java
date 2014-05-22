package environment;

import jade.core.Profile;
import jade.core.Runtime;
import jade.core.ProfileImpl;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import environment.MasTaskEnvironment.AgentData;
import agents.FacilitatorAgent;
import agents.ProcessAgent;


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
	 * @throws StaleProxyException
	 * 			- for an attempt to use an wrapper object
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, StaleProxyException, InterruptedException
	{
		MasTaskEnvironment env = null;
		try (InputStream input = new FileInputStream(SI))
		{
			env = new MasTaskEnvironment(input);
		}
		
        Properties properties = new ExtendedProperties();
        properties.setProperty(Profile.GUI, "false");
        properties.setProperty(Profile.MAIN, "true");
        properties.setProperty(Profile.CONTAINER_NAME, "MasTaskContainer");
        ProfileImpl profile = new ProfileImpl((jade.util.leap.Properties) properties);
        AgentContainer agentMainContainer = Runtime.instance().createMainContainer(profile);
               
        for (int i = 0; i < env.getNumberOfAgents(); i++) {
        	AgentData[] dataToSendToAgent = new AgentData[1];
        	dataToSendToAgent[0] = env.getAgent(i);
        	AgentController processorControllerAgent = agentMainContainer.createNewAgent("agent" + i, ProcessAgent.class.getName(), dataToSendToAgent);
        	processorControllerAgent.start();
        }
        
        Thread.sleep(2);
        MasTaskEnvironment[] dataToSendtoFacilitator = new MasTaskEnvironment[1];
        dataToSendtoFacilitator[0] = env;
        AgentController facilitatorControllerAgent = agentMainContainer.createNewAgent("facilitator", FacilitatorAgent.class.getName(), dataToSendtoFacilitator);
        facilitatorControllerAgent.start();
        
        
	}

}

