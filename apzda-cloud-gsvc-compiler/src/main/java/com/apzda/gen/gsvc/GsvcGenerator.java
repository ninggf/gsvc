package com.apzda.gen.gsvc;

import com.salesforce.jprotoc.ProtocPlugin;
import org.apache.dubbo.gen.AbstractGenerator;

/**
 * gsvc编译器: 将protobuf的IDL编译成支持gsvc的接口，而不是grpc-java的stub类.
 *
 * @author fengz
 */
public class GsvcGenerator extends AbstractGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            ProtocPlugin.generate(new GsvcGenerator());
        } else {
            ProtocPlugin.debug(new GsvcGenerator(), args[0]);
        }
    }

    @Override
    protected String getClassPrefix() {
        return "";
    }

    @Override
    protected String getClassSuffix() {
        return "Gsvc";
    }

    @Override
    protected String getTemplateFileName() {
        return "GsvcStub.mustache";
    }

    @Override
    protected String getInterfaceTemplateFileName() {
        return "GsvcInterfaceStub.mustache";
    }

    @Override
    protected String getSingleTemplateFileName() {
        throw new IllegalStateException("Do not support single template!");
    }

    @Override
    protected boolean enableMultipleTemplateFiles() {
        return true;
    }
}
