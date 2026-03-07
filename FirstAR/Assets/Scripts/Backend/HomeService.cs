using System;
using UnityEngine;

/// <summary>
/// Handles home page data: weather, daily quote, calendar.
/// </summary>
public class HomeService : MonoBehaviour
{
    public static HomeService Instance { get; private set; }

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
    }

    public void GetWeather(Action<WeatherResponse> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.homeWeather, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<WeatherResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to get weather");
        });
    }

    public void GetDailyQuote(Action<DailyQuoteResponse> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.homeDailyQuote, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<DailyQuoteResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to get quote");
        });
    }

    public void GetCalendar(Action<CalendarResponse> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.homeCalendar, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<CalendarResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to get calendar");
        });
    }

    public void GetNotificationCount(Action<int> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.notificationsUnreadCount, response =>
        {
            if (response.isSuccess)
            {
                var data = response.Parse<UnreadCountResponse>();
                onSuccess?.Invoke(data.count);
            }
            else
            {
                onError?.Invoke(response.error ?? "Failed to get notifications");
            }
        });
    }
}

// ──────────── DTOs ────────────

[Serializable]
public class WeatherResponse
{
    public string city;
    public float temperature;
    public float humidity;
    public string description;
    public string iconCode;
    public string recommendation;
}

[Serializable]
public class DailyQuoteResponse
{
    public string textAr;
    public string textEn;
    public string author;
    public string category;
}

[Serializable]
public class CalendarResponse
{
    public CalendarMonth[] months;
}

[Serializable]
public class CalendarMonth
{
    public int monthNumber;
    public string nameAr;
    public string nameEn;
    public string season;
    public string[] plantNames;
}

[Serializable]
public class UnreadCountResponse
{
    public int count;
}
