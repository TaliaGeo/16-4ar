using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using UnityEngine;
using UnityEngine.Networking;

/// <summary>
/// Core HTTP client for the Gharsih backend. Handles JWT auth headers,
/// JSON serialization, and common request/response patterns.
/// </summary>
public class ApiClient : MonoBehaviour
{
    [SerializeField] private BackendConfig config;

    private string _accessToken;
    private string _refreshToken;

    private const string TOKEN_KEY = "gharsih_access_token";
    private const string REFRESH_KEY = "gharsih_refresh_token";

    public BackendConfig Config => config;
    public bool IsAuthenticated => !string.IsNullOrEmpty(_accessToken);
    public string AccessToken => _accessToken;

    public static ApiClient Instance { get; private set; }

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
        DontDestroyOnLoad(gameObject);
        LoadTokens();
    }

    // ──────────── Token Management ────────────

    public void SetTokens(string accessToken, string refreshToken)
    {
        _accessToken = accessToken;
        _refreshToken = refreshToken;
        SaveTokens();
    }

    public void ClearTokens()
    {
        _accessToken = null;
        _refreshToken = null;
        PlayerPrefs.DeleteKey(TOKEN_KEY);
        PlayerPrefs.DeleteKey(REFRESH_KEY);
        PlayerPrefs.Save();
    }

    private void SaveTokens()
    {
        if (!string.IsNullOrEmpty(_accessToken))
            PlayerPrefs.SetString(TOKEN_KEY, _accessToken);
        if (!string.IsNullOrEmpty(_refreshToken))
            PlayerPrefs.SetString(REFRESH_KEY, _refreshToken);
        PlayerPrefs.Save();
    }

    private void LoadTokens()
    {
        _accessToken = PlayerPrefs.GetString(TOKEN_KEY, "");
        _refreshToken = PlayerPrefs.GetString(REFRESH_KEY, "");

#if UNITY_ANDROID && !UNITY_EDITOR
        // Always prefer token provided by Flutter Intent so AR app uses the same frontend user.
        TryLoadTokenFromFlutterIntent();
#endif

        Debug.Log("[ApiClient] Token state loaded. Authenticated=" + IsAuthenticated);
    }

