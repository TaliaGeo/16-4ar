using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.XR.ARFoundation;
using UnityEngine.XR.ARSubsystems;

/// <summary>
/// Central orchestrator — owns the spawned list, wires all services,
/// handles placement/delete/scale flow. Uses world-space anchors
/// and footprint-based overlap checks.
/// </summary>
[RequireComponent(typeof(ARRaycastManager))]
[RequireComponent(typeof(ARPlaneManager))]
[DefaultExecutionOrder(-50)]
public class ARPlacementController : MonoBehaviour
{
    // ── Inspector ───────────────────────────────────────────────────
    [Header("AR")]
    [SerializeField] private ARRaycastManager   raycastManager;
    [SerializeField] private ARPlaneManager     planeManager;
    [SerializeField] private ARAnchorManager    anchorManager;
    [SerializeField] private Camera             arCamera;

    [Header("Config")]
    [SerializeField] private ARPlacementConfig  config;
    [SerializeField] private PlantCatalogEntry[] catalog;
    [SerializeField] private Vector3 prefabRotationOffsetEuler = Vector3.zero;

    // ── Services (sibling MonoBehaviours) ───────────────────────────
    private ARAnchorService      _anchorService;
    private ARGestureController  _gestureCtrl;
    private ARScreenshotService  _screenshotSvc;
    private ARUIController       _uiCtrl;

    // ── Runtime state ───────────────────────────────────────────────
    private int  _selectedIndex;
    private bool _deleteMode;
    private readonly List<GameObject> _spawned = new List<GameObject>();
    private bool _spawnedDirty;

    private static readonly List<ARRaycastHit> sHits = new List<ARRaycastHit>();

    private static readonly TrackableType PlaneFilterTight = TrackableType.PlaneWithinPolygon;
    private static readonly TrackableType PlaneFilterLoose = TrackableType.PlaneWithinBounds;

    private GameObject _activeLabelPot;

    // Health-check throttle state
    private int            _healthCheckFrame;
    private ARSessionState _lastSessionState;

    // Placement reticle
    private GameObject _reticle;
    private Renderer   _reticleRenderer;
    private float      _reticleStableTime;
    private Vector3    _lastReticlePos;
    private const float ReticleStableThreshold = 0.4f;   // seconds needed
    private const float ReticleMoveTolerance   = 0.02f;  // meters

    // Screenshot UI state snapshot
    private (bool bar, bool menu, bool banner, bool toast) _screenshotUIState;

    // ════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════════════════

