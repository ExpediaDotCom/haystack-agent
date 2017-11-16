package com.expedia.www.haystack.agent.core.helpers

import com.expedia.www.haystack.agent.core.Agent
import com.typesafe.config.Config

class TestAgent extends Agent {

  var isInitialized = false

  /**
    * unique name of the agent, this is used to selectively load the agent by the name
    *
    * @return unique name of the agent
    */
  override def getName: String = "spans"

  /**
    * initialize the agent
    *
    * @param cfg config object
    * @throws Exception throws an exception if fail to initialize
    */
  override def initialize(cfg: Config): Unit = {
    isInitialized = true

    assert(cfg.getInt("port") == 8080)
    assert(cfg.getString("k1") == "v1")

    assert(cfg.getConfig("dispatchers") != null)
    assert(cfg.getConfig("dispatchers").hasPath("kinesis"))
  }

  /**
    * close the agent
    */
  override def close(): Unit = {
    assert(isInitialized, "Fail to close the uninitialized agent")
  }
}
