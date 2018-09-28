package ru.misterparser.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.beans.property.SimpleStringProperty;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.io.File;

@Configurable
@Log4j2
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ConfigHolder {

    private static ObjectMapper mapper;

    private Config config;
    private Class<Config> configClass;
    private SimpleStringProperty filename;

    static {
        mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void save() {
        try {
            log.trace("Сохранение конфигурации в файл {}", filename.getValue());
            String json = mapper.writeValueAsString(config);
            FileUtils.write(new File(filename.getValue()), json, "UTF-8");
        } catch (Exception e) {
            log.debug("Exception", e);
        }
    }

    @PostConstruct
    private void load() {
        try {
            log.debug("Загрузка конфигурации из файла {}", filename.getValue());
            String t = FileUtils.readFileToString(new File(filename.getValue()), "UTF-8");
            config = mapper.readValue(t, configClass);
        } catch (Exception e1) {
            log.debug("Exception", e1);
            try {
                config = configClass.newInstance();
            } catch (Exception e2) {
                log.debug("Exception", e2);
            }
        }
    }

    @Scheduled(fixedDelay = 10000)
    private void autoSave() {
        log.trace("Автосохранение конфигурации...");
        save();
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setConfigClass(String className) throws ClassNotFoundException {
        this.configClass = (Class<Config>) Class.forName(className);
    }

    public <T extends Config> T getConfig() {
        return (T) config;
    }

    public void setFilename(SimpleStringProperty filename) {
        this.filename = filename;
    }
}
