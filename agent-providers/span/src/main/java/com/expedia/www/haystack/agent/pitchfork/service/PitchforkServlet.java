/*
 *  Copyright 2019 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.agent.pitchfork.service;

import com.codahale.metrics.Meter;
import com.expedia.www.haystack.agent.core.metrics.SharedMetricRegistry;
import com.expedia.www.haystack.agent.pitchfork.processors.ZipkinSpanProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static com.expedia.www.haystack.agent.pitchfork.processors.ZipkinSpanProcessorFactory.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class PitchforkServlet extends HttpServlet {
    private final static Logger logger = LoggerFactory.getLogger(PitchforkServlet.class);
    private final Map<String, ZipkinSpanProcessor> processors;
    private final Meter requestRateMeter;
    private final Meter errorMeter;

    public PitchforkServlet(final String name,
                            final Map<String, ZipkinSpanProcessor> processors) {
        Validate.notEmpty(name, "pitchfork servlet name can't be empty or null");
        Validate.isTrue(processors != null && !processors.isEmpty(), "span processors can't be null");

        this.processors = processors;
        requestRateMeter = SharedMetricRegistry.newMeter("pitchfork." + name + ".request.rate");
        errorMeter = SharedMetricRegistry.newMeter("pitchfork." + name + ".error.rate");

        logger.info("Initializing http servlet with name = {}", name);
    }

    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        requestRateMeter.mark();
        logger.info("zipkin span dispatch request at {}", request.getRequestURI());

        final ZipkinSpanProcessor processor = getProcessor(request.getContentType());
        if (processor != null) {
            try {
                final byte[] inputBytes = readFromStream(request.getInputStream()).toByteArray();
                processor.process(inputBytes);
                response.setStatus(200);
            } catch (Exception ex) {
                errorMeter.mark();
                logger.error("Fail to process/forward the zipkin span, request made at {}", request.getRequestURI(), ex);
                response.sendError(503, "Fail to process/forward the zipkin span!");
            }
        } else {
            response.sendError(400, String.format("invalid content-type, supported values are %s, %s, %s, got '%s'",
                    JSON_CONTENT_TYPE, THRIFT_CONTENT_TYPE, PROTO_CONTENT_TYPE, request.getContentType()));
        }
    }

    private ZipkinSpanProcessor getProcessor(String contentType) {
        if(!isEmpty(contentType) ) {
            final String[] contentTypes = contentType.split(";");
            for (final String ctype : contentTypes) {
                final ZipkinSpanProcessor processor = processors.get(ctype.toLowerCase());
                if (processor != null) {
                    return processor;
                }
            }
        }
        return null;
    }

    private ByteArrayOutputStream readFromStream(final InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(input, output);
        return output;
    }
}
