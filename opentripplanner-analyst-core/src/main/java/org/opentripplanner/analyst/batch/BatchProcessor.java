package org.opentripplanner.analyst.batch;

import java.io.IOException;
import org.opentripplanner.analyst.request.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BatchProcessor {

    private static final String CONFIG = "application-context.xml";
    private static final Logger LOG = LoggerFactory.getLogger(BatchProcessor.class);

    @Autowired
    Renderer renderer;
    
    public static void main(String[] args) throws IOException {

        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(new ClassPathResource(CONFIG));
        ctx.refresh();
        ctx.registerShutdownHook();
        ctx.getBean(BatchProcessor.class).run();
        
    }

    private void run() {
        LOG.info("Hello, this is the batch processor. My renderer is: {}", renderer);
    }

}
