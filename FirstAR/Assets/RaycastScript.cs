using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using TMPro;
using UnityEngine;
using UnityEngine.Events;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;

public class ARPlacementManager : MonoBehaviour
{
    [Header("AR")]
    [SerializeField] private ARRaycastManager raycastManager;
    [SerializeField] private ARPlaneManager planeManager;
    [SerializeField] private ARAnchorManager anchorManager;
    [SerializeField] private Camera arCamera;

    [Header("Pot Prefabs (2 ONLY) - order MUST match potSelectButtons order)")]
    [SerializeField] private GameObject[] potPrefabs = new GameObject[2];

    [Header("Optional: Normalize each pot to a REAL height (cm) before user scaling")]
    [Tooltip("Set to 0 to disable. Example: [25, 18] means pot0 becomes 25cm tall, pot1 becomes 18cm tall.")]
    [SerializeField] private float[] potRealHeightCm = new float[2];

    [Header("Scale Settings (after prefab normalization)")]
    [SerializeField] private float defaultScale = 1f;
    [SerializeField] private float minScale = 0.2f;
    [SerializeField] private float maxScale = 3f;

    [Header("Placement")]
    [SerializeField] private float yOffset = 0f;

    [Tooltip("If ON: pot yaw faces the camera direction at the moment of placement (nice UX).")]
    [SerializeField] private bool faceCameraOnPlace = true;

    [Tooltip("Reject vertical/slanted planes. Keeps placement only on horizontal surfaces.")]
    [SerializeField] private bool allowOnlyHorizontalUpPlanes = true;

    [Tooltip("If enabled: requires plane classification = Floor or Table. If device returns None, placement is still allowed.")]
    [SerializeField] private bool requireFloorOrTableClassification = false;

    [Tooltip("If your model is sideways, set this (example: X = -90). BEST: fix prefab once and keep this (0,0,0).")]
    [SerializeField] private Vector3 prefabRotationOffsetEuler = Vector3.zero;

    [Header("Bounds Filter (IMPORTANT if your prefab contains a 'ground' mesh)")]
    [Tooltip("Any Renderer with a name containing these words will be ignored for snapping + measurement + collider fitting.")]
    [SerializeField] private string[] ignoreRendererNameContains = new string[] { "ground", "shadow", "plane" };

    [Header("Selection")]
    [SerializeField] private bool enableTapSelection = true;

    [Header("UI - Size Display (TOP LEFT)")]
    [SerializeField] private TMP_Text sizeText;

    [Header("UI - HUD (Main Screen)")]
    [SerializeField] private GameObject hudRoot;          // ARHUD (optional)
    [SerializeField] private Button openMenuButton;       // menu icon
    [SerializeField] private Button deleteSelectedButton; // trash icon
    [SerializeField] private Button screenshotButton;     // camera icon

    [Header("UI - Menu Screen")]
    [SerializeField] private GameObject menuPanel;        // MenuPanel
    [SerializeField] private Button closeMenuButton;      // Done button
    [SerializeField] private Button[] potSelectButtons;   // order must match potPrefabs

    [Header("Screenshot")]
    [SerializeField] private string screenshotFolderName = "MyDesigns";
    [SerializeField] private bool hideMenuDuringScreenshot = true;
    [SerializeField] private bool hideHudDuringScreenshot = true;
    public UnityEvent<string> onScreenshotSaved;

    [Header("Save Name Dialog")]
    [Tooltip("Optional panel shown before saving so the user can name the design. If not assigned, saves immediately with a timestamp.")]
    [SerializeField] private GameObject saveNamePanel;
    [SerializeField] private TMP_InputField saveNameInput;
    [SerializeField] private Button confirmSaveButton;
    [SerializeField] private Button cancelSaveButton;

    [Header("Toast (Success/Fail Message)")]
    [Tooltip("Assign a TextMeshProUGUI on your Canvas (e.g., ToastText). It will be shown briefly.")]
    [SerializeField] private TMP_Text toastText;
    [SerializeField] private float toastSeconds = 1.5f;

    [Header("Collider")]
    [SerializeField] private bool autoFitColliderOnPlace = true;

