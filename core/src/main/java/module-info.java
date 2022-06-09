module com.udacity.catpoint.core {
    exports com.udacity.catpoint.core;
    requires java.desktop;
    requires com.google.common;
    opens com.udacity.catpoint.core to com.google.gson;
}