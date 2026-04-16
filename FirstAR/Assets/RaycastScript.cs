using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using TMPro;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;
using UnityEngine.InputSystem;
using UnityEngine.InputSystem.EnhancedTouch;
using UnityEngine.Events;
using UnityEngine.Networking;
using ETouch = UnityEngine.InputSystem.EnhancedTouch.Touch;
using TouchPhaseIS = UnityEngine.InputSystem.TouchPhase;

// ================================================================
//  ARPlacementManager  -  PRODUCTION BUILD (v30-fix)
//
//  STABILITY GUARANTEES:
//  - Every placed pot is a child of a PLANE-ATTACHED ARAnchor.
//  - EnforceFrozenTransforms runs in FixedUpdate + LateUpdate.
//  - ABSOLUTE WORLD-POSITION LOCK: pots slammed to frozen world
//    position every frame - never move, shake, or drift.
//  - ALL Rigidbodies, Joints, Cloth, Animators, XRI destroyed.
//  - Pots on physics layer 31 - zero cross-interaction.
//  - Overlap / stacking blocked at placement AND during scale.
//  - Plane visuals HIDDEN after first pot (no green circles).
//  - Developer console fully suppressed.
//
//  Target: Google Pixel 4 (1080x2160), AR Foundation 5.x+
// ================================================================
[RequireComponent(typeof(ARRaycastManager))]
[RequireComponent(typeof(ARPlaneManager))]
[DefaultExecutionOrder(-50)]
public class ARPlacementManager : MonoBehaviour
{
    // -- THEME: Earthy Green Palette --
    private static readonly Color MOSS    = new Color(0.173f, 0.204f, 0.141f);
    private static readonly Color CYPRESS = new Color(0.298f, 0.345f, 0.243f);
    private static readonly Color CEDAR   = new Color(0.584f, 0.584f, 0.506f);
    private static readonly Color OLIVE   = new Color(0.463f, 0.502f, 0.392f);
    private static readonly Color ALOE    = new Color(0.855f, 0.871f, 0.847f);
    private static readonly Color CREAM   = new Color(0.94f, 0.93f, 0.90f);
    private static readonly Color COL_BAR      = new Color(0.173f, 0.204f, 0.141f, 0.92f);
    private static readonly Color COL_MENU_BG  = new Color(0.94f, 0.93f, 0.90f);
    private static readonly Color COL_WHITE    = new Color(0.855f, 0.871f, 0.847f);
    private static readonly Color COL_DARK     = new Color(0.173f, 0.204f, 0.141f);
    private static readonly Color COL_MGREY    = new Color(0.584f, 0.584f, 0.506f);
    private static readonly Color COL_DEL_TINT = new Color(0.78f, 0.32f, 0.28f, 0.80f);
    private static readonly Color COL_RED_BG   = new Color(0.72f, 0.28f, 0.24f);
    private static readonly Color COL_TOAST_BG = new Color(0.298f, 0.345f, 0.243f, 0.94f);

    // -- STABILITY CONSTANTS --
    private const float OVERLAP_RADIUS_FACTOR = 1.0f;
    private const float MIN_GAP_METRES        = 0.01f;
    private const float SURFACE_Y_TOLERANCE   = 0.12f;
    private const float STACK_Y_BLOCK         = 0.03f;
    private const float MIN_PLANE_AREA_SQM    = 0.012f;
    private const float MAX_SURFACE_ANGLE_DEG = 25f;
    private const float TAP_CD                = 0.22f;
    private const float DEFAULT_POT_HEIGHT_CM = 30f;
    private const int   ZTEST_LEQUAL          = 4;
    private const int   POT_PHYSICS_LAYER     = 31;

    // -- INSPECTOR --
    [Header("AR")]
    [SerializeField] private ARRaycastManager   raycastManager;
    [SerializeField] private ARPlaneManager     planeManager;
    [SerializeField] private ARAnchorManager    anchorManager;
    [SerializeField] private AROcclusionManager occlusionManager;
    [SerializeField] private Camera             arCamera;

    [Header("Pot Prefabs")]
    [SerializeField] private GameObject[] potPrefabs  = new GameObject[2];
    [SerializeField] private string[]     potNames    = new string[] { "Classic Pot", "Modern Pot" };
    [SerializeField] private float[]      potRealHeightCm = new float[] { 30f, 25f };

    [Header("Scale")]
    [SerializeField] private float defaultScale = 1f;
    [SerializeField] private float minScale     = 0.2f;
    [SerializeField] private float maxScale     = 3f;

    [Header("Placement")]
    [SerializeField] private float   yOffset = 0f;
    [SerializeField] private bool    faceCameraOnPlace = true;
    [SerializeField] private Vector3 prefabRotationOffsetEuler = Vector3.zero;

    [Header("Bounds Filter")]
    [SerializeField] private string[] ignoreRendererNameContains =
        new string[] { "ground", "shadow", "plane" };

    [Header("Screenshot")]
    [SerializeField] private string screenshotFolderName = "MyDesigns";
    public UnityEvent<string> onScreenshotSaved;

    private string _jwtToken = "";
    private string _apiBaseUrl = "";

    // -- RUNTIME STATE --
    private int  selectedPotIndex;
    private bool deleteMode;
    private readonly List<GameObject> spawned = new List<GameObject>();

    private static readonly List<ARRaycastHit> sHits = new List<ARRaycastHit>();
    private readonly List<RaycastResult> uiHits = new List<RaycastResult>();
    private float lastTapTime = -10f;

    private float      pinchStartDist;
    private Vector3    pinchStartScale;
    private GameObject pinchTarget;

    private GameObject activeLabelPot;
    private bool _planeVisualsHidden;

    private static readonly int ZTestMode = Shader.PropertyToID("unity_GUIZTestMode");

    // -- UI REFS --
    private Canvas     canvas;
    private GameObject barGO, menuGO, bannerGO, toastGO;
    private TMP_Text   toastTxt;
    private Coroutine  toastCR;
    private Image      sel1Img, sel2Img;
    private Sprite     spr1, spr2;

    private static readonly TrackableType PlaneFilterTight =
        TrackableType.PlaneWithinPolygon;
    private static readonly TrackableType PlaneFilterLoose =
        TrackableType.PlaneWithinBounds;

    private class PotInfo : MonoBehaviour
    {
        public int        idx;
        public float      surfaceY;
        public Vector3    normScale;
        public GameObject anchorObj;
        public GameObject measureLabel;
        public Vector3    frozenLocalPos;
        public Quaternion frozenLocalRot;
        public Vector3    frozenLocalScale;
        public Vector3    frozenWorldPos;
        public Quaternion frozenWorldRot;
        public bool       isFrozen;
    }

    // ================================================================
    //  LIFECYCLE
    // ================================================================
    private void Awake()
    {
        Physics.autoSyncTransforms = false;
        for (int i = 0; i < 32; i++)
            Physics.IgnoreLayerCollision(POT_PHYSICS_LAYER, i, true);
        SuppressDeveloperMode();

        if (!raycastManager)   raycastManager   = FindFirstObjectByType<ARRaycastManager>();
        if (!planeManager)     planeManager     = FindFirstObjectByType<ARPlaneManager>();
        if (!anchorManager)    anchorManager    = FindFirstObjectByType<ARAnchorManager>();
        if (!occlusionManager) occlusionManager = FindFirstObjectByType<AROcclusionManager>();
        if (!arCamera)         arCamera         = Camera.main;

        if (!anchorManager)
        {
            GameObject host = planeManager ? planeManager.gameObject : gameObject;
            anchorManager = host.AddComponent<ARAnchorManager>();
        }

        ConfigureOcclusion();
        ConfigurePlaneDetection();
        DisableXRIInteractors();
        ConfigureFeaturePoints();
        EnhancedTouchSupport.Enable();
        ReadIntentExtras();
        LoadPotSprites();
        canvas = CreateCanvas();
        SetupRuntimeUI();
    }

    private void Start()
    {
        menuGO.SetActive(false);
        bannerGO.SetActive(false);
        toastGO.SetActive(false);
        CleanPreExistingPots();
        ConfigurePlaneDetection();
        SuppressDeveloperMode();
    }

    private void OnApplicationPause(bool paused) { }
    private void OnApplicationFocus(bool hasFocus) { }