#if UNITY_ANDROID && !UNITY_EDITOR
    public void TryLoadTokenFromFlutterIntent()
    {
        try
        {
            using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using (var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
            using (var intent = activity.Call<AndroidJavaObject>("getIntent"))
            {
                string intentToken = intent.Call<string>("getStringExtra", "jwt_token");
                string intentBaseUrl = intent.Call<string>("getStringExtra", "api_base_url");

                if (!string.IsNullOrEmpty(intentBaseUrl) && config != null && config.baseUrl != intentBaseUrl)
                {
                    config.baseUrl = intentBaseUrl;
                    Debug.Log("[ApiClient] Backend base URL refreshed from Flutter Intent: " + config.baseUrl);
                }

                if (string.IsNullOrEmpty(intentToken))
                {
                    return;
                }

                if (_accessToken != intentToken)
                {
                    _accessToken = intentToken;
                    PlayerPrefs.SetString(TOKEN_KEY, _accessToken);
                    PlayerPrefs.Save();
                    Debug.Log("[ApiClient] JWT refreshed from Flutter Intent.");
                }
            }
        }
        catch (Exception e)
        {
            Debug.LogWarning("[ApiClient] Could not read Intent extra: " + e.Message);
        }
    }
#endif

    // ──────────── HTTP Methods ────────────

    /// <summary>GET request with auth header.</summary>
    public Coroutine Get(string endpoint, Action<ApiResponse> callback)
    {
        return StartCoroutine(SendRequest("GET", endpoint, null, callback));
    }

    /// <summary>POST request with JSON body.</summary>
    public Coroutine Post(string endpoint, object body, Action<ApiResponse> callback)
    {
        string json = body != null ? JsonUtility.ToJson(body) : null;
        return StartCoroutine(SendRequest("POST", endpoint, json, callback));
    }

    /// <summary>POST request with raw JSON string body.</summary>
    public Coroutine PostRaw(string endpoint, string json, Action<ApiResponse> callback)
    {
        return StartCoroutine(SendRequest("POST", endpoint, json, callback));
    }

    /// <summary>PUT request with JSON body.</summary>
    public Coroutine Put(string endpoint, object body, Action<ApiResponse> callback)
    {
        string json = body != null ? JsonUtility.ToJson(body) : null;
        return StartCoroutine(SendRequest("PUT", endpoint, json, callback));
    }

    /// <summary>DELETE request.</summary>
    public Coroutine Delete(string endpoint, Action<ApiResponse> callback)
    {
        return StartCoroutine(SendRequest("DELETE", endpoint, null, callback));
    }

    /// <summary>Upload a file (multipart/form-data).</summary>
    public Coroutine UploadFile(string endpoint, byte[] fileData, string fileName,
        string fieldName, Action<ApiResponse> callback)
    {
        return StartCoroutine(UploadFileCoroutine(endpoint, fileData, fileName, fieldName, null, callback));
    }

    /// <summary>Upload multipart form with file + text fields.</summary>
    public Coroutine UploadMultipart(string endpoint, byte[] fileData, string fileName,
        string fieldName, Dictionary<string, string> fields, Action<ApiResponse> callback)
    {
        return StartCoroutine(UploadFileCoroutine(endpoint, fileData, fileName, fieldName, fields, callback));
    }

    // ──────────── Core Request ────────────

    private IEnumerator SendRequest(string method, string endpoint, string jsonBody,
        Action<ApiResponse> callback)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        TryLoadTokenFromFlutterIntent();
#endif

        string url = config.GetFullUrl(endpoint);
        Debug.Log("[API] " + method + " " + url);

        UnityWebRequest request = new UnityWebRequest(url, method);

        if (!string.IsNullOrEmpty(jsonBody))
        {
            byte[] bodyRaw = Encoding.UTF8.GetBytes(jsonBody);
            request.uploadHandler = new UploadHandlerRaw(bodyRaw);
            request.SetRequestHeader("Content-Type", "application/json");
        }

        request.downloadHandler = new DownloadHandlerBuffer();
        request.certificateHandler = new AcceptAllCertificates();

        // Attach JWT if available
        if (!string.IsNullOrEmpty(_accessToken))
            request.SetRequestHeader("Authorization", "Bearer " + _accessToken);

        yield return request.SendWebRequest();

        ApiResponse response = new ApiResponse
        {
            statusCode = (int)request.responseCode,
            body = request.downloadHandler != null ? request.downloadHandler.text : null,
            isSuccess = request.result == UnityWebRequest.Result.Success,
            error = request.error
        };
        request.Dispose();

        // Handle 401 — try token refresh
        if (response.statusCode == 401 && !string.IsNullOrEmpty(_refreshToken))
        {
            yield return StartCoroutine(TryRefreshToken(success =>
            {
                if (success)
                    StartCoroutine(SendRequest(method, endpoint, jsonBody, callback));
                else
                {
                    response.error = "Session expired. Please login again.";
                    callback?.Invoke(response);
                }
            }));
            yield break;
        }

        // Handle 429 — rate limited, retry once after short delay
        if (response.statusCode == 429)
        {
            Debug.LogWarning("[API] Rate limited (429) — retrying after 2s");
            yield return new WaitForSecondsRealtime(2f);
            yield return StartCoroutine(SendRequest(method, endpoint, jsonBody, callback));
            yield break;
        }

        int previewLen = response.body != null ? Mathf.Min(200, response.body.Length) : 0;
        Debug.Log("[API] Response " + response.statusCode + ": " + (response.body != null ? response.body.Substring(0, previewLen) : ""));
        callback?.Invoke(response);
    }

    private IEnumerator UploadFileCoroutine(string endpoint, byte[] fileData, string fileName,
        string fieldName, Dictionary<string, string> fields, Action<ApiResponse> callback)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        TryLoadTokenFromFlutterIntent();
