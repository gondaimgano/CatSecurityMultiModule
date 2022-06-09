module com.udacity.catpoint.security {
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires com.udacity.catpoint.core;
    requires java.desktop;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.repository;
    opens  com.udacity.catpoint.security.service;
}