    private void ClearAllPots()
    {
        for (int i = spawned.Count - 1; i >= 0; i--)
        {
            var obj = spawned[i];
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info != null)
            {
                if (info.measureLabel) Destroy(info.measureLabel);
                GameObject toDestroy = info.anchorObj ? info.anchorObj : obj;
                Destroy(toDestroy);
            }
            else Destroy(obj);
        }
        spawned.Clear();
        activeLabelPot = null;
        pinchTarget = null;
        SetDeleteMode(false);
    }

    private void Update()
    {
        HandleTouch();
        HandlePinch();
        BillboardLabels();
    }

    private void FixedUpdate()
    {
        EnforceFrozenTransforms();
    }

    private void LateUpdate()
    {
        spawned.RemoveAll(o => o == null);
        EnforceFrozenTransforms();
    }

    private void EnforceFrozenTransforms()
    {
        bool trackingOK = ARSession.state == ARSessionState.SessionTracking;
        for (int i = 0; i < spawned.Count; i++)
        {
            var obj = spawned[i];
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info == null || !info.isFrozen) continue;

            if (!trackingOK && info.anchorObj != null)
            {
                var anchor = info.anchorObj.GetComponent<ARAnchor>();
                if (anchor != null && anchor.trackingState == TrackingState.None)
                {
                    SetRenderersVisible(obj, false);
                    continue;
                }
            }
            SetRenderersVisible(obj, true);

            // ABSOLUTE WORLD-POSITION LOCK
            obj.transform.position   = info.frozenWorldPos;
            obj.transform.rotation   = info.frozenWorldRot;
            obj.transform.localScale = info.frozenLocalScale;
            obj.transform.hasChanged = false;
        }
    }

    private static void SetRenderersVisible(GameObject root, bool visible)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
            if (r) r.enabled = visible;
    }

    // ================================================================
    //  CONFIGURATION
    // ================================================================
    private void ConfigurePlaneDetection()
    {
        if (planeManager != null)
            planeManager.requestedDetectionMode = PlaneDetectionMode.Horizontal;
    }

    private void ConfigureFeaturePoints()
    {
        var pointCloud = FindFirstObjectByType<ARPointCloudManager>();
        if (pointCloud != null) pointCloud.enabled = true;
    }

    private void SuppressDeveloperMode()
    {
        Debug.developerConsoleVisible = false;
#if !UNITY_EDITOR
        Debug.unityLogger.filterLogType = LogType.Error;
#endif
    }

    private void DisableXRIInteractors()
    {
        foreach (var mb in FindObjectsByType<MonoBehaviour>(FindObjectsSortMode.None))
        {
            string tn = mb.GetType().Name;
            if (tn.Contains("Interactor") || tn.Contains("XRController")
                || tn.Contains("ScreenSpaceController"))
                mb.enabled = false;
        }
    }

    private void ConfigureOcclusion()
    {
        if (!arCamera) return;
        if (!occlusionManager)
            occlusionManager = arCamera.GetComponent<AROcclusionManager>();
        if (occlusionManager)
        {
            occlusionManager.requestedEnvironmentDepthMode = EnvironmentDepthMode.Disabled;
            occlusionManager.requestedOcclusionPreferenceMode = OcclusionPreferenceMode.NoOcclusion;
            occlusionManager.enabled = false;
        }
        foreach (var c in arCamera.GetComponents<MonoBehaviour>())
            if (c.GetType().Name == "ARShaderOcclusion") c.enabled = false;
    }

    private void CleanPreExistingPots()
    {
        if (potPrefabs == null) return;
        var roots = UnityEngine.SceneManagement.SceneManager
                        .GetActiveScene().GetRootGameObjects();
        foreach (var root in roots)
        {
            if (root == gameObject) continue;
            if (spawned.Contains(root)) continue;
            foreach (var prefab in potPrefabs)
            {
                if (!prefab) continue;
                if (root.name == prefab.name
                    && root.GetComponentInChildren<MeshRenderer>())
                {
                    Destroy(root);
                    break;
                }
            }
        }
    }

    // ================================================================
    //  PLANE VISUAL HIDING (removes green circles after first pot)
    // ================================================================
    private void HidePlaneVisualsAfterPlacement()
    {
        if (_planeVisualsHidden) return;
        _planeVisualsHidden = true;
        if (planeManager == null) return;

        foreach (var plane in planeManager.trackables)
            HidePlaneVisual(plane.gameObject);

        if (planeManager.planePrefab != null)
            HidePlaneVisual(planeManager.planePrefab);
    }

    private static void HidePlaneVisual(GameObject planeGO)
    {
        foreach (var vis in planeGO.GetComponents<MonoBehaviour>())
        {
            string tn = vis.GetType().Name;
            if (tn.Contains("PlaneMeshVisualizer") || tn.Contains("PlaneVisualizer")
                || tn.Contains("ARFeatheredPlane"))
                vis.enabled = false;
        }
        foreach (var r in planeGO.GetComponentsInChildren<Renderer>(true))
            if (r) r.enabled = false;
        var mr = planeGO.GetComponent<MeshRenderer>();
        if (mr) mr.enabled = false;
        var lr = planeGO.GetComponent<LineRenderer>();
        if (lr) lr.enabled = false;
    }

    // ================================================================
    //  TOUCH INPUT
    // ================================================================
    private void HandleTouch()
    {
        if (menuGO && menuGO.activeSelf) return;
        Vector2 pos = Vector2.zero;
        bool tap = false;

        var touches = ETouch.activeTouches;
        if (touches.Count == 1 && touches[0].phase == TouchPhaseIS.Began)
        {
            pos = touches[0].screenPosition;
            tap = true;
        }
#if UNITY_EDITOR
        if (!tap && Mouse.current != null
            && Mouse.current.leftButton.wasPressedThisFrame)
        {
            pos = Mouse.current.position.ReadValue();
            tap = true;
        }
#endif
        if (!tap) return;
        if (IsOverUI(pos)) return;
        if (Time.unscaledTime - lastTapTime < TAP_CD) return;
        lastTapTime = Time.unscaledTime;

        Ray ray = arCamera.ScreenPointToRay(pos);
        Physics.SyncTransforms();
        if (Physics.Raycast(ray, out RaycastHit hit, 100f))
        {
            GameObject tapped = FindSpawnedRoot(hit.collider.transform);
            if (tapped != null)
            {
                if (deleteMode) { DeleteObject(tapped); return; }
                ShowLabelForPot(tapped);
                return;
            }
        }
        if (deleteMode) return;

        HideAllLabels();

        ARPlane bestPlane = null;
        ARRaycastHit bestHit = default;

        if (raycastManager.Raycast(pos, sHits, PlaneFilterTight))
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);

        if (bestPlane == null && raycastManager.Raycast(pos, sHits, PlaneFilterLoose))
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);

        if (bestPlane != null)
        {
            PlaceObject(bestHit.pose, bestPlane);
            return;
        }
        ShowToast("Slowly aim at a flat surface like floor or table");
    }

    private void PickBestPlaneHit(List<ARRaycastHit> hits,
        ref ARPlane bestPlane, ref ARRaycastHit bestHit)
    {
        float bestDist = float.MaxValue;
        for (int i = 0; i < hits.Count; i++)
        {
            var arHit = hits[i];
            ARPlane plane = arHit.trackable as ARPlane;
            if (!IsValidPlaneHit(arHit, plane)) continue;
            if (arHit.distance < bestDist)
            {
                bestDist  = arHit.distance;
                bestHit   = arHit;
                bestPlane = plane;
            }
        }
    }

    private bool IsValidPlaneHit(ARRaycastHit arHit, ARPlane plane)
    {
        if (plane == null) return false;
        float angle = Vector3.Angle(arHit.pose.up, Vector3.up);
        if (angle >= MAX_SURFACE_ANGLE_DEG) return false;
        if (plane.alignment != PlaneAlignment.HorizontalUp) return false;
        if (plane.trackingState == TrackingState.None) return false;
        if (plane.size.x * plane.size.y < MIN_PLANE_AREA_SQM) return false;
        return true;
    }

    private bool IsOverUI(Vector2 screenPos)
    {
        if (EventSystem.current == null) return false;
        var ped = new PointerEventData(EventSystem.current) { position = screenPos };
        uiHits.Clear();
        EventSystem.current.RaycastAll(ped, uiHits);
        for (int i = 0; i < uiHits.Count; i++)
        {
            var go = uiHits[i].gameObject;
            if (go.layer == 5) return true;
            if (go.GetComponentInParent<Button>()) return true;
        }
        return false;
    }

    private GameObject FindSpawnedRoot(Transform t)
    {
        while (t != null)
        {
            if (spawned.Contains(t.gameObject)) return t.gameObject;
            t = t.parent;
        }
        return null;
    }

    // ================================================================
    //  PLACEMENT
    // ================================================================
    private void PlaceObject(Pose pose, ARPlane plane)
    {
        if (potPrefabs == null || potPrefabs.Length == 0) return;
        int idx = Mathf.Clamp(selectedPotIndex, 0, potPrefabs.Length - 1);
        GameObject prefab = potPrefabs[idx];
        if (!prefab) return;

        if (ARSession.state != ARSessionState.SessionTracking)
        { ShowToast("Waiting for AR tracking..."); return; }
        if (plane == null)
        { ShowToast("No detected surface - aim at the floor"); return; }
        if (plane.alignment != PlaneAlignment.HorizontalUp)
        { ShowToast("Horizontal surface required"); return; }
        if (plane.trackingState == TrackingState.None)
        { ShowToast("Surface lost - move phone slowly"); return; }

        if (spawned.Count > 0)
        {
            Bounds candidate = EstimatePlacedBounds(pose, idx);
            if (WouldOverlapOrStack(candidate))
            { ShowToast("Too close to another pot!"); return; }
        }
        SpawnNewPot(pose, plane, idx, prefab);
    }

    private void SpawnNewPot(Pose pose, ARPlane plane, int idx, GameObject prefab)
    {
        if (!TryAnchor(pose, plane, out ARAnchor anchor)) return;

        GameObject obj = Instantiate(prefab, anchor.transform);
        obj.transform.localPosition = Vector3.zero;
        obj.transform.localRotation = Quaternion.identity;
        SetLayerRecursive(obj, POT_PHYSICS_LAYER);

        Vector3 ns = NormalizeHeight(obj, DEFAULT_POT_HEIGHT_CM);
        obj.transform.localScale = ns * Mathf.Max(defaultScale, 0.1f);

        if (arCamera)
        {
            Vector3 dir = arCamera.transform.position - pose.position;
            dir.y = 0f;
            if (dir.sqrMagnitude > 0.001f)
            {
                Quaternion worldRot = Quaternion.LookRotation(-dir.normalized)
                    * Quaternion.Euler(prefabRotationOffsetEuler);
                obj.transform.localRotation =
                    Quaternion.Inverse(anchor.transform.rotation) * worldRot;
            }
        }

        FitCollider(obj);
        GroundChild(obj);
        StripPhysicsAndScripts(obj);
        ForceOpaqueMaterials(obj);
        ForceRendererAlwaysVisible(obj);

        var topCol = obj.GetComponent<BoxCollider>();
        if (topCol) topCol.isTrigger = true;

        var info        = obj.AddComponent<PotInfo>();
        info.idx        = idx;
        info.surfaceY   = pose.position.y + yOffset;
        info.normScale  = ns;
        info.anchorObj  = anchor.gameObject;
        info.measureLabel = CreateMeasureLabel(obj, idx);

        info.frozenLocalPos   = obj.transform.localPosition;
        info.frozenLocalRot   = obj.transform.localRotation;
        info.frozenLocalScale = obj.transform.localScale;
        info.frozenWorldPos   = obj.transform.position;
        info.frozenWorldRot   = obj.transform.rotation;
        info.isFrozen         = true;

        ValidateParentChain(obj, anchor);
        Physics.SyncTransforms();

        spawned.Add(obj);
        ShowLabelForPot(obj);
        HidePlaneVisualsAfterPlacement();
        StartCoroutine(VerifyPlacement(obj, obj.transform.position));
        ShowToast("Pot placed!");
    }

    private IEnumerator VerifyPlacement(GameObject pot, Vector3 expectedWorldPos)
    {
        yield return new WaitForSeconds(2f);
        if (pot == null) yield break;
        float drift = Vector3.Distance(pot.transform.position, expectedWorldPos);
        if (drift > 0.05f)
            Debug.LogWarning("[AR] DRIFT DETECTED: " + drift.ToString("F3") + "m in 2s");
    }

    // ================================================================
    //  OVERLAP and ANTI-STACKING
    // ================================================================
    private bool WouldOverlapOrStack(Bounds candidate, GameObject exclude = null)
    {
        if (spawned.Count == 0) return false;
        Bounds padded = candidate;
        padded.Expand(MIN_GAP_METRES);

        for (int i = 0; i < spawned.Count; i++)
        {
            var existing = spawned[i];
            if (!existing || existing == exclude) continue;
            Bounds eb = WorldBounds(existing);
            if (eb.size == Vector3.zero) continue;
            if (!padded.Intersects(eb)) continue;

            float cBottom = candidate.min.y;
            float eTop    = eb.max.y;
            if (cBottom >= eTop - STACK_Y_BLOCK) return true;
            return true;
        }
        return false;
    }

    private Bounds EstimatePlacedBounds(Pose pose, int idx)
    {
        float rh = GetRealHeight(idx);
        GameObject prefab = potPrefabs[Mathf.Clamp(idx, 0, potPrefabs.Length - 1)];
        if (!prefab) return new Bounds(pose.position, Vector3.zero);
        Bounds lb = LocalBounds(prefab);
        if (lb.size == Vector3.zero) return new Bounds(pose.position, Vector3.zero);

        float normF = (rh / 100f) / Mathf.Max(0.0001f, lb.size.y);
        float scaleFactor = Mathf.Max(defaultScale, 0.1f) * normF;
        Vector3 size = lb.size * scaleFactor;
        Vector3 center = pose.position + new Vector3(0, size.y * 0.5f, 0);
        return new Bounds(center, size);
    }

    // ================================================================
    //  ANCHOR
    // ================================================================
    private bool TryAnchor(Pose pose, ARPlane plane, out ARAnchor anchor)
    {
        anchor = null;
        if (anchorManager == null) { ShowToast("Anchor system unavailable"); return false; }
        if (plane == null) { ShowToast("No plane - aim at a flat surface"); return false; }
        if (plane.trackingState == TrackingState.None) { ShowToast("Surface tracking lost"); return false; }

        anchor = anchorManager.AttachAnchor(plane, pose);
        if (anchor == null) { ShowToast("Anchor failed - try another spot"); return false; }
        return true;
    }

    // ================================================================
    //  GROUNDING and BOUNDS
    // ================================================================
    private void GroundChild(GameObject obj)
    {
        Physics.SyncTransforms();
        Bounds lb = LocalBounds(obj);
        if (lb.size == Vector3.zero) return;

        float scaleY = obj.transform.localScale.y;
        Vector3 lp = obj.transform.localPosition;
        lp.y = yOffset - lb.min.y * scaleY;
        obj.transform.localPosition = lp;
        Physics.SyncTransforms();

        Bounds wb = WorldBounds(obj);
        if (wb.size != Vector3.zero && obj.transform.parent != null)
        {
            float surfaceY = obj.transform.parent.position.y + yOffset;
            float gap = wb.min.y - surfaceY;
            if (Mathf.Abs(gap) > 0.001f)
            {
                lp = obj.transform.localPosition;
                lp.y -= gap;
                obj.transform.localPosition = lp;
                Physics.SyncTransforms();
            }
        }
    }

    private Bounds WorldBounds(GameObject root)
    {
        var rs = root.GetComponentsInChildren<Renderer>(true);
        Renderer first = null;
        foreach (var r in rs) if (!IgnoreRenderer(r)) { first = r; break; }
        if (!first) return new Bounds(root.transform.position, Vector3.zero);
        Bounds b = first.bounds;
        foreach (var r in rs) if (!IgnoreRenderer(r)) b.Encapsulate(r.bounds);
        return b;
    }

    private Bounds LocalBounds(GameObject root)
    {
        var rs = root.GetComponentsInChildren<Renderer>(true);
        Matrix4x4 w2l = root.transform.worldToLocalMatrix;
        Bounds lb = new Bounds(Vector3.zero, Vector3.zero);
        bool first = true;
        foreach (var r in rs)
        {
            if (IgnoreRenderer(r)) continue;
            Bounds wb = r.bounds;
            Vector3 mn = wb.min, mx = wb.max;
            for (int i = 0; i < 8; i++)
            {
                Vector3 corner = new Vector3(
                    (i & 1) == 0 ? mn.x : mx.x,
                    (i & 2) == 0 ? mn.y : mx.y,
                    (i & 4) == 0 ? mn.z : mx.z);
                Vector3 lp = w2l.MultiplyPoint3x4(corner);
                if (first) { lb = new Bounds(lp, Vector3.zero); first = false; }
                else lb.Encapsulate(lp);
            }
        }
        return lb;
    }

    private bool IgnoreRenderer(Renderer r)
    {
        if (!r) return true;
        if (ignoreRendererNameContains == null) return false;
        string n = r.gameObject.name.ToLowerInvariant();
        foreach (var s in ignoreRendererNameContains)
            if (!string.IsNullOrEmpty(s) && n.Contains(s.ToLowerInvariant())) return true;
        return false;
    }

    private void FitCollider(GameObject root)
    {
        Bounds w = WorldBounds(root);
        if (w.size == Vector3.zero) return;
        BoxCollider bc = root.GetComponent<BoxCollider>() ?? root.AddComponent<BoxCollider>();
        bc.center = root.transform.InverseTransformPoint(w.center);
        Vector3 ls = root.transform.lossyScale;
        bc.size = new Vector3(
            ls.x != 0 ? w.size.x / ls.x : w.size.x,
            ls.y != 0 ? w.size.y / ls.y : w.size.y,
            ls.z != 0 ? w.size.z / ls.z : w.size.z);
    }

    private Vector3 NormalizeHeight(GameObject root, float cm)
    {
        Bounds b = WorldBounds(root);
        float f = (cm / 100f) / Mathf.Max(0.0001f, b.size.y);
        root.transform.localScale = Vector3.one * f;
        return root.transform.localScale;
    }

    private float GetRealHeight(int i)
    {
        if (potRealHeightCm == null || i < 0 || i >= potRealHeightCm.Length)
            return DEFAULT_POT_HEIGHT_CM;
        float h = potRealHeightCm[i];
        return h > 0.01f ? h : DEFAULT_POT_HEIGHT_CM;
    }

    private void ValidateParentChain(GameObject pot, ARAnchor anchor)
    {
        Transform t = pot.transform.parent;
        while (t != null)
        {
            if (t == arCamera?.transform)
            {
                anchor.transform.SetParent(null);
                return;
            }
            if (t.name.Contains("Camera Offset") || t.name.Contains("CameraOffset"))
            {
                anchor.transform.SetParent(null);
                return;
            }
            t = t.parent;
        }
    }

    private static void SetLayerRecursive(GameObject obj, int layer)
    {
        obj.layer = layer;
        foreach (Transform child in obj.transform)
            SetLayerRecursive(child.gameObject, layer);
    }

    private static void ForceRendererAlwaysVisible(GameObject root)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
            if (r) r.allowOcclusionWhenDynamic = false;
    }

    private static void ForceOpaqueMaterials(GameObject root)
    {
        foreach (var r in root.GetComponentsInChildren<Renderer>(true))
        {
            if (!r) continue;
            foreach (var mat in r.materials)
            {
                if (!mat) continue;
                if (mat.HasProperty("_Surface"))
                { mat.SetFloat("_Surface", 0f); mat.SetFloat("_Blend", 0f); }
                if (mat.HasProperty("_Mode")) mat.SetFloat("_Mode", 0f);
                if (mat.HasProperty("_BaseColor"))
                { Color c = mat.GetColor("_BaseColor"); c.a = 1f; mat.SetColor("_BaseColor", c); }
                if (mat.HasProperty("_Color"))
                { Color c = mat.GetColor("_Color"); c.a = 1f; mat.SetColor("_Color", c); }
                mat.DisableKeyword("_ALPHAPREMULTIPLY_ON");
                mat.DisableKeyword("_ALPHATEST_ON");
                mat.DisableKeyword("_ALPHABLEND_ON");
                mat.DisableKeyword("_SURFACE_TYPE_TRANSPARENT");
                mat.renderQueue = 2000;
            }
        }
    }

    // ================================================================
    //  STRIP PHYSICS
    // ================================================================
    private static void StripPhysicsAndScripts(GameObject root)
    {
        foreach (var j in root.GetComponentsInChildren<Joint>(true)) Destroy(j);
        foreach (var rb in root.GetComponentsInChildren<Rigidbody>(true))
        { rb.isKinematic = true; rb.detectCollisions = false; Destroy(rb); }
        foreach (var cl in root.GetComponentsInChildren<Cloth>(true)) Destroy(cl);
        foreach (var a in root.GetComponentsInChildren<Animator>(true)) Destroy(a);
        foreach (var a in root.GetComponentsInChildren<Animation>(true)) Destroy(a);
        foreach (var cc in root.GetComponentsInChildren<CharacterController>(true)) Destroy(cc);
        foreach (var mb in root.GetComponentsInChildren<MonoBehaviour>(true))
        {
            if (mb is PotInfo) continue;
            string typeName = mb.GetType().Name;
            if (typeName.Contains("Interactable") || typeName.Contains("Interactor")
                || typeName.Contains("ARTranslation") || typeName.Contains("ARRotation")
                || typeName.Contains("ARScale") || typeName.Contains("ARPlacement")
                || typeName.Contains("Transformer") || typeName.Contains("Manipulator")
                || typeName.Contains("NavMeshAgent") || typeName.Contains("FollowTarget")
                || typeName.Contains("LookAt") || typeName.Contains("Constraint"))
            { Destroy(mb); continue; }
            mb.enabled = false;
        }
        var allCols = root.GetComponentsInChildren<Collider>(true);
        foreach (var col in allCols)
            if (col.gameObject != root) Destroy(col);
    }

    // ================================================================
    //  MEASUREMENT LABELS
    // ================================================================
    private void ShowLabelForPot(GameObject pot)
    {
        activeLabelPot = pot;
        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var pi = obj.GetComponent<PotInfo>();
            if (pi == null || !pi.measureLabel) continue;
            pi.measureLabel.SetActive(obj == pot);
        }
        if (pot != null)
        {
            var pi = pot.GetComponent<PotInfo>();
            if (pi != null && pi.measureLabel) pi.measureLabel.SetActive(true);
        }
    }

    private void HideAllLabels()
    {
        activeLabelPot = null;
        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var pi = obj.GetComponent<PotInfo>();
            if (pi != null && pi.measureLabel) pi.measureLabel.SetActive(false);
        }
    }

    private GameObject CreateMeasureLabel(GameObject pot, int idx)
    {
        Bounds lb = LocalBounds(pot);
        if (lb.size == Vector3.zero) return null;

        Vector3 ls = pot.transform.lossyScale;
        float hCm = lb.size.y * Mathf.Abs(ls.y) * 100f;
        float actualW = lb.size.x * Mathf.Abs(ls.x);
        float actualD = lb.size.z * Mathf.Abs(ls.z);
        float wCm = Mathf.Max(actualW, actualD) * 100f;
        float dCm = Mathf.Min(actualW, actualD) * 100f;
        Bounds wb = WorldBounds(pot);

        var labelGO = new GameObject("MeasureLabel");
        Transform labelParent = pot.transform.parent ? pot.transform.parent : pot.transform;
        labelGO.transform.SetParent(labelParent, true);

        var cv = labelGO.AddComponent<Canvas>();
        cv.renderMode   = RenderMode.WorldSpace;
        cv.sortingOrder = 200;
        labelGO.AddComponent<CanvasScaler>().dynamicPixelsPerUnit = 100;

        var cvRT = labelGO.GetComponent<RectTransform>();
        cvRT.sizeDelta = new Vector2(240, 150);
        float s = 0.0008f;
        cvRT.localScale = new Vector3(s, s, s);
        labelGO.transform.position = wb.center + new Vector3(0, wb.extents.y + 0.08f, 0);

        var card = new GameObject("Card");
        card.transform.SetParent(cvRT, false);
        var crt = card.AddComponent<RectTransform>();
        crt.anchorMin = new Vector2(0, 0.22f);
        crt.anchorMax = Vector2.one;
        crt.offsetMin = crt.offsetMax = Vector2.zero;
        var cardImg = card.AddComponent<Image>();
        cardImg.color = COL_WHITE;
        cardImg.raycastTarget = false;

        var hGO = new GameObject("HTxt");
        hGO.transform.SetParent(card.transform, false);
        var htmp = hGO.AddComponent<TextMeshProUGUI>();
        htmp.text      = string.Format("H: {0:F1} cm", hCm);
        htmp.fontSize  = 34;
        htmp.color     = COL_MGREY;
        htmp.alignment = TextAlignmentOptions.Center;
        htmp.raycastTarget = false;
        var hrt = hGO.GetComponent<RectTransform>();
        hrt.anchorMin = new Vector2(0, 0.5f);
        hrt.anchorMax = Vector2.one;
        hrt.offsetMin = hrt.offsetMax = Vector2.zero;

        var wGO = new GameObject("WDTxt");
        wGO.transform.SetParent(card.transform, false);
        var wtmp = wGO.AddComponent<TextMeshProUGUI>();
        wtmp.text      = string.Format("W: {0:F1}  D: {1:F1} cm", wCm, dCm);
        wtmp.fontSize  = 30;
        wtmp.color     = COL_MGREY;
        wtmp.alignment = TextAlignmentOptions.Center;
        wtmp.raycastTarget = false;
        var wrt = wGO.GetComponent<RectTransform>();
        wrt.anchorMin = Vector2.zero;
        wrt.anchorMax = new Vector2(1, 0.5f);
        wrt.offsetMin = wrt.offsetMax = Vector2.zero;

        var stem = new GameObject("Stem");
        stem.transform.SetParent(cvRT, false);
        var srt = stem.AddComponent<RectTransform>();
        srt.anchorMin = srt.anchorMax = new Vector2(0.5f, 0);
        srt.pivot = new Vector2(0.5f, 1);
        srt.anchoredPosition = new Vector2(0, cvRT.sizeDelta.y * 0.22f);
        srt.sizeDelta = new Vector2(4, 28);
        var stemImg = stem.AddComponent<Image>();
        stemImg.color = COL_MGREY;
        stemImg.raycastTarget = false;

        var head = new GameObject("Head");
        head.transform.SetParent(cvRT, false);
        var hdrt = head.AddComponent<RectTransform>();
        hdrt.anchorMin = hdrt.anchorMax = new Vector2(0.5f, 0);
        hdrt.pivot = new Vector2(0.5f, 0.5f);
        hdrt.anchoredPosition = new Vector2(0, -6);
        hdrt.sizeDelta = new Vector2(14, 14);
        hdrt.localRotation = Quaternion.Euler(0, 0, 45);
        var headImg = head.AddComponent<Image>();
        headImg.color = COL_MGREY;
        headImg.raycastTarget = false;

        foreach (var img in labelGO.GetComponentsInChildren<Image>(true))
        {
            Material m = new Material(img.material ?? Canvas.GetDefaultCanvasMaterial());
            m.SetInt(ZTestMode, ZTEST_LEQUAL);
            img.material = m;
        }
        foreach (var tmp in labelGO.GetComponentsInChildren<TextMeshProUGUI>(true))
        {
            Material m = new Material(tmp.fontSharedMaterial);
            m.SetInt(ZTestMode, ZTEST_LEQUAL);
            tmp.fontSharedMaterial = m;
        }
        return labelGO;
    }

    private void BillboardLabels()
    {
        if (!arCamera) return;
        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var info = obj.GetComponent<PotInfo>();
            if (info == null || !info.measureLabel || !info.measureLabel.activeSelf) continue;
            var t = info.measureLabel.transform;
            Vector3 dir = t.position - arCamera.transform.position;
            if (dir.sqrMagnitude > 0.001f)
                t.rotation = Quaternion.LookRotation(dir);
        }
    }

    // ================================================================
    //  PINCH SCALE
    // ================================================================
    private void HandlePinch()
    {
        var touches = ETouch.activeTouches;
        if (touches.Count != 2 || deleteMode) return;

        var t0 = touches[0];
        var t1 = touches[1];
        float dist = Vector2.Distance(t0.screenPosition, t1.screenPosition);

        if (t0.phase == TouchPhaseIS.Began || t1.phase == TouchPhaseIS.Began)
        {
            Vector2 mid = (t0.screenPosition + t1.screenPosition) * 0.5f;
            Ray ray = arCamera.ScreenPointToRay(mid);
            pinchTarget = null;
            Physics.SyncTransforms();
            if (Physics.Raycast(ray, out RaycastHit hit, 100f))
                pinchTarget = FindSpawnedRoot(hit.collider.transform);
            if (pinchTarget == null && spawned.Count > 0)
                pinchTarget = spawned[spawned.Count - 1];
            if (pinchTarget == null) return;
            pinchStartDist  = dist;
            pinchStartScale = pinchTarget.transform.localScale;
            return;
        }
        if (pinchTarget == null || pinchStartDist < 1f) return;

        var info = pinchTarget.GetComponent<PotInfo>();
        if (info != null) info.isFrozen = false;

        float ratio = dist / pinchStartDist;
        Vector3 bs = info != null ? info.normScale : Vector3.one;
        float curFactor = pinchStartScale.x / Mathf.Max(bs.x, 0.0001f);
        float tgtFactor = Mathf.Clamp(curFactor * ratio, minScale, maxScale);
        pinchTarget.transform.localScale = bs * tgtFactor;
        GroundChild(pinchTarget);

        if (tgtFactor > curFactor && spawned.Count > 1)
        {
            Bounds scaled = WorldBounds(pinchTarget);
            if (scaled.size != Vector3.zero && WouldOverlapOrStack(scaled, pinchTarget))
            {
                pinchTarget.transform.localScale = pinchStartScale;
                GroundChild(pinchTarget);
                ShowToast("Can't scale - touching another pot");
            }
        }

        if (info != null)
        {
            info.frozenLocalPos   = pinchTarget.transform.localPosition;
            info.frozenLocalRot   = pinchTarget.transform.localRotation;
            info.frozenLocalScale = pinchTarget.transform.localScale;
            info.frozenWorldPos   = pinchTarget.transform.position;
            info.frozenWorldRot   = pinchTarget.transform.rotation;
            info.isFrozen         = true;

            if (info.measureLabel) Destroy(info.measureLabel);
            info.measureLabel = CreateMeasureLabel(pinchTarget, info.idx);
        }
    }

    // ================================================================
    //  DELETE MODE
    // ================================================================
    private void SetDeleteMode(bool on)
    {
        deleteMode = on;
        if (bannerGO) bannerGO.SetActive(on);
    }

    private void DeleteObject(GameObject obj)
    {
        if (!obj) return;
        spawned.Remove(obj);
        var info = obj.GetComponent<PotInfo>();
        if (info != null)
        {
            if (info.measureLabel) Destroy(info.measureLabel);
            GameObject toDestroy = info.anchorObj ? info.anchorObj : obj;
            Destroy(toDestroy);
        }
        else Destroy(obj);
        spawned.RemoveAll(o => o == null);
        SetDeleteMode(false);
        ShowToast("Pot removed");
    }

    // ================================================================
    //  SCREENSHOT
    // ================================================================
    private void TakeScreenshot() => StartCoroutine(CaptureCoroutine());

    private IEnumerator CaptureCoroutine()
    {
        bool barOn   = barGO && barGO.activeSelf;
        bool menuOn  = menuGO && menuGO.activeSelf;
        bool banOn   = bannerGO && bannerGO.activeSelf;
        bool toastOn = toastGO && toastGO.activeSelf;

        if (barGO)    barGO.SetActive(false);
        if (menuGO)   menuGO.SetActive(false);
        if (bannerGO) bannerGO.SetActive(false);
        if (toastGO)  toastGO.SetActive(false);

        foreach (var obj in spawned)
        {
            if (!obj) continue;
            var pi = obj.GetComponent<PotInfo>();
            if (pi != null && pi.measureLabel) pi.measureLabel.SetActive(false);
        }

        yield return new WaitForEndOfFrame();

        Texture2D tex = ScreenCapture.CaptureScreenshotAsTexture();
        if (tex == null)
        {
            RestoreUI(barOn, menuOn, banOn, toastOn);
            if (activeLabelPot != null) ShowLabelForPot(activeLabelPot);
            ShowToast("Capture failed");
            yield break;
        }

        byte[] png = tex.EncodeToPNG();
        Destroy(tex);

        string designName = "MyDesign_" + DateTime.Now.ToString("yyyyMMdd_HHmmss");
        string fn = designName + ".png";
        string savedPath = null;

#if UNITY_ANDROID && !UNITY_EDITOR
        string folder = "/storage/emulated/0/DCIM/MyDesigns/";
        try
        {
            Directory.CreateDirectory(folder);
            savedPath = Path.Combine(folder, fn);
            File.WriteAllBytes(savedPath, png);
            using (var scanClass = new AndroidJavaClass("android.media.MediaScannerConnection"))
            using (var player    = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            using (var ctx       = player.GetStatic<AndroidJavaObject>("currentActivity"))
                scanClass.CallStatic("scanFile", ctx, new string[] { savedPath }, null, null);
            onScreenshotSaved?.Invoke(savedPath);
        }
        catch (Exception e) { Debug.LogError("[AR] Screenshot save: " + e.Message); }
#else
        string dir = Path.Combine(Application.persistentDataPath, screenshotFolderName);
        Directory.CreateDirectory(dir);
        savedPath = Path.Combine(dir, fn);
        File.WriteAllBytes(savedPath, png);
        onScreenshotSaved?.Invoke(savedPath);
#endif

        RestoreUI(barOn, menuOn, banOn, toastOn);
        if (activeLabelPot != null) ShowLabelForPot(activeLabelPot);

        if (DesignUploadService.Instance != null)
        {
            DesignUploadService.Instance.UploadDesign(
                png, designName, "AR Design", "front",
                DateTime.UtcNow.ToString("o"), "{}");
        }
        else
            StartCoroutine(UploadToBackend(png, fn));

        ShowToast("Screenshot Saved!");
    }

    // ================================================================
    //  INTENT EXTRAS
    // ================================================================
    private void ReadIntentExtras()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            using var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            using var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            using var intent = activity.Call<AndroidJavaObject>("getIntent");
            using var extras = intent.Call<AndroidJavaObject>("getExtras");
            if (extras != null)
            {
                _jwtToken = extras.Call<string>("getString", "jwt_token") ?? "";
                _apiBaseUrl = extras.Call<string>("getString", "api_base_url") ?? "";
            }
        }
        catch (Exception e)
        {
            Debug.LogError("[AR] Failed to read intent extras: " + e.Message);
        }