#endif

        string url = config.GetFullUrl(endpoint);
        Debug.Log("[API] UPLOAD " + url);

        List<IMultipartFormSection> form = new List<IMultipartFormSection>
        {
            new MultipartFormFileSection(fieldName, fileData, fileName, "image/png")
        };

        if (fields != null)
        {
            foreach (var kv in fields)
            {
                string value = kv.Value;
                if (string.IsNullOrEmpty(value)) continue;
                form.Add(new MultipartFormDataSection(kv.Key, value));
                Debug.Log($"[API] Multipart field {kv.Key}={value}");
            }
        }

        UnityWebRequest request = UnityWebRequest.Post(url, form);
        request.timeout = 30;

        if (!string.IsNullOrEmpty(_accessToken))
            request.SetRequestHeader("Authorization", "Bearer " + _accessToken);
        request.certificateHandler = new AcceptAllCertificates();

        yield return request.SendWebRequest();

        ApiResponse response = new ApiResponse
        {
            statusCode = (int)request.responseCode,
            body = request.downloadHandler != null ? request.downloadHandler.text : null,
            isSuccess = request.result == UnityWebRequest.Result.Success,
            error = request.error
        };
        request.Dispose();

        if (response.statusCode == 401 && !string.IsNullOrEmpty(_refreshToken))
        {
            bool refreshed = false;
            yield return StartCoroutine(TryRefreshToken(success => refreshed = success));

            if (refreshed)
            {
                yield return StartCoroutine(UploadFileCoroutine(endpoint, fileData, fileName, fieldName, fields, callback));
                yield break;
            }
        }

        // Handle 429 — rate limited, retry once after short delay
        if (response.statusCode == 429)
        {
            Debug.LogWarning("[API] Upload rate limited (429) — retrying after 2s");
            yield return new WaitForSecondsRealtime(2f);
            yield return StartCoroutine(UploadFileCoroutine(endpoint, fileData, fileName, fieldName, fields, callback));
            yield break;
        }

        int previewLen = response.body != null ? Mathf.Min(200, response.body.Length) : 0;
        Debug.Log("[API] Upload response " + response.statusCode + ": " +
            (response.body != null ? response.body.Substring(0, previewLen) : response.error));

        callback?.Invoke(response);
    }

    private IEnumerator TryRefreshToken(Action<bool> callback)
    {
        string url = config.GetFullUrl(config.authRefreshToken);
        string json = "{\"refreshToken\":\"" + _refreshToken + "\"}";

        UnityWebRequest request = new UnityWebRequest(url, "POST");
        byte[] bodyRaw = Encoding.UTF8.GetBytes(json);
        request.uploadHandler = new UploadHandlerRaw(bodyRaw);
        request.downloadHandler = new DownloadHandlerBuffer();
        request.SetRequestHeader("Content-Type", "application/json");
        request.certificateHandler = new AcceptAllCertificates();

        yield return request.SendWebRequest();

        if (request.result == UnityWebRequest.Result.Success)
        {
            TokenResponse tokenResponse = JsonUtility.FromJson<TokenResponse>(request.downloadHandler.text);
            request.Dispose();
            if (!string.IsNullOrEmpty(tokenResponse.accessToken))
            {
                SetTokens(tokenResponse.accessToken,
                    !string.IsNullOrEmpty(tokenResponse.refreshToken) ? tokenResponse.refreshToken : _refreshToken);
                callback?.Invoke(true);
                yield break;
            }
        }
        else
        {
            request.Dispose();
        }

        ClearTokens();
        callback?.Invoke(false);
    }

    // ──────────── Health Check ────────────

    public Coroutine CheckHealth(Action<bool, string> callback)
    {
        return StartCoroutine(HealthCheckCoroutine(callback));
    }

    private IEnumerator HealthCheckCoroutine(Action<bool, string> callback)
    {
        string url = config.GetFullUrl(config.healthCheck);
        UnityWebRequest request = UnityWebRequest.Get(url);
        request.timeout = 5;
        request.certificateHandler = new AcceptAllCertificates();

        yield return request.SendWebRequest();

        bool connected = request.result == UnityWebRequest.Result.Success;
        string msg = connected ? "Connected to Gharsih backend" : "Connection failed: " + request.error;
        request.Dispose();

        Debug.Log("[API] Health: " + msg);
        callback?.Invoke(connected, msg);
    }
}

// ──────────── Certificate Handler (local dev: skip cert CN check) ────────────

/// <summary>
/// Bypasses SSL certificate validation for local backend development.
/// The backend runs on a local IP (HTTP), so cert CN will never match.
/// Remove or restrict this before releasing to production.
/// </summary>
public class AcceptAllCertificates : CertificateHandler
{
    protected override bool ValidateCertificate(byte[] certificateData) => true;
}

// ──────────── Data Classes ────────────

[Serializable]
public class ApiResponse
{
    public int statusCode;
    public string body;
    public bool isSuccess;
    public string error;

    public T Parse<T>() => JsonUtility.FromJson<T>(body);
}

[Serializable]
public class TokenResponse
{
    public string accessToken;
    public string refreshToken;
    public string tokenType;
    public long expiresIn;
}