    private int selectedPotIndex = 0;
    private GameObject selectedObject;

    private float startPinchDistance;
    private Vector3 startScale;

    private bool deleteMode = false;

    private static readonly List<ARRaycastHit> hits = new List<ARRaycastHit>();
    private readonly List<GameObject> spawnedObjects = new List<GameObject>();

    // Stores plane Y used for snapping so we keep perfect contact during scaling
    private class PlacedPotInfo : MonoBehaviour
    {
        public int potIndex;
        public float planeY;
        public Vector3 normalizedBaseScale;
    }

    private void Awake()
    {
        if (!raycastManager) raycastManager = FindFirstObjectByType<ARRaycastManager>();
        if (!planeManager) planeManager = FindFirstObjectByType<ARPlaneManager>();
        if (!anchorManager) anchorManager = FindFirstObjectByType<ARAnchorManager>();
        if (!arCamera) arCamera = Camera.main;

        WireUI();
    }

    private void Start()
    {
        SelectPot(0);
        SetMenuOpen(false);
        SetDeleteMode(false);
        UpdateSizeUI();

        if (toastText) toastText.gameObject.SetActive(false);
        if (saveNamePanel) saveNamePanel.SetActive(false);
    }

    private void Update()
    {
        HandlePinchScaling();
        HandleTouch();
    }

    // ---------------- UI ----------------
    private void WireUI()
    {
        if (openMenuButton)
        {
            openMenuButton.onClick.RemoveAllListeners();
            openMenuButton.onClick.AddListener(() => SetMenuOpen(true));
        }

        if (closeMenuButton)
        {
            closeMenuButton.onClick.RemoveAllListeners();
            closeMenuButton.onClick.AddListener(() => SetMenuOpen(false));
        }

        if (deleteSelectedButton)
        {
            deleteSelectedButton.onClick.RemoveAllListeners();
            deleteSelectedButton.onClick.AddListener(() => SetDeleteMode(!deleteMode));
        }

        if (screenshotButton)
        {
            screenshotButton.onClick.RemoveAllListeners();
            screenshotButton.onClick.AddListener(ShowSaveNameDialog);
        }

        if (confirmSaveButton)
        {
            confirmSaveButton.onClick.RemoveAllListeners();
            confirmSaveButton.onClick.AddListener(ConfirmSave);
        }

        if (cancelSaveButton)
        {
            cancelSaveButton.onClick.RemoveAllListeners();
            cancelSaveButton.onClick.AddListener(CancelSave);
        }

        if (potSelectButtons != null)
        {
            for (int i = 0; i < potSelectButtons.Length; i++)
            {
                int idx = i;
                if (!potSelectButtons[idx]) continue;

                potSelectButtons[idx].onClick.RemoveAllListeners();
                potSelectButtons[idx].onClick.AddListener(() => SelectPot(idx));
            }
        }
    }

    private void SetMenuOpen(bool open)
    {
        if (menuPanel) menuPanel.SetActive(open);

        // Hide HUD root if assigned
        if (hudRoot) hudRoot.SetActive(!open);

        // FORCE hide individual HUD elements too (fixes your issue even if hierarchy is wrong)
        ForceSetHudElementsVisible(!open);

        // When menu opens, stop delete mode
        if (open) SetDeleteMode(false);
    }

    private void ForceSetHudElementsVisible(bool visible)
    {
        if (openMenuButton) openMenuButton.gameObject.SetActive(visible);
        if (deleteSelectedButton) deleteSelectedButton.gameObject.SetActive(visible);
        if (screenshotButton) screenshotButton.gameObject.SetActive(visible);
        if (sizeText) sizeText.gameObject.SetActive(visible);
    }

    private void SetDeleteMode(bool on)
    {
        deleteMode = on;

        // Visual feedback: tint trash icon
        if (deleteSelectedButton)
        {
            var img = deleteSelectedButton.GetComponent<Image>();
            if (img) img.color = deleteMode ? new Color(1f, 0.4f, 0.4f, 1f) : Color.white;
        }
    }

