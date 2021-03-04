package com.expedia.www.haystack.agent.core;

import com.expedia.www.haystack.agent.core.config.ConfigurationHelpers;
import com.expedia.www.haystack.agent.span.enricher.Enricher;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseAgent implements Agent {
    protected List<Dispatcher> dispatchers;
    protected final Logger logger;

    public BaseAgent(Logger logger) {
        this.dispatchers = new ArrayList<>();
        this.logger = logger;
    }

    @Override
    public void close() {
        try {
            for (final Dispatcher dispatcher : dispatchers) {
                dispatcher.close();
            }
            logger.info("shutting down gRPC server and jmx reporter");
            closeInternal();
        } catch (Exception ignored) {
        }
    }

    @VisibleForTesting
    public List<Dispatcher> loadAndInitializeDispatchers(final Config config, ClassLoader cl, String agentName) {
        final List<Dispatcher> dispatchers = new ArrayList<>();
        final ServiceLoader<Dispatcher> loadedDispatchers = ServiceLoader.load(Dispatcher.class, cl);
        for (final Dispatcher dispatcher : loadedDispatchers) {
            final Map<String, Config> dispatches = ConfigurationHelpers.readDispatchersConfig(config, agentName);
            dispatches
                    .entrySet()
                    .stream()
                    .filter((e) -> e.getKey().equalsIgnoreCase(dispatcher.getName()))
                    .forEach((conf) -> {
                        final Config dispatcherConfig = conf.getValue();
                        boolean isEnabled = !dispatcherConfig.hasPath("enabled") || dispatcherConfig.getBoolean("enabled");
                        if(isEnabled) {
                            dispatcher.initialize(dispatcherConfig);
                            dispatchers.add(dispatcher);
                        } else {
                            logger.info("dispatcher with name '{}' is disabled", dispatcher.getName());
                        }
                    });
        }

        Validate.notEmpty(
                dispatchers,
                "%s agent dispatchers can't be an empty set",
                StringUtils.capitalize(getName())
        );

        return dispatchers;
    }

    @VisibleForTesting
    public List<Enricher> loadSpanEnrichers(final Config config) {
        if (config.hasPath("enrichers")) {
            return config.getStringList("enrichers")
                    .stream()
                    .map(clazz -> {
                        try {
                            final Class c = Class.forName(clazz);
                            logger.info("Initializing the {} enricher with class name '{}'", getName(), clazz);
                            return (Enricher) c.newInstance();
                        } catch (Exception e) {
                            logger.error("Fail to initialize the enricher with clazz name {}", clazz, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    protected void closeInternal() throws Exception {}
}
