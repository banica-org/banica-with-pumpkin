package com.market.banica.aurora.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.market.banica.aurora.model.ChannelProperty;
import com.market.banica.common.util.ApplicationDirectoryUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.charset.StandardCharsets.UTF_8;

@EnableMBeanExport
@ManagedResource
@Service
@RequiredArgsConstructor
public class JMXConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(JMXConfig.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private ChannelManager channels;

    private Map<String, ChannelProperty> channelPropertyMap;

    private Publishers publishers;

    private String channelsBackupUrl;

    @Autowired
    public JMXConfig(Publishers publishers, ChannelManager channelManager, @Value("${aurora.channels.file.name}") String fileName) {
        this.channelsBackupUrl = fileName;
        this.publishers = publishers;
        this.channels = channelManager;
        this.channelPropertyMap = this.readChannelsConfigsFromFile();
        this.populateChannels(this.channelPropertyMap);

    }


    @ManagedOperation
    public void createChannel(String channelPrefix, String host, String port) {
        try {
            this.lock.writeLock().lock();
            if (!checkChannelCompatibility(channelPrefix)) {
                throw new IllegalArgumentException("Unsupported publisher");
            }
            LOGGER.info("Creating new channel {} from JMX server", channelPrefix);
            if (channelPropertyMap.containsKey(channelPrefix)) {
                LOGGER.error("Channel with prefix {} already exists", channelPrefix);
                throw new IllegalArgumentException("Channel with this name already exists");
            }

            ChannelProperty channelProperty = new ChannelProperty();
            channelProperty.setPort(Integer.parseInt(port));
            channelProperty.setHost(host);

            channelPropertyMap.put(channelPrefix, channelProperty);
            channels.addChannel(channelPrefix, channelProperty);
            this.writeBackUp();

            LOGGER.debug("New channel created from JMX server with host {} and port {}", host, port);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @ManagedOperation
    public void deleteChannel(String channelPrefix) {
        try {
            this.lock.writeLock().lock();
            LOGGER.info("Deleting channel {} from  JMX server", channelPrefix);

            if (!channelPropertyMap.containsKey(channelPrefix)) {
                LOGGER.error("Channel with prefix {} does not exists", channelPrefix);
                throw new IllegalArgumentException("Channel with this name does not exists");
            }

            this.channelPropertyMap.remove(channelPrefix);
            this.channels.deleteChannel(channelPrefix);
            this.writeBackUp();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @ManagedOperation
    public void editChannel(String channelPrefix, String host, String port) {
        try {
            this.lock.writeLock().lock();
            LOGGER.debug("Editing channel {} from  JMX server", channelPrefix);

            if (!channelPropertyMap.containsKey(channelPrefix)) {
                LOGGER.error("Channel with prefix {} does not exists", channelPrefix);
                throw new IllegalArgumentException("Channel with this name does not exists");
            }

            ChannelProperty channelProperty = this.channelPropertyMap.remove(channelPrefix);
            channelProperty.setHost(host);
            channelProperty.setPort(Integer.parseInt(port));

            this.channelPropertyMap.put(channelPrefix, channelProperty);
            this.channels.editChannel(channelPrefix, channelProperty);
            this.writeBackUp();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @ManagedOperation
    public String getChannelsStatus() {
        StringBuilder builder = new StringBuilder();
        builder.append("Channel : State of channel.\n");
        channels
                .getChannels()
                .entrySet()
                .forEach(entry -> builder.append(String.format("%s : %s \n",
                        entry.getKey(),
                        entry.getValue().getState(false))));

        return builder.toString();
    }

    @ManagedOperation
    public void refreshChannels() {
        channels.getChannels()
                .entrySet()
                .forEach(entry -> entry.getValue().getState(true));
    }


    protected void writeBackUp() {
        try {
            lock.writeLock().lock();
            LOGGER.debug("Writing back-up to json");
            ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
            try (Writer output = new OutputStreamWriter(new FileOutputStream(ApplicationDirectoryUtil.getConfigFile(channelsBackupUrl)), UTF_8)) {
                String jsonData = Utility.getObjectAsJsonString(this.channelPropertyMap, objectWriter);
                output.write(jsonData);
                LOGGER.debug("Back-up written successfully");
            } catch (IOException e) {
                LOGGER.error("Exception thrown during writing back-up : {}", e.getMessage());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void populateChannels(Map<String, ChannelProperty> channelPropertyMap) {
        channelPropertyMap.forEach((key, value) -> channels.addChannel(key, value));
    }


    private ConcurrentHashMap<String, ChannelProperty> readChannelsConfigsFromFile() {
        LOGGER.debug("Reading channel property from file.");
        try (InputStream input = new FileInputStream(ApplicationDirectoryUtil.getConfigFile(channelsBackupUrl))) {

            if (!ApplicationDirectoryUtil.doesFileExist(channelsBackupUrl)) {
                LOGGER.info("Creating \"{}\" file!", channelsBackupUrl);
                ApplicationDirectoryUtil.getConfigFile(channelsBackupUrl);
                return new ConcurrentHashMap<>();
            } else if (ApplicationDirectoryUtil.getConfigFile(channelsBackupUrl).length() == 0) {
                LOGGER.info("File \"{}\" is empty, no channels were loaded!", channelsBackupUrl);
                return new ConcurrentHashMap<>();
            }
            return new ObjectMapper().readValue(input,
                    new TypeReference<ConcurrentHashMap<String, ChannelProperty>>() {});
        } catch (IOException e) {
            LOGGER.error("Exception occurred during reading file {} with message : {}", channelsBackupUrl, e.getMessage());
        }
        return new ConcurrentHashMap<>();
    }

    private boolean checkChannelCompatibility(String channelPrefix) {
        List<String> allowedPublishers = publishers.getPublishersList();

        for (String publisher : allowedPublishers) {
            if (channelPrefix.contains(publisher)) {
                return true;
            }
        }
        return false;
    }
}