    // ---------------- Pot Selection ----------------
    public void SelectPot(int index)
    {
        if (potPrefabs == null || potPrefabs.Length == 0) return;

        selectedPotIndex = Mathf.Clamp(index, 0, potPrefabs.Length - 1);

        // Highlight selected icon
        if (potSelectButtons != null)
        {
            for (int i = 0; i < potSelectButtons.Length; i++)
            {
                if (!potSelectButtons[i]) continue;
                var img = potSelectButtons[i].GetComponent<Image>();
                if (!img) continue;

                img.color = (i == selectedPotIndex)
                    ? new Color(1f, 1f, 1f, 1f)
                    : new Color(1f, 1f, 1f, 0.75f);
            }
        }
    }

    // ---------------- Touch ----------------
    private void HandleTouch()
    {
        if (menuPanel && menuPanel.activeSelf) return;

        // If pinch (2 fingers), do not place/select/delete
        if (Input.touchCount != 1) return;

        Touch touch = Input.GetTouch(0);
        if (touch.phase != TouchPhase.Began) return;

        // Ignore UI touches
        if (EventSystem.current != null &&
            EventSystem.current.IsPointerOverGameObject(touch.fingerId))
            return;

        // Raycast to pots (select / delete)
        Ray ray = arCamera.ScreenPointToRay(touch.position);
        if (Physics.Raycast(ray, out RaycastHit hit))
        {
            GameObject tapped = FindSpawnedRootFromHit(hit.collider.transform);
            if (tapped != null)
            {
                if (deleteMode)
                {
                    DeleteObject(tapped);
                    SetDeleteMode(false); // exit after deleting one
                }
                else if (enableTapSelection)
                {
                    selectedObject = tapped;
                    UpdateSizeUI();
                }
                return;
            }
        }

        // If delete mode and tap empty -> cancel delete mode
        if (deleteMode)
        {
            SetDeleteMode(false);
            return;
        }

        // Otherwise place
        PlaceObject(touch.position);
    }

    private GameObject FindSpawnedRootFromHit(Transform hitTransform)
    {
        Transform t = hitTransform;
        while (t != null)
        {
            if (spawnedObjects.Contains(t.gameObject))
                return t.gameObject;

            t = t.parent;
        }
        return null;
    }

    // ---------------- Placement ----------------
    private void PlaceObject(Vector2 screenPosition)
    {
        if (potPrefabs == null || potPrefabs.Length == 0)
        {
            Debug.LogError("No pot prefabs assigned.");
            return;
        }

        GameObject prefab = potPrefabs[selectedPotIndex];
        if (!prefab)
        {
            Debug.LogError($"Pot prefab index {selectedPotIndex} is NULL.");
            return;
        }

        // PlaneWithinPolygon only: confirmed ground polygons, never estimated/floating surfaces
        if (!raycastManager.Raycast(screenPosition, hits, TrackableType.PlaneWithinPolygon))
            return;

        var hit = hits[0];

        if (!IsAllowedPlane(hit))
            return;

        Pose pose = hit.pose;

        Quaternion baseRot;
        if (faceCameraOnPlace && arCamera)
        {
            Vector3 dirToCamera = arCamera.transform.position - pose.position;
            dirToCamera.y = 0f;

            if (dirToCamera.sqrMagnitude > 0.001f)
                baseRot = Quaternion.LookRotation(-dirToCamera.normalized);
            else
                baseRot = Quaternion.identity;
        }
        else
        {
            baseRot = Quaternion.identity;
        }

        Quaternion finalRot = baseRot * Quaternion.Euler(prefabRotationOffsetEuler);

        GameObject obj = Instantiate(prefab, pose.position, finalRot);

        // Normalize to real height (optional)
        Vector3 normalizedScale = Vector3.one;
        float realHeightCm = GetRealHeightForIndex(selectedPotIndex);
        if (realHeightCm > 0.01f)
            normalizedScale = NormalizeObjectToHeightCm(obj, realHeightCm);

        obj.transform.localScale = normalizedScale * defaultScale;

        if (autoFitColliderOnPlace)
            FitBoxColliderToRenderers(obj);

        // Read planeY from the actual hit pose — this is the confirmed surface Y
        float planeY = pose.position.y + yOffset;

        var info = obj.GetComponent<PlacedPotInfo>() ?? obj.AddComponent<PlacedPotInfo>();
        info.potIndex = selectedPotIndex;
        info.planeY = planeY;
        info.normalizedBaseScale = normalizedScale;

        // Snap bottom of pot exactly to the surface — must happen AFTER scale is final
        AlignObjectBottomToPlaneY(obj, planeY);

        // Lock the pot to the real world so it NEVER drifts when camera/phone moves
        // ARAnchor tells ARCore/ARKit to track this exact world position permanently
        if (obj.GetComponent<ARAnchor>() == null)
            obj.AddComponent<ARAnchor>();

        spawnedObjects.Add(obj);
        selectedObject = obj;

        UpdateSizeUI();
    }

