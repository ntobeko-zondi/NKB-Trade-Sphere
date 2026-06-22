package com.example.nkbtradesphere.network;

/**
 * Single API configuration point for the whole app.
 * If api.php is uploaded to a different public folder, change the URL here only.
 */
public final class ApiConfig {
    private ApiConfig() {}

    public static final String API_URL = "https://wmc.ms.wits.ac.za/students/sgroup2729/api.php";

    public static String pingUrl() {
        return API_URL + "?action=ping";
    }

    public static String dbTestUrl() {
        return API_URL + "?action=dbTest";
    }
}