#endif
    }

    // ================================================================
    //  UPLOAD TO BACKEND
    // ================================================================
    private IEnumerator UploadToBackend(byte[] pngData, string fileName)
    {
        if (ApiClient.Instance != null && ApiClient.Instance.IsAuthenticated)
        {
            string designName = Path.GetFileNameWithoutExtension(fileName);
            var fields = new Dictionary<string, string>
            {
                { "designName", designName },
                { "projectName", "AR Design" },
                { "designSide", "front" },
                { "clientTimestamp", DateTime.UtcNow.ToString("o") },
                { "metadataJson", "{}" }
            };
            ApiClient.Instance.UploadMultipart("/api/user/designs/upload", pngData,
                fileName, "file", fields, response =>
            {
                if (response.isSuccess)
                    ShowToast("Saved to My Designs!");
                else
                    Debug.LogWarning("[AR] Upload failed: " + response.error);
            });
            yield break;
        }

        if (string.IsNullOrEmpty(_jwtToken)) ReadIntentExtras();
        if (string.IsNullOrEmpty(_jwtToken)) yield break;

        string baseUrl = _apiBaseUrl.TrimEnd('/');
        if (string.IsNullOrEmpty(baseUrl)) baseUrl = "http://192.168.1.253:8081";

        var form = new WWWForm();
        form.AddBinaryData("file", pngData, fileName ?? "ar_design.png", "image/png");
        form.AddField("designName", "AR Design " + DateTime.Now.ToString("yyyy-MM-dd HH:mm"));

        using var request = UnityWebRequest.Post(baseUrl + "/api/user/designs/upload", form);
        request.SetRequestHeader("Authorization", "Bearer " + _jwtToken);
        request.timeout = 15;
        yield return request.SendWebRequest();

        if (request.result == UnityWebRequest.Result.Success)
            ShowToast("Saved to My Designs!");
        else
            Debug.LogError("[AR] Upload failed: " + request.error);
    }

    private void RestoreUI(bool bar, bool menu, bool banner, bool toast)
    {
        if (barGO)    barGO.SetActive(bar);
        if (menuGO)   menuGO.SetActive(menu);
        if (bannerGO) bannerGO.SetActive(banner);
        if (toastGO)  toastGO.SetActive(toast);
    }

    // ================================================================
    //  TOAST
    // ================================================================
    private void ShowToast(string msg)
    {
        if (!toastGO) return;
        if (toastCR != null) StopCoroutine(toastCR);
        if (toastTxt) toastTxt.text = msg;
        toastCR = StartCoroutine(ToastAnimation());
    }

    private IEnumerator ToastAnimation()
    {
        toastGO.SetActive(true);
        var cg = toastGO.GetComponent<CanvasGroup>();
        cg.alpha = 0;
        for (float t = 0; t < 0.2f; t += Time.unscaledDeltaTime)
        { cg.alpha = t / 0.2f; yield return null; }
        cg.alpha = 1;
        yield return new WaitForSecondsRealtime(2f);
        for (float t = 0; t < 0.3f; t += Time.unscaledDeltaTime)
        { cg.alpha = 1f - t / 0.3f; yield return null; }
        cg.alpha = 0;
        toastGO.SetActive(false);
        toastCR = null;
    }

    // ================================================================
    //  POT SPRITES
    // ================================================================
    private void LoadPotSprites()
    {
        spr1 = LoadSpr("potsphoto/Screenshot 2026-02-27 213635");
        if (spr1 == null) spr1 = LoadSpr("potsphoto/pot2");
        spr2 = LoadSpr("potsphoto/pot2 1");
    }

    private Sprite LoadSpr(string path)
    {
        var tex = Resources.Load<Texture2D>(path);
        if (tex == null) return null;
        return Sprite.Create(tex, new Rect(0, 0, tex.width, tex.height),
            new Vector2(0.5f, 0.5f));
    }

    // ================================================================
    //  CANVAS
    // ================================================================
    private Canvas CreateCanvas()
    {
        var existES = FindFirstObjectByType<EventSystem>(FindObjectsInactive.Include);
        if (existES != null) existES.transform.SetParent(null);
        foreach (var c in FindObjectsByType<Canvas>(FindObjectsSortMode.None))
            c.gameObject.SetActive(false);

        if (EventSystem.current == null)
        {
            if (existES != null) existES.gameObject.SetActive(true);
            else
            {
                var esGo = new GameObject("EventSystem");
                esGo.AddComponent<EventSystem>();
                esGo.AddComponent<UnityEngine.InputSystem.UI.InputSystemUIInputModule>();
            }
        }

        var go = new GameObject("ARCanvas");
        var cv = go.AddComponent<Canvas>();
        cv.renderMode = RenderMode.ScreenSpaceOverlay;
        cv.sortingOrder = 100;

        var sc = go.AddComponent<CanvasScaler>();
        sc.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
        sc.referenceResolution = new Vector2(1080, 2160);
        sc.matchWidthOrHeight = 0.5f;
        go.AddComponent<GraphicRaycaster>();
        return cv;
    }

    // ================================================================
    //  UI SETUP
    // ================================================================
    private void SetupRuntimeUI()
    {
        Transform root = canvas.transform;
        BuildBottomBar(root);
        BuildExitButton(root);
        BuildDeleteBanner(root);
        BuildToast(root);
        BuildMenu(root);
    }

    private void BuildBottomBar(Transform root)
    {
        float barH = 200f;
        barGO = new GameObject("BottomBar");
        barGO.transform.SetParent(root, false);
        var rt = barGO.AddComponent<RectTransform>();
        rt.anchorMin = new Vector2(0, 0);
        rt.anchorMax = new Vector2(1, 0);
        rt.pivot     = new Vector2(0.5f, 0);
        rt.offsetMin = Vector2.zero;
        rt.offsetMax = new Vector2(0, barH);

        var bg = barGO.AddComponent<Image>();
        bg.color = COL_BAR;
        bg.raycastTarget = true;

        var shadowLine = new GameObject("ShadowEdge");
        shadowLine.transform.SetParent(barGO.transform, false);
        var slrt = shadowLine.AddComponent<RectTransform>();
        slrt.anchorMin = new Vector2(0, 1); slrt.anchorMax = new Vector2(1, 1);
        slrt.pivot = new Vector2(0.5f, 0); slrt.offsetMin = Vector2.zero;
        slrt.offsetMax = Vector2.zero; slrt.sizeDelta = new Vector2(0, 8f);
        var slImg = shadowLine.AddComponent<Image>();
        slImg.color = new Color(0, 0, 0, 0.15f);
        slImg.raycastTarget = false;

        float gap = 240f;
        float iconSz = 68f;

        var camBtn = MakeBarIcon(barGO.transform, "CameraBtn", new Vector2(-gap, 8), 130);
        DrawCameraIcon3D(camBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(camBtn.transform, "Capture", new Vector2(0, -48f));
        camBtn.onClick.AddListener(TakeScreenshot);

        var menuBtn = MakeBarIcon(barGO.transform, "MenuBtn", new Vector2(0, 8), 140);
        DrawMenuGridIcon(menuBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(menuBtn.transform, "Menu", new Vector2(0, -48f));
        menuBtn.onClick.AddListener(() => SetMenuOpen(true));

        var trashBtn = MakeBarIcon(barGO.transform, "TrashBtn", new Vector2(gap, 8), 130);
        DrawTrashIcon3D(trashBtn.transform, iconSz, ALOE, new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.5f));
        MakeBarLabel(trashBtn.transform, "Remove", new Vector2(0, -48f));
        trashBtn.onClick.AddListener(() => SetDeleteMode(!deleteMode));
    }

    private void MakeBarLabel(Transform parent, string text, Vector2 offset)
    {
        var go = new GameObject("Lbl");
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = offset;
        rt.sizeDelta = new Vector2(160, 36);
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text = text; tmp.fontSize = 24f; tmp.fontStyle = FontStyles.Bold;
        tmp.color = new Color(ALOE.r, ALOE.g, ALOE.b, 0.85f);
        tmp.alignment = TextAlignmentOptions.Center;
        tmp.raycastTarget = false;
    }

    private void BuildExitButton(Transform root)
    {
        float safeT = SafeTopPx() + 22f;
        var go = new GameObject("ExitBtn");
        go.transform.SetParent(root, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(1, 1);
        rt.pivot = new Vector2(1, 1);
        rt.anchoredPosition = new Vector2(-22, -safeT);
        rt.sizeDelta = new Vector2(90, 90);

        var img = go.AddComponent<Image>();
        img.color = new Color(MOSS.r, MOSS.g, MOSS.b, 0.45f);
        img.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = img;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);
        btn.onClick.AddListener(() => Application.Quit());

        go.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        go.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);
        DrawForwardArrow3D(go.transform, 34f, ALOE, new Color(CEDAR.r, CEDAR.g, CEDAR.b, 0.5f));
    }

    private void BuildDeleteBanner(Transform root)
    {
        float safeT = SafeTopPx() + 18f;
        bannerGO = new GameObject("DeleteBanner");
        bannerGO.transform.SetParent(root, false);
        var rt = bannerGO.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 1);
        rt.pivot = new Vector2(0.5f, 1);
        rt.anchoredPosition = new Vector2(0, -safeT);
        rt.sizeDelta = new Vector2(600, 72);

        var bg = bannerGO.AddComponent<Image>();
        bg.color = COL_RED_BG;
        bg.raycastTarget = false;
        bannerGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        bannerGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);

        var txt = AddTMP(bannerGO.transform, "Txt",
            "Tap a plant to remove", 30f, CREAM, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)txt).fontStyle = FontStyles.Italic;
        ((TextMeshProUGUI)txt).characterSpacing = 1.5f;
        StretchFill(txt.gameObject);
    }

    private void BuildToast(Transform root)
    {
        float safeT = SafeTopPx() + 110f;
        toastGO = new GameObject("Toast");
        toastGO.transform.SetParent(root, false);
        var rt = toastGO.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 1);
        rt.pivot = new Vector2(0.5f, 1);
        rt.anchoredPosition = new Vector2(0, -safeT);
        rt.sizeDelta = new Vector2(540, 68);

        var bg = toastGO.AddComponent<Image>();
        bg.color = COL_TOAST_BG;
        bg.raycastTarget = false;
        toastGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.20f);
        toastGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -3);
        toastGO.AddComponent<CanvasGroup>();

        toastTxt = AddTMP(toastGO.transform, "Txt", "", 27f, CREAM,
            TextAlignmentOptions.Center);
        ((TextMeshProUGUI)toastTxt).fontStyle = FontStyles.Normal;
        ((TextMeshProUGUI)toastTxt).characterSpacing = 0.8f;
        StretchFill(toastTxt.gameObject);
    }

    private void BuildMenu(Transform root)
    {
        menuGO = new GameObject("MenuOverlay");
        menuGO.transform.SetParent(root, false);
        var rt = menuGO.AddComponent<RectTransform>();
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;

        var bg = menuGO.AddComponent<Image>();
        bg.color = CREAM;
        bg.raycastTarget = true;

        float safeT = SafeTopPx();

        var headerGO = new GameObject("Header");
        headerGO.transform.SetParent(menuGO.transform, false);
        var hrt = headerGO.AddComponent<RectTransform>();
        hrt.anchorMin = new Vector2(0, 1); hrt.anchorMax = new Vector2(1, 1);
        hrt.pivot = new Vector2(0.5f, 1);
        hrt.offsetMin = Vector2.zero; hrt.offsetMax = Vector2.zero;
        hrt.sizeDelta = new Vector2(0, safeT + 280f);

        var headerBg = headerGO.AddComponent<Image>();
        headerBg.color = new Color(OLIVE.r, OLIVE.g, OLIVE.b, 0.06f);
        headerBg.raycastTarget = false;

        var titleTmp = AddTMP(headerGO.transform, "Title",
            "Menu", 82f, MOSS, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)titleTmp).fontStyle = FontStyles.Bold;
        ((TextMeshProUGUI)titleTmp).characterSpacing = 10f;
        var trt = titleTmp.GetComponent<RectTransform>();
        trt.anchorMin = new Vector2(0, 0); trt.anchorMax = new Vector2(1, 0);
        trt.pivot = new Vector2(0.5f, 0);
        trt.anchoredPosition = new Vector2(0, 52);
        trt.sizeDelta = new Vector2(-60, 100);

        var subTmp = AddTMP(headerGO.transform, "Subtitle",
            "Pick your plant, then tap to place", 30f,
            new Color(MOSS.r, MOSS.g, MOSS.b, 0.70f), TextAlignmentOptions.Center);
        ((TextMeshProUGUI)subTmp).fontStyle = FontStyles.Italic;
        ((TextMeshProUGUI)subTmp).characterSpacing = 1.2f;
        var srt2 = subTmp.GetComponent<RectTransform>();
        srt2.anchorMin = new Vector2(0, 0); srt2.anchorMax = new Vector2(1, 0);
        srt2.pivot = new Vector2(0.5f, 1);
        srt2.anchoredPosition = new Vector2(0, 46);
        srt2.sizeDelta = new Vector2(-80, 42);

        var sep = new GameObject("Sep");
        sep.transform.SetParent(headerGO.transform, false);
        var seprt = sep.AddComponent<RectTransform>();
        seprt.anchorMin = new Vector2(0.15f, 0); seprt.anchorMax = new Vector2(0.85f, 0);
        seprt.pivot = new Vector2(0.5f, 0);
        seprt.offsetMin = seprt.offsetMax = Vector2.zero;
        seprt.sizeDelta = new Vector2(0, 1.5f);
        sep.AddComponent<Image>().color = new Color(CEDAR.r, CEDAR.g, CEDAR.b, 0.20f);
        sep.GetComponent<Image>().raycastTarget = false;

        float imgSz  = 380f;
        float imgGap = 44f;
        float totalW = imgSz * 2 + imgGap;
        float x1 = -totalW / 2f + imgSz / 2f;
        float x2 = x1 + imgSz + imgGap;
        float cardsY = 200f;

        var btn1 = MakePotImageButton(menuGO.transform, "Pot1Btn",
            spr1, new Vector2(x1, cardsY), imgSz, 0);
        sel1Img = btn1.GetComponent<Image>();

        MakePotNameLabel(menuGO.transform, "Name1",
            (potNames != null && potNames.Length > 0) ? potNames[0] : "Plant 1",
            new Vector2(x1, cardsY - imgSz / 2f - 30f));

        var btn2 = MakePotImageButton(menuGO.transform, "Pot2Btn",
            spr2, new Vector2(x2, cardsY), imgSz, 1);
        sel2Img = btn2.GetComponent<Image>();

        MakePotNameLabel(menuGO.transform, "Name2",
            (potNames != null && potNames.Length > 1) ? potNames[1] : "Plant 2",
            new Vector2(x2, cardsY - imgSz / 2f - 30f));

        var doneGO = new GameObject("DoneBtn");
        doneGO.transform.SetParent(menuGO.transform, false);
        var drt = doneGO.AddComponent<RectTransform>();
        drt.anchorMin = drt.anchorMax = new Vector2(0.5f, 0);
        drt.pivot = new Vector2(0.5f, 0);
        drt.anchoredPosition = new Vector2(0, 120);
        drt.sizeDelta = new Vector2(460, 96);

        var doneBg = doneGO.AddComponent<Image>();
        doneBg.color = OLIVE;
        doneBg.raycastTarget = true;
        doneGO.AddComponent<Shadow>().effectColor = new Color(0, 0, 0, 0.18f);
        doneGO.GetComponent<Shadow>().effectDistance = new Vector2(0, -5);

        var doneBtn = doneGO.AddComponent<Button>();
        doneBtn.targetGraphic = doneBg;
        NavNone(doneBtn);
        doneBtn.onClick.AddListener(() => SetMenuOpen(false));

        var doneTxt = AddTMP(doneGO.transform, "DoneTxt",
            "Place It", 34f, CREAM, TextAlignmentOptions.Center);
        ((TextMeshProUGUI)doneTxt).fontStyle = FontStyles.Bold;
        ((TextMeshProUGUI)doneTxt).characterSpacing = 3f;
        StretchFill(doneTxt.gameObject);

        HighlightSelection();
    }

    private void MakePotNameLabel(Transform parent, string name, string text, Vector2 pos)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = pos;
        rt.sizeDelta = new Vector2(380, 52);
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text = text; tmp.fontSize = 26f; tmp.color = MOSS;
        tmp.alignment = TextAlignmentOptions.Center;
        tmp.fontStyle = FontStyles.Bold;
        tmp.characterSpacing = 1.5f;
        tmp.raycastTarget = false;
    }

    private Button MakePotImageButton(Transform parent, string name,
        Sprite spr, Vector2 pos, float size, int index)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.anchoredPosition = pos;
        rt.sizeDelta = new Vector2(size, size);

        var bgImg = go.AddComponent<Image>();
        bgImg.color = Color.clear;
        bgImg.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = bgImg;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);

        int idx = index;
        btn.onClick.AddListener(() =>
        {
            selectedPotIndex = idx;
            HighlightSelection();
        });

        if (spr != null)
        {
            var imgGO = new GameObject("Img");
            imgGO.transform.SetParent(go.transform, false);
            var irt = imgGO.AddComponent<RectTransform>();
            irt.anchorMin = new Vector2(0.04f, 0.04f);
            irt.anchorMax = new Vector2(0.96f, 0.96f);
            irt.offsetMin = irt.offsetMax = Vector2.zero;
            var img = imgGO.AddComponent<Image>();
            img.sprite = spr;
            img.preserveAspect = true;
            img.raycastTarget = false;
        }
        return btn;
    }

    private void HighlightSelection()
    {
        HighlightCard(sel1Img, selectedPotIndex == 0);
        HighlightCard(sel2Img, selectedPotIndex == 1);
    }

    private void HighlightCard(Image card, bool selected)
    {
        if (!card) return;
        card.color = Color.clear;
        var outline = card.GetComponent<Outline>();
        if (outline) outline.enabled = false;
        var rt = card.GetComponent<RectTransform>();
        if (rt)
        {
            Vector3 target = selected ? new Vector3(1.10f, 1.10f, 1f) : Vector3.one;
            StopCoroutine("AnimateCardScale");
            StartCoroutine(AnimateCardScale(rt, target, 0.15f));
        }
    }

    private IEnumerator AnimateCardScale(RectTransform rt, Vector3 target, float duration)
    {
        Vector3 start = rt.localScale;
        for (float t = 0; t < duration; t += Time.unscaledDeltaTime)
        {
            float p = t / duration;
            float ease = 1f - Mathf.Pow(1f - p, 3f);
            rt.localScale = Vector3.LerpUnclamped(start, target, ease);
            yield return null;
        }
        rt.localScale = target;
    }

    private void SetMenuOpen(bool open)
    {
        if (menuGO)  menuGO.SetActive(open);
        if (barGO)   barGO.SetActive(!open);
        if (open) SetDeleteMode(false);
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================
    private float SafeTopPx()
    {
        return (Screen.height - Screen.safeArea.yMax) / canvas.scaleFactor;
    }

    private TMP_Text AddTMP(Transform parent, string name, string text,
        float fontSize, Color color, TextAlignmentOptions align)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;
        var tmp = go.AddComponent<TextMeshProUGUI>();
        tmp.text = text; tmp.fontSize = fontSize;
        tmp.color = color; tmp.alignment = align;
        tmp.raycastTarget = false;
        return tmp;
    }

    private void StretchFill(GameObject go)
    {
        var rt = go.GetComponent<RectTransform>();
        if (!rt) return;
        rt.anchorMin = Vector2.zero;
        rt.anchorMax = Vector2.one;
        rt.offsetMin = rt.offsetMax = Vector2.zero;
    }

    private Button MakeBarIcon(Transform parent, string name, Vector2 offset, float hitSize = 120)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.sizeDelta = new Vector2(hitSize, hitSize);
        rt.anchoredPosition = offset;

        var img = go.AddComponent<Image>();
        img.color = Color.clear;
        img.raycastTarget = true;

        var btn = go.AddComponent<Button>();
        btn.targetGraphic = img;
        btn.transition = Selectable.Transition.None;
        NavNone(btn);
        return btn;
    }

    private void NavNone(Button b)
    {
        var nav = b.navigation;
        nav.mode = Navigation.Mode.None;
        b.navigation = nav;
    }

    private GameObject MakeIconRect(Transform parent, string name,
        Vector2 size, Vector2 offset, Color color)
    {
        var go = new GameObject(name);
        go.transform.SetParent(parent, false);
        var rt = go.AddComponent<RectTransform>();
        rt.anchorMin = rt.anchorMax = new Vector2(0.5f, 0.5f);
        rt.pivot = new Vector2(0.5f, 0.5f);
        rt.sizeDelta = size;
        rt.anchoredPosition = offset;
        var img = go.AddComponent<Image>();
        img.color = color;
        img.raycastTarget = false;
        return go;
    }

    // ================================================================
    //  3D ICON DRAWING
    // ================================================================
    private void DrawCameraIcon3D(Transform p, float sz, Color front, Color shadow)
    {
        MakeIconRect(p, "BodySh", new Vector2(sz * 0.92f, sz * 0.60f),
            new Vector2(sz * 0.03f, -sz * 0.07f), shadow);
        MakeIconRect(p, "Body", new Vector2(sz * 0.92f, sz * 0.60f),
            new Vector2(0, -sz * 0.02f), front);
        MakeIconRect(p, "VF", new Vector2(sz * 0.28f, sz * 0.16f),
            new Vector2(-sz * 0.12f, sz * 0.35f), front);
        MakeIconRect(p, "VFSh", new Vector2(sz * 0.28f, sz * 0.16f),
            new Vector2(-sz * 0.09f, sz * 0.33f), shadow);
        MakeIconRect(p, "Flash", new Vector2(sz * 0.10f, sz * 0.10f),
            new Vector2(sz * 0.30f, sz * 0.35f), front);
        MakeIconRect(p, "LensOuter", new Vector2(sz * 0.32f, sz * 0.32f),
            new Vector2(0, -sz * 0.02f), shadow);
        MakeIconRect(p, "LensInner", new Vector2(sz * 0.22f, sz * 0.22f),
            new Vector2(0, -sz * 0.02f), new Color(front.r, front.g, front.b, 0.45f));
        MakeIconRect(p, "LensDot", new Vector2(sz * 0.08f, sz * 0.08f),
            new Vector2(sz * 0.04f, sz * 0.04f), front);
    }

    private void DrawMenuGridIcon(Transform p, float sz, Color front, Color shadow)
    {
        float dot = sz * 0.22f;
        float sp  = sz * 0.17f;
        for (int r = 0; r < 2; r++)
        for (int c = 0; c < 2; c++)
        {
            float x = (c == 0 ? -sp : sp);
            float y = (r == 0 ? -sp : sp);
            MakeIconRect(p, "DSh" + r + c, new Vector2(dot, dot),
                new Vector2(x + 2f, y - 2f), shadow);
            MakeIconRect(p, "D" + r + c, new Vector2(dot, dot),
                new Vector2(x, y), front);
        }
        MakeIconRect(p, "PlusH", new Vector2(sz * 0.06f, dot * 1.6f), Vector2.zero,
            new Color(front.r, front.g, front.b, 0.35f));
        MakeIconRect(p, "PlusV", new Vector2(dot * 1.6f, sz * 0.06f), Vector2.zero,
            new Color(front.r, front.g, front.b, 0.35f));
    }

    private void DrawTrashIcon3D(Transform p, float sz, Color front, Color shadow)
    {
        float top = sz * 0.24f;
        MakeIconRect(p, "BodySh", new Vector2(sz * 0.52f, sz * 0.54f),
            new Vector2(2f, top - sz * 0.36f - 2f), shadow);
        MakeIconRect(p, "Body", new Vector2(sz * 0.52f, sz * 0.54f),
            new Vector2(0, top - sz * 0.36f), front);
        MakeIconRect(p, "LidSh", new Vector2(sz * 0.72f, sz * 0.08f),
            new Vector2(2f, top - 2f), shadow);
        MakeIconRect(p, "Lid", new Vector2(sz * 0.72f, sz * 0.08f),
            new Vector2(0, top), front);
        MakeIconRect(p, "Handle", new Vector2(sz * 0.22f, sz * 0.12f),
            new Vector2(0, top + sz * 0.12f), front);
        for (int i = -1; i <= 1; i++)
            MakeIconRect(p, "Line" + i, new Vector2(sz * 0.04f, sz * 0.36f),
                new Vector2(i * sz * 0.12f, top - sz * 0.36f),
                new Color(shadow.r, shadow.g, shadow.b, 0.5f));
    }

    private void DrawForwardArrow3D(Transform p, float sz, Color front, Color shadow)
    {
        float thick = sz * 0.16f;
        MakeIconRect(p, "ShaftSh", new Vector2(sz * 0.55f, thick),
            new Vector2(-sz * 0.04f + 2f, -2f), shadow);
        MakeIconRect(p, "Shaft", new Vector2(sz * 0.55f, thick),
            new Vector2(-sz * 0.04f, 0), front);
        var s1 = MakeIconRect(p, "Arm1Sh", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f + 2f, sz * 0.14f - 2f), shadow);
        s1.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, -40);
        var s2 = MakeIconRect(p, "Arm2Sh", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f + 2f, -sz * 0.14f - 2f), shadow);
        s2.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, 40);
        var a1 = MakeIconRect(p, "Arm1", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f, sz * 0.14f), front);
        a1.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, -40);
        var a2 = MakeIconRect(p, "Arm2", new Vector2(sz * 0.44f, thick),
            new Vector2(sz * 0.16f, -sz * 0.14f), front);
        a2.GetComponent<RectTransform>().localRotation = Quaternion.Euler(0, 0, 40);
    }

    // ================================================================
    //  SUPPRESS DEVELOPER MODE
    // ================================================================
    private void OnGUI()
    {
        if (Debug.developerConsoleVisible) Debug.developerConsoleVisible = false;
        if (Event.current != null
            && (Event.current.type == EventType.KeyDown || Event.current.type == EventType.KeyUp))
        {
            if (Event.current.keyCode == KeyCode.BackQuote
                || Event.current.keyCode == KeyCode.F1
                || Event.current.keyCode == KeyCode.F2)
                Event.current.Use();
        }
    }
}
