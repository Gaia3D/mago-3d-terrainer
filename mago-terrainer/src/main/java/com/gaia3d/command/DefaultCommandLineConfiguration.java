package com.gaia3d.command;

import org.apache.commons.cli.*;

public class DefaultCommandLineConfiguration implements CommandLineConfiguration {

    @Override
    public Options createOptions() {
        return LoggingConfiguration.createOptions();
    }

    @Override
    public CommandLine createCommandLine(Options options, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
}
