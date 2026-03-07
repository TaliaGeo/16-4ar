using System;
using UnityEngine;

/// <summary>
/// Handles user authentication: login, register, logout, and session state.
/// </summary>
public class AuthService : MonoBehaviour
{
    public static AuthService Instance { get; private set; }

    public bool IsLoggedIn => ApiClient.Instance != null && ApiClient.Instance.IsAuthenticated;

    public event Action OnLoginSuccess;
    public event Action<string> OnLoginFailed;
    public event Action OnLogout;

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
    }

    // ──────────── Login ────────────

    public void Login(string email, string password)
    {
        var body = new LoginRequest { email = email, password = password };
        ApiClient.Instance.Post(ApiClient.Instance.Config.authLogin, body, response =>
        {
            if (response.isSuccess)
            {
                var token = response.Parse<TokenResponse>();
                ApiClient.Instance.SetTokens(token.accessToken, token.refreshToken);
                Debug.Log("[Auth] Login successful");
                OnLoginSuccess?.Invoke();
            }
            else
            {
                string error = TryParseError(response.body) ?? response.error ?? "Login failed";
                Debug.LogWarning($"[Auth] Login failed: {error}");
                OnLoginFailed?.Invoke(error);
            }
        });
    }

    // ──────────── Register ────────────

    public void Register(string fullName, string email, string password,
        Action<bool, string> callback)
    {
        var body = new RegisterRequest
        {
            fullName = fullName,
            email = email,
            password = password
        };

        ApiClient.Instance.Post(ApiClient.Instance.Config.authRegister, body, response =>
        {
            if (response.isSuccess)
            {
                var token = response.Parse<TokenResponse>();
                ApiClient.Instance.SetTokens(token.accessToken, token.refreshToken);
                Debug.Log("[Auth] Registration successful");
                OnLoginSuccess?.Invoke();
                callback?.Invoke(true, "Registration successful");
            }
            else
            {
                string error = TryParseError(response.body) ?? response.error ?? "Registration failed";
                callback?.Invoke(false, error);
            }
        });
    }

    // ──────────── Logout ────────────

    public void Logout()
    {
        ApiClient.Instance.ClearTokens();
        Debug.Log("[Auth] Logged out");
        OnLogout?.Invoke();
    }

    // ──────────── Get Current User ────────────

    public void GetCurrentUser(Action<UserProfile> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.userMe, response =>
        {
            if (response.isSuccess)
            {
                var user = response.Parse<UserProfile>();
                onSuccess?.Invoke(user);
            }
            else
            {
                onError?.Invoke(response.error ?? "Failed to get user profile");
            }
        });
    }

    private string TryParseError(string json)
    {
        if (string.IsNullOrEmpty(json)) return null;
        try
        {
            var err = JsonUtility.FromJson<ErrorResponse>(json);
            return err?.message;
        }
        catch { return null; }
    }
}

// ──────────── DTOs ────────────

[Serializable]
public class LoginRequest
{
    public string email;
    public string password;
}

[Serializable]
public class RegisterRequest
{
    public string fullName;
    public string email;
    public string password;
}

[Serializable]
public class UserProfile
{
    public long id;
    public string fullName;
    public string email;
    public string profilePicture;
    public bool active;
}

[Serializable]
public class ErrorResponse
{
    public string message;
    public string error;
    public int status;
}
