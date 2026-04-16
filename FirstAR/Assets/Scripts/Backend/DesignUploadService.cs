using System;
using System.Collections.Generic;
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
    public void UploadDesign(byte[] pngData,
        string designName,
        string projectName,
        string designSide,
        string clientTimestamp,
        string metadataJson)
    {
        Debug.Log($"[DesignUpload] Save requested name={designName}, project={projectName}, side={designSide}, bytes={(pngData != null ? pngData.Length : 0)}");

#if UNITY_ANDROID && !UNITY_EDITOR
        ApiClient.Instance.TryLoadTokenFromFlutterIntent();
#endif

        if (!ApiClient.Instance.IsAuthenticated)
        {
            Debug.LogWarning("[DesignUpload] Not authenticated — saving locally only");
            OnUploadFailed?.Invoke("Not logged in");
            return;
        }

        if (pngData == null || pngData.Length == 0)
        {
            Debug.LogWarning("[DesignUpload] No file data found in upload request.");
            OnUploadFailed?.Invoke("No file data");
            return;
        }

        string fileName = designName + ".png";
        var fields = new Dictionary<string, string>
        {
            { "designName", string.IsNullOrEmpty(designName) ? "untitled" : designName },
            { "projectName", string.IsNullOrEmpty(projectName) ? "default" : projectName },
            { "designSide", string.IsNullOrEmpty(designSide) ? "unknown" : designSide },
            { "clientTimestamp", string.IsNullOrEmpty(clientTimestamp) ? DateTime.UtcNow.ToString("o") : clientTimestamp },
            { "metadataJson", string.IsNullOrEmpty(metadataJson) ? "{}" : metadataJson }
        };

        ApiClient.Instance.UploadMultipart("/api/user/designs/upload", pngData, fileName, "file", fields, response =>
        {
            if (response.isSuccess)
            {
                Debug.Log($"[DesignUpload] Uploaded successfully: {designName}. Response={response.body}");
                OnUploadSuccess?.Invoke(designName);
            }
            else
            {
                Debug.LogWarning($"[DesignUpload] Upload failed status={response.statusCode}, error={response.error}, body={response.body}");
                string message = response.body ?? response.error ?? "Upload failed";
                if (response.statusCode == 413)
                {
                    message = "File too large for backend upload limit";
                }
                OnUploadFailed?.Invoke(message);
            }
        });
    }

    // ──────────── List Designs (newest first) ────────────

    /// <summary>Fetch user's designs, sorted newest-first by the backend.</summary>
    public void ListDesigns(Action<DesignListResponse> onSuccess, Action<string> onError = null)
    {
        string endpoint = ApiClient.Instance.Config.designsList + "?sort=createdAt,desc";
        ApiClient.Instance.Get(endpoint, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<DesignListResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to load designs");
        });
    }

    // ──────────── Rename Design ────────────

    public void RenameDesign(long designId, string newName, Action<bool, string> callback)
    {
        string endpoint = ApiClient.Instance.Config.designsRename.Replace("{id}", designId.ToString());
        var body = new RenameDesignRequest { designName = newName };
        ApiClient.Instance.Put(endpoint, body, response =>
        {
            callback?.Invoke(response.isSuccess,
                response.isSuccess ? "Renamed" : response.error ?? "Rename failed");
        });
    }

    // ──────────── Delete Design ────────────

    public void DeleteDesign(long designId, Action<bool, string> callback)
    {
        string endpoint = ApiClient.Instance.Config.designsDelete.Replace("{id}", designId.ToString());
        ApiClient.Instance.Delete(endpoint, response =>
        {
            callback?.Invoke(response.isSuccess,
                response.isSuccess ? "Deleted" : response.error ?? "Delete failed");
        });
    }
}

// ──────────── DTOs ────────────

[Serializable]
public class DesignListResponse
{
    public DesignItem[] designs;
}

[Serializable]
public class DesignItem
{
    public long id;
    public string designName;
    public string imageUrl;
    public string createdAt;
    public string projectName;
}

[Serializable]
public class RenameDesignRequest
{
    public string designName;
}
