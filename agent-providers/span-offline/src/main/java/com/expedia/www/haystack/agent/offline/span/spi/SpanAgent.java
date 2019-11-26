package com.expedia.www.haystack.agent.offline.span.spi;

import com.amazon.kinesis.streaming.agent.AgentContext;
import com.amazon.kinesis.streaming.agent.config.AgentConfiguration;
import com.amazon.kinesis.streaming.agent.config.ConfigurationException;
import com.expedia.www.haystack.agent.core.Agent;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class SpanAgent implements Agent {
    private final static Logger LOGGER = LoggerFactory.getLogger(SpanAgent.class);

    @Override
    public String getName() {
        return "spansoffline";
    }

    @Override
    public void initialize(Config config) throws IOException {
        final Map<String, Object> agentConfigMap = getAgentConfigMap(config);
        final AgentConfiguration agentConfiguration = new AgentConfiguration(agentConfigMap);

        try {
            AgentContext agentContext = new AgentContext(agentConfiguration);
            if (agentContext.flows().isEmpty()) {
                throw new ConfigurationException("There are no flows configured in configuration file.");
            }
            final com.amazon.kinesis.streaming.agent.Agent kinesisAgent = new com.amazon.kinesis.streaming.agent.Agent(agentContext);

            kinesisAgent.startAsync();
            kinesisAgent.awaitRunning();
            kinesisAgent.awaitTerminated();
        } catch (Exception ex) {
            LOGGER.error("Unhandled error.", ex);
            ex.printStackTrace();
            System.exit(1);
        }

    }

    private Map<String, Object> getAgentConfigMap(Config config) {
        final Map<String, Object> agentConfigMap;
        agentConfigMap = config.entrySet().stream().collect(Collectors.toMap(c -> c.getKey(), c -> c.getValue()));

        return agentConfigMap;
    }

    @Override
    public void close() throws Exception {

    }
}
