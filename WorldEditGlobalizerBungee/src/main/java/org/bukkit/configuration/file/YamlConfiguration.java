package org.bukkit.configuration.file;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfigurationOptions;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.apache.commons.lang3.Validate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link Configuration} which saves all files in Yaml.
 * Note that this implementation is not synchronized.
 */
public class YamlConfiguration extends FileConfiguration {
    private String configName;
    protected static final String COMMENT_PREFIX = "# ";
    protected static final String BLANK_CONFIG = "{}\n";
    private final DumperOptions yamlOptions = new DumperOptions();
    private final Representer yamlRepresenter = new YamlRepresenter();
    private final Yaml yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions);
    private Map<String,Object> data = new HashMap<>();




    @Override
    public String saveToString() {
        this.yamlOptions.setIndent(options().indent());
        this.yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        String header = buildHeader();
        String dump = this.yaml.dump(getValues(false));
        if (dump.equals("{}\n")) {
            dump = "";
        }
        return header + dump;
    }


    private static String join(List<String> list, String conjunction) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : list) {
            if (first) {
                first = false;
            } else {
                sb.append(conjunction);
            }
            sb.append(item);
        }

        return sb.toString();
    }

    @Override
    public void loadFromString(String contents) throws InvalidConfigurationException {
        Validate.notNull(contents, "Contents cannot be null");

        Map<?, ?> input;
        try {
            input = (Map<?, ?>) yaml.load(contents);
        } catch (YAMLException e) {
            throw new InvalidConfigurationException(e);
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }

        String header = parseHeader(contents);
        if (header.length() > 0) {
            options().header(header);
        }

        if (input != null) {
            convertMapsToSections(input, this);
        }
    }

    protected void convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Map) {
                convertMapsToSections((Map<?, ?>) value, section.createSection(key));
            } else {
                section.set(key, value);
            }
        }
    }

    protected String parseHeader(String input) {
        String[] lines = input.split("\r?\n", -1);
        StringBuilder result = new StringBuilder();
        boolean readingHeader = true;
        boolean foundHeader = false;

        for (int i = 0; (i < lines.length) && (readingHeader); i++) {
            String line = lines[i];

            if (line.startsWith(COMMENT_PREFIX)) {
                if (i > 0) {
                    result.append("\n");
                }

                if (line.length() > COMMENT_PREFIX.length()) {
                    result.append(line.substring(COMMENT_PREFIX.length()));
                }

                foundHeader = true;
            } else if ((foundHeader) && (line.length() == 0)) {
                result.append("\n");
            } else if (foundHeader) {
                readingHeader = false;
            }
        }

        return result.toString();
    }

    @Override
    protected String buildHeader() {
        String header = options().header();

        if (options().copyHeader()) {
            Configuration def = getDefaults();

            if ((def != null) && (def instanceof FileConfiguration)) {
                FileConfiguration filedefaults = (FileConfiguration) def;
                String defaultsHeader = filedefaults.buildHeader();

                if ((defaultsHeader != null) && (defaultsHeader.length() > 0)) {
                    return defaultsHeader;
                }
            }
        }

        if (header == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] lines = header.split("\r?\n", -1);
        boolean startedHeader = false;

        for (int i = lines.length - 1; i >= 0; i--) {
            builder.insert(0, "\n");

            if ((startedHeader) || (lines[i].length() != 0)) {
                builder.insert(0, lines[i]);
                builder.insert(0, COMMENT_PREFIX);
                startedHeader = true;
            }
        }

        return builder.toString();
    }

    @Override
    public YamlConfigurationOptions options() {
        if (options == null) {
            options = new YamlConfigurationOptions(this);
        }

        return (YamlConfigurationOptions) options;
    }

    /**
     * Creates a new {@link org.bukkit.configuration.file.YamlConfiguration}, loading from the given file.
     * <p>
     * Any errors loading the Configuration will be logged and then ignored.
     * If the specified input is not a valid config, a blank config will be
     * returned.
     * <p>
     * The encoding used may follow the system dependent default.
     *
     * @param file Input file
     * @return Resulting configuration
     * @throws IllegalArgumentException Thrown if file is null
     */
    public static org.bukkit.configuration.file.YamlConfiguration loadConfiguration(File file) {
        Validate.notNull(file, "File cannot be null");

        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

        try {
            config.load(file);
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InvalidConfigurationException ex) {
            ex.printStackTrace();
        }

        return config;
    }

    /**
     * Creates a new {@link org.bukkit.configuration.file.YamlConfiguration}, loading from the given stream.
     * <p>
     * Any errors loading the Configuration will be logged and then ignored.
     * If the specified input is not a valid config, a blank config will be
     * returned.
     *
     * @param stream Input stream
     * @return Resulting configuration
     * @throws IllegalArgumentException Thrown if stream is null
     * @deprecated does not properly consider encoding
     * @see #load(InputStream)
     * @see #loadConfiguration(Reader)
     */
    @Deprecated
    public static org.bukkit.configuration.file.YamlConfiguration loadConfiguration(InputStream stream) {
        Validate.notNull(stream, "Stream cannot be null");

        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

        try {
            config.load(stream);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InvalidConfigurationException ex) {
            ex.printStackTrace();
        }

        return config;
    }



    public void load(File file, String configname) throws IOException, InvalidConfigurationException {
        this.configName = configname;
        super.load(file);
    }

    /**
     * Creates a new {@link org.bukkit.configuration.file.YamlConfiguration}, loading from the given reader.
     * <p>
     * Any errors loading the Configuration will be logged and then ignored.
     * If the specified input is not a valid config, a blank config will be
     * returned.
     *
     * @param reader input
     * @return resulting configuration
     * @throws IllegalArgumentException Thrown if stream is null
     */

    public static org.bukkit.configuration.file.YamlConfiguration loadConfiguration(Reader reader) {
        Validate.notNull(reader, "Stream cannot be null");

        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

        try {
            config.load(reader);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InvalidConfigurationException ex) {
            ex.printStackTrace();
        }

        return config;
    }
}
