using UnityEngine;
using TMPro;
using UnityEngine.UI;

/// <summary>
/// Debug/Test UI panel to verify backend connection from within the app.
/// Attach to a debug panel in the scene. Can be removed for production.
/// </summary>
public class ConnectionTestUI : MonoBehaviour
{
    [Header("UI Elements")]
    [SerializeField] private TMP_Text statusText;
    [SerializeField] private Button testConnectionBtn;
    [SerializeField] private Button loginBtn;
    [SerializeField] private TMP_InputField emailInput;
    [SerializeField] private TMP_InputField passwordInput;
    [SerializeField] private Button fetchProfileBtn;
    [SerializeField] private Button fetchCropsBtn;

    private void Start()
    {
        if (testConnectionBtn)
            testConnectionBtn.onClick.AddListener(TestConnection);
        if (loginBtn)
            loginBtn.onClick.AddListener(TestLogin);
        if (fetchProfileBtn)
            fetchProfileBtn.onClick.AddListener(FetchProfile);
        if (fetchCropsBtn)
            fetchCropsBtn.onClick.AddListener(FetchCrops);

        SetStatus("Ready. Tap 'Test Connection' to start.");
    }

    private void TestConnection()
    {
        SetStatus("Testing connection...");
        ApiClient.Instance.CheckHealth((ok, msg) =>
        {
            SetStatus(ok ? "CONNECTED to backend!" : $"FAILED: {msg}");
        });
    }

    private void TestLogin()
    {
        string email = emailInput ? emailInput.text : "";
        string password = passwordInput ? passwordInput.text : "";

        if (string.IsNullOrEmpty(email) || string.IsNullOrEmpty(password))
        {
            SetStatus("Enter email and password");
            return;
        }

        SetStatus("Logging in...");
        AuthService.Instance.Login(email, password);
        AuthService.Instance.OnLoginSuccess += () => SetStatus("LOGIN SUCCESS!");
        AuthService.Instance.OnLoginFailed += (err) => SetStatus($"LOGIN FAILED: {err}");
    }

    private void FetchProfile()
    {
        if (!ApiClient.Instance.IsAuthenticated)
        {
            SetStatus("Not logged in!");
            return;
        }

        SetStatus("Fetching profile...");
        AuthService.Instance.GetCurrentUser(
            user => SetStatus($"Welcome {user.fullName}\n{user.email}"),
            err => SetStatus($"Error: {err}")
        );
    }

    private void FetchCrops()
    {
        if (!ApiClient.Instance.IsAuthenticated)
        {
            SetStatus("Not logged in!");
            return;
        }

        SetStatus("Fetching crops...");
        PlantService.Instance.GetCropsOverview(
            data => SetStatus($"Crops: {data.plannedCount} planned, {data.plantedCount} planted, {data.harvestedCount} harvested"),
            err => SetStatus($"Error: {err}")
        );
    }

    private void SetStatus(string msg)
    {
        Debug.Log($"[ConnectionTest] {msg}");
        if (statusText) statusText.text = msg;
    }
}