    private void Awake()
    {
        // ── Resolve AR components ───────────────────────────────────
        if (!raycastManager) raycastManager = GetComponent<ARRaycastManager>();
        if (!planeManager)   planeManager   = GetComponent<ARPlaneManager>();
        if (!anchorManager)  anchorManager  = FindFirstObjectByType<ARAnchorManager>();
        if (!arCamera)       arCamera       = Camera.main;

        // ── Ensure config exists ────────────────────────────────────
        if (config == null)
        {
            config = ScriptableObject.CreateInstance<ARPlacementConfig>();
            Debug.LogWarning("[ARPlacement] No ARPlacementConfig assigned — using runtime defaults. " +
                "Run menu AR → Setup Scene to fix this permanently.");
        }

        // ── Validate camera ────────────────────────────────────────
        if (!arCamera)
            Debug.LogError("[ARPlacement] No Camera found! AR will not work. " +
                "Assign a Camera in the Inspector or ensure Camera.main exists.");

        // ── Validate & auto-add AR managers ────────────────────────
        if (!raycastManager)
        {
            raycastManager = gameObject.AddComponent<ARRaycastManager>();
            Debug.LogWarning("[ARPlacement] ARRaycastManager was missing — added at runtime.");
        }
        if (!planeManager)
        {
            planeManager = gameObject.AddComponent<ARPlaneManager>();
            Debug.LogWarning("[ARPlacement] ARPlaneManager was missing — added at runtime.");
        }
        if (!anchorManager)
        {
            GameObject host = planeManager ? planeManager.gameObject : gameObject;
            anchorManager = host.AddComponent<ARAnchorManager>();
            Debug.LogWarning("[ARPlacement] ARAnchorManager was missing — added at runtime to " + host.name);
        }

        // ── Disable old ARPlacementManager if present ───────────────
        DisableOldSystem();

        // ── Ensure sibling services exist ───────────────────────────
        _anchorService  = GetOrAdd<ARAnchorService>();
        _gestureCtrl    = GetOrAdd<ARGestureController>();
        _screenshotSvc  = GetOrAdd<ARScreenshotService>();
        _uiCtrl         = GetOrAdd<ARUIController>();

        // Init anchor service
        _anchorService.Init(planeManager, anchorManager);
        _anchorService.Spawned = _spawned;

        // Init gesture controller
        _gestureCtrl.Init(arCamera, config.tapCooldown);
        _gestureCtrl.Spawned = _spawned;
        _gestureCtrl.FindSpawnedRoot = FindSpawnedRoot;

        // Wire gesture events
        _gestureCtrl.OnSurfaceTap  += OnSurfaceTap;
        _gestureCtrl.OnObjectTap   += OnObjectTap;
        _gestureCtrl.OnPinchBegan  += OnPinchBegan;
        _gestureCtrl.OnPinchMoved  += OnPinchMoved;
        _gestureCtrl.OnPinchEnded  += OnPinchEnded;

        // ── Build UI ────────────────────────────────────────────────
        if (catalog == null || catalog.Length == 0)
            Debug.LogWarning("[ARPlacement] Catalog is EMPTY — menu will show no plants. " +
                "Run menu AR → Setup Scene to create catalog entries automatically.");

        try
        {
            _uiCtrl.BuildUI(catalog);
            Debug.Log("[ARPlacement] UI initialized successfully.");
        }
        catch (System.Exception e)
        {
            Debug.LogError("[ARPlacement] UI FAILED to initialize: " + e.Message + "\n" + e.StackTrace);
        }

        _uiCtrl.OnCaptureClicked  = () => _screenshotSvc.Capture();
        _uiCtrl.OnMenuOpened      = () => SetDeleteMode(false);
        _uiCtrl.OnDeleteToggled   = on => SetDeleteMode(!_deleteMode);
        _uiCtrl.OnPotSelected     = idx => _selectedIndex = idx;
        _uiCtrl.OnExitClicked     = () => Application.Quit();

        // Init screenshot service callbacks
        _screenshotSvc.ShowToast = msg => _uiCtrl.ShowToast(msg);
        _screenshotSvc.HideUI = () =>
        {
            _screenshotUIState = _uiCtrl.HideAllUI();
        };
        _screenshotSvc.RestoreUI = () =>
        {
            _uiCtrl.RestoreUI(
                _screenshotUIState.bar, _screenshotUIState.menu,
                _screenshotUIState.banner, _screenshotUIState.toast);
        };
        _screenshotSvc.HideLabels = () => HideAllLabels();
        _screenshotSvc.RestoreLabels = () => { if (_activeLabelPot) ShowLabelForPot(_activeLabelPot); };

        // Platform bridge
        ARPlatformBridge.ReadIntentExtras();
        _screenshotSvc.JwtToken  = ARPlatformBridge.JwtToken;
        _screenshotSvc.ApiBaseUrl = ARPlatformBridge.ApiBaseUrl;

        // ── Startup diagnostic log ──────────────────────────────────
        LogStartupDiagnostics();
    }

    private void Start()
    {
        CleanPreExistingPots();
        CreateReticle();
        _uiCtrl.ShowToast("Slowly aim at a flat surface like floor or table");
    }

    private void OnDestroy()
    {
        if (_gestureCtrl)
        {
            _gestureCtrl.OnSurfaceTap  -= OnSurfaceTap;
            _gestureCtrl.OnObjectTap   -= OnObjectTap;
            _gestureCtrl.OnPinchBegan  -= OnPinchBegan;
            _gestureCtrl.OnPinchMoved  -= OnPinchMoved;
            _gestureCtrl.OnPinchEnded  -= OnPinchEnded;
        }
        if (_reticle) Destroy(_reticle);
    }

    private void LateUpdate()
    {
        if (_spawnedDirty)
        {
            _spawned.RemoveAll(o => o == null);
            _spawnedDirty = false;
        }

        _anchorService.MonitorAnchorHealth(
            ref _healthCheckFrame, config.healthCheckInterval, ref _lastSessionState);
        BillboardLabels();
        UpdateReticle();
    }

