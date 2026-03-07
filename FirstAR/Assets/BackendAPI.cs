using UnityEngine;

/// <summary>
/// Legacy BackendAPI kept for backward compatibility.
/// The real connection logic is now in Scripts/Backend/.
/// This script verifies the Backend services are available on Start.
/// </summary>
public class BackendAPI : MonoBehaviour
{
    void Start()
    {
        if (ApiClient.Instance != null)
        {
            ApiClient.Instance.CheckHealth((connected, message) =>
            {
                if (connected)
                    Debug.Log("[BackendAPI] ✅ Backend is reachable");
                else
                    Debug.LogWarning("[BackendAPI] ⚠️ Backend not reachable: " + message);
            });
        }
        else
        {
            Debug.LogWarning("[BackendAPI] ApiClient not found. Add BackendManager to the scene.");
        }
    }
}