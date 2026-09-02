package fi.helsinki.cs.tmc.intellij.io;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.OauthCredentials;

import com.google.common.base.Optional;

import com.intellij.util.xmlb.annotations.Property;

import com.intellij.openapi.application.ApplicationInfo;

import fi.helsinki.cs.tmc.core.domain.Organization;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import javax.swing.filechooser.FileSystemView;

/** TMC Settings component from Core, has all the necessary settings. */
public class SettingsTmc implements TmcSettings, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SettingsTmc.class);
    @Property private String username;
    @Property private String password;
    @Property private Course course;
    @Property private Organization organization;
    @Property private String token;
    @Property private OauthCredentials oauthCredentials;
    @Property private String serverAddress;
    @Property private String projectBasePath;
    @Property private boolean checkForExercises;
    @Property private boolean sendDiagnostics;
    @Property private boolean firstRun;

    public SettingsTmc(String serverAddress, String username, String password) {
        this.sendDiagnostics = true;
        this.checkForExercises = false;
        this.serverAddress = serverAddress;
        this.username = username;
        this.password = password;
        this.firstRun = true;
    }

    /** Sets the default folder for TMC project files -> home/IdeaProjects/TMCProjects . */
    public SettingsTmc() {
        this.sendDiagnostics = true;
        this.checkForExercises = false;
        this.firstRun = true;
        logger.info("Setting default folder for TMC project files. @SettingsTmc");
        serverAddress = "https://tmc.mooc.fi/";
        projectBasePath =
                FileSystemView.getFileSystemView().getDefaultDirectory().toString()
                        + File.separator
                        + "IdeaProjects"
                        + File.separator
                        + "TMCProjects";
    }

    public boolean isCheckForExercises() {
        return checkForExercises;
    }

    public void setCheckForExercises(boolean checkForExercises) {
        this.checkForExercises = checkForExercises;
    }

    public void setUsername(String username) {
        logger.info("Setting username -> {}. @SettingsTmc", username);
        this.username = username == null || username.trim().isEmpty() ? null : username.trim();
    }

    public void setPassword(String password) {
        logger.info("Setting password. @SettingsTmc");
        this.password = password == null || password.isEmpty() ? null : password;
    }

    public String getCourseName() {
        String courseName = course == null ? null : course.getName();
        logger.info("Getting course name <- {}. @SettingsTmc", courseName);
        return courseName;
    }

    public void setServerAddress(String serverAddress) {
        logger.info("Setting server address -> {}. @SettingsTmc", serverAddress);
        this.serverAddress = serverAddress;
    }

    public String getProjectBasePath() {
        logger.info("Getting project base path <- {}. @SettingsTmc", projectBasePath);
        if (projectBasePath == null || projectBasePath.trim().isEmpty()) {
            projectBasePath =
                    FileSystemView.getFileSystemView().getDefaultDirectory().toString()
                            + File.separator
                            + "IdeaProjects"
                            + File.separator
                            + "TMCProjects";
        }
        return projectBasePath;
    }

    public void setProjectBasePath(String projectBasePath) {
        logger.info("Setting project base path -> {}. @SettingsTmc", projectBasePath);
        if (projectBasePath == null || projectBasePath.trim().isEmpty()) {
            this.projectBasePath = getProjectBasePath();
        } else if (projectBasePath.contains("TMCProjects")) {
            this.projectBasePath = projectBasePath;
        } else {
            this.projectBasePath = projectBasePath + File.separator + "TMCProjects";
        }
    }

    @Override
    public String getServerAddress() {
        logger.info("Getting server address <- {}. @SettingsTmc", serverAddress);
        if (serverAddress == null || serverAddress.trim().isEmpty()) {
            serverAddress = "https://tmc.mooc.fi/";
        }
        return serverAddress;
    }

    @Override
    public Optional<String> getPassword() {
        logger.info("Getting user password. @SettingsTmc");
        return Optional.fromNullable(password);
    }

    @Override
    public void setPassword(Optional<String> password) {
        logger.info("Setting password. @SettingsTmc");
        this.password = password.isPresent() && !password.get().isEmpty()
                ? password.get()
                : null;
    }

    @Override
    public Optional<String> getUsername() {
        logger.info("Getting username <- {}. @SettingsTmc", username);
        return Optional.fromNullable(username);
    }

    @Override
    public boolean userDataExists() {
        logger.info("Checking if user data exists. @SettingsTmc");
        return this.username != null && !this.username.isEmpty();
    }

    @Override
    public Optional<Course> getCurrentCourse() {
        logger.info("Getting current course <- {}. @SettingsTmc", course);
        return Optional.fromNullable(course);
    }

    @Override
    public String clientName() {
        return "idea_plugin";
    }

    @Override
    public String clientVersion() {
        return "2.2.0";
    }

    @Override
    public Path getTmcProjectDirectory() {
        return Paths.get(projectBasePath);
    }

    @Override
    public Locale getLocale() {
        logger.info("Getting locale. @SettingsTmc");
        return Locale.ENGLISH;
    }

    @Override
    public SystemDefaultRoutePlanner proxy() {
        // implement?
        return null;
    }

    @Override
    public void setCourse(Optional<Course> course) {
        logger.info("Setting course -> {}. @SettingsTmc", course);
        this.course = course.orNull();
    }

    @Override
    public Path getConfigRoot() {
        return FileSystemView.getFileSystemView().getDefaultDirectory().toPath();
    }

    @Override
    public boolean getSendDiagnostics() {
        return this.sendDiagnostics;
    }

    @Override
    public Optional<OauthCredentials> getOauthCredentials() {
        logger.info("Getting oauthCredentials. @SettingsTmc");
        return Optional.fromNullable(this.oauthCredentials);
    }

    public void setOauthCredentials(Optional<OauthCredentials> oauthCredentials) {
        logger.info("Setting oauthCredentials. @SettingsTmc");
        this.oauthCredentials = oauthCredentials.orNull();
    }

    @Override
    public void setToken(Optional<String> token) {
        logger.info("Setting token. @SettingsTmc");
        this.token = token.orNull();
    }

    @Override
    public Optional<String> getToken() {
        logger.info("Getting token. @SettingsTmc");
        return Optional.fromNullable(token);
    }

    @Override
    public Optional<Organization> getOrganization() {
        logger.info("Getting organization <- {}", organization);
        return Optional.fromNullable(this.organization);
    }

    @Override
    public void setOrganization(Optional<Organization> org) {
        if (org.isPresent()) {
            logger.info("Setting organization -> {}", org.get().getName());
        }
        this.organization = org.orNull();
    }

    public void setSendDiagnostics(boolean value) {
        this.sendDiagnostics = value;
    }

    @Override
    public String hostProgramName() {
        return ApplicationInfo.getInstance().getVersionName();
    }

    @Override
    public String hostProgramVersion() {
        return ApplicationInfo.getInstance().getFullVersion();
    }

    public void setFirstRun(boolean value) {
        this.firstRun = value;
    }

    public boolean getFirstRun() {
        return this.firstRun;
    }
}
