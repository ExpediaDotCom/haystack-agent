package com.expedia.www.haystack.agent.core.helpers

import java.util

import com.expedia.www.haystack.agent.core.config.{AgentConfig, Config, ConfigReader}

class TestFileConfigReader extends ConfigReader {
  override def getName: String = "file"

  override def read(): Config = {
    val cfg = new Config()
    val agentConfig = new AgentConfig()
    agentConfig.setName("spans")

    val props = new util.HashMap[String, Object]()
    props.put("port", new Integer(8080))
    props.put("k1", "v1")
    agentConfig.setProps(props)

    val dispatchers = new util.HashMap[String, util.Map[String, Object]]()
    val kafkaDispatcherCfg = new util.HashMap[String, Object]()
    dispatchers.put("kafka", kafkaDispatcherCfg)
    agentConfig.setDispatchers(dispatchers)

    cfg.setAgentConfigs(util.Arrays.asList(agentConfig))
    cfg
  }
}