    private float GetRealHeightForIndex(int index)
    {
        if (potRealHeightCm == null) return 0f;
        if (index < 0 || index >= potRealHeightCm.Length) return 0f;
        return potRealHeightCm[index];
    }

    private bool IsAllowedPlane(ARRaycastHit hit)
    {
        if (!planeManager) return true;

        ARPlane plane = planeManager.GetPlane(hit.trackableId);
        if (!plane) return true;

        // Always restrict to horizontal surfaces (floor/table) — never walls
        if (plane.alignment != PlaneAlignment.HorizontalUp)
            return false;

        if (requireFloorOrTableClassification)
        {
            PlaneClassification c = plane.classification;
            if (c != PlaneClassification.None &&
                c != PlaneClassification.Table &&
                c != PlaneClassification.Floor)
                return false;
        }

        return true;
    }

    private void AlignObjectBottomToPlaneY(GameObject obj, float planeY)
    {
        Bounds b = GetWorldBoundsFromRenderers(obj);
        float bottomY = b.min.y;
        obj.transform.position += new Vector3(0f, planeY - bottomY, 0f);
    }

    // ---------------- Delete ----------------
    private void DeleteObject(GameObject obj)
    {
        if (!obj) return;

        if (selectedObject == obj)
            selectedObject = null;

        spawnedObjects.Remove(obj);
        Destroy(obj);
        UpdateSizeUI();
    }

    // ---------------- Pinch Scale ----------------
    private void HandlePinchScaling()
    {
        if (Input.touchCount != 2 || !selectedObject) return;

        Touch t0 = Input.GetTouch(0);
        Touch t1 = Input.GetTouch(1);

        float dist = Vector2.Distance(t0.position, t1.position);

        if (t0.phase == TouchPhase.Began || t1.phase == TouchPhase.Began)
        {
            startPinchDistance = dist;
            startScale = selectedObject.transform.localScale;
            return;
        }

        if (Mathf.Approximately(startPinchDistance, 0f)) return;

        float factor = dist / startPinchDistance;
        float target = Mathf.Clamp(startScale.x * factor, minScale, maxScale);

        selectedObject.transform.localScale = Vector3.one * target;

        // Keep touching plane
        var info = selectedObject.GetComponent<PlacedPotInfo>();
        if (info) AlignObjectBottomToPlaneY(selectedObject, info.planeY);

        UpdateSizeUI();
    }

    // ---------------- Measurement (Top-left) ----------------
    private void UpdateSizeUI()
    {
        if (deleteSelectedButton)
            deleteSelectedButton.interactable = true; // trash always available (delete mode)

        if (!sizeText) return;

        if (!selectedObject)
        {
            sizeText.text = "";
            return;
        }

        // Accurate: renderer world bounds after snapping + scaling
        Bounds b = GetWorldBoundsFromRenderers(selectedObject);
        Vector3 sizeMeters = b.size;

        sizeText.text =
            $"W: {(sizeMeters.x * 100f):F1} cm\n" +
            $"D: {(sizeMeters.z * 100f):F1} cm\n" +
            $"H: {(sizeMeters.y * 100f):F1} cm";
    }

