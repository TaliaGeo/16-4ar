using UnityEngine;

/// <summary>
/// Centralized backend configuration. Attach to a persistent GameObject or use as ScriptableObject.
/// </summary>
[CreateAssetMenu(fileName = "BackendConfig", menuName = "Gharsih/Backend Config")]
public class BackendConfig : ScriptableObject
{
    [Header("Server")]
    [Tooltip("Base URL of the Spring Boot backend (no trailing slash). Use PC WiFi IP for real device.")]
    public string baseUrl = "http://192.168.1.217:8081";

    [Header("Endpoints")]
    public string authLogin = "/api/auth/login";
    public string authRegister = "/api/auth/register";
    public string authRefreshToken = "/api/auth/refresh-token";
    public string userMe = "/api/users/me";
    public string userPreferencesLocation = "/api/user/preferences/location";
    public string userPreferencesCities = "/api/user/preferences/cities";
    public string homeWeather = "/api/user/home/weather";
    public string homeDailyQuote = "/api/user/home/daily-quote";
    public string homeCalendar = "/api/user/home/calendar";
    public string plantQuestions = "/api/user/plant-recommendation/questions";
    public string plantSubmitAnswers = "/api/user/plant-recommendation/submit-answers";
    public string myCropsOverview = "/api/user/my-crops/overview";
    public string myCropsPlanned = "/api/user/my-crops/planned";
    public string myCropsPlanted = "/api/user/my-crops/planted";
    public string notifications = "/api/user/notifications";
    public string notificationsUnreadCount = "/api/user/notifications/unread-count";
    public string healthCheck = "/api/security/health";

    [Header("Auto Login (set for demo/testing)")]
    [Tooltip("Email to auto-login with on startup. Leave empty to skip auto-login.")]
    public string autoLoginEmail = "";
    [Tooltip("Password for auto-login.")]
    public string autoLoginPassword = "";

    public string GetFullUrl(string endpoint) => baseUrl + endpoint;
}
