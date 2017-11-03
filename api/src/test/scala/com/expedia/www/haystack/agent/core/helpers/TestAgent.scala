package com.expedia.www.haystack.agent.core.helpers

import com.expedia.www.haystack.agent.core.Agent
import com.expedia.www.haystack.agent.core.config.AgentConfig

class TestAgent extends Agent {
  /**
    * unique name of the agent, this is used to selectively load the agent by the name
    *
    * @return unique name of the agent
    */
  override def getName: String = "spans"

  /**
    * initialize the agent
    *
    * @param config config object
    * @throws Exception throws an exception if fail to initialize
    */
  override def initialize(config: AgentConfig): Unit = {
    assert(config.getName.equalsIgnoreCase("spans"))
    assert(config.getProps.get("port").equals(8080))
    assert(config.getProps.get("k1").equals("v1"))
    assert(config.getDispatchers.containsKey("kafka"))
  }
}