    // ---------------- Renderer filtering + bounds ----------------
    private bool ShouldIgnoreRenderer(Renderer r)
    {
        if (!r) return true;
        if (ignoreRendererNameContains == null || ignoreRendererNameContains.Length == 0) return false;

        string n = r.gameObject.name.ToLowerInvariant();
        for (int i = 0; i < ignoreRendererNameContains.Length; i++)
        {
            string key = ignoreRendererNameContains[i];
            if (string.IsNullOrEmpty(key)) continue;
            if (n.Contains(key.ToLowerInvariant()))
                return true;
        }
        return false;
    }

    private Bounds GetWorldBoundsFromRenderers(GameObject root)
    {
        Renderer[] renderers = root.GetComponentsInChildren<Renderer>(true);
        Renderer first = null;

        for (int i = 0; i < renderers.Length; i++)
        {
            if (ShouldIgnoreRenderer(renderers[i])) continue;
            first = renderers[i];
            break;
        }

        if (!first)
            return new Bounds(root.transform.position, Vector3.zero);

        Bounds b = first.bounds;
        for (int i = 0; i < renderers.Length; i++)
        {
            Renderer r = renderers[i];
            if (ShouldIgnoreRenderer(r)) continue;
            b.Encapsulate(r.bounds);
        }
        return b;
    }

    private void FitBoxColliderToRenderers(GameObject root)
    {
        Bounds world = GetWorldBoundsFromRenderers(root);
        if (world.size == Vector3.zero) return;

        BoxCollider bc = root.GetComponent<BoxCollider>();
        if (!bc) bc = root.AddComponent<BoxCollider>();

        Vector3 localCenter = root.transform.InverseTransformPoint(world.center);
        Vector3 lossy = root.transform.lossyScale;

        Vector3 localSize = new Vector3(
            lossy.x != 0 ? world.size.x / lossy.x : world.size.x,
            lossy.y != 0 ? world.size.y / lossy.y : world.size.y,
            lossy.z != 0 ? world.size.z / lossy.z : world.size.z
        );

        bc.center = localCenter;
        bc.size = localSize;
    }

    private Vector3 NormalizeObjectToHeightCm(GameObject root, float targetHeightCm)
    {
        Bounds b = GetWorldBoundsFromRenderers(root);
        float currentHeightM = Mathf.Max(0.0001f, b.size.y);
        float targetHeightM = targetHeightCm / 100f;

        float factor = targetHeightM / currentHeightM;
        root.transform.localScale = Vector3.one * factor;
        return root.transform.localScale;
    }

    // ---------------- Screenshot (Save to Gallery + Toast) ----------------

    // Called by the 📷 button — shows naming dialog if assigned, else saves immediately
    private void ShowSaveNameDialog()
    {
        if (!saveNamePanel)
        {
            // No dialog assigned: save immediately with timestamp
            TakeScreenshot();
            return;
        }

        // Pre-fill with a readable default name
        if (saveNameInput)
            saveNameInput.text = "MyDesign_" + DateTime.Now.ToString("yyyyMMdd_HHmmss");

        saveNamePanel.SetActive(true);
    }

    // Called by the Confirm / Save button inside the dialog
    private void ConfirmSave()
    {
        if (saveNamePanel) saveNamePanel.SetActive(false);

        string name = saveNameInput ? saveNameInput.text.Trim() : "";

        // Fallback to timestamp if user left it blank
        if (string.IsNullOrEmpty(name))
            name = "MyDesign_" + DateTime.Now.ToString("yyyyMMdd_HHmmss");

        // Strip illegal filename characters
        foreach (char c in Path.GetInvalidFileNameChars())
            name = name.Replace(c, '_');

        TakeScreenshotWithName(name);
    }

    // Called by the Cancel button inside the dialog
    private void CancelSave()
    {
        if (saveNamePanel) saveNamePanel.SetActive(false);
    }

    // Rename an already-saved design file (call this from a Rename UI later)
    public void RenameDesign(string oldPath, string newName)
    {
        if (!File.Exists(oldPath))
        {
            ShowToast("❌ File not found");
            return;
        }

        // Sanitize the new name
        foreach (char c in Path.GetInvalidFileNameChars())
            newName = newName.Replace(c, '_');

        string dir = Path.GetDirectoryName(oldPath);
        string newPath = Path.Combine(dir, newName + ".png");

        try
        {
            File.Move(oldPath, newPath);
            ShowToast("✅ Renamed successfully");
            onScreenshotSaved?.Invoke(newPath);
        }
        catch (Exception e)
        {
            Debug.LogError("Rename failed: " + e.Message);
            ShowToast("❌ Rename failed");
        }
    }

