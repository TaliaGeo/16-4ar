using UnityEngine;

/// <summary>
/// Reads Android intent extras (JWT token, API base URL) passed from
/// Flutter or any hosting app. Static access — no MonoBehaviour needed.
/// </summary>
public static class ARPlatformBridge
{
    public static string JwtToken  { get; private set; } = "";
    public static string ApiBaseUrl { get; private set; } = "";

    /// <summary>
    /// Read intent extras. Safe to call multiple times;
    /// re-reads only if values are still empty.
    /// </summary>
    public static void ReadIntentExtras()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!string.IsNullOrEmpty(JwtToken)) return;
        try
        {
            using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            using var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            using var intent = activity.Call<AndroidJavaObject>("getIntent");
            using var extras = intent.Call<AndroidJavaObject>("getExtras");
            if (extras != null)
            {
                JwtToken  = extras.Call<string>("getString", "jwt_token") ?? "";
                ApiBaseUrl = extras.Call<string>("getString", "api_base_url") ?? "";
            }
            Debug.Log($"[ARPlatformBridge] baseUrl={ApiBaseUrl}, tokenPresent={!string.IsNullOrEmpty(JwtToken)}");
        }
        catch (System.Exception e)
        {
            Debug.LogError("[ARPlatformBridge] Failed to read intent extras: " + e.Message);
        }
#endif
    }
}
