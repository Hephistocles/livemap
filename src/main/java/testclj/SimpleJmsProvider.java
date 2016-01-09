package main.java.testclj;

import backtype.storm.contrib.jms.JmsProvider;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Created by christoph on 08/01/16.
 */
public class SimpleJmsProvider implements JmsProvider {
    private ConnectionFactory connectionFactory;
    private Destination destination;

    public SimpleJmsProvider(String connString, String queueName) throws Exception {
        this.connectionFactory = new ActiveMQConnectionFactory(connString);

        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY,"org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        props.setProperty(Context.PROVIDER_URL, connString);
        props.setProperty("queue." + queueName, queueName);
        Context jndiContext = new InitialContext(props);
        this.destination = (Destination) jndiContext.lookup(queueName);
    }

    public ConnectionFactory connectionFactory() throws Exception {
        return this.connectionFactory;
    }

    public Destination destination() throws Exception {
        return this.destination;
    }
}
