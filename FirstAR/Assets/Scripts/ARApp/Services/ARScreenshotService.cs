using System;
using System.Collections;
using System.IO;
using System.Text.RegularExpressions;
using UnityEngine;
using UnityEngine.Networking;

/// <summary>
/// Captures a clean screenshot (no UI, no labels), saves to DCIM on
/// Android (or persistentDataPath elsewhere), and uploads via
/// DesignUploadService or raw JWT fallback.
/// </summary>
public class ARScreenshotService : MonoBehaviour
{
    [SerializeField] private string screenshotFolderName = "MyDesigns";

    /// <summary>Fired after the PNG is saved to disk. Arg = file path.</summary>
    public event Action<string> OnScreenshotSaved;

    /// <summary>
    /// External callbacks to hide / restore UI around the capture frame.
    /// Set by the controller at init time.
    /// </summary>
    public Action HideUI { get; set; }
    public Action RestoreUI { get; set; }

    /// <summary>
    /// External callback to temporarily hide all measurement labels.
    /// </summary>
    public Action HideLabels { get; set; }

    /// <summary>
    /// External callback to restore the previously-active label.
    /// </summary>
    public Action RestoreLabels { get; set; }

    /// <summary>Show feedback to the user.</summary>
    public Action<string> ShowToast { get; set; }

    /// <summary>JWT and API URL for fallback upload.</summary>
    public string JwtToken  { get; set; } = "";
    public string ApiBaseUrl { get; set; } = "";

    public void Capture() => Capture(null);

    public void Capture(string customDesignName) => StartCoroutine(CaptureCoroutine(customDesignName));

    private IEnumerator CaptureCoroutine(string customDesignName)
    {
        HideUI?.Invoke();
        HideLabels?.Invoke();

        yield return new WaitForEndOfFrame();

        Texture2D tex = ScreenCapture.CaptureScreenshotAsTexture();
        if (tex == null)
        {
            RestoreUI?.Invoke();
            RestoreLabels?.Invoke();
            ShowToast?.Invoke("Capture failed");
            yield break;
        }

        byte[] png = tex.EncodeToPNG();
        Destroy(tex);

        string designName = BuildDesignName(customDesignName);
        string fn = designName + ".png";
        string savedPath = null;

#if UNITY_ANDROID && !UNITY_EDITOR
        string folder = $"/storage/emulated/0/DCIM/{screenshotFolderName}/";
        try
        {
            Directory.CreateDirectory(folder);
            savedPath = Path.Combine(folder, fn);
            File.WriteAllBytes(savedPath, png);
            using (var scanClass = new AndroidJavaClass("android.media.MediaScannerConnection"))
            using (var player    = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using (var ctx       = player.GetStatic<AndroidJavaObject>("currentActivity"))
                scanClass.CallStatic("scanFile", ctx, new string[] { savedPath }, null, null);
            OnScreenshotSaved?.Invoke(savedPath);
        }
        catch (Exception e) { Debug.LogError("[ARScreenshot] Save failed: " + e.Message); }
#else
        string dir = Path.Combine(Application.persistentDataPath, screenshotFolderName);
        Directory.CreateDirectory(dir);
        savedPath = Path.Combine(dir, fn);
        File.WriteAllBytes(savedPath, png);
        OnScreenshotSaved?.Invoke(savedPath);
        Debug.Log("[ARScreenshot] Saved locally: " + savedPath);
#endif

        RestoreUI?.Invoke();
        RestoreLabels?.Invoke();

        // Upload via DesignUploadService (primary) or raw fallback
        if (DesignUploadService.Instance != null)
        {
            DesignUploadService.Instance.UploadDesign(
                png, designName, "AR Design", "front",
                DateTime.UtcNow.ToString("o"), "{}");
        }
        else
        {
            StartCoroutine(UploadFallback(png, fn));
        }

        ShowToast?.Invoke($"Saved: {designName}");
    }

    private string BuildDesignName(string customDesignName)
    {
        string trimmed = (customDesignName ?? string.Empty).Trim();
        if (string.IsNullOrEmpty(trimmed))
            return "MyDesign_" + DateTime.Now.ToString("yyyyMMdd_HHmmss");
        string sanitized = Regex.Replace(trimmed, "[^a-zA-Z0-9 _-]", "");
        sanitized = Regex.Replace(sanitized, "\\s+", " ").Trim();
        if (string.IsNullOrEmpty(sanitized))
            sanitized = "MyDesign_" + DateTime.Now.ToString("yyyyMMdd_HHmmss");
        return sanitized;
    }

    private IEnumerator UploadFallback(byte[] pngData, string fileName)
    {
        // Try ApiClient first
        if (ApiClient.Instance != null && ApiClient.Instance.IsAuthenticated)
        {
            string designName = Path.GetFileNameWithoutExtension(fileName);
            var fields = new System.Collections.Generic.Dictionary<string, string>
            {
                { "designName", designName },
                { "projectName", "AR Design" },
                { "designSide", "front" },
                { "clientTimestamp", DateTime.UtcNow.ToString("o") },
                { "metadataJson", "{}" }
            };
            ApiClient.Instance.UploadMultipart(
                "/api/user/designs/upload", pngData, fileName,
                "file", fields, response =>
                {
                    if (response.isSuccess)
                        Debug.Log("[ARScreenshot] Uploaded via ApiClient");
                    else
                        Debug.LogWarning("[ARScreenshot] Upload failed: " + response.error);
                });
            yield break;
        }

        // Raw JWT fallback
        if (string.IsNullOrEmpty(JwtToken))
        {
            Debug.LogWarning("[ARScreenshot] No JWT — saved locally only");
            yield break;
        }

        string baseUrl = ApiBaseUrl.TrimEnd('/');
        if (string.IsNullOrEmpty(baseUrl))
        {
            Debug.LogWarning("[ARScreenshot] No API URL configured \u2014 saved locally only");
            yield break;
        }

        string url = baseUrl + "/api/user/designs/upload";
        var form = new WWWForm();
        form.AddBinaryData("file", pngData, fileName ?? "ar_design.png", "image/png");
        form.AddField("designName", "AR Design " + DateTime.Now.ToString("yyyy-MM-dd HH:mm"));

        using var request = UnityWebRequest.Post(url, form);
        request.SetRequestHeader("Authorization", "Bearer " + JwtToken);
        request.timeout = 15;
        yield return request.SendWebRequest();

        if (request.result == UnityWebRequest.Result.Success)
            Debug.Log("[ARScreenshot] Uploaded: " + request.downloadHandler.text);
        else
            Debug.LogError("[ARScreenshot] Upload failed: " + request.error);
    }
}
