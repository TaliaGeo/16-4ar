using System;
using UnityEngine;

/// <summary>
/// Uploads AR design screenshots to the backend.
/// Can be called directly from ARPlacementManager after a screenshot is captured.
/// </summary>
public class DesignUploadService : MonoBehaviour
{
    public static DesignUploadService Instance { get; private set; }

    public event Action<string> OnUploadSuccess;
    public event Action<string> OnUploadFailed;

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
    }

    /// <summary>
    /// Upload a screenshot PNG to the backend.
    /// </summary>
    /// <param name="pngData">Raw PNG bytes from the screenshot.</param>
    /// <param name="designName">Name for the design file.</param>
    public void UploadDesign(byte[] pngData, string designName)
    {
        if (!ApiClient.Instance.IsAuthenticated)
        {
            Debug.LogWarning("[DesignUpload] Not authenticated — saving locally only");
            OnUploadFailed?.Invoke("Not logged in");
            return;
        }

        string fileName = designName + ".png";

        // Upload to a general-purpose upload endpoint
        // The backend can store this via its existing file upload infrastructure
        ApiClient.Instance.UploadFile("/api/user/designs/upload", pngData, fileName, "file", response =>
        {
            if (response.isSuccess)
            {
                Debug.Log($"[DesignUpload] Uploaded: {designName}");
                OnUploadSuccess?.Invoke(designName);
            }
            else
            {
                Debug.LogWarning($"[DesignUpload] Upload failed: {response.error}");
                OnUploadFailed?.Invoke(response.error ?? "Upload failed");
            }
        });
    }
}