    // ════════════════════════════════════════════════════════════════
    //  GESTURE EVENT HANDLERS
    // ════════════════════════════════════════════════════════════════

    private void OnSurfaceTap(Vector2 screenPos)
    {
        if (_uiCtrl.IsMenuOpen) return;

        HideAllLabels();

        // Gate: require reticle to have been stable for the threshold duration
        if (!_reticle || !_reticle.activeSelf || _reticleStableTime < ReticleStableThreshold)
        {
            _uiCtrl.ShowToast("Hold steady on a surface to place");
            return;
        }

        // Two-pass plane raycast
        ARPlane bestPlane = null;
        ARRaycastHit bestHit = default;

        if (raycastManager.Raycast(screenPos, sHits, PlaneFilterTight))
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);

        if (bestPlane == null && raycastManager.Raycast(screenPos, sHits, PlaneFilterLoose))
        {
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);
            if (bestPlane != null && bestPlane.trackingState != TrackingState.Tracking)
                bestPlane = null;
        }

        if (bestPlane != null)
        {
            PlaceObject(bestHit.pose, bestPlane);
            return;
        }

        _uiCtrl.ShowToast("Slowly aim at a flat surface like floor or table");
    }

    private void OnObjectTap(GameObject tapped)
    {
        if (_uiCtrl.IsMenuOpen) return;
        if (_deleteMode) { DeleteObject(tapped); return; }
        ShowLabelForPot(tapped);
    }

    private void OnPinchBegan(GameObject target, float startDist, Vector3 startScale)
    {
        // No action needed at begin — scale is applied per-move and finalized at end.
    }

    private void OnPinchMoved(GameObject target, float ratio)
    {
        var ctrl = target.GetComponent<ARObjectController>();
        if (ctrl == null) return;

        // Compute target scale from the STORED start scale, not current scale.
        // Using current scale each frame causes exponential compounding.
        Vector3 startScale = _gestureCtrl.PinchStartScale;
        Vector3 bs = ctrl.NormalizedScale;
        float startFactor = startScale.x / Mathf.Max(bs.x, 0.0001f);
        float tgtFactor = Mathf.Clamp(startFactor * ratio, config.minScale, config.maxScale);
        target.transform.localScale = bs * tgtFactor;

        // Do NOT ground, freeze, or rebuild labels here — that happens on pinch end.
        // Doing it every frame causes sluggish manipulation.
    }

    private void OnPinchEnded(GameObject target)
    {
        var ctrl = target.GetComponent<ARObjectController>();
        if (ctrl == null) return;

        string[] ignoreNames = ctrl.CatalogEntry != null ? ctrl.CatalogEntry.ignoreRendererNames : null;

        ARBoundsUtility.Ground(target, config.yOffset, ignoreNames);

        // Check footprint overlap at final scale
        if (_spawned.Count > 1)
        {
            Bounds footprint = ARBoundsUtility.BaseFootprint(target, ignoreNames);
            if (footprint.size != Vector3.zero && WouldOverlapFootprint(footprint, target))
            {
                target.transform.localScale = _gestureCtrl.PinchStartScale;
                ARBoundsUtility.Ground(target, config.yOffset, ignoreNames);
                _uiCtrl.ShowToast("Can't scale — touching another pot");
            }
        }

        // Rebuild measurement label
        if (ctrl.MeasureLabel) Destroy(ctrl.MeasureLabel);
        ctrl.MeasureLabel = _uiCtrl.CreateMeasureLabel(target, ignoreNames);
    }

    // ════════════════════════════════════════════════════════════════
    //  PLACEMENT
    // ════════════════════════════════════════════════════════════════

    private void PlaceObject(Pose pose, ARPlane plane)
    {
        if (catalog == null || catalog.Length == 0) return;
        int idx = Mathf.Clamp(_selectedIndex, 0, catalog.Length - 1);
        PlantCatalogEntry entry = catalog[idx];
        if (entry == null || entry.prefab == null) return;

        if (ARSession.state != ARSessionState.SessionTracking)
        { _uiCtrl.ShowToast("Waiting for AR tracking…"); return; }
        if (plane == null || plane.alignment != PlaneAlignment.HorizontalUp)
        { _uiCtrl.ShowToast("Horizontal surface required"); return; }
        if (plane.trackingState == TrackingState.None)
        { _uiCtrl.ShowToast("Surface lost – move phone slowly"); return; }

        SpawnNewPot(pose, plane, entry);
    }

    private void SpawnNewPot(Pose pose, ARPlane plane, PlantCatalogEntry entry)
    {
        // World-space anchor (not plane-attached)
        ARAnchor anchor = _anchorService.TryCreateWorldAnchor(pose, plane);
        if (anchor == null)
        { _uiCtrl.ShowToast("Anchor failed – try another spot"); return; }

        GameObject obj = ARObjectFactory.Create(
            entry, anchor.transform, config, arCamera, prefabRotationOffsetEuler);
        if (obj == null)
        { Destroy(anchor.gameObject); return; }

        var ctrl = obj.GetComponent<ARObjectController>();
        ctrl.AnchorObject  = anchor.gameObject;
        ctrl.SourcePlaneId = plane.trackableId;
        ctrl.MeasureLabel  = _uiCtrl.CreateMeasureLabel(obj, entry.ignoreRendererNames);

        // Validate: ensure the anchor is not under the camera rig
        if (!ValidateParentChain(obj, anchor))
        {
            if (ctrl.MeasureLabel) Destroy(ctrl.MeasureLabel);
            Destroy(anchor.gameObject);
            _uiCtrl.ShowToast("Placement failed — try another spot");
            return;
        }

        // Overlap check using the REAL grounded footprint of the instantiated object
        if (_spawned.Count > 0)
        {
            Bounds footprint = ARBoundsUtility.BaseFootprint(obj, entry.ignoreRendererNames);
            if (footprint.size != Vector3.zero && WouldOverlapFootprint(footprint, obj))
            {
                if (ctrl.MeasureLabel) Destroy(ctrl.MeasureLabel);
                Destroy(anchor.gameObject);
                _uiCtrl.ShowToast("Too close to another pot!");
                return;
            }
        }

        Physics.SyncTransforms();
        _spawned.Add(obj);
        ShowLabelForPot(obj);
        _anchorService.DisablePlaneVisualsAfterPlacement();
        StartCoroutine(VerifyPlacement(obj, obj.transform.position));
        _uiCtrl.ShowToast("Pot placed!");
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════════════════════

    private void SetDeleteMode(bool on)
    {
        _deleteMode = on;
        _gestureCtrl.DeleteMode = on;
        _uiCtrl.SetDeleteBanner(on);
    }

    private void DeleteObject(GameObject obj)
    {
        if (!obj) return;
        _spawned.Remove(obj);
        var ctrl = obj.GetComponent<ARObjectController>();
        if (ctrl != null)
        {
            if (ctrl.MeasureLabel) Destroy(ctrl.MeasureLabel);
            GameObject toDestroy = ctrl.AnchorObject ? ctrl.AnchorObject : obj;
            Destroy(toDestroy);
        }
        else
            Destroy(obj);
        _spawnedDirty = true;
        SetDeleteMode(false);
        _uiCtrl.ShowToast("Pot removed");
    }

    // ════════════════════════════════════════════════════════════════
    //  LABELS
    // ════════════════════════════════════════════════════════════════

    private void ShowLabelForPot(GameObject pot)
    {
        _activeLabelPot = pot;
        foreach (var obj in _spawned)
        {
            if (!obj) continue;
            var ctrl = obj.GetComponent<ARObjectController>();
            if (ctrl != null && ctrl.MeasureLabel)
                ctrl.MeasureLabel.SetActive(obj == pot);
        }
    }

    private void HideAllLabels()
    {
        _activeLabelPot = null;
        foreach (var obj in _spawned)
        {
            if (!obj) continue;
            var ctrl = obj.GetComponent<ARObjectController>();
            if (ctrl != null && ctrl.MeasureLabel)
                ctrl.MeasureLabel.SetActive(false);
        }
    }

    private void BillboardLabels()
    {
        if (!arCamera || _activeLabelPot == null) return;
        foreach (var obj in _spawned)
        {
            if (!obj) continue;
            var ctrl = obj.GetComponent<ARObjectController>();
            if (ctrl == null || !ctrl.MeasureLabel
                || !ctrl.MeasureLabel.activeSelf) continue;
            var t = ctrl.MeasureLabel.transform;
            Vector3 dir = t.position - arCamera.transform.position;
            if (dir.sqrMagnitude > 0.001f)
                t.rotation = Quaternion.LookRotation(dir);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  OVERLAP (footprint-based)
    // ════════════════════════════════════════════════════════════════

    private bool WouldOverlapFootprint(Bounds candidateFP, GameObject exclude = null)
    {
        for (int i = 0; i < _spawned.Count; i++)
        {
            var existing = _spawned[i];
            if (!existing || existing == exclude) continue;
            var ctrl = existing.GetComponent<ARObjectController>();
            string[] ignore = ctrl?.CatalogEntry?.ignoreRendererNames;
            Bounds existingFP = ARBoundsUtility.BaseFootprint(existing, ignore);
            if (existingFP.size == Vector3.zero) continue;

            if (ARBoundsUtility.FootprintsOverlap(candidateFP, existingFP, config.minGapMetres))
                return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════
    //  RAYCAST HELPERS
    // ════════════════════════════════════════════════════════════════

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
        if (angle >= config.maxSurfaceAngleDeg) return false;
        if (plane.alignment != PlaneAlignment.HorizontalUp) return false;
        if (plane.trackingState == TrackingState.None) return false;
        if (plane.size.x * plane.size.y < config.minPlaneAreaSqm) return false;
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  UTIL
    // ════════════════════════════════════════════════════════════════

    private GameObject FindSpawnedRoot(Transform t)
    {
        while (t != null)
        {
            if (_spawned.Contains(t.gameObject)) return t.gameObject;
            t = t.parent;
        }
        return null;
    }

    private bool ValidateParentChain(GameObject pot, ARAnchor anchor)
    {
        if (arCamera == null) return true;
        // Walk up from the pot: if the camera or its parent rig is an ancestor, abort.
        Transform camTr = arCamera.transform;
        Transform camParent = camTr.parent; // e.g. XR Origin / Camera Offset
        Transform camRoot = camParent != null ? camParent.parent : null; // e.g. XR Origin

        Transform t = pot.transform.parent;
        while (t != null)
        {
            if (t == camTr || t == camParent || t == camRoot)
            {
                Debug.LogError("[ARPlacement] Pot parented under camera rig — aborting.");
                return false;
            }
            t = t.parent;
        }
        return true;
    }

    private IEnumerator VerifyPlacement(GameObject pot, Vector3 expectedWorldPos)
    {
        yield return new WaitForSeconds(2f);
        if (pot == null) yield break;
        float drift = Vector3.Distance(pot.transform.position, expectedWorldPos);
        if (drift > 0.10f)
            Debug.LogWarning("[ARPlacement] Excessive drift: " + drift.ToString("F3") + "m");
    }

    private void CleanPreExistingPots()
    {
        if (catalog == null) return;
        var roots = UnityEngine.SceneManagement.SceneManager
            .GetActiveScene().GetRootGameObjects();
        foreach (var root in roots)
        {
            if (root == gameObject || _spawned.Contains(root)) continue;
            foreach (var entry in catalog)
            {
                if (entry == null || entry.prefab == null) continue;
                if (root.name == entry.prefab.name
                    && root.GetComponentInChildren<MeshRenderer>())
                {
                    Destroy(root);
                    break;
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  RETICLE (placement preview with stability tracking)
    // ════════════════════════════════════════════════════════════════

    private void CreateReticle()
    {
        _reticle = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
        _reticle.name = "PlacementReticle";
        _reticle.transform.localScale = new Vector3(0.12f, 0.002f, 0.12f);

        var col = _reticle.GetComponent<Collider>();
        if (col) Destroy(col);

        _reticleRenderer = _reticle.GetComponent<Renderer>();
        Material mat = new Material(Shader.Find("Sprites/Default"));
        mat.color = new Color(0.46f, 0.50f, 0.39f, 0.35f);
        _reticleRenderer.material = mat;
        _reticleRenderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
        _reticleRenderer.receiveShadows = false;

        _reticle.SetActive(false);
        _reticleStableTime = 0f;
        _lastReticlePos = Vector3.zero;
    }

    private void UpdateReticle()
    {
        if (!_reticle) return;
        if (_uiCtrl.IsMenuOpen || _deleteMode)
        {
            if (_reticle.activeSelf) _reticle.SetActive(false);
            _reticleStableTime = 0f;
            return;
        }

        Vector2 screenCenter = new Vector2(Screen.width * 0.5f, Screen.height * 0.5f);

        ARPlane bestPlane = null;
        ARRaycastHit bestHit = default;

        if (raycastManager.Raycast(screenCenter, sHits, PlaneFilterTight))
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);
        if (bestPlane == null && raycastManager.Raycast(screenCenter, sHits, PlaneFilterLoose))
            PickBestPlaneHit(sHits, ref bestPlane, ref bestHit);

        if (bestPlane != null)
        {
            _reticle.SetActive(true);
            Vector3 pos = bestHit.pose.position;
            _reticle.transform.position = pos;
            _reticle.transform.rotation = bestHit.pose.rotation;

            // Track stability
            float moved = Vector3.Distance(pos, _lastReticlePos);
            if (moved < ReticleMoveTolerance)
                _reticleStableTime += Time.deltaTime;
            else
                _reticleStableTime = 0f;
            _lastReticlePos = pos;

            // Visual feedback: fade from dim to bright as confidence builds
            float confidence = Mathf.Clamp01(_reticleStableTime / ReticleStableThreshold);
            float alpha = Mathf.Lerp(0.25f, 0.7f, confidence);
            if (_reticleRenderer)
            {
                Color c = _reticleRenderer.material.color;
                c.a = alpha;
                _reticleRenderer.material.color = c;
            }
            return;
        }

        if (_reticle.activeSelf) _reticle.SetActive(false);
        _reticleStableTime = 0f;
    }

    private T GetOrAdd<T>() where T : Component
    {
        var c = GetComponent<T>();
        return c ? c : gameObject.AddComponent<T>();
    }

    // ════════════════════════════════════════════════════════════════
    //  OLD SYSTEM CONFLICT CHECK
    // ════════════════════════════════════════════════════════════════

    private void DisableOldSystem()
    {
        // The old monolithic class was called ARPlacementManager in RaycastScript.cs.
        // If it somehow still exists as a component, disable it to prevent conflicts.
        foreach (var mb in GetComponents<MonoBehaviour>())
        {
            if (mb == null || mb == this) continue;
            string typeName = mb.GetType().Name;
            if (typeName == "ARPlacementManager")
            {
                mb.enabled = false;
                Debug.LogWarning("[ARPlacement] Disabled old ARPlacementManager — " +
                    "the modular system replaces it. Remove it from your scene.");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  STARTUP DIAGNOSTICS
    // ════════════════════════════════════════════════════════════════

    private void LogStartupDiagnostics()
    {
        Debug.Log("══════════════════════════════════════════════════");
        Debug.Log("[ARPlacement] Startup diagnostics:");
        Debug.Log("  Config:        " + (config != null ? "OK" : "MISSING"));
        Debug.Log("  Catalog:       " + (catalog != null ? catalog.Length + " entries" : "NULL"));
        if (catalog != null)
        {
            for (int i = 0; i < catalog.Length; i++)
            {
                if (catalog[i] == null)
                    Debug.LogWarning($"  Catalog[{i}]:   NULL entry");
                else
                    Debug.Log($"  Catalog[{i}]:   {catalog[i].displayName} " +
                        $"prefab={( catalog[i].prefab ? catalog[i].prefab.name : "MISSING")} " +
                        $"sprite={(catalog[i].menuSprite ? "OK" : "MISSING")}");
            }
        }
        Debug.Log("  Camera:        " + (arCamera ? arCamera.name : "MISSING"));
        Debug.Log("  RaycastMgr:    " + (raycastManager ? "OK" : "MISSING"));
        Debug.Log("  PlaneMgr:      " + (planeManager ? "OK" : "MISSING"));
        Debug.Log("  AnchorMgr:     " + (anchorManager ? anchorManager.gameObject.name : "MISSING"));
        Debug.Log("  UI Controller: " + (_uiCtrl ? "OK" : "MISSING"));
        Debug.Log("  Gesture Ctrl:  " + (_gestureCtrl ? "OK" : "MISSING"));
        Debug.Log("  Screenshot:    " + (_screenshotSvc ? "OK" : "MISSING"));
        Debug.Log("══════════════════════════════════════════════════");
    }
}
