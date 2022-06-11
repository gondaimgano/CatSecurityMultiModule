module com.udacity.catpoint.security {
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    requires com.udacity.catpoint.core;
    requires java.desktop;
    requires com.udacity.catpoint.image;
    exports com.udacity.catpoint.security;
    opens com.udacity.catpoint.security to com.udacity.catpoint.image;
}