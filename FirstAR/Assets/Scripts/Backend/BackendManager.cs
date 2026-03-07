using System;
using UnityEngine;
using UnityEngine.Events;

/// <summary>
/// Main entry point for all backend connectivity. 
/// Attach this to a "BackendManager" GameObject in the scene.
/// It initializes all services and performs health check on startup.
/// </summary>
public class BackendManager : MonoBehaviour
{
    [Header("Configuration")]
    [SerializeField] private BackendConfig config;

    [Header("Status")]
    [SerializeField] private bool autoConnectOnStart = true;

    [Header("Events")]
    public UnityEvent OnConnected;
    public UnityEvent<string> OnConnectionFailed;

    public bool IsConnected { get; private set; }
    public static BackendManager Instance { get; private set; }

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
        DontDestroyOnLoad(gameObject);
    }

    private void Start()
    {
        if (autoConnectOnStart)
            TestConnection();
    }

    /// <summary>
    /// Test backend connectivity with health check endpoint.
    /// </summary>
    public void TestConnection()
    {
        if (ApiClient.Instance == null)
        {
            Debug.LogError("[BackendManager] ApiClient not found! Make sure it's on a GameObject in the scene.");
            OnConnectionFailed?.Invoke("ApiClient not initialized");
            return;
        }

        Debug.Log($"[BackendManager] Testing connection to {config.baseUrl}...");

        ApiClient.Instance.CheckHealth((connected, message) =>
        {
            IsConnected = connected;

            if (connected)
            {
                Debug.Log($"[BackendManager] {message}");
                OnConnected?.Invoke();

                // If user has saved tokens, verify they're still valid
                if (ApiClient.Instance.IsAuthenticated)
                    ValidateSession();
                else
                    TryAutoLogin(); // No saved token — auto-login on startup
            }
            else
            {
                Debug.LogWarning($"[BackendManager] {message}");
                OnConnectionFailed?.Invoke(message);
            }
        });
    }

    private void ValidateSession()
    {
        AuthService.Instance?.GetCurrentUser(
            user => Debug.Log($"[BackendManager] Session valid — Welcome back, {user.fullName}!"),
            error =>
            {
                Debug.LogWarning($"[BackendManager] Saved session invalid: {error}");
                ApiClient.Instance.ClearTokens();
                // Session expired — try auto-login again
                TryAutoLogin();
            }
        );
    }

    private void TryAutoLogin()
    {
        if (config == null) return;
        string email = config.autoLoginEmail;
        string password = config.autoLoginPassword;
        if (string.IsNullOrEmpty(email) || string.IsNullOrEmpty(password)) return;

        Debug.Log($"[BackendManager] Auto-login as {email}...");
        AuthService.Instance?.Login(email, password);
        if (AuthService.Instance != null)
        {
            AuthService.Instance.OnLoginSuccess += () =>
                Debug.Log("[BackendManager] Auto-login successful — uploads enabled");
            AuthService.Instance.OnLoginFailed += err =>
                Debug.LogWarning($"[BackendManager] Auto-login failed: {err}");
        }
    }

    /// <summary>
    /// Quick login for testing. Call from UI or test scripts.
    /// </summary>
    public void QuickLogin(string email, string password, Action<bool, string> callback)
    {
        AuthService.Instance.OnLoginSuccess += () => callback?.Invoke(true, "Login successful");
        AuthService.Instance.OnLoginFailed += (err) => callback?.Invoke(false, err);
        AuthService.Instance.Login(email, password);
    }
}
