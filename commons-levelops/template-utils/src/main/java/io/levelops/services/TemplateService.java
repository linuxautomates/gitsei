package io.levelops.services;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.util.Map;

public class TemplateService {

    private final VelocityEngine velocity;

    public TemplateService() {
        velocity = new VelocityEngine();
        velocity.init();
    }

    public String evaluateTemplate(String template, Map<String, Object> context) {

        VelocityContext velocityContext = new VelocityContext(context);

        StringWriter writer = new StringWriter();
        velocity.evaluate(velocityContext, writer, "", template);
        return writer.toString();
    }

    public String evaluateTemplateFromResource(String resourceFile, Map<String, Object> context) {

        Template t = velocity.getTemplate(resourceFile);
        VelocityContext velocityContext = new VelocityContext(context);

        StringWriter writer = new StringWriter();
        t.merge(velocityContext, writer);
        return writer.toString();
    }

}