    public void TakeScreenshot()
    {
        StartCoroutine(CaptureScreenshotCoroutine(null));
    }

    public void TakeScreenshotWithName(string customName)
    {
        StartCoroutine(CaptureScreenshotCoroutine(customName));
    }

    private IEnumerator CaptureScreenshotCoroutine(string customName)
    {
        bool menuWasActive = menuPanel && menuPanel.activeSelf;
        bool hudWasActive = hudRoot && hudRoot.activeSelf;

        // Only hide the menu panel — HUD buttons stay visible
        if (hideMenuDuringScreenshot && menuPanel)
            menuPanel.SetActive(false);

        yield return new WaitForEndOfFrame();

        Texture2D tex = ScreenCapture.CaptureScreenshotAsTexture();
        if (tex == null)
        {
            RestoreUI(menuWasActive, hudWasActive);
            ShowToast("❌ Screenshot failed");
            yield break;
        }

        byte[] png = tex.EncodeToPNG();
        Destroy(tex);

        string toastMessage;
        string designName = string.IsNullOrEmpty(customName)
            ? "MyDesign_" + DateTime.Now.ToString("yyyyMMdd_HHmmss")
            : customName;

#if UNITY_ANDROID && !UNITY_EDITOR
        string filename = designName + ".png";
        string folder = "/storage/emulated/0/DCIM/MyDesigns/";

        try
        {
            Directory.CreateDirectory(folder);
            string galleryPath = Path.Combine(folder, filename);
            File.WriteAllBytes(galleryPath, png);

            // Force Android MediaStore scan so it appears in Gallery immediately
            using (AndroidJavaClass mediaScanClass = new AndroidJavaClass("android.media.MediaScannerConnection"))
            using (AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using (AndroidJavaObject context = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
            {
                mediaScanClass.CallStatic("scanFile", context, new string[] { galleryPath }, null, null);
            }

            toastMessage = "✅ Screenshot saved";
            onScreenshotSaved?.Invoke(galleryPath);
        }
        catch (Exception e)
        {
            Debug.LogError("Screenshot save failed: " + e.Message);
            toastMessage = "❌ Screenshot save failed";
        }
#else
        // Editor / non-Android: save to persistentDataPath/MyDesigns
        string dir = Path.Combine(Application.persistentDataPath, screenshotFolderName);
        Directory.CreateDirectory(dir);

        string filename = designName + ".png";
        string fullPath = Path.Combine(dir, filename);

        File.WriteAllBytes(fullPath, png);
        Debug.Log("✅ Screenshot saved: " + fullPath);

        toastMessage = "✅ Screenshot saved";
        onScreenshotSaved?.Invoke(fullPath);
#endif

        // Upload to backend if connected and authenticated
        if (DesignUploadService.Instance != null && ApiClient.Instance != null
            && ApiClient.Instance.IsAuthenticated)
        {
            DesignUploadService.Instance.UploadDesign(png, designName);
        }

        RestoreUI(menuWasActive, hudWasActive);
        ShowToast(toastMessage);
    }

    private void RestoreUI(bool menuWasActive, bool hudWasActive)
    {
        if (menuPanel) menuPanel.SetActive(menuWasActive);

        if (hudRoot) hudRoot.SetActive(hudWasActive);

        // If menu is open, HUD must stay hidden
        bool shouldShowHudElements = !(menuPanel && menuPanel.activeSelf);
        ForceSetHudElementsVisible(shouldShowHudElements);
    }

    // ---------------- Toast ----------------
    private void ShowToast(string message)
    {
        if (!toastText) return;
        StopCoroutine(nameof(ToastRoutine));
        toastText.text = message;
        StartCoroutine(nameof(ToastRoutine));
    }

    private IEnumerator ToastRoutine()
    {
        toastText.gameObject.SetActive(true);
        yield return new WaitForSeconds(toastSeconds);
        toastText.gameObject.SetActive(false);
    }
}